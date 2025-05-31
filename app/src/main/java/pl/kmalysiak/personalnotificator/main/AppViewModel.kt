package pl.kmalysiak.personalnotificator.main

import android.content.ContentValues
import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import pl.kmalysiak.personalnotificator.events.EventWithDateTimeAndStatus
import pl.kmalysiak.personalnotificator.events.PermEvent
import pl.kmalysiak.personalnotificator.events.TokenEvent
import java.time.LocalDateTime

class AppViewModel : ViewModel() {
    private val _token = MutableStateFlow("Token unknown")
    private val _tableData =
        MutableStateFlow<List<Triple<String, LocalDateTime, String>>>(emptyList())
    private val _permissionData =
        MutableStateFlow<List<Triple<String, LocalDateTime, String>>>(emptyList())

    val token: StateFlow<String> get() = _token
    val tableData: StateFlow<List<Triple<String, LocalDateTime, String>>> = _tableData
    val permissionData: StateFlow<List<Triple<String, LocalDateTime, String>>> = _permissionData
    private val _lastGateOpened = MutableStateFlow(LocalDateTime.of(2025, 1, 1, 1, 2, 3))
    val lastGateOpened: StateFlow<LocalDateTime> get() = _lastGateOpened
    private val _lastGateClosed = MutableStateFlow(LocalDateTime.of(2025, 1, 2, 2, 3, 4))
    val lastGateClosed: StateFlow<LocalDateTime> get() = _lastGateClosed
    private val _lastMovement = MutableStateFlow(LocalDateTime.of(2025, 1, 3, 3, 4, 5))
    val lastMovement: StateFlow<LocalDateTime> get() = _lastMovement


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
}