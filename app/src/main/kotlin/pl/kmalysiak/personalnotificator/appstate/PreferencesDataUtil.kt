package pl.kmalysiak.personalnotificator.appstate

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesDataUtil @Inject constructor(@ApplicationContext context: Context) {

    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    // master: górny limit nakładany na głośność per-typ (NotificationTypePreferenceEntity.volume)
    fun saveTtsVolume(volume: Float) = prefs.edit { putFloat("tts_volume", volume) }
    fun loadTtsVolume(): Float = prefs.getFloat("tts_volume", 1.0f)

    fun saveTtsReady(ready: Boolean) = prefs.edit { putBoolean("tts_ready", ready) }
    fun loadTtsReady(): Boolean? = if (prefs.contains("tts_ready")) prefs.getBoolean("tts_ready", false) else null

    // epoch sekund ostatniej wiadomości WS widzianej przez klienta - przetrwa restart procesu, wysyłane backendowi w pongu
    fun saveLastSeenEpochSeconds(value: Long) = prefs.edit { putLong("last_seen_epoch_seconds", value) }
    fun loadLastSeenEpochSeconds(): Long = prefs.getLong("last_seen_epoch_seconds", 0L)
}
