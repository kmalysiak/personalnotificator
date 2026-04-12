package pl.kmalysiak.personalnotificator.model.secure

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme
import androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurePrefsService @Inject constructor(@ApplicationContext context: Context) {

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        PREF_NAME,
        MASTER_KEY_ALIAS,
        context,
        PrefKeyEncryptionScheme.AES256_SIV,
        PrefValueEncryptionScheme.AES256_GCM
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

    fun loadIdToken(): String = prefs.getString("idToken", "")!!
}
