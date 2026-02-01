package pl.kmalysiak.personalnotificator.main

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import pl.kmalysiak.personalnotificator.model.dao.NotificatorDatabase
import pl.kmalysiak.personalnotificator.model.data.NotificationEntity
import pl.kmalysiak.personalnotificator.model.event.EventWithDateTimeAndStatus
import pl.kmalysiak.personalnotificator.model.event.HeartbeatEvent
import pl.kmalysiak.personalnotificator.model.event.PermEvent
import pl.kmalysiak.personalnotificator.model.event.TokenEvent
import pl.kmalysiak.personalnotificator.model.secure.SecurePrefsService
import pl.kmalysiak.personalnotificator.preference.PreferencesDataUtil

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.collections.map

//
class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val _token = MutableStateFlow("Token unknown")
    private val _tableData = MutableStateFlow<List<Triple<String, LocalDateTime, String>>>(emptyList())


    private val _permissionData = MutableStateFlow<List<Triple<String, LocalDateTime, String>>>(emptyList())
    internal val token: StateFlow<String> get() = _token
    internal val tableData: StateFlow<List<Triple<String, LocalDateTime, String>>> = _tableData
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
        val (user, pass) = SecurePrefsService.loadCredentials(context)
        username.value = user
        password.value = pass
    }

    fun saveCredentials(context: Context, user: String, pass: String) {
        SecurePrefsService.saveCredentials(context, user, pass)
        username.value = user
        password.value = pass
    }


    init {
        EventBus.getDefault().register(this)

        viewModelScope.launch(Dispatchers.IO) {
            val daos = dao.getAll()
            val fl = daos.map { entity ->
                Triple(
                    entity.entFriendlyName,
                    toLocalDateTime(entity.timestamp),
                    entity.content
                )
            }
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
            toLocalDateTime(sorted.firstOrNull { it.entType == "entry" && "otwarta".contains(it.content) }?.timestamp ?: -1),
            toLocalDateTime(sorted.firstOrNull { it.entType == "entry" && "zamknięta".contains(it.content) }?.timestamp ?: -1),
            toLocalDateTime(sorted.firstOrNull { it.entType == "move" }?.timestamp ?: -1)
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
            val fl = dao.getAll().map { entity ->
                Triple(
                    entity.entFriendlyName,
                    toLocalDateTime(entity.timestamp),
                    entity.content
                )
            }
            _tableData.value = fl
            _lastRefreshed.value =  Instant.now()
                .atZone(ZoneId.of("Europe/Warsaw"))
                .toLocalDateTime()

            val prefs = PreferencesDataUtil.get()
            prefs.updatePreferences(fl.map { e -> e.first })

        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onHeartBeatEvent(event: HeartbeatEvent) {
        viewModelScope.launch(Dispatchers.IO) {
            _lastHeartBeat.value = event.timestamp

        }
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

    private fun toLocalDateTime(timestamp: Long): LocalDateTime = if(timestamp == -1L) LocalDateTime.MIN else Instant.ofEpochSecond(timestamp).atZone(ZoneId.of("Europe/Warsaw")).toLocalDateTime()

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPermEvent(event: PermEvent) {
        val newRow = Triple(event.type, event.dt, event.status)
        _permissionData.value = _permissionData.value + newRow
        Log.d(ContentValues.TAG, "onPermEvent:${event}")
    }

}