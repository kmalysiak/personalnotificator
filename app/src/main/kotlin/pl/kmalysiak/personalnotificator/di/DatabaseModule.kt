package pl.kmalysiak.personalnotificator.di

import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import pl.kmalysiak.personalnotificator.config.AppConfig
import pl.kmalysiak.personalnotificator.model.dao.NotificationEntityDao
import pl.kmalysiak.personalnotificator.model.dao.NotificatorDatabase
import java.nio.charset.StandardCharsets
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppConfig(@ApplicationContext context: Context): AppConfig {
        val json = context.assets.open("config.json")
            .bufferedReader(StandardCharsets.UTF_8)
            .use { it.readText() }
        return Gson().fromJson(json, AppConfig::class.java)
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NotificatorDatabase =
        Room.databaseBuilder(context, NotificatorDatabase::class.java, "notificator_db")
            .fallbackToDestructiveMigration(true)
            .build()

    @Provides
    fun provideDao(db: NotificatorDatabase): NotificationEntityDao =
        db.notificationEntityDao()
}
