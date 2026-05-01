package pl.kmalysiak.personalnotificator.ws

import android.content.Context
import android.os.PowerManager
import kotlinx.coroutines.CoroutineScope
import org.slf4j.LoggerFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import pl.kmalysiak.personalnotificator.auth.BackendAuthService
import pl.kmalysiak.personalnotificator.model.secure.SecurePrefsService
import java.util.concurrent.TimeUnit

//to nie powinien być Singleton -> cykl życia związany z WebSocketService
class WebSocketManager(
    private val context: Context,
    private val serverUrl: String,
    private val securePrefs: SecurePrefsService,
    private val backendAuthService: BackendAuthService,
    private val onMessage: (String) -> Unit,
    private val onStateChange: (State) -> Unit,
) {
    enum class State { CONNECTING, CONNECTED, RECONNECTING, RECONNECTING_REAUTH, AUTH_ERROR, STOPPED }

    private val log = LoggerFactory.getLogger(WebSocketManager::class.java)

    private val client = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)   // Cloudflare timeout ~100s, websockets autoping
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)     // wait indefinitely for read as websockets can be long opened
        .writeTimeout(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)      // custom retry handling
        .build()

    @Volatile private var webSocket: WebSocket? = null
    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var reconnectJob: Job? = null
    private val isRunning = AtomicBoolean(false)
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
    fun start(idToken: String, fcmToken:String) {
        if (!isRunning.compareAndSet(false, true)) {
            log.warn("start() called but already running — ignoring")
            return
        }
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        securePrefs.saveIdToken(idToken)
        acquireWakeLock()
        connect(idToken, fcmToken)
    }

    //stopped by websocket service
    fun stop() {
        isRunning.set(false)
        reconnectJob?.cancel()
        webSocket?.close(1000, "Service stopped")
        releaseWakeLock()
        scope.cancel()
        onStateChange(State.STOPPED)
    }

    private fun connect(token: String, fcmToken: String) {
        webSocket?.close(1000, "Reconnecting")
        webSocket = null

        onStateChange(State.CONNECTING)

        val request = Request.Builder()
            .url(serverUrl)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("X-FCM-Token", fcmToken)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                failureCount = 0
                reconnectJob?.cancel()
                onStateChange(State.CONNECTED)
                log.info("Connected")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (text == "heartbeat" || text == "ping") {
                    webSocket.send("pong")
                    return
                }
                onMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (response?.code == 401) {
                    log.warn("Unauthorized (401) — scheduling reconnect with re-authentication")
                    scheduleReconnect(reAuthenticate = true)
                } else {
                    log.warn("Failure #{}: {}", failureCount, t.message)
                    scheduleReconnect()
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                log.warn("Closed: {} / {}", code, reason)
                if (isRunning.get()) scheduleReconnect()
            }
        })
    }
    private fun scheduleReconnect(reAuthenticate: Boolean = false) {
        if (!isRunning.get()) return
        if(reAuthenticate)
            onStateChange(State.RECONNECTING_REAUTH)
        else
            onStateChange(State.RECONNECTING)

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            // exponential backoff: 2s, 4s, 8s, ... max 60s
            val delay = minOf(2_000L * (1L shl minOf(failureCount, 5)), 60_000L)
            failureCount++
            log.info("Reconnect in {}ms (attempt {})", delay, failureCount)
            delay(delay)
            if (!isRunning.get()) return@launch
            try {
                if (reAuthenticate) {
                    log.info("Re-authenticating before reconnect")
                    backendAuthService.authenticateAndSendCredHome()
                }
                connect(securePrefs.loadIdToken(), securePrefs.loadFcmToken())
            } catch (e: Exception) {
                log.error("Reconnect preparation failed: {}", e.message)
                scheduleReconnect()
            }
        }
    }
}