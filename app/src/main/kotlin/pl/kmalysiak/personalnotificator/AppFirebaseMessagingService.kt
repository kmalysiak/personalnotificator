package pl.kmalysiak.personalnotificator

import android.Manifest
import android.content.Intent
import androidx.annotation.RequiresPermission
import org.slf4j.LoggerFactory
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import pl.kmalysiak.personalnotificator.auth.BackendAuthService
import javax.inject.Inject


//when a message arrives, Android wakes calls onMessageReceived(...).
//This works even if: app is not running and screen is off.
//or app was killed (for notification messages only — not guaranteed for data-only).
//For notification messages, Android can handle them without starting  service at all.
//For data-only messages, FirebaseMessagingService will be started only if
// the app is running or allowed in the background — and only for high-priority messages.
//https://firebase.google.com/docs/cloud-messaging/concept-options#notifications_and_data_messages
//https://firebase.google.com/docs/cloud-messaging/concept-options#setting-the-priority-of-a-message
//https://developer.android.com/about/versions/oreo/background
@AndroidEntryPoint
class AppFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var backendAuthService: BackendAuthService
    @Inject lateinit var appStateRepo: AppStateRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val log = LoggerFactory.getLogger(AppFirebaseMessagingService::class.java)

    override fun onCreate() {
        super.onCreate()
        log.debug("CREATED")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        log.debug("DESTROYED")
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        log.debug("Refreshed token: {}", token)
        appStateRepo.setFcmToken(token)
        serviceScope.launch {
            val result = backendAuthService.authenticateAndSendCredHome()
            if (result.isRegister) {
                log.debug("Token re-registered with backend successfully")
            } else {
                log.error("Failed to re-register token with backend: {}", result.errorDesc)
            }
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        log.info("Msg received")
        if (remoteMessage.data.isEmpty()) return

        val entFriendlyName = remoteMessage.data["entityFriendlyName"] ?: return
        val type = remoteMessage.data["type"] ?: "nieznany"
        val msg = remoteMessage.data["msg"] ?: ""
        val timestamp =  remoteMessage.data["timestamp"]?.toLongOrNull() ?: return


        log.info("Starting TTS for msg type: {} {}", entFriendlyName, msg)
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