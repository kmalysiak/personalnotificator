package pl.kmalysiak.personalnotificator.ws

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.SystemClock
import androidx.annotation.RequiresPermission
import org.slf4j.LoggerFactory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.greenrobot.eventbus.EventBus
import pl.kmalysiak.personalnotificator.DateTimeUtil
import pl.kmalysiak.personalnotificator.NotificationForegroundService
import pl.kmalysiak.personalnotificator.NotificationHandler
import pl.kmalysiak.personalnotificator.auth.BackendAuthService
import pl.kmalysiak.personalnotificator.config.ConfigRepository
import pl.kmalysiak.personalnotificator.model.event.HeartbeatEvent
import pl.kmalysiak.personalnotificator.model.event.WsStateEvent
import pl.kmalysiak.personalnotificator.model.secure.SecurePrefsService
import java.time.Instant
import javax.inject.Inject

@AndroidEntryPoint
@SuppressLint("MissingPermission")
class WebSocketService : Service() {

    private val log = LoggerFactory.getLogger(WebSocketService::class.java)

    @Inject lateinit var securePrefs: SecurePrefsService
    @Inject lateinit var notificationHandler: NotificationHandler
    @Inject lateinit var configRepository: ConfigRepository
    @Inject lateinit var backendAuthService: BackendAuthService

    private lateinit var wsManager: WebSocketManager
    private val NOTIF_ID = 4
    private val job = SupervisorJob()
    private val serviceScope = CoroutineScope(job + Dispatchers.IO)
    private var startJob: kotlinx.coroutines.Job? = null


    override fun onCreate() {
        super.onCreate()
        val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING
        else
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        try {
            startForeground(NOTIF_ID, notificationHandler.getNotificationForWsStart("Łączenie..."), fgsType)
        } catch (e: Exception) {
            log.error("startForeground failed: {}", e.message)
        }

        wsManager = WebSocketManager(
            context = this,
            serverUrl = configRepository.currentConfig.wsApiUrl + "ws/notifications",
            securePrefs = securePrefs,
            backendAuthService = backendAuthService,
            onMessage = { msg -> handlePushMessage(msg) },
            onStateChange = { state -> updateNotification(state) },
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // START_STICKY: Android restartuje service, intent może być null
        // Guard: nie startuj ponownie jeśli poprzedni start jest jeszcze w toku
        if (startJob?.isActive == true) {
            log.warn("onStartCommand called but start already in progress — ignoring")
            return START_STICKY
        }
        startJob = serviceScope.launch {
            backendAuthService.authenticateAndSendCredHome()
            wsManager.start(securePrefs.loadIdToken(), securePrefs.loadFcmToken())
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Samsung / niektóre ROM-y ubijają service gdy użytkownik wymiata z recent apps
        val restartIntent = Intent(applicationContext, WebSocketService::class.java)
        val pendingIntent = PendingIntent.getService(
            this, 1, restartIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000, pendingIntent)
        super.onTaskRemoved(rootIntent)
    }

    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    override fun onDestroy() {
        job.cancel()
        wsManager.stop()
        scheduleRestart()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null

    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    private fun scheduleRestart() {
        val restartIntent = Intent(applicationContext, ServiceRestartReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 2, restartIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + 2000,
            pendingIntent
        )
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun handlePushMessage(msg: String) {
        log.debug("Push: {}", msg)
        when {
            msg.contains("auth_error")          -> handleAuthError()
            msg.contains("auth_ok")             -> handleAuthOk()
            msg.contains("entityFriendlyName")  -> handleNotification(msg)
            msg.contains("timestamp")           -> handleHeartbeat(msg)
            else                                -> log.warn("Unknown WS message: {}", msg)
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun handleAuthOk() {
        startNotificationService("websocket", "websocket", "usługa", "auth ok", Instant.now().epochSecond)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun handleAuthError() {
        startNotificationService("websocket", "websocket", "usługa", "auth nok", Instant.now().epochSecond)
        wsManager.stop()
        serviceScope.launch {
            backendAuthService.authenticateAndSendCredHome()
            wsManager.start(securePrefs.loadIdToken(), securePrefs.loadFcmToken())
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun handleHeartbeat(msg: String) {
        val obj = Json.parseToJsonElement(msg).jsonObject
        val timestamp = obj["timestamp"]?.jsonPrimitive?.long ?: Instant.now().epochSecond
        startNotificationService("websocket", "websocket", "websocket", DateTimeUtil.dateFormatterHrMinSec.format(DateTimeUtil.toWarsawDt(timestamp)), Instant.now().epochSecond)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun handleNotification(msg: String) {
        log.info("WS notification: {}", msg)
        val obj = runCatching { Json.parseToJsonElement(msg).jsonObject }.getOrNull()
        val entityFriendlyName = obj?.get("entityFriendlyName")?.jsonPrimitive?.content ?: "websocket"
        val type               = obj?.get("type")?.jsonPrimitive?.content ?: ""
        val content            = obj?.get("msg")?.jsonPrimitive?.content ?: ""
        val timestamp          = obj?.get("timestamp")?.jsonPrimitive?.content?.toLongOrNull() ?: Instant.now().epochSecond

        if (type == "ws_ping") {
            EventBus.getDefault().post(HeartbeatEvent(DateTimeUtil.toWarsawDt(timestamp), "ws_ping"))
            return
        }

        startNotificationService(entityFriendlyName, entityFriendlyName, type, content, timestamp)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun startNotificationService(source: String, entFriendlyName: String, entType: String, content: String, timestamp: Long) {
        val intent = Intent(this, NotificationForegroundService::class.java).apply {
            putExtra("source", source)
            putExtra("entFriendlyName", entFriendlyName)
            putExtra("entType", entType)
            putExtra("notificationContent", content)
            putExtra("notificationTimestamp", timestamp)
        }
        startForegroundService(intent)
    }

    private fun updateNotification(state: WebSocketManager.State) {
        val text = when (state) {
            WebSocketManager.State.CONNECTED    -> "Połączono WS"
            WebSocketManager.State.CONNECTING   -> "Łączenie WS"
            WebSocketManager.State.RECONNECTING -> "Ponowne łączenie WS"
            WebSocketManager.State.RECONNECTING_REAUTH -> "Ponowne łączenie WS po zalogowaniu"
            WebSocketManager.State.STOPPED      -> "Zatrzymano WS"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIF_ID, notificationHandler.getNotificationForWsStart(text))
        EventBus.getDefault().postSticky(WsStateEvent(state))
    }
}
