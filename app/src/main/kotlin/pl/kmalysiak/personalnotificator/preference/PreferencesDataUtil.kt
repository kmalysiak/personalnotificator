package pl.kmalysiak.personalnotificator.preference

import android.content.Context
import androidx.core.content.edit


class PreferencesDataUtil private constructor(context: Context) {

    private val prefs =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    // internal prefix for keys
    private val PREFIX = "tts_enabled_"

    private fun key(name: String) = PREFIX + name
    private fun strip(key: String) = key.removePrefix(PREFIX)

    companion object {
        private lateinit var INSTANCE: PreferencesDataUtil

        /** Call once from Application.onCreate() */
        fun init(context: Context) {
            if (!::INSTANCE.isInitialized) {
                INSTANCE = PreferencesDataUtil(context.applicationContext)
            }
        }

        /** Access singleton anywhere */
        fun get(): PreferencesDataUtil {
            if (!::INSTANCE.isInitialized) {
                throw IllegalStateException("PreferencesUtil not initialized")
            }
            return INSTANCE
        }
    }

    /** Sync keys with provided list (UI names) */
    fun updatePreferences(names: List<String>, defaultValue: Boolean = true) {
        prefs.edit() {

            // remove obsolete keys
            for (storedKey in prefs.all.keys) {
                if (storedKey.startsWith(PREFIX)) {
                    val uiName = strip(storedKey)
                    if (uiName !in names) {
                        remove(storedKey)
                    }
                }
            }

            // add missing keys
            for (name in names) {
                val k = key(name)
                if (!prefs.contains(k)) {
                    putBoolean(k, defaultValue)
                }
            }

        }
    }

    /** Get value by UI name */
    fun getByName(name: String): Boolean {
        return prefs.getBoolean(key(name), false)
    }

    /** Set value (switch toggle) */
    fun setPreference(name: String, enabled: Boolean) {
        prefs.edit() {
            putBoolean(key(name), enabled)
        }
    }

    /** Get all preferences as map (UI-safe) */
    fun getAll(): Map<String, Boolean> {
        return prefs.all
            .filterKeys { it.startsWith(PREFIX) }
            .mapKeys { strip(it.key) }
            .mapValues { it.value as Boolean }
    }

    /** Ready for UI display */
    fun getForDisplay(): List<UiPref> {
        return getAll()
            .map { UiPref(it.key, it.value) }
            .sortedBy { it.name }
    }
}

data class UiPref(
    val name: String,
    val enabled: Boolean
)
