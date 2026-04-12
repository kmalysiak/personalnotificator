package pl.kmalysiak.personalnotificator

import android.app.Application
import android.content.Intent
import dagger.hilt.android.HiltAndroidApp
import pl.kmalysiak.personalnotificator.model.secure.SecurePrefsService
import pl.kmalysiak.personalnotificator.ws.WebSocketService
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {

    @Inject lateinit var securePrefs: SecurePrefsService

    override fun onCreate() {
        super.onCreate()
        tryStartWebSocketService()
    }

    private fun tryStartWebSocketService() {
        val token = securePrefs.loadIdToken().ifEmpty { return }
        startForegroundService(Intent(this, WebSocketService::class.java).putExtra("idToken", token))
    }
}
