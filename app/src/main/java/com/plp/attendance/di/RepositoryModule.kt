package com.plp.attendance.di

import com.plp.attendance.data.local.PLPDatabase
import com.plp.attendance.data.local.SessionManager
import com.plp.attendance.data.local.TestDataSeeder
import com.plp.attendance.data.local.TestDataSeederV2
import com.plp.attendance.data.preferences.DeveloperPreferences
import com.plp.attendance.data.remote.api.AttendanceApi
import com.plp.attendance.data.remote.api.AuthApi
import com.plp.attendance.data.remote.api.LeaveApi
import com.plp.attendance.data.remote.api.UserApi
import com.plp.attendance.services.LocationService
import com.plp.attendance.data.repository.AttendanceRepositoryImpl
import com.plp.attendance.data.repository.AuthRepositoryImpl
import com.plp.attendance.data.repository.LeaveRepositoryImpl
import com.plp.attendance.data.repository.MockAuthRepository
import com.plp.attendance.data.repository.RemoteAuthRepository
import com.plp.attendance.data.repository.UserRepositoryImpl
import com.plp.attendance.domain.repository.AttendanceRepository
import com.plp.attendance.domain.repository.AuthRepository
import com.plp.attendance.domain.repository.LeaveRepository
import com.plp.attendance.domain.repository.UserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    @Provides
    @Singleton
    fun provideAuthRepository(
        authApi: AuthApi,
        database: PLPDatabase,
        sessionManager: SessionManager,
        testDataSeeder: TestDataSeeder
    ): AuthRepository {
        // Use RemoteAuthRepository for real API authentication
        return RemoteAuthRepository(authApi, database, sessionManager, testDataSeeder)
    }
    
    @Provides
    @Singleton
    fun provideAttendanceRepository(
        attendanceApi: AttendanceApi,
        database: PLPDatabase,
        sessionManager: SessionManager,
        locationService: LocationService
    ): AttendanceRepository {
        return AttendanceRepositoryImpl(
            attendanceApi = attendanceApi,
            attendanceDao = database.attendanceDao(),
            sessionManager = sessionManager,
            locationService = locationService,
            userDao = database.userDao()
        )
    }
    
    @Provides
    @Singleton
    fun provideUserRepository(
        userApi: UserApi,
        database: PLPDatabase,
        sessionManager: SessionManager
    ): UserRepository {
        return UserRepositoryImpl(
            userApi = userApi,
            userDao = database.userDao(),
            sessionManager = sessionManager,
            database = database
        )
    }
    
    @Provides
    @Singleton
    fun provideLeaveRepository(
        leaveApi: LeaveApi,
        database: PLPDatabase,
        sessionManager: SessionManager
    ): LeaveRepository {
        return LeaveRepositoryImpl(
            leaveApi = leaveApi,
            leaveDao = database.leaveDao(),
            sessionManager = sessionManager
        )
    }
}