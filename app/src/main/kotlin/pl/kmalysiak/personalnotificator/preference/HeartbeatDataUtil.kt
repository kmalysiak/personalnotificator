package pl.kmalysiak.personalnotificator.preference

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

private const val LAST_CHECK_INSTANT = "last_check_instant"
private const val LAST_CHECK_STATUS = "last_check_status"

@Singleton
class HeartbeatDataUtil @Inject constructor(@ApplicationContext context: Context) {

    private val prefs = context.getSharedPreferences("heartbeat_prefs", Context.MODE_PRIVATE)

    fun updateHeartbeatState(checked: Instant, status: String): Boolean {
        val lastStatus = prefs.getString(LAST_CHECK_STATUS, "OK")
        prefs.edit {
            putLong(LAST_CHECK_INSTANT, checked.toEpochMilli())
            putString(LAST_CHECK_STATUS, status)
        }
        return !lastStatus.equals(status)
    }
}
