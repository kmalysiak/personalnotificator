package pl.kmalysiak.personalnotificator.ws

import android.content.Context
import android.os.PowerManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import pl.kmalysiak.personalnotificator.model.secure.SecurePrefsService
import java.util.concurrent.TimeUnit

//to nie powinien być Singleton -> cykl życia związany z WebSocketService
class WebSocketManager(
    private val context: Context,
    private val serverUrl: String,
    private val securePrefs: SecurePrefsService,
    private val onMessage: (String) -> Unit,
    private val onStateChange: (State) -> Unit
) {
    enum class State { CONNECTING, CONNECTED, RECONNECTING, STOPPED }

    private val client = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)   // Cloudflare timeout ~100s, websockets autoping
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)     // wait indefinitely for read as websockets can be long opened
        .writeTimeout(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)      // custom retry handling
        .build()

    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var reconnectJob: Job? = null
    private var isRunning = false
    private var failureCount = 0

    // WakeLock — trzyma CPU aktywny przez połączenie
    private val wakeLock: PowerManager.WakeLock by lazy {
        (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::WebSocketWakeLock")
            .apply { setReferenceCounted(false) } //single acquire -> active, single release - released
    }

    private fun acquireWakeLock() {
        if (!wakeLock.isHeld) {
            wakeLock.acquire(12 * 60 * 60 * 1000L)  //max 12h, then reacquired
        }
    }

    private fun releaseWakeLock() {
        if (wakeLock.isHeld) wakeLock.release()
    }

    //stared by websocket service
    fun start(idToken: String) {
        isRunning = true
       // TokenStore.save(idToken)
        securePrefs.saveIdToken(idToken)
        acquireWakeLock()
        connect(idToken)
    }

    //stopped by websocket service
    fun stop() {
        isRunning = false
        reconnectJob?.cancel()
        webSocket?.close(1000, "Service stopped")
        releaseWakeLock()
        scope.cancel()
        onStateChange(State.STOPPED)
    }

    private fun connect(token: String) {
        onStateChange(State.CONNECTING)
        val request = Request.Builder()
            .url(serverUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(ws: WebSocket, response: Response) {
                failureCount = 0
                reconnectJob?.cancel()
                ws.send("""{"type":"auth","idToken":"$token"}""")
                onStateChange(State.CONNECTED)
                Log.i("WSManager", "Connected and auth sent")
            }

            override fun onMessage(ws: WebSocket, text: String) {
                if (text == "heartbeat" || text == "ping") {
                    ws.send("pong")  // odpowiedz na heartbeat serwera
                    return
                }
                onMessage(text)
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.w("WSManager", "Failure #$failureCount: ${t.message}")
                scheduleReconnect()
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.w("WSManager", "Closed: $code / $reason")
                if (isRunning) scheduleReconnect()
            }
        })
    }

    private fun scheduleReconnect() {
        if (!isRunning) return
        onStateChange(State.RECONNECTING)
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            // exponential backoff: 2s, 4s, 8s, ... max 60s
            val delay = minOf(2_000L * (1L shl minOf(failureCount, 5)), 60_000L)
            failureCount++
            Log.i("WSManager", "Reconnect in ${delay}ms (attempt $failureCount)")
            delay(delay)
            if (!isRunning) return@launch
            try {
              //  val token = refreshToken()
                connect(securePrefs.loadIdToken())
            } catch (e: Exception) {
                Log.e("WSManager", "Token refresh failed: ${e.message}")
                scheduleReconnect()
            }
        }
    }
}