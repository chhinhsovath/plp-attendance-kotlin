package com.plp.attendance.data.repository

import android.util.Log
import com.plp.attendance.data.local.PLPDatabase
import com.plp.attendance.data.local.SessionManager
import com.plp.attendance.data.local.TestDataSeeder
import com.plp.attendance.data.local.entities.UserEntity
import com.plp.attendance.data.remote.RemoteConfig
import com.plp.attendance.data.remote.api.AuthApi
import com.plp.attendance.data.remote.dto.LoginRequest
import com.plp.attendance.data.remote.dto.RefreshTokenRequest
import com.plp.attendance.domain.model.User
import com.plp.attendance.domain.model.UserRole
import com.plp.attendance.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enhanced Authentication Repository that connects to remote server
 * Handles both online and offline scenarios with proper sync
 */
@Singleton
class RemoteAuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val database: PLPDatabase,
    private val sessionManager: SessionManager,
    private val testDataSeeder: TestDataSeeder
) : AuthRepository {
    
    companion object {
        private const val TAG = "RemoteAuthRepository"
    }
    
    override suspend fun login(email: String, password: String): Result<User> {
        return try {
            // Always authenticate with remote server for real users
            Log.d(TAG, "Attempting remote login for: $email")
            
            val response = authApi.login(LoginRequest(email, password))
            
            Log.d(TAG, "Login response code: ${response.code()}")
            
            if (response.isSuccessful && response.body() != null) {
                val loginResponse = response.body()!!
                Log.d(TAG, "Login response body: $loginResponse")
                
                if (loginResponse.success && loginResponse.data != null) {
                    val userData = loginResponse.data.user
                    val tokens = loginResponse.data.tokens
                    
                    Log.d(TAG, "User data: $userData")
                    Log.d(TAG, "Access token: ${tokens.accessToken}")
                    
                    // Map API response to domain model
                    val user = User(
                        id = userData.id,
                        name = "${userData.firstName} ${userData.lastName}",
                        email = userData.email,
                        role = mapApiRoleToAppRole(userData.role),
                        phoneNumber = null, // API doesn't provide phone number in login response
                        profilePictureUrl = null, // API doesn't provide profile picture in login response
                        departmentId = userData.department,
                        schoolId = userData.schoolId,
                        schoolName = userData.schoolName,
                        schoolLatitude = userData.schoolLatitude,
                        schoolLongitude = userData.schoolLongitude,
                        isActive = userData.isActive,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    
                    // Save to local database for offline access
                    database.userDao().insertUser(user.toEntity())
                    
                    // Save session
                    sessionManager.saveSession(
                        userId = user.id,
                        userEmail = user.email,
                        userName = user.name,
                        userRole = user.role.name,
                        token = tokens.accessToken,
                        refreshToken = tokens.refreshToken
                    )
                    
                    Log.d(TAG, "Login successful for: ${user.email}, token saved")
                    Result.success(user)
                } else {
                    val errorMessage = loginResponse.message ?: "Login failed"
                    Log.e(TAG, errorMessage)
                    Result.failure(Exception(errorMessage))
                }
            } else {
                val errorMessage = "ចូលប្រព័ន្ធបរាជ័យ: ${response.code()} ${response.message()}"
                Log.e(TAG, errorMessage)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: HttpException) {
            Log.e(TAG, "HTTP error during login", e)
            Result.failure(Exception("Network error: ${e.code()} ${e.message()}"))
        } catch (e: Exception) {
            Log.e(TAG, "កំហុសចូលប្រព័ន្ធ", e)
            Result.failure(e)
        }
    }
    
    override suspend fun logout(): Result<Unit> {
        return try {
            val token = sessionManager.getAuthTokenSuspend()
            
            if (RemoteConfig.Features.USE_REMOTE_API && token != null && !token.startsWith("test_") && !token.startsWith("offline_")) {
                // Real token - logout from server
                try {
                    authApi.logout("Bearer $token")
                } catch (e: Exception) {
                    Log.w(TAG, "Remote logout failed, continuing with local logout", e)
                }
            }
            
            // Clear local session
            sessionManager.clearSession()
            
            Log.d(TAG, "Logout successful")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Logout error", e)
            Result.failure(e)
        }
    }
    
    override suspend fun refreshToken(): Result<String> {
        return try {
            val refreshToken = sessionManager.getRefreshToken().first()
                ?: return Result.failure(Exception("No refresh token available"))
            
            if (refreshToken.startsWith("test_") || refreshToken.startsWith("offline_")) {
                // Test/offline token - just return success
                return Result.success(refreshToken)
            }
            
            if (RemoteConfig.Features.USE_REMOTE_API) {
                val response = authApi.refreshToken(RefreshTokenRequest(refreshToken))
                
                if (response.isSuccessful && response.body() != null) {
                    val refreshResponse = response.body()!!
                    if (refreshResponse.success && refreshResponse.data != null) {
                        val newToken = refreshResponse.data.accessToken
                        sessionManager.updateToken(newToken)
                        Result.success(newToken)
                    } else {
                        Result.failure(Exception(refreshResponse.message ?: "Token refresh failed"))
                    }
                } else {
                    Result.failure(Exception("Token refresh failed"))
                }
            } else {
                Result.success(refreshToken)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Token refresh error", e)
            Result.failure(e)
        }
    }
    
    override fun getCurrentUser(): Flow<User?> = flow {
        val userId = sessionManager.getUserIdSuspend()
        if (userId != null) {
            val userEntity = database.userDao().getUserById(userId)
            emit(userEntity?.toDomainModel())
        } else {
            emit(null)
        }
    }
    
    override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> {
        return try {
            val token = sessionManager.getAuthTokenSuspend()
                ?: return Result.failure(Exception("Not authenticated"))
            
            if (token.startsWith("test_") || token.startsWith("offline_")) {
                // Test/offline account - just return success
                return Result.success(Unit)
            }
            
            if (RemoteConfig.Features.USE_REMOTE_API) {
                val response = authApi.changePassword(
                    token = "Bearer $token",
                    request = mapOf(
                        "currentPassword" to currentPassword,
                        "newPassword" to newPassword
                    )
                )
                
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Password change failed"))
                }
            } else {
                Result.failure(Exception("Cannot change password in offline mode"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Change password error", e)
            Result.failure(e)
        }
    }
    
    override suspend fun validateSession(): Result<Boolean> {
        return try {
            val token = sessionManager.getAuthTokenSuspend()
                ?: return Result.success(false)
            
            if (token.startsWith("test_") || token.startsWith("offline_")) {
                // Test/offline token - always valid
                return Result.success(true)
            }
            
            if (RemoteConfig.Features.USE_REMOTE_API) {
                val response = authApi.validateToken("Bearer $token")
                Result.success(response.isSuccessful)
            } else {
                Result.success(true) // Offline mode - assume valid
            }
        } catch (e: Exception) {
            Log.e(TAG, "Session validation error", e)
            Result.success(false)
        }
    }
    
    override suspend fun forgotPassword(email: String): Result<Unit> {
        return try {
            // Check if this is a test account
            val localUser = database.userDao().getUserByEmail(email)
            if (localUser != null && localUser.email.endsWith("@test.com")) {
                // Test account - just return success
                Log.d(TAG, "Forgot password request for test account: $email")
                return Result.success(Unit)
            }
            
            if (RemoteConfig.Features.USE_REMOTE_API) {
                val response = authApi.forgotPassword(mapOf("email" to email))
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to send password reset email"))
                }
            } else {
                Result.failure(Exception("Cannot reset password in offline mode"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Forgot password error", e)
            Result.failure(e)
        }
    }
    
    /**
     * Initialize test data if database is empty
     */
    suspend fun initializeTestDataIfNeeded() {
        try {
            val userCount = database.userDao().getUserCount()
            if (userCount == 0) {
                Log.d(TAG, "No users found, seeding test data...")
                testDataSeeder.seedTestData()
                Log.d(TAG, "Test data seeded successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing test data", e)
        }
    }
    
    // Helper methods
    private fun mapApiRoleToAppRole(apiRole: String): UserRole {
        return when (apiRole.uppercase()) {
            "ADMINISTRATOR", "ADMIN" -> UserRole.ADMINISTRATOR
            "ZONE_MANAGER" -> UserRole.ZONE_MANAGER
            "PROVINCIAL_MANAGER" -> UserRole.PROVINCIAL_MANAGER
            "DEPARTMENT_MANAGER" -> UserRole.DEPARTMENT_MANAGER
            "CLUSTER_HEAD" -> UserRole.CLUSTER_HEAD
            "DIRECTOR" -> UserRole.DIRECTOR
            "TEACHER" -> UserRole.TEACHER
            else -> UserRole.TEACHER
        }
    }
    
    private fun UserEntity.toDomainModel(): User {
        return User(
            id = id,
            name = name,
            email = email,
            role = role,
            phoneNumber = phoneNumber,
            profilePictureUrl = profilePictureUrl,
            departmentId = departmentId,
            schoolId = schoolId,
            schoolName = schoolName,
            schoolLatitude = schoolLatitude,
            schoolLongitude = schoolLongitude,
            isActive = isActive,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
    
    private fun User.toEntity(): UserEntity {
        return UserEntity(
            id = id,
            name = name,
            email = email,
            role = role,
            phoneNumber = phoneNumber,
            profilePictureUrl = profilePictureUrl,
            departmentId = departmentId,
            schoolId = schoolId,
            schoolName = schoolName,
            schoolLatitude = schoolLatitude,
            schoolLongitude = schoolLongitude,
            isActive = isActive,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}