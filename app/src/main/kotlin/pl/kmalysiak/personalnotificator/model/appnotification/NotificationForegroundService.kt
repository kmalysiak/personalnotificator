package pl.kmalysiak.personalnotificator.model.appnotification

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresPermission
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import pl.kmalysiak.personalnotificator.appstate.AppStateRepository
import pl.kmalysiak.personalnotificator.model.dao.NotificationRepository
import pl.kmalysiak.personalnotificator.model.dao.NotificationEntity
import pl.kmalysiak.personalnotificator.appstate.PreferencesDataUtil
import pl.kmalysiak.personalnotificator.sysnotification.SysNotificationHandler
import java.time.Instant
import javax.inject.Inject

@AndroidEntryPoint
class NotificationForegroundService : Service() {

    private val log = LoggerFactory.getLogger(NotificationForegroundService::class.java)

    @Inject
    lateinit var sysNotificationHandler: SysNotificationHandler
    @Inject
    lateinit var prefsDataUtil: PreferencesDataUtil
    @Inject
    lateinit var notificationRepository: NotificationRepository
    @Inject
    lateinit var appStateRepo: AppStateRepository

    private lateinit var ttsPlayer: TtsPlayer
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onCreate() {
        log.info("NotificationForegroundService, onCreate")
        super.onCreate()
        val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
        try {
            startForeground(1, sysNotificationHandler.getNotificationForTtsStart(), fgsType)
        } catch (e: Exception) {
            startForeground(1, sysNotificationHandler.getNotificationForTtsStart())
        }
        ttsPlayer = TtsPlayer(this, prefsDataUtil, appStateRepo)
        ttsPlayer.initialize()
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val safeIntent = intent?.takeIf { !it.getStringExtra("source").isNullOrBlank() }
            ?: return START_NOT_STICKY
        serviceScope.launch {
            val notification = NotificationEntity(
                entFriendlyName = safeIntent.getStringExtra("entFriendlyName") ?: "nieznany",
                entType = safeIntent.getStringExtra("entType") ?: "nieznany",
                content = safeIntent.getStringExtra("notificationContent") ?: "",
                timestamp = safeIntent.getLongExtra("notificationTimestamp", Instant.now().epochSecond),
                timestampReceived = Instant.now().epochSecond
            )
            val isNew = notificationRepository.save(notification)
            if (!isNew) {
                log.info("Duplikat powiadomienia, pomijam TTS: {}", notification.entFriendlyName)
            } else if (prefsDataUtil.getByName(notification.entType)) {
                ttsPlayer.enqueue("${notification.entFriendlyName}, ${notification.content}")
            } else {
                log.info("TTS dla powiadomienia:{} jest wyłączone w preferencjach", notification.entFriendlyName)
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        ttsPlayer.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}