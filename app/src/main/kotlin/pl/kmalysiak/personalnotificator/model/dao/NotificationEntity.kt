package pl.kmalysiak.personalnotificator.model.dao

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val entFriendlyName: String,
    val entType:String,
    val content: String,
    val timestamp: Long,
    val timestampReceived: Long
)