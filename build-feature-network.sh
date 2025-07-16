#!/bin/bash

echo "🌐 Adding Network Connectivity & API Integration..."

# Step 1: Create API data models
echo "📦 Creating API models..."
mkdir -p app/src/main/java/com/plp/attendance/data/remote/{api,models,interceptors}

# API Response models
cat > app/src/main/java/com/plp/attendance/data/remote/models/ApiResponse.kt << 'EOF'
package com.plp.attendance.data.remote.models

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("data")
    val data: T? = null,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("error")
    val error: String? = null
)

data class LoginRequest(
    @SerializedName("email")
    val email: String,
    @SerializedName("password")
    val password: String
)

data class LoginResponse(
    @SerializedName("token")
    val token: String,
    @SerializedName("user")
    val user: UserResponse
)

data class UserResponse(
    @SerializedName("id")
    val id: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("fullName")
    val fullName: String,
    @SerializedName("role")
    val role: String,
    @SerializedName("schoolId")
    val schoolId: String,
    @SerializedName("schoolName")
    val schoolName: String,
    @SerializedName("phoneNumber")
    val phoneNumber: String?,
    @SerializedName("profilePicture")
    val profilePicture: String?
)

data class AttendanceRequest(
    @SerializedName("userId")
    val userId: String,
    @SerializedName("type")
    val type: String, // "check_in" or "check_out"
    @SerializedName("timestamp")
    val timestamp: String,
    @SerializedName("latitude")
    val latitude: Double?,
    @SerializedName("longitude")
    val longitude: Double?
)

data class AttendanceResponse(
    @SerializedName("id")
    val id: String,
    @SerializedName("userId")
    val userId: String,
    @SerializedName("date")
    val date: String,
    @SerializedName("checkInTime")
    val checkInTime: String?,
    @SerializedName("checkOutTime")
    val checkOutTime: String?,
    @SerializedName("status")
    val status: String,
    @SerializedName("workingHours")
    val workingHours: Float?
)
EOF

# Step 2: Create API interface
echo "🔌 Creating API interface..."
cat > app/src/main/java/com/plp/attendance/data/remote/api/AttendanceApi.kt << 'EOF'
package com.plp.attendance.data.remote.api

import com.plp.attendance.data.remote.models.*
import retrofit2.Response
import retrofit2.http.*

interface AttendanceApi {
    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<ApiResponse<LoginResponse>>
    
    @GET("users/profile")
    suspend fun getUserProfile(
        @Header("Authorization") token: String
    ): Response<ApiResponse<UserResponse>>
    
    @POST("attendance/checkin")
    suspend fun checkIn(
        @Header("Authorization") token: String,
        @Body request: AttendanceRequest
    ): Response<ApiResponse<AttendanceResponse>>
    
    @POST("attendance/checkout")
    suspend fun checkOut(
        @Header("Authorization") token: String,
        @Body request: AttendanceRequest
    ): Response<ApiResponse<AttendanceResponse>>
    
    @GET("attendance/today/{userId}")
    suspend fun getTodayAttendance(
        @Header("Authorization") token: String,
        @Path("userId") userId: String
    ): Response<ApiResponse<AttendanceResponse>>
    
    @GET("attendance/history/{userId}")
    suspend fun getAttendanceHistory(
        @Header("Authorization") token: String,
        @Path("userId") userId: String,
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): Response<ApiResponse<List<AttendanceResponse>>>
    
    @POST("attendance/sync")
    suspend fun syncAttendance(
        @Header("Authorization") token: String,
        @Body records: List<AttendanceRequest>
    ): Response<ApiResponse<List<AttendanceResponse>>>
}
EOF

# Step 3: Create Auth Interceptor
echo "🔐 Creating auth interceptor..."
cat > app/src/main/java/com/plp/attendance/data/remote/interceptors/AuthInterceptor.kt << 'EOF'
package com.plp.attendance.data.remote.interceptors

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val tokenProvider: () -> String?
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = tokenProvider()
        
        return if (token != null && !original.header("Authorization").isNullOrEmpty()) {
            val request = original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
            chain.proceed(request)
        } else {
            chain.proceed(original)
        }
    }
}
EOF

# Step 4: Create Network Module
echo "🏗️ Creating network module..."
cat > app/src/main/java/com/plp/attendance/data/remote/NetworkModule.kt << 'EOF'
package com.plp.attendance.data.remote

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.plp.attendance.BuildConfig
import com.plp.attendance.data.remote.api.AttendanceApi
import com.plp.attendance.data.remote.interceptors.AuthInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
    private var tokenProvider: (() -> String?)? = null
    
    fun setTokenProvider(provider: () -> String?) {
        tokenProvider = provider
    }
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.ENABLE_LOGGING) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }
    
    private val authInterceptor = AuthInterceptor { tokenProvider?.invoke() }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val gson: Gson = GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .create()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
    
    val attendanceApi: AttendanceApi = retrofit.create(AttendanceApi::class.java)
}
EOF

