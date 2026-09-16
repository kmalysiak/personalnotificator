package pl.kmalysiak.personalnotificator.model.dao

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import org.slf4j.LoggerFactory
import pl.kmalysiak.personalnotificator.appstate.AppStateRepository
import pl.kmalysiak.personalnotificator.main.DateTimeUtil
import pl.kmalysiak.personalnotificator.model.dao.NotificationEntity
import pl.kmalysiak.personalnotificator.appstate.SecurePrefsService
import pl.kmalysiak.personalnotificator.ws.WebSocketService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val dao: NotificationEntityDao,
    private val appStateRepo: AppStateRepository,
    private val securePrefs: SecurePrefsService,
    @ApplicationContext private val context: Context
) {
    private val log = LoggerFactory.getLogger(NotificationRepository::class.java)

    suspend fun getAll(): List<NotificationEntity> = dao.getAll()

    suspend fun deleteAll() {
        dao.deleteALl()
    }

    // zwraca false jeśli to retransmisja tej samej treści (hash już w bazie) - insert zignorowany
    suspend fun save(notification: NotificationEntity): Boolean {
        val rowId = dao.insert(notification)
        if (rowId == -1L) {
            log.info("Zignorowano duplikat powiadomienia (hash={})", notification.hash)
            return false
        }
        dao.keepLatest500()
        appStateRepo.emitNotificationSaved()
        if (notification.entType == "reconnect_request") {
            appStateRepo.setLastReconnectRequest(DateTimeUtil.Companion.toWarsawDt(notification.timestamp))
            val token = securePrefs.loadIdToken()
            ContextCompat.startForegroundService(
                context,
                Intent(context, WebSocketService::class.java).putExtra("idToken", token)
            )
            log.info("reconnect_request: restarting WebSocketService")
        }
        return true
    }
}