package dev.reynardus.flinkly.di

import android.content.Context
import androidx.room.Room
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.reynardus.flinkly.data.local.AppDatabase
import dev.reynardus.flinkly.data.local.dao.HouseholdDao
import dev.reynardus.flinkly.data.local.dao.RoomDao
import dev.reynardus.flinkly.data.local.dao.TaskDao
import dev.reynardus.flinkly.data.remote.ApiClient
import dev.reynardus.flinkly.data.remote.ApiService
import dev.reynardus.flinkly.data.store.PreferencesStore
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providePreferencesStore(@ApplicationContext context: Context): PreferencesStore =
        PreferencesStore(context)

    @Provides
    @Singleton
    fun provideApiClient(preferencesStore: PreferencesStore): ApiClient =
        ApiClient(preferencesStore)

    @Provides
    @Singleton
    fun provideApiService(apiClient: ApiClient): ApiService =
        apiClient.service

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "flinkly.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideHouseholdDao(db: AppDatabase): HouseholdDao = db.householdDao()
    @Provides fun provideRoomDao(db: AppDatabase): RoomDao = db.roomDao()
    @Provides fun provideTaskDao(db: AppDatabase): TaskDao = db.taskDao()
}
