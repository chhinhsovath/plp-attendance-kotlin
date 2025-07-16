package com.plp.attendance.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.plp.attendance.services.BiometricSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

@Singleton
class AppPreferences @Inject constructor(
    private val context: Context
) {
    private val FIRST_RUN_KEY = booleanPreferencesKey("first_run_complete")
    private val DEMO_MODE_KEY = booleanPreferencesKey("use_demo_mode")
    private val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
    private val USER_ID = stringPreferencesKey("user_id")
    private val USER_EMAIL = stringPreferencesKey("user_email")
    private val USER_ROLE = stringPreferencesKey("user_role")
    private val LANGUAGE = stringPreferencesKey("language")
    private val THEME = stringPreferencesKey("theme")
    private val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
    private val BIOMETRIC_ATTENDANCE_ENABLED = booleanPreferencesKey("biometric_attendance_enabled")
    private val BIOMETRIC_SENSITIVE_ENABLED = booleanPreferencesKey("biometric_sensitive_enabled")
    private val BIOMETRIC_FALLBACK_PIN = booleanPreferencesKey("biometric_fallback_pin")
    private val BIOMETRIC_TIMEOUT_MINUTES = intPreferencesKey("biometric_timeout_minutes")
    private val LOCATION_ENABLED = booleanPreferencesKey("location_enabled")
    private val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    
    val isFirstRunComplete: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[FIRST_RUN_KEY] ?: false
    }
    
    val useDemoMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DEMO_MODE_KEY] ?: true
    }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_LOGGED_IN] ?: false
    }

    val userId: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_ID] ?: ""
    }

    val userEmail: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_EMAIL] ?: ""
    }

    val userRole: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_ROLE] ?: ""
    }

    val language: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[LANGUAGE] ?: "km"  // Default to Khmer
    }

    val theme: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[THEME] ?: "system"
    }

    val isBiometricEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[BIOMETRIC_ENABLED] ?: false
    }
    
    val isBiometricEnabledForAttendance: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[BIOMETRIC_ATTENDANCE_ENABLED] ?: false
    }
    
    val isBiometricEnabledForSensitiveActions: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[BIOMETRIC_SENSITIVE_ENABLED] ?: false
    }
    
    val biometricSettings: Flow<BiometricSettings> = context.dataStore.data.map { preferences ->
        BiometricSettings(
            isEnabledForAttendance = preferences[BIOMETRIC_ATTENDANCE_ENABLED] ?: false,
            isEnabledForSensitiveActions = preferences[BIOMETRIC_SENSITIVE_ENABLED] ?: false,
            allowFallbackToPin = preferences[BIOMETRIC_FALLBACK_PIN] ?: true,
            reAuthenticationTimeoutMinutes = preferences[BIOMETRIC_TIMEOUT_MINUTES] ?: 15
        )
    }

    val isLocationEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[LOCATION_ENABLED] ?: true
    }

    val isNotificationsEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[NOTIFICATIONS_ENABLED] ?: true
    }
    
    suspend fun setFirstRunComplete() {
        context.dataStore.edit { preferences ->
            preferences[FIRST_RUN_KEY] = true
        }
    }
    
    suspend fun setUseDemoMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DEMO_MODE_KEY] = enabled
        }
    }

    suspend fun setLoggedIn(isLoggedIn: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN] = isLoggedIn
        }
    }

    suspend fun setUserInfo(userId: String, email: String, role: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID] = userId
            preferences[USER_EMAIL] = email
            preferences[USER_ROLE] = role
        }
    }

    suspend fun setLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE] = language
        }
    }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME] = theme
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BIOMETRIC_ENABLED] = enabled
        }
    }
    
    suspend fun setBiometricEnabledForAttendance(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BIOMETRIC_ATTENDANCE_ENABLED] = enabled
        }
    }
    
    suspend fun setBiometricEnabledForSensitiveActions(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BIOMETRIC_SENSITIVE_ENABLED] = enabled
        }
    }
    
    suspend fun updateBiometricSettings(settings: BiometricSettings) {
        context.dataStore.edit { preferences ->
            preferences[BIOMETRIC_ATTENDANCE_ENABLED] = settings.isEnabledForAttendance
            preferences[BIOMETRIC_SENSITIVE_ENABLED] = settings.isEnabledForSensitiveActions
            preferences[BIOMETRIC_FALLBACK_PIN] = settings.allowFallbackToPin
            preferences[BIOMETRIC_TIMEOUT_MINUTES] = settings.reAuthenticationTimeoutMinutes
        }
    }

    suspend fun setLocationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[LOCATION_ENABLED] = enabled
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun clear() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}