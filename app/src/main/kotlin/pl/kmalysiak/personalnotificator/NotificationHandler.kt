package pl.kmalysiak.personalnotificator

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHandler @Inject constructor(@ApplicationContext private val context: Context) {

//    private val TTS_CHANNEL_ID = "1"
//    private val HEALT_CHECK_CHANNEL_ID = "2"
//    private val WS_CHANNEL_ID = "3"

    private val FOREGROUND_SERVICES_CHANNEL_ID = "4";
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun getNotificationForTtsStart(type: String): Notification {
        createForegroundNotificationChannel()
        return NotificationCompat.Builder(context, FOREGROUND_SERVICES_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Powiadomienie TTS uruchomione.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(null)
            .build()
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun getNotificationForRestHealthCheckStart(type: String): Notification {
        createForegroundNotificationChannel()
        return NotificationCompat.Builder(context, FOREGROUND_SERVICES_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Badanie stanu serwera uruchomione.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(null)
            .build()
    }

    fun getNotificationForWsStart(status: String): Notification {
        createForegroundNotificationChannel()
        return NotificationCompat.Builder(context, FOREGROUND_SERVICES_CHANNEL_ID)
            .setContentTitle("Personal Notificator")
            .setContentText(status)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setSilent(true)
            .build()
    }

//    fun createTtsNotificationChannel() {
//        val channelName = "PersonalNotificator TTS channel"
//        val channelDescription = "Channel for important TTS personal notifications"
//        val channel = NotificationChannel(
//            WS_CHANNEL_ID,
//            channelName,
//            NotificationManager.IMPORTANCE_HIGH
//        ).apply {
//            description = channelDescription
//        }
//        val manager = context.getSystemService(NotificationManager::class.java)
//        manager?.createNotificationChannel(channel)
//    }

    fun createForegroundNotificationChannel() {
        val channelName = "PersonalNotificator channel"
        val channelDescription = "Channel for important personal notifications"
        val channel = NotificationChannel(
            FOREGROUND_SERVICES_CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = channelDescription
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

//    fun createRestHealthCheckNotificationChannel() {
//        val channelName = "PersonalNotificator rest health check channel"
//        val channelDescription = "Channel for important REST health check personal notifications"
//        val channel = NotificationChannel(
//            HEALT_CHECK_CHANNEL_ID,
//            channelName,
//            NotificationManager.IMPORTANCE_HIGH
//        ).apply {
//            description = channelDescription
//        }
//        val manager = context.getSystemService(NotificationManager::class.java)
//        manager?.createNotificationChannel(channel)
//    }

//    fun createWsNotificationHealthCheckChannel() {
//        val channelName = "PersonalNotificator WS health check channel"
//        val channelDescription = "Channel for important WS health check personal notifications"
//        val channel = NotificationChannel(
//            WS_CHANNEL_ID,
//            channelName,
//            NotificationManager.IMPORTANCE_HIGH
//        ).apply {
//            description = channelDescription
//        }
//        val manager = context.getSystemService(NotificationManager::class.java)
//        manager?.createNotificationChannel(channel)
//    }
}


