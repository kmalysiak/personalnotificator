package pl.kmalysiak.personalnotificator.ws

import android.content.BroadcastReceiver
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import pl.kmalysiak.personalnotificator.model.secure.SecurePrefsService
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var securePrefs: SecurePrefsService

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "Boot receiver onReceive")
        val token = securePrefs.loadIdToken().ifEmpty { return }
        ContextCompat.startForegroundService(
            context,
            Intent(context, WebSocketService::class.java).putExtra("idToken", token)
        )
    }
}
