package pl.kmalysiak.personalnotificator.auth

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import pl.kmalysiak.personalnotificator.model.Payload
import pl.kmalysiak.personalnotificator.model.SecurePrefs
import java.util.*

class BackendAuth : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val client = OkHttpClient()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scope.launch {
            authenticateAndSend()
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onBind(p0: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    private suspend fun authenticateAndSend() {
        try {
            val credentials = SecurePrefs.loadCredentials(this)
            val authResult = firebaseAuth.signInWithEmailAndPassword(credentials.first, credentials.second).await()
            val idToken = authResult.user?.getIdToken(false)?.await()?.token ?: throw Exception("Failed to get ID token")
            val fcmToken = FirebaseMessaging.getInstance().token.await()
            val body = Payload(uuid = UUID.randomUUID().toString(), fcmToken = fcmToken)
            val jsonBody = Json.encodeToString(body)
            val request = Request.Builder().url("https://your.api/endpoint").addHeader("Authorization", "Bearer $idToken").post(jsonBody.toRequestBody("application/json".toMediaType())).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("Request failed: ${response.code}")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}