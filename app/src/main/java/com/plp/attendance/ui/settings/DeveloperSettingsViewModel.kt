package com.plp.attendance.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plp.attendance.data.local.PLPDatabase
import com.plp.attendance.data.local.TestDataSeeder
import com.plp.attendance.data.preferences.DeveloperPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeveloperSettingsUiState(
    val isDeveloperModeEnabled: Boolean = false,
    val useMockAuth: Boolean = false,
    val showTestAccounts: Boolean = false,
    val isSeeding: Boolean = false,
    val message: String? = null,
    val apiUrl: String = "http://137.184.109.21:3000"
)

@HiltViewModel
class DeveloperSettingsViewModel @Inject constructor(
    private val developerPreferences: DeveloperPreferences,
    private val testDataSeeder: TestDataSeeder,
    private val database: PLPDatabase
) : ViewModel() {
    
    private val _message = MutableStateFlow<String?>(null)
    
    val uiState: StateFlow<DeveloperSettingsUiState> = combine(
        developerPreferences.isDeveloperModeEnabled,
        developerPreferences.useMockAuth,
        developerPreferences.showTestAccounts,
        _message
    ) { isDeveloperMode, useMockAuth, showTestAccounts, message ->
        DeveloperSettingsUiState(
            isDeveloperModeEnabled = isDeveloperMode,
            useMockAuth = useMockAuth,
            showTestAccounts = showTestAccounts,
            message = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DeveloperSettingsUiState()
    )
    
    fun setDeveloperMode(enabled: Boolean) {
        viewModelScope.launch {
            developerPreferences.setDeveloperMode(enabled)
            _message.value = if (enabled) {
                "Developer mode enabled. Test features are now available."
            } else {
                "Developer mode disabled. Using production settings."
            }
        }
    }
    
    fun setUseMockAuth(enabled: Boolean) {
        viewModelScope.launch {
            developerPreferences.setUseMockAuth(enabled)
            _message.value = if (enabled) {
                "Using local SQLite authentication"
            } else {
                "Using remote API authentication"
            }
        }
    }
    
    fun setShowTestAccounts(show: Boolean) {
        viewModelScope.launch {
            developerPreferences.setShowTestAccounts(show)
        }
    }
    
    fun seedDatabase() {
        viewModelScope.launch {
            try {
                _message.value = "Seeding test data..."
                testDataSeeder.seedTestData()
                _message.value = "Test data seeded successfully!"
            } catch (e: Exception) {
                _message.value = "Failed to seed data: ${e.message}"
            }
        }
    }
    
    fun clearDatabase() {
        viewModelScope.launch {
            try {
                // Clear all tables
                database.clearAllTables()
                _message.value = "Database cleared successfully!"
            } catch (e: Exception) {
                _message.value = "Failed to clear database: ${e.message}"
            }
        }
    }
    
    fun clearMessage() {
        _message.value = null
    }
}