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

//funkcja: ServiceRestartReceiver służy do restartu WebSocketService gdy zostanie zabity przez system.
//  Przepływ:
//  1. WebSocketService.onDestroy() jest wywoływane gdy serwis ginie
//  2. Planuje alarm przez AlarmManager za 2 sekundy
//  3. Alarm odpala ServiceRestartReceiver
//  4. Receiver pobiera token i startuje WebSocketService ponownie
//
//  To "ostatnia deska ratunku" żeby serwis żył jak najdłużej.
@AndroidEntryPoint
class ServiceRestartReceiver : BroadcastReceiver() {

    @Inject lateinit var securePrefs: SecurePrefsService

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "ServiceRestartReceiver receiver onReceived")
        val token = securePrefs.loadIdToken().ifEmpty { return }
        val serviceIntent = Intent(context, WebSocketService::class.java)
            .putExtra("idToken", token)
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
