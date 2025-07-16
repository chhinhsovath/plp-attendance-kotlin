package com.plp.attendance

import android.app.Application
import android.content.Context
import android.content.res.Configuration as ResConfiguration
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.plp.attendance.data.sync.SyncManager
import com.plp.attendance.services.LocalizationManager
// import com.plp.attendance.utils.Logger
// import com.plp.attendance.utils.PerformanceMonitor
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import java.util.Locale
import javax.inject.Inject

@HiltAndroidApp
class PLPApplication : Application(), Configuration.Provider {
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    @Inject
    lateinit var localizationManager: LocalizationManager
    
    @Inject
    lateinit var syncManager: SyncManager
    
    override fun attachBaseContext(base: Context) {
        // Set default locale to Khmer before anything else
        val config = ResConfiguration(base.resources.configuration)
        config.setLocale(Locale("km", "KH"))
        val context = base.createConfigurationContext(config)
        super.attachBaseContext(context)
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize localization with Khmer as default
        localizationManager.initializeLocale()
        
        // Initialize WorkManager
        WorkManager.initialize(
            this,
            workManagerConfiguration
        )
        
        // Start automatic data sync
        syncManager.startAutoSync()
        
        android.util.Log.i("PLPApplication", "Application initialized successfully with sync")
    }
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(
                if (BuildConfig.DEBUG) android.util.Log.DEBUG else android.util.Log.INFO
            )
            .setWorkerFactory(workerFactory)
            .build()
}
