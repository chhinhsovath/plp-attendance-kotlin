package com.plp.attendance.di

import android.content.Context
import androidx.room.Room
import com.plp.attendance.data.local.PLPDatabase
import com.plp.attendance.data.local.dao.AttendanceDao
import com.plp.attendance.data.local.dao.LeaveDao
import com.plp.attendance.data.local.dao.UserDao
import com.plp.attendance.data.local.dao.SyncQueueDao
import com.plp.attendance.data.local.dao.NotificationDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideApplicationContext(@ApplicationContext context: Context): Context = context

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PLPDatabase {
        return Room.databaseBuilder(
            context,
            PLPDatabase::class.java,
            PLPDatabase.DATABASE_NAME
        )
        .fallbackToDestructiveMigration() // For testing, allow destructive migration
        .build()
    }

    @Provides
    fun provideUserDao(database: PLPDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    fun provideAttendanceDao(database: PLPDatabase): AttendanceDao {
        return database.attendanceDao()
    }

    @Provides
    fun provideLeaveDao(database: PLPDatabase): LeaveDao {
        return database.leaveDao()
    }
    
    @Provides
    fun provideSyncQueueDao(database: PLPDatabase): SyncQueueDao {
        return database.syncQueueDao()
    }
    
    @Provides
    fun provideNotificationDao(database: PLPDatabase): NotificationDao {
        return database.notificationDao()
    }
}