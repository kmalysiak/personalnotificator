package pl.kmalysiak.personalnotificator.auth

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import pl.kmalysiak.config.ConfigLoader
import pl.kmalysiak.personalnotificator.model.Payload
import pl.kmalysiak.personalnotificator.model.SecurePrefs
import java.util.UUID

class BackendAuth : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val client = OkHttpClient()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val ctx = this.applicationContext;
        scope.launch {
            authenticateAndSendCredHomeAsync(ctx)
            stopSelf()
        }
        return START_NOT_STICKY
    }


    override fun onBind(p0: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    suspend fun authenticateAndSendCredHomeAsync(ctx: Context): RegisterResult {
        return try {
            val credentials = SecurePrefs.loadCredentials(ctx)

            val authResult = firebaseAuth
                .signInWithEmailAndPassword(credentials.first, credentials.second)
                .await()

            val idToken = authResult.user?.getIdToken(false)?.await()?.token
                ?: throw Exception("Failed to get ID token")

            val fcmToken = FirebaseMessaging.getInstance().token.await()

            val body = Payload(uuid = UUID.randomUUID().toString(), fcmToken = fcmToken, login=credentials.first)
            val jsonBody = Json.encodeToString(body)


            val request = Request.Builder()
                .url(ConfigLoader.getConfig().apiUrl + "api/register")
                .addHeader("Authorization", "Bearer $idToken")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()


             withContext(Dispatchers.IO) {
                client.newCall(request).execute().use { response ->
                    val bodyString = response.body?.string()
                        ?: return@withContext RegisterResult(false, "Home returned nothing.")
                    Gson().fromJson(bodyString, RegisterResult::class.java)
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            RegisterResult(false, "Exception calling home.")
        }
    }
}
