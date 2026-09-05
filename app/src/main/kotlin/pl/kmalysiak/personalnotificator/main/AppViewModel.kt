package pl.kmalysiak.personalnotificator.main

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.kmalysiak.personalnotificator.App
import pl.kmalysiak.personalnotificator.appstate.AppStateRepository
import pl.kmalysiak.personalnotificator.main.DateTimeUtil
import pl.kmalysiak.personalnotificator.model.dao.NotificationRepository
import pl.kmalysiak.personalnotificator.auth.BackendAuthResult
import pl.kmalysiak.personalnotificator.auth.BackendAuthService
import pl.kmalysiak.personalnotificator.appstate.ConfigRepository
import pl.kmalysiak.personalnotificator.model.dao.NotificationEntity
import pl.kmalysiak.personalnotificator.appstate.SecurePrefsService
import pl.kmalysiak.personalnotificator.appstate.PreferencesDataUtil
import pl.kmalysiak.personalnotificator.ws.WebSocketManager
import pl.kmalysiak.personalnotificator.ws.WebSocketService
import pl.kmalysiak.personalnotificator.haier.HaierHeatPumpDto
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    application: Application,
    private val securePrefs: SecurePrefsService,
    private val prefsDataUtil: PreferencesDataUtil,
    private val backendAuthService: BackendAuthService,
    private val configRepository: ConfigRepository,
    private val notificationRepository: NotificationRepository,
    private val appStateRepo: AppStateRepository
) : AndroidViewModel(application) {

    private val _tableData = MutableStateFlow<List<EventViewDto>>(emptyList())
    internal val tableData: StateFlow<List<EventViewDto>> = _tableData

    private val _lastGateOpened = MutableStateFlow(LocalDateTime.MIN)
    internal val lastGateOpened: StateFlow<LocalDateTime> get() = _lastGateOpened
    private val _lastGateClosed = MutableStateFlow(LocalDateTime.MIN)
    internal val lastGateClosed: StateFlow<LocalDateTime> get() = _lastGateClosed
    private val _lastMovement = MutableStateFlow(LocalDateTime.MIN)
    internal val lastMovement: StateFlow<LocalDateTime> get() = _lastMovement

    internal val token: StateFlow<String> = appStateRepo.fcmToken
    internal val wsState: StateFlow<WebSocketManager.State> = appStateRepo.wsState
    internal val lastHeartbeat: StateFlow<LocalDateTime> = appStateRepo.lastHeartbeat
    internal val ttsReady: StateFlow<Boolean?> = appStateRepo.ttsReady
    internal val lastReconnectRequest: StateFlow<LocalDateTime> = appStateRepo.lastReconnectRequest
    internal val permissionData: StateFlow<List<Triple<String, LocalDateTime, String>>> = appStateRepo.permissions

    internal val haierData: StateFlow<HaierHeatPumpDto?> = appStateRepo.haierData

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
            val result = backendAuthService.authenticateAndSendCredHome()
            _authResult.value = result
            if (result.isRegister) {
                getApplication<App>().tryStartWebSocketService()
            }
        }
    }

    init {
        if (appStateRepo.ttsReady.value == null) {
            appStateRepo.setTtsReady(prefsDataUtil.loadTtsReady())
        }

        viewModelScope.launch(Dispatchers.IO) {
            reloadFromDb()
        }

        viewModelScope.launch {
            appStateRepo.notificationSaved.collect {
                withContext(Dispatchers.IO) { reloadFromDb() }
            }
        }
    }

    private suspend fun reloadFromDb() {
        val entities = notificationRepository.getAll()
        _tableData.value = entitiesToEventViews(entities)
        prefsDataUtil.updatePreferences(entities.map { it.entType })
        val (opened, closed, movement) = filterLastEvents(entities)
        _lastGateOpened.value = opened
        _lastGateClosed.value = closed
        _lastMovement.value = movement
    }

    private fun filterLastEvents(entities: List<NotificationEntity>): Triple<LocalDateTime, LocalDateTime, LocalDateTime> {
        val sorted = entities.sortedByDescending { it.timestamp }
        return Triple(
            DateTimeUtil.toWarsawDt(sorted.firstOrNull { isGateOpened(it) }?.timestamp ?: -1),
            DateTimeUtil.toWarsawDt(sorted.firstOrNull { isGateClosed(it) }?.timestamp ?: -1),
            DateTimeUtil.toWarsawDt(sorted.firstOrNull { isMovement(it) }?.timestamp ?: -1)
        )
    }

    private fun isGateOpened(e: NotificationEntity) =
        (e.entType == "wejście" && "otwarta".contains(e.content)) ||
        (e.entType == "gate"    && "opened".contains(e.content))

    private fun isGateClosed(e: NotificationEntity) =
        (e.entType == "wejście" && "zamknięta".contains(e.content)) ||
        (e.entType == "gate"    && "closed".contains(e.content))

    private fun isMovement(e: NotificationEntity) =
        e.entType == "ruch" || e.entType == "movement"

    private fun entitiesToEventViews(daos: List<NotificationEntity>): List<EventViewDto> = daos.mapIndexed { idx, entity ->
        val nextDt = daos.getOrNull(idx + 1)
        val isLastAtDay = nextDt != null && !DateTimeUtil.areTheSameDay(entity.timestamp, nextDt.timestamp)
        EventViewDto(
            entity.entFriendlyName,
            DateTimeUtil.toWarsawDt(entity.timestamp),
            entity.content,
            isLastAtDay,
            DateTimeUtil.toWarsawDt(entity.timestampReceived)
        )
    }

    fun getPrefsForDisplay() = prefsDataUtil.getForDisplay()

    fun setPreference(name: String, enabled: Boolean) {
        prefsDataUtil.setPreference(name, enabled)
    }

    fun clearDb() {
        viewModelScope.launch(Dispatchers.IO) {
            notificationRepository.deleteAll()
            _tableData.value = emptyList()
            _lastGateOpened.value = LocalDateTime.MIN
            _lastGateClosed.value = LocalDateTime.MIN
            _lastMovement.value = LocalDateTime.MIN
        }
    }
}
