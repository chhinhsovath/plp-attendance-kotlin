package com.plp.attendance.startup

import android.content.Context
import androidx.startup.Initializer
import com.plp.attendance.data.local.PLPDatabase
import com.plp.attendance.data.local.TestDataSeeder
import com.plp.attendance.data.preferences.DeveloperPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import android.util.Log
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore by preferencesDataStore(name = "settings")

class TestDataInitializer : Initializer<Unit> {
    
    override fun create(context: Context) {
        Log.d("TestDataInitializer", "Checking if test data needs to be seeded...")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = PLPDatabase.getInstance(context)
                val developerPreferences = DeveloperPreferences(context)
                val testDataSeeder = TestDataSeeder(database)
                
                // Check if developer mode is enabled
                val isDeveloperMode = developerPreferences.isDeveloperModeEnabled.first()
                
                if (isDeveloperMode) {
                    // Check if database is empty
                    val userCount = database.userDao().getUserCount()
                    
                    if (userCount == 0) {
                        Log.i("TestDataInitializer", "Seeding test data for first run...")
                        testDataSeeder.seedTestData()
                        Log.i("TestDataInitializer", "Test data seeded successfully!")
                    } else {
                        Log.d("TestDataInitializer", "Test data already exists (found $userCount users)")
                    }
                } else {
                    Log.d("TestDataInitializer", "Developer mode is disabled, skipping test data")
                }
            } catch (e: Exception) {
                Log.e("TestDataInitializer", "Error during test data initialization", e)
            }
        }
    }
    
    override fun dependencies(): List<Class<out Initializer<*>>> {
        // No dependencies
        return emptyList()
    }
}