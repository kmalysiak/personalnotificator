package pl.kmalysiak.personalnotificator.model.dao

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationTypePreferenceRepository @Inject constructor(
    private val dao: NotificationTypePreferenceDao
) {
    suspend fun getAll(): List<NotificationTypePreferenceEntity> = dao.getAll()

    suspend fun getByType(entType: String): NotificationTypePreferenceEntity? = dao.getByType(entType)

    // domyślne dla nowego typu: włączone, max głośność, brak alarmu
    suspend fun ensureDefault(entType: String) = dao.insertDefaultIfAbsent(NotificationTypePreferenceEntity(entType = entType))

    suspend fun setEnabled(entType: String, enabled: Boolean) = dao.setEnabled(entType, enabled)

    suspend fun setVolume(entType: String, volume: Float) = dao.setVolume(entType, volume)

    suspend fun setAlarmEnabled(entType: String, alarmEnabled: Boolean) = dao.setAlarmEnabled(entType, alarmEnabled)

    suspend fun delete(entType: String) = dao.delete(entType)
}
