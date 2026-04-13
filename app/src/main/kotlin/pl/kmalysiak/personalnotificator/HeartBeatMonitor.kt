package pl.kmalysiak.personalnotificator

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
import dagger.hilt.android.AndroidEntryPoint
import pl.kmalysiak.personalnotificator.config.AppConfig
import pl.kmalysiak.personalnotificator.model.event.EventWithDateTimeAndStatus
import pl.kmalysiak.personalnotificator.model.event.HeartbeatEvent
import pl.kmalysiak.personalnotificator.preference.HeartbeatDataUtil
import java.io.IOException
import java.time.Instant
import java.time.LocalDateTime
import javax.inject.Inject

@AndroidEntryPoint
class HeartBeatMonitor : Service() {

    @Inject lateinit var heartbeatDataUtil: HeartbeatDataUtil
    @Inject lateinit var notificationHandler: NotificationHandler
    @Inject lateinit var appConfig: AppConfig

    private val handler = Handler(Looper.getMainLooper())
    private val checkInterval = 120_000L // 2 min
    private val client = OkHttpClient()
    private var newState: Boolean = true
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
            checkRunnable.run()
        }
        return START_STICKY
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun createNotification(): Notification {
        return notificationHandler.getNotificationForRestHealthCheckStart("HC")
    }

    private fun checkServerHealth() {
        serviceScope.launch {
            try {
                val request = Request.Builder()
                    .url("https://" + appConfig.apiUrl + "api/heartbeat")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Log.d("ServerMonitor", "OK. Następne sprawdzenie za: ${checkInterval / 1000}s")
                        newState = true
                    } else {
                        Log.d("ServerMonitor", "Błąd:${response.code}, Następne sprawdzenie za: ${checkInterval / 1000}s")
                        newState = false
                    }
                }
            } catch (e: IOException) {
                Log.d("ServerMonitor", "Błąd:${e.message}, Następne sprawdzenie za: ${checkInterval / 1000}s")
                newState = false
            } finally {
                val now = Instant.now()
                EventBus.getDefault().post(HeartbeatEvent(DateTimeUtil.toWarsawDt(now), newState.toString()))
                val stateChanged = heartbeatDataUtil.updateHeartbeatState(now, if (newState) "OK" else "NOK")

                if (stateChanged) {
                    EventBus.getDefault().post(EventWithDateTimeAndStatus("Healthcheck", LocalDateTime.now(), newState.toString()))


                    val intent = Intent(this@HeartBeatMonitor, NotificationForegroundService::class.java).apply {
                        putExtra("source", "onMessageReceived")
                        putExtra("entFriendlyName", "połączenie")
                        putExtra("entType", "połączenie")
                        putExtra("notificationContent", if (newState) "ustanowione" else "utracone")
                        putExtra("notificationTimestamp", DateTimeUtil.nowUtc())
                    }
                    startForegroundService(intent)

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