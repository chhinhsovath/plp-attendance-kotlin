package com.plp.attendance

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.plp.attendance.services.LocalizationManager
import com.plp.attendance.ui.navigation.NavigationGraph
import com.plp.attendance.ui.theme.PLPTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var localizationManager: LocalizationManager
    
    private var initialRoute by mutableStateOf<String?>(null)
    
    override fun attachBaseContext(newBase: Context) {
        // This will be called before onCreate, but we need injection first
        super.attachBaseContext(newBase)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize localization with Khmer as default
        localizationManager.initializeLocale()
        
        // Handle notification intent
        handleIntent(intent)
        
        setContent {
            PLPTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavigationGraph(
                        initialRoute = initialRoute
                    )
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
    
    private fun handleIntent(intent: Intent) {
        // Check if opened from notification
        val action = intent.getStringExtra("action")
        when (action) {
            "attendance" -> initialRoute = "attendance"
            "leave" -> initialRoute = "leave"
            "notifications" -> initialRoute = "notifications"
        }
    }
}
