package pl.kmalysiak.personalnotificator.config

import android.content.Context
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    enum class ConfigMode { REMOTE, LOCAL }

    private val prefs = context.getSharedPreferences("config_prefs", Context.MODE_PRIVATE)

    private val _configMode = MutableStateFlow(loadMode())
    val configMode: StateFlow<ConfigMode> = _configMode.asStateFlow()

    val currentConfig: AppConfig get() = loadConfig(_configMode.value)

    fun setMode(mode: ConfigMode) {
        prefs.edit().putString("config_mode", mode.name).apply()
        _configMode.value = mode
    }

    private fun loadMode(): ConfigMode {
        val saved = prefs.getString("config_mode", ConfigMode.REMOTE.name)
        return runCatching { ConfigMode.valueOf(saved!!) }.getOrDefault(ConfigMode.REMOTE)
    }

    private fun loadConfig(mode: ConfigMode): AppConfig {
        val filename = when (mode) {
            ConfigMode.REMOTE -> "config.json"
            ConfigMode.LOCAL -> "config_dev.json"
        }
        val json = context.assets.open(filename)
            .bufferedReader(StandardCharsets.UTF_8)
            .use { it.readText() }
        return Gson().fromJson(json, AppConfig::class.java)
    }
}
