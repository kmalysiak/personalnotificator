package pl.kmalysiak.personalnotificator.model

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme
import androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme

object SecurePrefs {

    private const val PREF_NAME = "secret_prefs"
    private const val MASTER_KEY_ALIAS = "master_key_alias"

    fun saveCredentials(context: Context, username: String, password: String) {
        getPrefs(context).edit().apply {
            putString("username", username)
            putString("password", password)
            apply()
        }
    }

    fun loadCredentials(context: Context): Pair<String, String> {
        val prefs = getPrefs(context)
        return prefs.getString("username", "")!! to prefs.getString("password", "")!!
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return EncryptedSharedPreferences.create(
            PREF_NAME,
            MASTER_KEY_ALIAS,
            context,
            PrefKeyEncryptionScheme.AES256_SIV,
            PrefValueEncryptionScheme.AES256_GCM
        )
    }


}