package pl.kmalysiak.personalnotificator.model.dao

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import pl.kmalysiak.personalnotificator.model.data.NotificationEntity

@Database(entities = [NotificationEntity::class], version = 1)
abstract class NotificatorDatabase : RoomDatabase() {
    abstract fun notificationEntityDao(): NotificationEntityDao

    companion object {
        @Volatile private var INSTANCE: NotificatorDatabase? = null
        fun getDatabase(context: Context): NotificatorDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext, NotificatorDatabase::class.java, "notificator_db")
                    .build().also { INSTANCE = it }
            }
        }
    }
}