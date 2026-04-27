package pl.kmalysiak.personalnotificator.main

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.mutableStateOf
import org.slf4j.LoggerFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import pl.kmalysiak.personalnotificator.DateTimeUtil
import pl.kmalysiak.personalnotificator.auth.BackendAuthResult
import pl.kmalysiak.personalnotificator.auth.BackendAuthService
import pl.kmalysiak.personalnotificator.config.ConfigRepository
import pl.kmalysiak.personalnotificator.model.dao.NotificationEntityDao
import pl.kmalysiak.personalnotificator.model.data.NotificationEntity
import pl.kmalysiak.personalnotificator.model.event.EventWithDateTimeAndStatus
import pl.kmalysiak.personalnotificator.model.event.HeartbeatEvent
import pl.kmalysiak.personalnotificator.model.event.PermEvent
import pl.kmalysiak.personalnotificator.model.event.TokenEvent
import pl.kmalysiak.personalnotificator.model.event.ReconnectRequestEvent
import pl.kmalysiak.personalnotificator.model.event.WsStateEvent
import pl.kmalysiak.personalnotificator.ws.WebSocketManager
import pl.kmalysiak.personalnotificator.ws.WebSocketService
import pl.kmalysiak.personalnotificator.model.secure.SecurePrefsService
import pl.kmalysiak.personalnotificator.preference.PreferencesDataUtil
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    application: Application,
    private val securePrefs: SecurePrefsService,
    private val prefsDataUtil: PreferencesDataUtil,
    private val backendAuthService: BackendAuthService,
    private val configRepository: ConfigRepository,
    private val dao: NotificationEntityDao
) : AndroidViewModel(application) {
    private val log = LoggerFactory.getLogger(AppViewModel::class.java)
    private val _token = MutableStateFlow("Token unknown")
    private val _tableData = MutableStateFlow<List<EventViewDto>>(emptyList())


    private val _permissionData = MutableStateFlow<List<Triple<String, LocalDateTime, String>>>(emptyList())
    internal val token: StateFlow<String> get() = _token
    internal val tableData: StateFlow<List<EventViewDto>> = _tableData
    internal val permissionData: StateFlow<List<Triple<String, LocalDateTime, String>>> = _permissionData
    private val _lastGateOpened = MutableStateFlow(LocalDateTime.MIN)
    internal val lastGateOpened: StateFlow<LocalDateTime> get() = _lastGateOpened
    private val _lastGateClosed = MutableStateFlow(LocalDateTime.MIN)
    internal val lastGateClosed: StateFlow<LocalDateTime> get() = _lastGateClosed
    private val _lastMovement = MutableStateFlow(LocalDateTime.MIN)
    internal val lastMovement: StateFlow<LocalDateTime> get() = _lastMovement
    private val _lastRefreshed = MutableStateFlow(LocalDateTime.MIN)
    internal val lastRefreshed: StateFlow<LocalDateTime> get() = _lastRefreshed

    private val _lastHeartBeat = MutableStateFlow(LocalDateTime.MIN)
    internal val lastHeartbeat: StateFlow<LocalDateTime> get() = _lastHeartBeat

    private val _wsState = MutableStateFlow(WebSocketManager.State.STOPPED)
    internal val wsState: StateFlow<WebSocketManager.State> get() = _wsState

    private val _lastReconnectRequest = MutableStateFlow(LocalDateTime.MIN)
    internal val lastReconnectRequest: StateFlow<LocalDateTime> get() = _lastReconnectRequest

    private val _ttsReady = MutableStateFlow<Boolean?>(prefsDataUtil.loadTtsReady())
    internal val ttsReady: StateFlow<Boolean?> get() = _ttsReady

    private val _ttsVolume = MutableStateFlow(prefsDataUtil.loadTtsVolume())
    internal val ttsVolume: StateFlow<Float> get() = _ttsVolume

    fun setTtsVolume(volume: Float) {
        prefsDataUtil.saveTtsVolume(volume)
        _ttsVolume.value = volume
    }

    val configMode: StateFlow<ConfigRepository.ConfigMode> = configRepository.configMode

    fun setConfigMode(mode: ConfigRepository.ConfigMode) {
        configRepository.setMode(mode)
        val ctx = getApplication<Application>()
        ctx.stopService(Intent(ctx, WebSocketService::class.java))
    }

    internal val authResult: StateFlow<BackendAuthResult?> get() = _authResult
    private val _authResult = MutableStateFlow<BackendAuthResult?>(null)


    var username = mutableStateOf("")
        private set

    var password = mutableStateOf("")
        private set

    fun loadCredentials() {
        val (user, pass) = securePrefs.loadCredentials()
        username.value = user
        password.value = pass
    }

    fun saveCredentials(user: String, pass: String) {
        securePrefs.saveCredentials(user, pass)
        username.value = user
        password.value = pass
    }

    fun authenticate() {
        viewModelScope.launch {
            _authResult.value = null
            _authResult.value = backendAuthService.authenticateAndSendCredHome()
        }
    }

    init {
        EventBus.getDefault().register(this)

        viewModelScope.launch(Dispatchers.IO) {
            val daos = dao.getAll()
            val fl = entitiesToEventViews(daos)
            _tableData.value = fl
            val lastEvents = filterLastEvents(daos)
            _lastGateOpened.value = lastEvents.first
            _lastGateClosed.value = lastEvents.second
            _lastMovement.value = lastEvents.third
        }
    }

    private fun CoroutineScope.filterLastEvents(entities: List<NotificationEntity>): Triple<LocalDateTime, LocalDateTime, LocalDateTime> {
        val sorted = entities.sortedByDescending { it.timestamp }
        return Triple(
            DateTimeUtil.toWarsawDt(sorted.firstOrNull { it.entType == "wejście" && "otwarta".contains(it.content) }?.timestamp ?: -1),
            DateTimeUtil.toWarsawDt(sorted.firstOrNull { it.entType == "wejście" && "zamknięta".contains(it.content) }?.timestamp ?: -1),
            DateTimeUtil.toWarsawDt(sorted.firstOrNull { it.entType == "ruch" }?.timestamp ?: -1)
        )
    }


    override fun onCleared() {
        super.onCleared()
        EventBus.getDefault().unregister(this)
    }



    private fun entitiesToEventViews(daos: List<NotificationEntity>): List<EventViewDto> = daos.mapIndexed { idx, entity ->

        val nextDt = daos.getOrNull(idx + 1)
        val isLastAtDay =
            nextDt != null && !DateTimeUtil.areTheSameDay(entity.timestamp, nextDt.timestamp)

        EventViewDto(
            entity.entFriendlyName,
            DateTimeUtil.toWarsawDt(entity.timestamp),
            entity.content,
            isLastAtDay,
            DateTimeUtil.toWarsawDt(entity.timestampReceived))
    }


    fun getPrefsForDisplay() = prefsDataUtil.getForDisplay()

    fun setPreference(name: String, enabled: Boolean) {
        prefsDataUtil.setPreference(name, enabled)
    }

    fun clearDb() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteALl()
            _tableData.value = mutableListOf()
            val lastEvents = filterLastEvents(mutableListOf())
            _lastGateOpened.value = lastEvents.first
            _lastGateClosed.value = lastEvents.second
            _lastMovement.value = lastEvents.third
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onHeartBeatEvent(event: HeartbeatEvent) {
        _lastHeartBeat.value = event.timestamp
    }


    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onTokenReceivedEvent(event: TokenEvent) {
        _token.value = event.token
        log.debug("Token received: {}", event.token)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDateTimeEvent(event: EventWithDateTimeAndStatus) {
        if (event.type == "TTS") {
            _ttsReady.value = event.status == "OK"
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val entities = dao.getAll() //TODO:optimize me update only necessary, not whole
            val daos = dao.getAll()
            val fl = entitiesToEventViews(daos)
            _tableData.value = fl
            _lastRefreshed.value = DateTimeUtil.nowToWarsawDt()
            prefsDataUtil.updatePreferences(entities.map { e -> e.entType })
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onWsStateEvent(event: WsStateEvent) {
        _wsState.value = event.state
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReconnectRequestEvent(event: ReconnectRequestEvent) {
        _lastReconnectRequest.value = event.timestamp
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPermEvent(event: PermEvent) {
        val newRow = Triple(event.type, event.dt, event.status)
        _permissionData.value += newRow
        log.debug("onPermEvent: {}", event)
    }

}