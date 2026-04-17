package pl.kmalysiak.personalnotificator.auth

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
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
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import org.slf4j.LoggerFactory

@Singleton
class BackendAuthService @Inject constructor(
    private val securePrefs: SecurePrefsService,
    private val appConfig: AppConfig,
    @ApplicationContext private val context: Context
) {
    private val log = LoggerFactory.getLogger(BackendAuthService::class.java)
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val client = OkHttpClient()

    suspend fun authenticateAndSendCredHome(): BackendAuthResult {
        return try {
            withContext(Dispatchers.IO) {
                val credentials = securePrefs.loadCredentials()

                val user = firebaseAuth
                    .currentUser ?: firebaseAuth.signInWithEmailAndPassword(credentials.first, credentials.second)
                        .await().user ?: throw Exception("Auth failed")


                val idToken = user.getIdToken(true).await()?.token
                    ?: throw Exception("Failed to get ID token")

                val fcmToken = FirebaseMessaging.getInstance().token.await()

                val body = Payload(uuid = UUID.randomUUID().toString(), fcmToken = fcmToken, login = credentials.first)
                val jsonBody = Json.encodeToString(body)

                val request = Request.Builder()
                    .url("https://" + appConfig.apiUrl + "api/register")
                    .addHeader("Authorization", "Bearer $idToken")
                    .post(jsonBody.toRequestBody("application/json".toMediaType()))
                    .build()


                client.newCall(request).execute().use { response ->
                    val bodyString = response.body.string()
                    val result =  Gson().fromJson(bodyString, BackendAuthResult::class.java)

                    if (result.isRegister) {
                        securePrefs.saveIdToken(idToken)
//                        val wsIntent = Intent(context, WebSocketService::class.java)
//                            .putExtra("idToken", idToken)
//                        context.startForegroundService(wsIntent)
                    }

                    result
                }
            }

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.error("Exception calling home", e)
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
