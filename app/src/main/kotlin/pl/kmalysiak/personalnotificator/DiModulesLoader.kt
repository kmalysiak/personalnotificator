package pl.kmalysiak.personalnotificator

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import pl.kmalysiak.personalnotificator.model.dao.NotificationEntityDao
import pl.kmalysiak.personalnotificator.model.dao.NotificationTypePreferenceDao
import pl.kmalysiak.personalnotificator.model.dao.NotificatorDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DiModulesLoader {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NotificatorDatabase =
        Room.databaseBuilder(context, NotificatorDatabase::class.java, "notificator_db")
            .fallbackToDestructiveMigration(true)
            .build()

    @Provides
    fun provideDao(db: NotificatorDatabase): NotificationEntityDao =
        db.notificationEntityDao()

    @Provides
    fun provideTypePreferenceDao(db: NotificatorDatabase): NotificationTypePreferenceDao =
        db.notificationTypePreferenceDao()
}