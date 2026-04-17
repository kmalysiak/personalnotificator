package pl.kmalysiak.personalnotificator.ws

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import org.slf4j.LoggerFactory
import dagger.hilt.android.AndroidEntryPoint
import pl.kmalysiak.personalnotificator.model.secure.SecurePrefsService
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    private val log = LoggerFactory.getLogger(BootReceiver::class.java)

    @Inject lateinit var securePrefs: SecurePrefsService

    override fun onReceive(context: Context, intent: Intent) {
        log.info("Boot receiver onReceive")
        val token = securePrefs.loadIdToken().ifEmpty { return }
        ContextCompat.startForegroundService(
            context,
            Intent(context, WebSocketService::class.java).putExtra("idToken", token)
        )
    }
}
