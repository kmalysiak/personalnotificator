package pl.kmalysiak.personalnotificator.auth

import android.content.Context
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import pl.kmalysiak.personalnotificator.config.AppConfig
import pl.kmalysiak.personalnotificator.model.secure.SecurePrefsService
import pl.kmalysiak.personalnotificator.ws.WebSocketService
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackendAuthService @Inject constructor(
    private val securePrefs: SecurePrefsService,
    private val appConfig: AppConfig
) {
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val client = OkHttpClient()

    suspend fun authenticateAndSendCredHomeAsync(ctx: Context): BackendAuthResult {
        return try {
            val credentials = securePrefs.loadCredentials()

            val authResult = firebaseAuth
                .signInWithEmailAndPassword(credentials.first, credentials.second)
                .await()

            val idToken = authResult.user?.getIdToken(false)?.await()?.token
                ?: throw Exception("Failed to get ID token")

            val fcmToken = FirebaseMessaging.getInstance().token.await()

            val body = Payload(uuid = UUID.randomUUID().toString(), fcmToken = fcmToken, login = credentials.first)
            val jsonBody = Json.encodeToString(body)

            val request = Request.Builder()
                .url("https://" + appConfig.apiUrl + "api/register")
                .addHeader("Authorization", "Bearer $idToken")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            withContext(Dispatchers.IO) {
                client.newCall(request).execute().use { response ->
                    val bodyString = response.body.string()

                    val result = Gson().fromJson(bodyString, BackendAuthResult::class.java)

                    if (result.isRegister) {
                        securePrefs.saveIdToken(idToken)
                        val wsIntent = Intent(ctx, WebSocketService::class.java)
                            .putExtra("idToken", idToken)
                        ctx.startForegroundService(wsIntent)
                    }

                    return@withContext result
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            BackendAuthResult(false, "Exception calling home - " + e.message)
        }
    }

    @Serializable
    data class Payload(
        val uuid: String,
        val fcmToken: String,
        val login: String
    )
}
