package pl.kmalysiak.personalnotificator.model.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import pl.kmalysiak.personalnotificator.model.dao.NotificationEntity

@Dao
interface NotificationEntityDao {
    // IGNORE + unique index na hash -> zwraca -1 gdy to duplikat treści
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(note: NotificationEntity): Long

    @Query("SELECT * FROM notification ORDER BY timestamp DESC")
    suspend fun getAll(): List<NotificationEntity>

    @Query("DELETE FROM notification")
    suspend fun deleteALl()

    @Query("""
DELETE FROM notification
WHERE id NOT IN (
    SELECT id FROM notification
    ORDER BY timestamp DESC
    LIMIT 500
)
""")
    suspend fun keepLatest500()
}