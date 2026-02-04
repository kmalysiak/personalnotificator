package pl.kmalysiak.personalnotificator;

import android.Manifest
import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.greenrobot.eventbus.EventBus
import pl.kmalysiak.personalnotificator.config.ConfigLoader
import pl.kmalysiak.personalnotificator.model.dao.NotificatorDatabase
import pl.kmalysiak.personalnotificator.model.data.NotificationEntity
import pl.kmalysiak.personalnotificator.model.event.EventWithDateTimeAndStatus
import pl.kmalysiak.personalnotificator.model.event.HeartbeatEvent
import pl.kmalysiak.personalnotificator.preference.HeartbeatDataUtil
import java.io.IOException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import kotlin.math.min

class HeartBeatMonitor : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val checkInterval = 120_000L // 2 min
    private val client = OkHttpClient()
    private lateinit var db: NotificatorDatabase
    private var newState: Boolean = true;
    private val checkRunnable = object : Runnable {
        override fun run() {
            checkServerHealth()
        }
    }
    private val serviceScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO
    )
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(2, createNotification())
        serviceScope.launch(Dispatchers.IO) {
            ConfigLoader.init(this@HeartBeatMonitor)
            db = NotificatorDatabase.getDatabase(applicationContext)
            checkRunnable.run()
        }
        return START_STICKY
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun createNotification(): Notification {
        ChannelCreator.createNotificationTtsChannel(this)
        return NotificationHandler.getNotificationWithForHealthCheck("HC", this)
    }

    private fun checkServerHealth() {
       serviceScope.launch {
            try {
                val request = Request.Builder()
                    .url(ConfigLoader.getConfig().apiUrl + "api/heartbeat")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Log.d("ServerMonitor", "OK. Następne sprawdzenie za: ${checkInterval / 1000}s")
                        newState = true;
                    } else {
                        Log.d("ServerMonitor", "Błąd:${response.code}, Następne sprawdzenie za: ${checkInterval / 1000}s")
                        newState = false;
                    }
                }
            } catch (e: IOException) {
                Log.d("ServerMonitor", "Błąd:${e.message}, Następne sprawdzenie za: ${checkInterval / 1000}s")
                newState = false;
            } finally {
                val now = Instant.now()
                EventBus.getDefault().post(HeartbeatEvent(DateTimeUtil.toWarsawDt(now), newState.toString()))
                val stateChanged = HeartbeatDataUtil.get().updateHeartbeatState(now, if (newState) "OK" else "NOK")

                if (stateChanged) {
                    EventBus.getDefault().post(EventWithDateTimeAndStatus("Healthcheck", LocalDateTime.now(), newState.toString()))
                    val notification = NotificationEntity( //TODO: each db insert requires TTS; unify
                        entFriendlyName = "połączenie",
                        entType = "connection",
                        content = if (newState) "OK" else "NOK",
                        timestamp = Instant.now().epochSecond
                    )
                    db.notificationEntityDao().insert(notification)
                    db.notificationEntityDao().keepLatest500()
                }


                handler.postDelayed(checkRunnable, checkInterval)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(checkRunnable)
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }
}