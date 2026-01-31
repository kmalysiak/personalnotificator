package pl.kmalysiak.personalnotificator

import android.app.Application
import pl.kmalysiak.personalnotificator.preference.PreferencesUtil

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        PreferencesUtil.init(this)
    }

    companion object {
    }
}