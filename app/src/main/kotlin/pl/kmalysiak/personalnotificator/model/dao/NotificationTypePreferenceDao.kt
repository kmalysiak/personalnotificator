package pl.kmalysiak.personalnotificator.model.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface NotificationTypePreferenceDao {
    @Query("SELECT * FROM notification_type_preference ORDER BY entType")
    suspend fun getAll(): List<NotificationTypePreferenceEntity>

    @Query("SELECT * FROM notification_type_preference WHERE entType = :entType LIMIT 1")
    suspend fun getByType(entType: String): NotificationTypePreferenceEntity?

    // IGNORE -> no-op jeśli typ już ma wpis, więc bezpieczne do wywołania dla każdego zapisanego powiadomienia
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefaultIfAbsent(pref: NotificationTypePreferenceEntity)

    @Query("UPDATE notification_type_preference SET enabled = :enabled WHERE entType = :entType")
    suspend fun setEnabled(entType: String, enabled: Boolean)

    @Query("UPDATE notification_type_preference SET volume = :volume WHERE entType = :entType")
    suspend fun setVolume(entType: String, volume: Float)

    @Query("UPDATE notification_type_preference SET alarmEnabled = :alarmEnabled WHERE entType = :entType")
    suspend fun setAlarmEnabled(entType: String, alarmEnabled: Boolean)

    @Query("DELETE FROM notification_type_preference WHERE entType = :entType")
    suspend fun delete(entType: String)
}
