package pl.kmalysiak.personalnotificator.preference

import android.content.Context
import androidx.core.content.edit
import java.time.Instant


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
        val lastChecked = Instant.ofEpochSecond(get().prefs.getLong("last_check_instant", 0))
        val lastStatus = get().prefs.getString("last_check_state", "OK")

        get().prefs.edit() {
            putLong("last_check_instant", checked.toEpochMilli())
            putString("last_check_status", status)
        }

        return !lastStatus.equals(status)
    }
}
