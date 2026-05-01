package pl.kmalysiak.personalnotificator.appstate

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import pl.kmalysiak.personalnotificator.ws.WebSocketManager
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppStateRepository @Inject constructor() {

    private val _fcmToken = MutableStateFlow("Token unknown")
    val fcmToken: StateFlow<String> = _fcmToken

    private val _wsState = MutableStateFlow(WebSocketManager.State.STOPPED)
    val wsState: StateFlow<WebSocketManager.State> = _wsState

    private val _lastHeartbeat = MutableStateFlow(LocalDateTime.MIN)
    val lastHeartbeat: StateFlow<LocalDateTime> = _lastHeartbeat

    private val _ttsReady = MutableStateFlow<Boolean?>(null)
    val ttsReady: StateFlow<Boolean?> = _ttsReady

    private val _lastReconnectRequest = MutableStateFlow(LocalDateTime.MIN)
    val lastReconnectRequest: StateFlow<LocalDateTime> = _lastReconnectRequest

    private val _permissions = MutableStateFlow<List<Triple<String, LocalDateTime, String>>>(emptyList())
    val permissions: StateFlow<List<Triple<String, LocalDateTime, String>>> = _permissions

    private val _notificationSaved = MutableSharedFlow<Unit>(extraBufferCapacity = 16)
    val notificationSaved: SharedFlow<Unit> = _notificationSaved

    fun setFcmToken(token: String) { _fcmToken.value = token }
    fun setWsState(state: WebSocketManager.State) { _wsState.value = state }
    fun setLastHeartbeat(dt: LocalDateTime) { _lastHeartbeat.value = dt }
    fun setTtsReady(ready: Boolean?) { _ttsReady.value = ready }
    fun setLastReconnectRequest(dt: LocalDateTime) { _lastReconnectRequest.value = dt }
    fun addPermission(type: String, dt: LocalDateTime, status: String) { _permissions.value += Triple(type, dt, status) }
    fun emitNotificationSaved() { _notificationSaved.tryEmit(Unit) }
}