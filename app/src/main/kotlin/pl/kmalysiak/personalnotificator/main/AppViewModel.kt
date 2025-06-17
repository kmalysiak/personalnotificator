package pl.kmalysiak.personalnotificator.main

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import pl.kmalysiak.personalnotificator.model.dao.NotificatorDatabase
import pl.kmalysiak.personalnotificator.model.data.NotificationEntity
import pl.kmalysiak.personalnotificator.model.event.EventWithDateTimeAndStatus
import pl.kmalysiak.personalnotificator.model.event.PermEvent
import pl.kmalysiak.personalnotificator.model.event.TokenEvent
import pl.kmalysiak.personalnotificator.model.secure.SecurePrefsService
import java.time.LocalDateTime

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
        val newRow = Triple(event.type, event.dt, event.status)
        _tableData.value = _tableData.value + newRow
        //  Log.d(TAG, "Topic event received:${event.msg}")
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPermEvent(event: PermEvent) {
        val newRow = Triple(event.type, event.dt, event.status)
        _permissionData.value = _permissionData.value + newRow
        Log.d(ContentValues.TAG, "onPermEvent:${event}")
    }

    val notifications: StateFlow<List<NotificationEntity>> =
        dao.getAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}