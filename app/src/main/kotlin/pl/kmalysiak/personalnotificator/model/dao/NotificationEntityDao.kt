package pl.kmalysiak.personalnotificator.model.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import pl.kmalysiak.personalnotificator.model.data.NotificationEntity

@Dao
interface NotificationEntityDao {
    @Insert
    suspend fun insert(note: NotificationEntity)

    @Query("SELECT * FROM notification ORDER BY id DESC")
    fun getAll(): Flow<List<NotificationEntity>>
}