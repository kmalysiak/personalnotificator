package pl.kmalysiak.personalnotificator

import android.app.Application
import android.content.Intent
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import dagger.hilt.android.HiltAndroidApp
import org.slf4j.LoggerFactory
import pl.kmalysiak.personalnotificator.model.secure.SecurePrefsService
import pl.kmalysiak.personalnotificator.ws.WebSocketService
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {

    @Inject lateinit var securePrefs: SecurePrefsService

    override fun onCreate() {
        super.onCreate()
        initLogback()
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

    private fun tryStartWebSocketService() {
        val token = securePrefs.loadIdToken().ifEmpty { return }
        startForegroundService(Intent(this, WebSocketService::class.java).putExtra("idToken", token))
    }
}
