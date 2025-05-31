package pl.kmalysiak.personalnotificator

import android.Manifest
import android.content.ContentValues.TAG
import android.content.Intent
import android.util.Log
import androidx.annotation.RequiresPermission
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.greenrobot.eventbus.EventBus
import pl.kmalysiak.personalnotificator.events.TokenEvent


class AppFirebaseMessagingService : FirebaseMessagingService() {
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "CREATED")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "DESTROYED")
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: $token")
//        val sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
//        sharedPreferences.edit { putString("FCM_TOKEN", token) }
        EventBus.getDefault().post(TokenEvent(token))
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.i(TAG, "Msg received")
        //val soundUri = getSoundUri("close")
        //  NotificationHandler.showNotification("close",  getSoundUri("close"), this)
        if (remoteMessage.data.isEmpty()) return

        val type = remoteMessage.data["type"] ?: return
        Log.i(TAG, "Starting TTS for msg type:" + type)
        val intent = Intent(this, TtsForegroundService::class.java).apply {
            putExtra("notificationText", type)
        }
        startForegroundService(intent)

        return

    }

}