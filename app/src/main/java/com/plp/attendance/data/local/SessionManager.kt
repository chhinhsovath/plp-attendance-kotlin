package com.plp.attendance.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.sessionDataStore
    
    companion object {
        private val USER_ID = stringPreferencesKey("user_id")
        private val USER_EMAIL = stringPreferencesKey("user_email")
        private val USER_NAME = stringPreferencesKey("user_name")
        private val USER_ROLE = stringPreferencesKey("user_role")
        private val AUTH_TOKEN = stringPreferencesKey("auth_token")
        private val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    }
    
    suspend fun saveSession(
        userId: String,
        userEmail: String,
        userName: String,
        userRole: String,
        token: String,
        refreshToken: String
    ) {
        dataStore.edit { preferences ->
            preferences[USER_ID] = userId
            preferences[USER_EMAIL] = userEmail
            preferences[USER_NAME] = userName
            preferences[USER_ROLE] = userRole
            preferences[AUTH_TOKEN] = token
            preferences[REFRESH_TOKEN] = refreshToken
        }
    }
    
    suspend fun clearSession() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
    
    fun getUserId(): Flow<String?> = dataStore.data.map { preferences ->
        preferences[USER_ID]
    }
    
    fun getUserEmail(): Flow<String?> = dataStore.data.map { preferences ->
        preferences[USER_EMAIL]
    }
    
    fun getUserName(): Flow<String?> = dataStore.data.map { preferences ->
        preferences[USER_NAME]
    }
    
    fun getUserRole(): Flow<String?> = dataStore.data.map { preferences ->
        preferences[USER_ROLE]
    }
    
    fun getAuthToken(): Flow<String?> = dataStore.data.map { preferences ->
        preferences[AUTH_TOKEN]
    }
    
    fun getRefreshToken(): Flow<String?> = dataStore.data.map { preferences ->
        preferences[REFRESH_TOKEN]
    }
    
    suspend fun updateToken(newToken: String) {
        dataStore.edit { preferences ->
            preferences[AUTH_TOKEN] = newToken
        }
    }
    
    suspend fun isLoggedIn(): Boolean {
        return dataStore.data.map { preferences ->
            preferences[USER_ID] != null && preferences[AUTH_TOKEN] != null
        }.first()
    }
    
    suspend fun getUserIdSuspend(): String? {
        return dataStore.data.map { preferences ->
            preferences[USER_ID]
        }.first()
    }
    
    suspend fun getUserEmailSuspend(): String? {
        return dataStore.data.map { preferences ->
            preferences[USER_EMAIL]
        }.first()
    }
    
    suspend fun getUserRoleSuspend(): String? {
        return dataStore.data.map { preferences ->
            preferences[USER_ROLE]
        }.first()
    }
    
    suspend fun getAuthTokenSuspend(): String? {
        return dataStore.data.map { preferences ->
            preferences[AUTH_TOKEN]
        }.first()
    }
    
    suspend fun getRefreshTokenSuspend(): String? {
        return dataStore.data.map { preferences ->
            preferences[REFRESH_TOKEN]
        }.first()
    }
    
    suspend fun updateAuthToken(newToken: String) {
        dataStore.edit { preferences ->
            preferences[AUTH_TOKEN] = newToken
        }
    }
}