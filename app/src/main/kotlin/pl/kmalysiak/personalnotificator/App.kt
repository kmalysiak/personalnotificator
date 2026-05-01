package pl.kmalysiak.personalnotificator

import android.app.Application
import android.app.ForegroundServiceStartNotAllowedException
import android.content.Intent
import android.os.Build
import android.util.Log
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import dagger.hilt.android.HiltAndroidApp
import org.slf4j.LoggerFactory
import pl.kmalysiak.personalnotificator.appstate.SecurePrefsService
import pl.kmalysiak.personalnotificator.ws.WebSocketService
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {

    @Inject lateinit var securePrefs: SecurePrefsService

    override fun onCreate() {
        initLogback()
        super.onCreate()
        tryStartWebSocketService()
    }

    private fun initLogback() {
        val lc = LoggerFactory.getILoggerFactory() as LoggerContext
        lc.reset()
        lc.putProperty("LOG_DIR", filesDir.absolutePath)
        val config = JoranConfigurator()
        config.context = lc
        config.doConfigure(assets.open("logback.xml"))
    }

    fun tryStartWebSocketService() {
        val token = securePrefs.loadIdToken().ifEmpty { return }
        val intent = Intent(this, WebSocketService::class.java).putExtra("idToken", token)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                startForegroundService(intent)
            } catch (e: ForegroundServiceStartNotAllowedException) {
                Log.w("App", "Cannot start foreground service from background, will retry from foreground")
            }
        } else {
            startForegroundService(intent)
        }
    }
}
