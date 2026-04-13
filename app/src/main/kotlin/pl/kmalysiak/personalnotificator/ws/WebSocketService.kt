package pl.kmalysiak.personalnotificator.ws

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import androidx.annotation.RequiresPermission
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import pl.kmalysiak.personalnotificator.DateTimeUtil
import pl.kmalysiak.personalnotificator.NotificationHandler
import pl.kmalysiak.personalnotificator.config.ConfigLoader
import pl.kmalysiak.personalnotificator.model.dao.NotificationEntityDao
import pl.kmalysiak.personalnotificator.model.data.NotificationEntity
import pl.kmalysiak.personalnotificator.model.event.EventWithDateTimeAndStatus
import pl.kmalysiak.personalnotificator.model.secure.SecurePrefsService
import java.time.Instant
import javax.inject.Inject

@AndroidEntryPoint
class WebSocketService : Service() {

    @Inject lateinit var securePrefs: SecurePrefsService
    @Inject lateinit var notificationHandler: NotificationHandler
    @Inject lateinit var dao: NotificationEntityDao
    private lateinit var wsManager: WebSocketManager
    private val NOTIF_ID = 4
    private val job = SupervisorJob()
    private val serviceScope = CoroutineScope(job + Dispatchers.IO)



    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIF_ID, notificationHandler.getNotificationForWsStart("Łączenie..."))

        wsManager = WebSocketManager(
            context = this,
            serverUrl = "wss://" + ConfigLoader.getConfig().apiUrl + "ws/notifications",
            securePrefs = securePrefs,
            onMessage = { msg -> handlePushMessage(msg) },
            onStateChange = { state -> updateNotification(state) }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // START_STICKY: Android restartuje service, intent może być null
        val token = intent?.getStringExtra("idToken") ?: securePrefs.loadIdToken()
        wsManager.start(token)
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Samsung / niektóre ROM-y ubijają service gdy użytkownik wymiata z recent apps
        // Restartuj natychmiast
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
        // Ostatnia deska ratunku — zaplanuj restart przez AlarmManager
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

    private fun handlePushMessage(msg: String) {
        Log.d("WSService", "Push: $msg")
        val entity = NotificationEntity(
            entFriendlyName = "ws_heartbeat",
            entType = "ws_heartbeat",
            content = "ws_heartbeat",
            timestamp = Instant.now().epochSecond,
            timestampReceived = Instant.now().epochSecond
        )
        serviceScope.launch {
            dao.insert(entity)
            dao.keepLatest500()
            EventBus.getDefault().post(EventWithDateTimeAndStatus("Event", DateTimeUtil.toWarsawDt(entity.timestamp), "ws_heartbeat"))
        }
    }

    private fun updateNotification(state: WebSocketManager.State) {
        val text = when (state) {
            WebSocketManager.State.CONNECTED -> "Połączono WS"
            WebSocketManager.State.CONNECTING -> "Łączenie WS..."
            WebSocketManager.State.RECONNECTING -> "Ponowne łączenie WS..."
            WebSocketManager.State.STOPPED -> "Zatrzymano WS"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIF_ID, notificationHandler.getNotificationForWsStart(text))
    }

//    private fun createNotificationChannel() {
//        val channel = NotificationChannel(
//            CHANNEL_ID,
//            "Połączenie z serwerem",
//            NotificationManager.IMPORTANCE_LOW  // LOW = brak dźwięku, ale nie można ukryć (to ważne!)
//        ).apply {
//            setShowBadge(false)
//            lockscreenVisibility = Notification.VISIBILITY_SECRET
//        }
//        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
//    }
//
//    private fun buildNotification(status: String) = NotificationCompat.Builder(this, CHANNEL_ID)
//        .setContentTitle("Personal Notificator")
//        .setContentText(status)
//        .setSmallIcon(R.drawable.ic_notification)
//        .setOngoing(true)
//        .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
//        .setSilent(true)
//        .build()
}