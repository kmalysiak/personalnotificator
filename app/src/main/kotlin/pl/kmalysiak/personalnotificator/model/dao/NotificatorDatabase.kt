package pl.kmalysiak.personalnotificator.model.dao

import androidx.room.Database
import androidx.room.RoomDatabase
import pl.kmalysiak.personalnotificator.model.dao.NotificationEntity

@Database(entities = [NotificationEntity::class], version = 5)
abstract class NotificatorDatabase : RoomDatabase() {
    abstract fun notificationEntityDao(): NotificationEntityDao
}