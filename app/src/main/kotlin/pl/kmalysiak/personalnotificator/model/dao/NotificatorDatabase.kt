package pl.kmalysiak.personalnotificator.model.dao

import androidx.room.Database
import androidx.room.RoomDatabase
import pl.kmalysiak.personalnotificator.model.dao.NotificationEntity

@Database(entities = [NotificationEntity::class, NotificationTypePreferenceEntity::class], version = 6)
abstract class NotificatorDatabase : RoomDatabase() {
    abstract fun notificationEntityDao(): NotificationEntityDao
    abstract fun notificationTypePreferenceDao(): NotificationTypePreferenceDao
}