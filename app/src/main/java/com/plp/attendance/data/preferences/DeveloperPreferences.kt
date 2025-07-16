package com.plp.attendance.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

private val Context.developerDataStore: DataStore<Preferences> by preferencesDataStore(name = "developer_preferences")

@Singleton
class DeveloperPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.developerDataStore
    companion object {
        private val USE_MOCK_AUTH = booleanPreferencesKey("use_mock_auth")
        private val SHOW_TEST_ACCOUNTS = booleanPreferencesKey("show_test_accounts")
        private val ENABLE_DEVELOPER_MODE = booleanPreferencesKey("enable_developer_mode")
    }
    
    val useMockAuth: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[USE_MOCK_AUTH] ?: false
    }
    
    val showTestAccounts: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[SHOW_TEST_ACCOUNTS] ?: false
    }
    
    val isDeveloperModeEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[ENABLE_DEVELOPER_MODE] ?: false
    }
    
    suspend fun setUseMockAuth(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[USE_MOCK_AUTH] = enabled
        }
    }
    
    suspend fun setShowTestAccounts(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[SHOW_TEST_ACCOUNTS] = show
        }
    }
    
    suspend fun setDeveloperMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[ENABLE_DEVELOPER_MODE] = enabled
            // When enabling developer mode, also enable test features
            if (enabled) {
                preferences[USE_MOCK_AUTH] = true
                preferences[SHOW_TEST_ACCOUNTS] = true
            }
        }
    }
}