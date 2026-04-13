package pl.kmalysiak.personalnotificator.model.dao

import androidx.room.Database
import androidx.room.RoomDatabase
import pl.kmalysiak.personalnotificator.model.data.NotificationEntity

@Database(entities = [NotificationEntity::class], version = 3)
abstract class NotificatorDatabase : RoomDatabase() {
    abstract fun notificationEntityDao(): NotificationEntityDao
}