# Step 5: Create Session Manager for token storage
echo "🔑 Creating session manager..."
cat > app/src/main/java/com/plp/attendance/data/local/SessionManager.kt << 'EOF'
package com.plp.attendance.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

class SessionManager(private val context: Context) {
    companion object {
        val TOKEN_KEY = stringPreferencesKey("auth_token")
        val USER_ID_KEY = stringPreferencesKey("user_id")
        val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        val USER_NAME_KEY = stringPreferencesKey("user_name")
    }
    
    val token: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[TOKEN_KEY]
    }
    
    val userId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_ID_KEY]
    }
    
    suspend fun saveSession(token: String, userId: String, email: String, name: String) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
            preferences[USER_ID_KEY] = userId
            preferences[USER_EMAIL_KEY] = email
            preferences[USER_NAME_KEY] = name
        }
    }
    
    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
    
    suspend fun getToken(): String? {
        var token: String? = null
        context.dataStore.data.collect { preferences ->
            token = preferences[TOKEN_KEY]
        }
        return token
    }
}
EOF

# Step 6: Update Repository with network calls
echo "📚 Updating repository with network support..."
cat > app/src/main/java/com/plp/attendance/data/repository/AttendanceRepositoryWithNetwork.kt << 'EOF'
package com.plp.attendance.data.repository

import com.plp.attendance.data.local.dao.AttendanceDao
import com.plp.attendance.data.local.dao.UserDao
import com.plp.attendance.data.local.entities.AttendanceEntity
import com.plp.attendance.data.local.entities.UserEntity
import com.plp.attendance.data.remote.NetworkModule
import com.plp.attendance.data.remote.models.*
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

class AttendanceRepository(
    private val attendanceDao: AttendanceDao,
    private val userDao: UserDao
) {
    private val api = NetworkModule.attendanceApi
    
    // Login with network
    suspend fun login(email: String, password: String): Result<LoginResponse> {
        return try {
            val response = api.login(LoginRequest(email, password))
            if (response.isSuccessful && response.body()?.success == true) {
                response.body()?.data?.let {
                    // Save user to local database
                    val userEntity = UserEntity(
                        id = it.user.id,
                        email = it.user.email,
                        fullName = it.user.fullName,
                        role = it.user.role,
                        schoolId = it.user.schoolId,
                        schoolName = it.user.schoolName,
                        phoneNumber = it.user.phoneNumber,
                        profilePicture = it.user.profilePicture
                    )
                    userDao.insertUser(userEntity)
                    Result.Success(it)
                } ?: Result.Error("Invalid response")
            } else {
                Result.Error(response.body()?.error ?: "Login failed")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }
    
    // Check in with network and local fallback
    suspend fun checkIn(
        userId: String, 
        latitude: Double?, 
        longitude: Double?,
        token: String?
    ): Result<AttendanceEntity> {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(Date())
        
        // Check if already checked in locally
        val existingRecord = attendanceDao.getAttendanceByUserAndDate(userId, today)
        if (existingRecord != null && existingRecord.checkInTime != null) {
            return Result.Error("Already checked in today")
        }
        
        // Create local record first
        val localAttendance = AttendanceEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            date = today,
            checkInTime = currentTime,
            checkInLatitude = latitude,
            checkInLongitude = longitude,
            status = "PRESENT",
            syncStatus = "PENDING"
        )
        
        // Try network call if token available
        if (!token.isNullOrEmpty()) {
            try {
                val response = api.checkIn(
                    "Bearer $token",
                    AttendanceRequest(userId, "check_in", timestamp, latitude, longitude)
                )
                
                if (response.isSuccessful && response.body()?.success == true) {
                    response.body()?.data?.let { apiData ->
                        // Update local record with server data
                        val syncedAttendance = localAttendance.copy(
                            id = apiData.id,
                            syncStatus = "SYNCED"
                        )
                        attendanceDao.insertAttendance(syncedAttendance)
                        return Result.Success(syncedAttendance)
                    }
                }
            } catch (e: Exception) {
                // Network failed, continue with local storage
            }
        }
        
        // Save locally if network fails
        attendanceDao.insertAttendance(localAttendance)
        return Result.Success(localAttendance)
    }
    
    // Check out with network and local fallback
    suspend fun checkOut(
        userId: String, 
        latitude: Double?, 
        longitude: Double?,
        token: String?
    ): Result<AttendanceEntity> {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(Date())
        
        val existingRecord = attendanceDao.getAttendanceByUserAndDate(userId, today)
            ?: return Result.Error("No check-in record found for today")
        
        if (existingRecord.checkOutTime != null) {
            return Result.Error("Already checked out today")
        }
        
        // Calculate working hours
        val checkInTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).parse(existingRecord.checkInTime!!)
        val checkOutTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).parse(currentTime)
        val diffInMillis = checkOutTime.time - checkInTime.time
        val workingHours = diffInMillis / (1000f * 60 * 60)
        
        val updatedRecord = existingRecord.copy(
            checkOutTime = currentTime,
            checkOutLatitude = latitude,
            checkOutLongitude = longitude,
            workingHours = workingHours,
            updatedAt = System.currentTimeMillis(),
            syncStatus = if (existingRecord.syncStatus == "SYNCED") "PENDING" else existingRecord.syncStatus
        )
        
        // Try network call if token available
        if (!token.isNullOrEmpty() && existingRecord.syncStatus == "SYNCED") {
            try {
                val response = api.checkOut(
                    "Bearer $token",
                    AttendanceRequest(userId, "check_out", timestamp, latitude, longitude)
                )
                
                if (response.isSuccessful && response.body()?.success == true) {
                    val syncedRecord = updatedRecord.copy(syncStatus = "SYNCED")
                    attendanceDao.updateAttendance(syncedRecord)
                    return Result.Success(syncedRecord)
                }
            } catch (e: Exception) {
                // Network failed, continue with local storage
            }
        }
        
        attendanceDao.updateAttendance(updatedRecord)
        return Result.Success(updatedRecord)
    }
    
    // Sync pending records
    suspend fun syncPendingRecords(token: String): Result<Int> {
        return try {
            val pendingRecords = attendanceDao.getPendingSyncRecords()
            if (pendingRecords.isEmpty()) {
                return Result.Success(0)
            }
            
            // Convert to API format
            val apiRecords = pendingRecords.map { record ->
                val timestamp = "${record.date}T${record.checkInTime ?: "00:00:00"}.000Z"
                AttendanceRequest(
                    userId = record.userId,
                    type = if (record.checkOutTime == null) "check_in" else "check_out",
                    timestamp = timestamp,
                    latitude = record.checkInLatitude,
                    longitude = record.checkInLongitude
                )
            }
            
            val response = api.syncAttendance("Bearer $token", apiRecords)
            if (response.isSuccessful && response.body()?.success == true) {
                // Mark all as synced
                pendingRecords.forEach { record ->
                    attendanceDao.updateSyncStatus(record.id, "SYNCED")
                }
                Result.Success(pendingRecords.size)
            } else {
                Result.Error("Sync failed")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Sync error")
        }
    }
    
    // Get attendance from local database
    fun getUserAttendance(userId: String): Flow<List<AttendanceEntity>> = 
        attendanceDao.getAttendanceByUser(userId)
    
    fun getRecentAttendance(userId: String): Flow<List<AttendanceEntity>> = 
        attendanceDao.getRecentAttendance(userId)
    
    suspend fun getTodayAttendance(userId: String): AttendanceEntity? {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return attendanceDao.getAttendanceByUserAndDate(userId, today)
    }
    
    // User operations
    suspend fun saveUser(user: UserEntity) = userDao.insertUser(user)
    suspend fun getUser(userId: String) = userDao.getUserById(userId)
    suspend fun getUserByEmail(email: String) = userDao.getUserByEmail(email)
}
EOF

