package pl.kmalysiak.personalnotificator

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import androidx.core.net.toUri
import com.google.android.datatransport.runtime.scheduling.persistence.EventStoreModule_PackageNameFactory.packageName


object ChannelCreator {
    const val CHANNEL_ID = "my_notification_channel"
    const val MOVE_CHANNEL_ID = "move_channel_id"
    const val TTS_CHANNEL_ID = "tts_channel_id"
    fun createNotificationChannel(context: Context) {
        val channelName = "My Notifications"
        val channelDescription = "Channel for important notifications"
        val channel = NotificationChannel(
            CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = channelDescription
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    fun createNotificationMovementChannel(context: Context) {
        val channelName = "MovementChannel"
        val channelDescription = "Channel for Movement"
        val soundUri = "android.resource://${packageName(context)}/raw/move".toUri()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .build()

        val channel = NotificationChannel(
            MOVE_CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = channelDescription
            setSound(soundUri, audioAttributes)

        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }


//     fun createNotification(): Notification {
//
//        ChannelCreator.createNotificationMovementChannel()
//
//        val channelId = "tts_channel"
//        val channel = NotificationChannel(
//            channelId, "TTS Service", NotificationManager.IMPORTANCE_LOW // Set LOW to minimize visibility
//        )
//        val manager = getSystemService(NotificationManager::class.kotlin)
//        manager.createNotificationChannel(channel)
//
//        return NotificationCompat.Builder(this, channelId)
//            .setContentTitle("Processing Sound")
//            .setContentText(" ")
//            .setSmallIcon(R.drawable.ic_notification)
//            .setPriority(NotificationCompat.PRIORITY_MIN) // Hides notification from lock screen
//            .setSilent(true) // No vibration or sound from this notification
//            .build()
//    }


//    return NotificationCompat.Builder(this, channelId)
//    .setContentTitle("Processing Sound")
//    .setContentText(" ")
//    .setSmallIcon(R.drawable.ic_notification)
//    .setPriority(NotificationCompat.PRIORITY_MIN) // Hides notification from lock screen
//    .setSilent(true) // No vibration or sound from this notification
//    .build()


    fun createNotificationTtsChannel(context: Context) {
        val channelName = "TTS_CHANNEL"
        val channelDescription = "Channel with TTS"

        val channel = NotificationChannel(
            MOVE_CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = channelDescription
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

}

