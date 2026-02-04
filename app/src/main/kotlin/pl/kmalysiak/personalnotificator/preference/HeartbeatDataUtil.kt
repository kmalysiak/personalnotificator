package pl.kmalysiak.personalnotificator.preference

import android.content.Context
import androidx.core.content.edit
import java.time.Instant


private const val LAST_CHECK_INSTANT = "last_check_instant"
private const val LAST_CHECK_STATUS = "last_check_status"

class HeartbeatDataUtil private constructor(context: Context) {

    private val prefs =
        context.getSharedPreferences("heartbeat_prefs", Context.MODE_PRIVATE)

    companion object {
        private lateinit var INSTANCE: HeartbeatDataUtil

        /** Call once from Application.onCreate() */
        fun init(context: Context) {
            if (!::INSTANCE.isInitialized) {
                INSTANCE = HeartbeatDataUtil(context.applicationContext)
            }
        }

        /** Access singleton anywhere */
        fun get(): HeartbeatDataUtil {
            if (!::INSTANCE.isInitialized) {
                throw IllegalStateException("HeartbeatDataUtil not initialized")
            }
            return INSTANCE
        }

    }

    fun updateHeartbeatState(checked: Instant, status: String): Boolean {
        val lastChecked = Instant.ofEpochSecond(get().prefs.getLong(LAST_CHECK_INSTANT, 0))
        val lastStatus = get().prefs.getString(LAST_CHECK_STATUS, "OK")

        get().prefs.edit() {
            putLong(LAST_CHECK_INSTANT, checked.toEpochMilli())
            putString(LAST_CHECK_STATUS, status)
        }

        return !lastStatus.equals(status)
    }
}
