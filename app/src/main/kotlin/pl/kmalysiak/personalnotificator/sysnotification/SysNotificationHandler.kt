package pl.kmalysiak.personalnotificator.sysnotification

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import pl.kmalysiak.personalnotificator.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SysNotificationHandler @Inject constructor(@ApplicationContext private val context: Context) {

    private val channel = "foreground_service"

    init {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(
            NotificationChannel(channel, "Usługi działające w tle", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "WebSocket i TTS" }
        )
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun getNotificationForTtsStart(): Notification {
        return NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Powiadomienie TTS uruchomione.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(null)
            .build()
    }


    fun getNotificationForWsStart(status: String): Notification {
        return NotificationCompat.Builder(context, channel)
            .setContentTitle("Połączenie WS uruchomione")
            .setContentText(status)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setSilent(true)
            .build()
    }
}