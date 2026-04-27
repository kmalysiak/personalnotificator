package pl.kmalysiak.personalnotificator.preference

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesDataUtil @Inject constructor(@ApplicationContext context: Context) {

    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    private val PREFIX = "tts_enabled_"

    private fun key(name: String) = PREFIX + name
    private fun strip(key: String) = key.removePrefix(PREFIX)

    fun updatePreferences(names: List<String>, defaultValue: Boolean = true) {
        prefs.edit {
            for (storedKey in prefs.all.keys) {
                if (storedKey.startsWith(PREFIX)) {
                    val uiName = strip(storedKey)
                    if (uiName !in names) remove(storedKey)
                }
            }
            for (name in names) {
                val k = key(name)
                if (!prefs.contains(k)) putBoolean(k, defaultValue)
            }
        }
    }

    fun getByName(name: String): Boolean = prefs.getBoolean(key(name), false)

    fun setPreference(name: String, enabled: Boolean) {
        prefs.edit { putBoolean(key(name), enabled) }
    }

    fun getAll(): Map<String, Boolean> = prefs.all
        .filterKeys { it.startsWith(PREFIX) }
        .mapKeys { strip(it.key) }
        .mapValues { it.value as Boolean }

    fun getForDisplay(): List<UiPref> = getAll()
        .map { UiPref(it.key, it.value) }
        .sortedBy { it.name }

    fun saveTtsVolume(volume: Float) = prefs.edit { putFloat("tts_volume", volume) }
    fun loadTtsVolume(): Float = prefs.getFloat("tts_volume", 1.0f)

    fun saveTtsReady(ready: Boolean) = prefs.edit { putBoolean("tts_ready", ready) }
    fun loadTtsReady(): Boolean? = if (prefs.contains("tts_ready")) prefs.getBoolean("tts_ready", false) else null
}

data class UiPref(val name: String, val enabled: Boolean)
