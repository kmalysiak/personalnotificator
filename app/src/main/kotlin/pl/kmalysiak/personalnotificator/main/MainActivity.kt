package pl.kmalysiak.personalnotificator.main

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import pl.kmalysiak.personalnotificator.DateTimeUtil
import pl.kmalysiak.personalnotificator.HeartBeatMonitor
import pl.kmalysiak.personalnotificator.NotificationForegroundService
import pl.kmalysiak.personalnotificator.model.event.PermEvent
import pl.kmalysiak.personalnotificator.model.event.TokenEvent
import java.time.LocalDateTime

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true

        val that = this
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)

        } else {
            lifecycleScope.launch {
                delay(2000)
                notifyOnPermission(context = that)
            }
        }


        startForegroundService(Intent(this, NotificationForegroundService::class.java))
      //  startForegroundService(Intent(this, HeartBeatMonitor::class.java))

        setContent {
            val viewModel: AppViewModel = viewModel()
            MainView(viewModel = viewModel)
        }

        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(ContentValues.TAG, "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }
            val tokenT = task.result
            Log.d(
                ContentValues.TAG,
                "Fetching FCM registration token completed, sending token to bus"
            )
            EventBus.getDefault().post(TokenEvent(tokenT))

        })
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    @Preview(showBackground = true)
    @Composable
    fun PreviewMyCustomView() {
        MainView()
    }

    fun notifyOnPermission(context: Context) {
        val requiredPermissions = setOf(
            Triple<Int, String, String>(
                Build.VERSION_CODES.TIRAMISU,
                "Foreground Service",
                "android.permission.FOREGROUND_SERVICE"
            ),
            Triple<Int, String, String>(
                Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
                "Media Playback",
                "android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK"
            ),
            Triple<Int, String, String>(
                Build.VERSION_CODES.TIRAMISU,
                "Internet",
                "android.permission.INTERNET"
            ),
            Triple<Int, String, String>(
                Build.VERSION_CODES.TIRAMISU,
                "Notifications",
                "android.permission.POST_NOTIFICATIONS"
            ),
        )
        val apiLevel = Build.VERSION.SDK_INT
        Log.d("permissionCheck API_LEVEL", "Current API Level: $apiLevel")
        requiredPermissions.forEach { permission ->
            if (Build.VERSION.SDK_INT >= permission.first) {
                val permGranted = (ContextCompat.checkSelfPermission(
                    context,
                    permission.third
                ) == PackageManager.PERMISSION_GRANTED).toString()
                Log.d(
                    ContentValues.TAG,
                    "permissionCheck p:${permission.second}, p:${permission.third}, st:$permGranted"
                )
                EventBus.getDefault().postSticky(
                    PermEvent(
                        permission.second,
                        DateTimeUtil.nowToWarsawDt(),
                        permGranted
                    )
                )
            }
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("PERMISSION", "Notification permission granted")
            notifyOnPermission(context = this)
            // Proceed with showing notifications or initializing FCM
        } else {
            Log.d("PERMISSION", "Notification permission denied")
            // Optionally show rationale or fallback
        }
    }


}