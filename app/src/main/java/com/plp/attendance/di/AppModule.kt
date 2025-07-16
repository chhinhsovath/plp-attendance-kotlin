package com.plp.attendance.di

import android.content.Context
import com.plp.attendance.data.preferences.AppPreferences
import com.plp.attendance.services.LocalizationManager
import com.plp.attendance.utils.NetworkConnectivityManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideAppPreferences(
        @ApplicationContext context: Context
    ): AppPreferences {
        return AppPreferences(context)
    }
    
    @Provides
    @Singleton
    fun provideLocalizationManager(
        @ApplicationContext context: Context,
        appPreferences: AppPreferences
    ): LocalizationManager {
        return LocalizationManager(context, appPreferences)
    }
    
    @Provides
    @Singleton
    fun provideNetworkConnectivityManager(
        @ApplicationContext context: Context
    ): NetworkConnectivityManager {
        return NetworkConnectivityManager(context)
    }
}