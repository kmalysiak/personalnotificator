package pl.kmalysiak.personalnotificator.main

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
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
import pl.kmalysiak.personalnotificator.model.dao.NotificatorDatabase
import pl.kmalysiak.personalnotificator.model.data.NotificationEntity
import pl.kmalysiak.personalnotificator.model.event.EventWithDateTimeAndStatus
import pl.kmalysiak.personalnotificator.model.event.HeartbeatEvent
import pl.kmalysiak.personalnotificator.model.event.PermEvent
import pl.kmalysiak.personalnotificator.model.event.TokenEvent
import pl.kmalysiak.personalnotificator.model.secure.SecurePrefsService
import pl.kmalysiak.personalnotificator.preference.PreferencesDataUtil
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    application: Application,
    private val securePrefs: SecurePrefsService,
    private val prefsDataUtil: PreferencesDataUtil,
    private val backendAuthService: pl.kmalysiak.personalnotificator.auth.BackendAuthService
) : AndroidViewModel(application) {
    private val _token = MutableStateFlow("Token unknown")
    private val _tableData = MutableStateFlow<List<EventViewDto>>(emptyList())


    private val _permissionData = MutableStateFlow<List<Triple<String, LocalDateTime, String>>>(emptyList())
    internal val token: StateFlow<String> get() = _token
    internal val tableData: StateFlow<List<EventViewDto>> = _tableData
    internal val permissionData: StateFlow<List<Triple<String, LocalDateTime, String>>> = _permissionData
    private val _lastGateOpened = MutableStateFlow(LocalDateTime.of(2025, 1, 1, 1, 2, 3))
    internal val lastGateOpened: StateFlow<LocalDateTime> get() = _lastGateOpened
    private val _lastGateClosed = MutableStateFlow(LocalDateTime.of(2025, 1, 2, 2, 3, 4))
    internal val lastGateClosed: StateFlow<LocalDateTime> get() = _lastGateClosed
    private val _lastMovement = MutableStateFlow(LocalDateTime.of(2025, 1, 3, 3, 4, 5))
    internal val lastMovement: StateFlow<LocalDateTime> get() = _lastMovement
    private val _lastRefreshed = MutableStateFlow(LocalDateTime.of(2025, 1, 3, 3, 4, 5))
    internal val lastRefreshed: StateFlow<LocalDateTime> get() = _lastRefreshed

    private val _lastHeartBeat = MutableStateFlow(LocalDateTime.of(2025, 1, 3, 3, 4, 5))
    internal val lastHeartbeat: StateFlow<LocalDateTime> get() = _lastHeartBeat

    private val db = NotificatorDatabase.getDatabase(application)
    private val dao = db.notificationEntityDao()

    var username = mutableStateOf("")
        private set

    var password = mutableStateOf("")
        private set

    fun loadCredentials(context: Context) {
        val (user, pass) = securePrefs.loadCredentials()
        username.value = user
        password.value = pass
    }

    fun saveCredentials(context: Context, user: String, pass: String) {
        securePrefs.saveCredentials(user, pass)
        username.value = user
        password.value = pass
    }

    suspend fun authenticate(context: Context) =
        backendAuthService.authenticateAndSendCredHomeAsync(context)


    init {
        EventBus.getDefault().register(this)

        viewModelScope.launch(Dispatchers.IO) {
            val daos = dao.getAll()
            val fl =entitiesToEventViews(daos)
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
            DateTimeUtil.toWarsawDt(sorted.firstOrNull { it.entType == "entry" && "otwarta".contains(it.content) }?.timestamp ?: -1),
            DateTimeUtil.toWarsawDt(sorted.firstOrNull { it.entType == "entry" && "zamknięta".contains(it.content) }?.timestamp ?: -1),
            DateTimeUtil.toWarsawDt(sorted.firstOrNull { it.entType == "move" }?.timestamp ?: -1)
        )
    }


    override fun onCleared() {
        super.onCleared()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onTokenReceivedEvent(event: TokenEvent) {
        _token.value = event.token
        Log.d(ContentValues.TAG, "Token received:${event.token}")
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDateTimeEvent(event: EventWithDateTimeAndStatus) {
        viewModelScope.launch(Dispatchers.IO) {
            val entities = dao.getAll() //TODO:optimize me update only necessary, not whole
            val daos = dao.getAll()
            val fl = entitiesToEventViews(daos)
            _tableData.value = fl
            _lastRefreshed.value = DateTimeUtil.nowToWarsawDt()

            prefsDataUtil.updatePreferences(entities.map { e -> e.entType })

        }
    }

    private fun entitiesToEventViews(daos: List<NotificationEntity>): List<EventViewDto> = daos.mapIndexed { idx, entity ->

        val nextDt = daos.getOrNull(idx + 1)
        val isLastAtDay =
            nextDt != null && !DateTimeUtil.areTheSameDay(entity.timestamp, nextDt.timestamp)

        EventViewDto(
            entity.entFriendlyName,
            DateTimeUtil.toWarsawDt(entity.timestamp),
            entity.content,
            isLastAtDay
        )
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onHeartBeatEvent(event: HeartbeatEvent) {
        viewModelScope.launch(Dispatchers.IO) {
            _lastHeartBeat.value = event.timestamp

        }
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
    fun onPermEvent(event: PermEvent) {
        val newRow = Triple(event.type, event.dt, event.status)
        _permissionData.value = _permissionData.value + newRow
        Log.d(ContentValues.TAG, "onPermEvent:${event}")
    }

}