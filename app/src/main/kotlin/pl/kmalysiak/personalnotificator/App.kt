package pl.kmalysiak.personalnotificator

import android.app.Application
import pl.kmalysiak.personalnotificator.preference.HeartbeatDataUtil
import pl.kmalysiak.personalnotificator.preference.PreferencesDataUtil

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        PreferencesDataUtil.init(this)
        HeartbeatDataUtil.init(this)
    }

    companion object {
    }
}