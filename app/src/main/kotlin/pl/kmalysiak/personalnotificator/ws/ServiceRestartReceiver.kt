package pl.kmalysiak.personalnotificator.ws

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import org.slf4j.LoggerFactory
import dagger.hilt.android.AndroidEntryPoint
import pl.kmalysiak.personalnotificator.appstate.SecurePrefsService
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

    private val log = LoggerFactory.getLogger(ServiceRestartReceiver::class.java)

    @Inject lateinit var securePrefs: SecurePrefsService

    override fun onReceive(context: Context, intent: Intent) {
        log.info("ServiceRestartReceiver receiver onReceived")
        val token = securePrefs.loadIdToken().ifEmpty { return }
        val serviceIntent = Intent(context, WebSocketService::class.java)
            .putExtra("idToken", token)
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
