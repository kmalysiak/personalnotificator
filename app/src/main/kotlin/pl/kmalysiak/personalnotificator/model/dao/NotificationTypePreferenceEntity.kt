package pl.kmalysiak.personalnotificator.model.dao

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_type_preference")
data class NotificationTypePreferenceEntity(
    @PrimaryKey val entType: String,
    val enabled: Boolean = true,
    val volume: Float = 1.0f,
    val alarmEnabled: Boolean = false
)
