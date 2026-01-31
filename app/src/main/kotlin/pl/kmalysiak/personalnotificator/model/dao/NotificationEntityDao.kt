package pl.kmalysiak.personalnotificator.model.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import pl.kmalysiak.personalnotificator.model.data.NotificationEntity

@Dao
interface NotificationEntityDao {
    @Insert
    suspend fun insert(note: NotificationEntity)

    @Query("SELECT * FROM notification ORDER BY timestamp DESC")
    fun getAll(): List<NotificationEntity>


    @Query("""
DELETE FROM notification
WHERE id NOT IN (
    SELECT id FROM notification
    ORDER BY timestamp DESC
    LIMIT 500
)
""")
    fun keepLatest500()
}