package pl.kmalysiak.personalnotificator

import android.Manifest
import android.content.ContentValues.TAG
import android.content.Intent
import android.util.Log
import androidx.annotation.RequiresPermission
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.greenrobot.eventbus.EventBus
import pl.kmalysiak.personalnotificator.model.event.TokenEvent


//when a message arrives, Android wakes calls onMessageReceived(...).
//This works even if: app is not running and screen is off.
//or app was killed (for notification messages only — not guaranteed for data-only).
//For notification messages, Android can handle them without starting  service at all.
//For data-only messages, FirebaseMessagingService will be started only if
// the app is running or allowed in the background — and only for high-priority messages.
//https://firebase.google.com/docs/cloud-messaging/concept-options#notifications_and_data_messages
//https://firebase.google.com/docs/cloud-messaging/concept-options#setting-the-priority-of-a-message
//https://developer.android.com/about/versions/oreo/background
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
        if (remoteMessage.data.isEmpty()) return

        val entFriendlyName = remoteMessage.data["entityFriendlyName"] ?: return
        val type = remoteMessage.data["type"] ?: "nieznany"
        val msg = remoteMessage.data["msg"] ?: ""
        val timestamp =  remoteMessage.data["timestamp"]?.toLongOrNull() ?: return


        Log.i(TAG, "Starting TTS for msg type:$entFriendlyName $msg")
        val intent = Intent(this, NotificationForegroundService::class.java).apply {
            putExtra("source", "onMessageReceived")
            putExtra("entFriendlyName", entFriendlyName)
            putExtra("entType", type)
            putExtra("notificationContent", msg)
            putExtra("notificationTimestamp", timestamp)
        }
        //usage of heavy work should not be done directly at onMessageReceiced as system
        //might kill it silently; better start foreground service
        startForegroundService(intent)

        return

    }

}