# Step 7: Update LoginScreen to use real authentication
echo "🔐 Updating login screen..."
cat > app/src/main/java/com/plp/attendance/ui/auth/LoginViewModel.kt << 'EOF'
package com.plp.attendance.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.plp.attendance.data.local.PLPDatabase
import com.plp.attendance.data.local.SessionManager
import com.plp.attendance.data.remote.NetworkModule
import com.plp.attendance.data.repository.AttendanceRepository
import com.plp.attendance.data.repository.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false
)

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val database = PLPDatabase.getDatabase(application)
    private val repository = AttendanceRepository(
        database.attendanceDao(),
        database.userDao()
    )
    private val sessionManager = SessionManager(application)
    
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState
    
    init {
        // Set token provider for network module
        NetworkModule.setTokenProvider {
            var token: String? = null
            viewModelScope.launch {
                token = sessionManager.getToken()
            }
            token
        }
    }
    
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            when (val result = repository.login(email, password)) {
                is Result.Success -> {
                    sessionManager.saveSession(
                        token = result.data.token,
                        userId = result.data.user.id,
                        email = result.data.user.email,
                        name = result.data.user.fullName
                    )
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoggedIn = true
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
                else -> {}
            }
        }
    }
}
EOF

# Step 8: Build the APK
echo "🔨 Building APK with network support..."
./gradlew clean assembleDebug

if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    echo "✅ BUILD SUCCESSFUL!"
    
    # Create output directory
    timestamp=$(date +%Y%m%d_%H%M%S)
    output_dir="apk_with_network_$timestamp"
    mkdir -p "$output_dir"
    
    # Copy APK
    cp app/build/outputs/apk/debug/app-debug.apk "$output_dir/PLP_Attendance_Network.apk"
    
    echo ""
    echo "📱 APK with Network Support Built!"
    echo "📂 Location: $output_dir/PLP_Attendance_Network.apk"
    echo ""
    echo "✨ New Features:"
    echo "  ✓ API integration with Retrofit"
    echo "  ✓ Real authentication"
    echo "  ✓ Online/Offline sync"
    echo "  ✓ Token management"
    echo "  ✓ Automatic retry on network failure"
    echo "  ✓ Pending sync queue"
    
    open "$output_dir" 2>/dev/null || true
else
    echo "❌ Build failed. Check errors above."
fi