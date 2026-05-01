package pl.kmalysiak.personalnotificator.appstate

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurePrefsService @Inject constructor(@ApplicationContext context: Context) {

    private val log = LoggerFactory.getLogger(SecurePrefsService::class.java)

    private val prefs: SharedPreferences = createPrefs(context)

    private fun createPrefs(context: Context): SharedPreferences {
        return try {
            buildEncryptedPrefs(context)
        } catch (e: Exception) {
            // Keystore key corrupted (e.g. after reinstall, backup restore, some ROMs).
            // Wipe the prefs file and recreate — stored tokens/credentials will be lost.
            log.error("EncryptedSharedPreferences init failed, wiping and recreating: {}", e.message)
            context.deleteSharedPreferences(PREF_NAME)
            buildEncryptedPrefs(context)
        }
    }

    @Suppress("DEPRECATION")
    private fun buildEncryptedPrefs(context: Context): SharedPreferences =
        EncryptedSharedPreferences.create(
            PREF_NAME,
            MASTER_KEY_ALIAS,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

    companion object {
        private const val PREF_NAME = "secret_prefs"
        private const val MASTER_KEY_ALIAS = "master_key_alias"
    }

    fun saveCredentials(username: String, password: String) {
        prefs.edit().apply {
            putString("username", username)
            putString("password", password)
            apply()
        }
    }

    fun loadCredentials(): Pair<String, String> =
        prefs.getString("username", "")!! to prefs.getString("password", "")!!

    fun saveIdToken(idToken: String) {
        prefs.edit().apply {
            putString("idToken", idToken)
            apply()
        }
    }

    fun saveFcmToken(fcmToken: String) {
        prefs.edit().apply {
            putString("fcmToken", fcmToken)
            apply()
        }
    }

    fun loadFcmToken(): String = prefs.getString("fcmToken", "")!!

    fun loadIdToken(): String = prefs.getString("idToken", "")!!
}