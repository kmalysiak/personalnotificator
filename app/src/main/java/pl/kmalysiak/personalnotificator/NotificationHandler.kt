package pl.kmalysiak.personalnotificator

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import pl.kmalysiak.personalnotificator.ChannelCreator.CHANNEL_ID
import pl.kmalysiak.personalnotificator.ChannelCreator.MOVE_CHANNEL_ID

object NotificationHandler {

//    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
//    fun showNotificationWithCustomSound(type: String, soundUri: Uri, ctx:Context) {
//        ChannelCreator.createNotificationChannel(ctx)
//        val builder = NotificationCompat.Builder(ctx, CHANNEL_ID)
//            .setSmallIcon(R.drawable.ic_notification)
//            .setContentTitle("New Notification")
//            .setContentText("Type: $type")
//            .setSound(soundUri)
//            .setPriority(NotificationCompat.PRIORITY_HIGH)
//        NotificationManagerCompat.from(ctx).notify(0, builder.build())
//    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun getNotificationWithForTts(type: String, ctx:Context): Notification {
        ChannelCreator.createNotificationChannel(ctx)
        val builder = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("TTS service started")
            //.setContentText("Type: $type")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(null)
       return builder.build()
    }

//    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
//    fun showNotificationMovement(type: String, soundUri: Uri, ctx:Context) {
//        ChannelCreator.createNotificationMovementChannel(ctx)
//        val builder = NotificationCompat.Builder(ctx, MOVE_CHANNEL_ID)
//            .setSmallIcon(R.drawable.ic_notification)
//            .setContentTitle("Wykryto ruch!")
//            .setContentText("Type: $type")
//            .setSound(soundUri)
//            .setPriority(NotificationCompat.PRIORITY_HIGH)
//        NotificationManagerCompat.from(ctx).notify(0, builder.build())
//    }

//    fun buildNotificationForeground(pendingIntent: PendingIntent?, ctx:Context): Notification {
//        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
//            .setSmallIcon(R.drawable.ic_notification)
//            .setContentTitle("Foreground Service")
//            .setContentText("Service is running...")
//            .setContentIntent(pendingIntent)
//            .build()
//        return notification
//    }

//    fun buildNotificationMoveForeground(pendingIntent: PendingIntent?, ctx:Context): Notification {
//        val notification = NotificationCompat.Builder(ctx, MOVE_CHANNEL_ID)
//            .setSmallIcon(R.drawable.ic_notification)
//            .setContentTitle("Foreground Service")
//            .setContentText("Service is running...")
//            .setContentIntent(pendingIntent)
//            .build()
//        return notification
//    }
}