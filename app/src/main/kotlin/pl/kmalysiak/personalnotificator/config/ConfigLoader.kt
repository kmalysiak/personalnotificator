package pl.kmalysiak.personalnotificator.config

import android.content.Context
import com.google.gson.Gson
import java.nio.charset.StandardCharsets

object ConfigLoader {
    private var config: AppConfig? = null

    fun init(context: Context) {
        if (config == null) {
            val json = context.assets.open("config.json")
                .bufferedReader(StandardCharsets.UTF_8)
                .use { it.readText() }
            config = Gson().fromJson(json, AppConfig::class.java)
        }
    }

    fun getConfig(): AppConfig {
        return config ?: throw IllegalStateException("ConfigLoader not initialized. Call init() first.")
    }
}
