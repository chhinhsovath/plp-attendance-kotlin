package com.plp.attendance.data.repository

import android.util.Log
import com.plp.attendance.data.local.PLPDatabase
import com.plp.attendance.data.local.SessionManager
import com.plp.attendance.data.local.entities.UserEntity
import com.plp.attendance.data.remote.api.AuthApi
import com.plp.attendance.data.remote.dto.*
import com.plp.attendance.domain.model.User
import com.plp.attendance.domain.model.UserRole
import com.plp.attendance.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val database: PLPDatabase,
    private val sessionManager: SessionManager
) : AuthRepository {
    
    override suspend fun login(email: String, password: String): Result<User> {
        return try {
            val response = authApi.login(LoginRequest(identifier = email, password = password))
            
            if (response.isSuccessful && response.body()?.success == true) {
                val loginResponse = response.body()!!
                val loginData = loginResponse.data!!
                val userData = loginData.user
                
                // Map API role to app role
                val userRole = mapApiRoleToAppRole(userData.role)
                
                // Create user entity for local storage
                val userEntity = UserEntity(
                    id = userData.id,
                    email = userData.email,
                    name = "${userData.firstName} ${userData.lastName}",
                    role = userRole,
                    phoneNumber = null, // API doesn't provide phone number
                    profilePictureUrl = null, // API doesn't provide profile picture
                    departmentId = userData.schoolId,
                    schoolId = userData.schoolId,
                    schoolName = userData.schoolName,
                    schoolLatitude = userData.schoolLatitude,
                    schoolLongitude = userData.schoolLongitude,
                    isActive = userData.isActive,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                
                // Save to local database
                database.userDao().insertUser(userEntity)
                
                // Save session
                sessionManager.saveSession(
                    userId = userData.id,
                    userEmail = userData.email,
                    userName = "${userData.firstName} ${userData.lastName}",
                    userRole = userRole.name,
                    token = loginData.tokens.accessToken,
                    refreshToken = loginData.tokens.refreshToken
                )
                
                // Convert to domain model
                val user = User(
                    id = userEntity.id,
                    email = userEntity.email,
                    name = userEntity.name,
                    role = userEntity.role,
                    phoneNumber = userEntity.phoneNumber,
                    profilePictureUrl = userEntity.profilePictureUrl,
                    departmentId = userEntity.departmentId,
                    schoolId = userData.schoolId,
                    schoolName = userData.schoolName,
                    schoolLatitude = userData.schoolLatitude,
                    schoolLongitude = userData.schoolLongitude,
                    isActive = userEntity.isActive,
                    createdAt = userEntity.createdAt,
                    updatedAt = userEntity.updatedAt
                )
                
                Result.success(user)
            } else {
                val errorBody = response.body()
                val errorMessage = errorBody?.error?.message ?: response.message() ?: "Login failed"
                Log.e("AuthRepository", "ចូលប្រព័ន្ធបរាជ័យ: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "កំហុសចូលប្រព័ន្ធ", e)
            Result.failure(e)
        }
    }
    
    override suspend fun logout(): Result<Unit> {
        return try {
            val token = sessionManager.getAuthTokenSuspend()
            if (token != null) {
                authApi.logout("Bearer $token")
            }
            sessionManager.clearSession()
            Result.success(Unit)
        } catch (e: Exception) {
            // Even if API logout fails, clear local session
            sessionManager.clearSession()
            Result.success(Unit)
        }
    }
    
    override suspend fun refreshToken(): Result<String> {
        return try {
            val refreshToken = sessionManager.getRefreshToken().first()
                ?: return Result.failure(Exception("No refresh token"))
            
            val response = authApi.refreshToken(RefreshTokenRequest(refreshToken))
            
            if (response.isSuccessful && response.body()?.success == true) {
                val newToken = response.body()!!.data!!.accessToken
                sessionManager.updateToken(newToken)
                Result.success(newToken)
            } else {
                Result.failure(Exception("Token refresh failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getCurrentUser(): Flow<User?> = 
        sessionManager.getUserId().flatMapLatest { userId ->
            if (userId != null) {
                flow {
                    val userEntity = database.userDao().getUserById(userId)
                    if (userEntity != null) {
                        emit(User(
                            id = userEntity.id,
                            email = userEntity.email,
                            name = userEntity.name,
                            role = userEntity.role,
                            phoneNumber = userEntity.phoneNumber,
                            profilePictureUrl = userEntity.profilePictureUrl,
                            departmentId = userEntity.departmentId,
                            schoolId = userEntity.schoolId,
                            schoolName = userEntity.schoolName,
                            schoolLatitude = userEntity.schoolLatitude,
                            schoolLongitude = userEntity.schoolLongitude,
                            isActive = userEntity.isActive,
                            createdAt = userEntity.createdAt,
                            updatedAt = userEntity.updatedAt
                        ))
                    } else {
                        // Try to fetch from API
                        val token = sessionManager.getAuthTokenSuspend()
                        if (token != null) {
                            try {
                                val response = authApi.getCurrentUser("Bearer $token")
                                if (response.isSuccessful && response.body() != null) {
                                    val userData = response.body()!!
                                    val userRole = mapApiRoleToAppRole(userData.role)
                                    
                                    val userEntity = UserEntity(
                                        id = userData.id,
                                        email = userData.email,
                                        name = "${userData.firstName} ${userData.lastName}",
                                        role = userRole,
                                        phoneNumber = null,
                                        profilePictureUrl = null,
                                        departmentId = userData.organizationId,
                                        schoolId = userData.schoolId,
                                        schoolName = userData.schoolName,
                                        schoolLatitude = userData.schoolLatitude,
                                        schoolLongitude = userData.schoolLongitude,
                                        isActive = userData.isActive,
                                        createdAt = System.currentTimeMillis(),
                                        updatedAt = System.currentTimeMillis()
                                    )
                                    
                                    database.userDao().insertUser(userEntity)
                                    
                                    emit(User(
                                        id = userEntity.id,
                                        email = userEntity.email,
                                        name = userEntity.name,
                                        role = userEntity.role,
                                        phoneNumber = userEntity.phoneNumber,
                                        profilePictureUrl = userEntity.profilePictureUrl,
                                        departmentId = userEntity.departmentId,
                                        schoolId = userData.schoolId,
                                        schoolName = userData.schoolName,
                                        schoolLatitude = userData.schoolLatitude,
                                        schoolLongitude = userData.schoolLongitude,
                                        isActive = userEntity.isActive,
                                        createdAt = userEntity.createdAt,
                                        updatedAt = userEntity.updatedAt
                                    ))
                                } else {
                                    emit(null)
                                }
                            } catch (e: Exception) {
                                emit(null)
                            }
                        } else {
                            emit(null)
                        }
                    }
                }
            } else {
                flowOf(null)
            }
        }
    
    override suspend fun changePassword(
        currentPassword: String,
        newPassword: String
    ): Result<Unit> {
        return try {
            val token = sessionManager.getAuthTokenSuspend()
                ?: return Result.failure(Exception("Not authenticated"))
            
            val response = authApi.changePassword(
                "Bearer $token",
                mapOf(
                    "currentPassword" to currentPassword,
                    "newPassword" to newPassword
                )
            )
            
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Password change failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun forgotPassword(email: String): Result<Unit> {
        // Not implemented in the current API
        return Result.failure(Exception("Not implemented"))
    }
    
    override suspend fun validateSession(): Result<Boolean> {
        return try {
            val token = sessionManager.getAuthTokenSuspend()
            if (token != null) {
                val response = authApi.getCurrentUser("Bearer $token")
                Result.success(response.isSuccessful)
            } else {
                Result.success(false)
            }
        } catch (e: Exception) {
            Result.success(false)
        }
    }
    
    private fun mapApiRoleToAppRole(apiRole: String): UserRole {
        return when (apiRole.uppercase()) {
            "ADMINISTRATOR", "ADMIN" -> UserRole.ADMINISTRATOR
            "ZONE_MANAGER" -> UserRole.ZONE_MANAGER
            "PROVINCIAL_MANAGER" -> UserRole.PROVINCIAL_MANAGER
            "DEPARTMENT_MANAGER" -> UserRole.DEPARTMENT_MANAGER
            "CLUSTER_HEAD" -> UserRole.CLUSTER_HEAD
            "DIRECTOR" -> UserRole.DIRECTOR
            "TEACHER" -> UserRole.TEACHER
            else -> UserRole.TEACHER // Default to teacher
        }
    }
}