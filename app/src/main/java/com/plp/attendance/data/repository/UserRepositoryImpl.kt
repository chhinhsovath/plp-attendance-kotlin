package com.plp.attendance.data.repository

import android.util.Log
import com.plp.attendance.data.local.PLPDatabase
import com.plp.attendance.data.local.SessionManager
import com.plp.attendance.data.local.dao.UserDao
import com.plp.attendance.data.local.entities.UserEntity
import com.plp.attendance.data.remote.api.UserApi
import com.plp.attendance.data.remote.dto.*
import com.plp.attendance.domain.model.User
import com.plp.attendance.domain.model.UserRole
import com.plp.attendance.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userApi: UserApi,
    private val userDao: UserDao,
    private val sessionManager: SessionManager,
    private val database: PLPDatabase
) : UserRepository {
    
    companion object {
        private const val TAG = "UserRepository"
    }
    
    // Remote operations
    override suspend fun getUsers(
        page: Int,
        limit: Int,
        role: String?,
        search: String?
    ): Result<List<User>> {
        return try {
            val token = getAuthToken() ?: return Result.failure(Exception("Not authenticated"))
            
            val response = userApi.getUsers(
                token = "Bearer $token",
                page = page,
                limit = limit,
                role = role,
                search = search
            )
            
            if (response.isSuccessful && response.body()?.success == true) {
                val users = response.body()!!.data?.users?.map { userData ->
                    mapUserDataToDomain(userData)
                } ?: emptyList()
                
                // Cache users in local database
                users.forEach { user ->
                    userDao.insertUser(user.toEntity())
                }
                
                Result.success(users)
            } else {
                val errorMessage = response.body()?.error?.message ?: "Failed to fetch users"
                Log.e(TAG, "Get users failed: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Get users error", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getUserById(userId: String): Result<User> {
        return try {
            val token = getAuthToken() ?: return Result.failure(Exception("Not authenticated"))
            
            val response = userApi.getUserById("Bearer $token", userId)
            
            if (response.isSuccessful && response.body() != null) {
                val userData = response.body()!!
                val user = mapUserResponseToDomain(userData)
                
                // Cache user in local database
                userDao.insertUser(user.toEntity())
                
                Result.success(user)
            } else {
                val errorMessage = "Failed to fetch user"
                Log.e(TAG, "Get user by ID failed: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Get user by ID error", e)
            Result.failure(e)
        }
    }
    
    override suspend fun createUser(
        email: String,
        username: String,
        password: String,
        firstName: String,
        lastName: String,
        role: String,
        phoneNumber: String?,
        organizationId: String?
    ): Result<User> {
        return try {
            val token = getAuthToken() ?: return Result.failure(Exception("Not authenticated"))
            
            val request = CreateUserRequest(
                email = email,
                username = username,
                password = password,
                firstName = firstName,
                lastName = lastName,
                role = role,
                phoneNumber = phoneNumber,
                organizationId = organizationId
            )
            
            val response = userApi.createUser("Bearer $token", request)
            
            if (response.isSuccessful && response.body() != null) {
                val userData = response.body()!!
                val user = mapUserResponseToDomain(userData)
                
                // Cache user in local database
                userDao.insertUser(user.toEntity())
                
                Result.success(user)
            } else {
                val errorMessage = "Failed to create user"
                Log.e(TAG, "Create user failed: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Create user error", e)
            Result.failure(e)
        }
    }
    
    override suspend fun updateUser(
        userId: String,
        email: String?,
        username: String?,
        firstName: String?,
        lastName: String?,
        role: String?,
        phoneNumber: String?,
        organizationId: String?,
        isActive: Boolean?
    ): Result<User> {
        return try {
            val token = getAuthToken() ?: return Result.failure(Exception("Not authenticated"))
            
            val request = UpdateUserRequest(
                email = email,
                username = username,
                firstName = firstName,
                lastName = lastName,
                role = role,
                phoneNumber = phoneNumber,
                organizationId = organizationId,
                isActive = isActive
            )
            
            val response = userApi.updateUser("Bearer $token", userId, request)
            
            if (response.isSuccessful && response.body() != null) {
                val userData = response.body()!!
                val user = mapUserResponseToDomain(userData)
                
                // Update cached user in local database
                userDao.updateUser(user.toEntity())
                
                Result.success(user)
            } else {
                val errorMessage = "Failed to update user"
                Log.e(TAG, "Update user failed: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Update user error", e)
            Result.failure(e)
        }
    }
    
    override suspend fun deleteUser(userId: String): Result<Unit> {
        return try {
            val token = getAuthToken() ?: return Result.failure(Exception("Not authenticated"))
            
            val response = userApi.deleteUser("Bearer $token", userId)
            
            if (response.isSuccessful) {
                // Remove user from local database
                userDao.deleteUserById(userId)
                Result.success(Unit)
            } else {
                val errorMessage = "Failed to delete user"
                Log.e(TAG, "Delete user failed: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Delete user error", e)
            Result.failure(e)
        }
    }
    
    override suspend fun activateUser(userId: String): Result<User> {
        return try {
            val token = getAuthToken() ?: return Result.failure(Exception("Not authenticated"))
            
            val response = userApi.activateUser("Bearer $token", userId)
            
            if (response.isSuccessful && response.body() != null) {
                val userData = response.body()!!
                val user = mapUserResponseToDomain(userData)
                
                // Update cached user in local database
                userDao.updateUser(user.toEntity())
                
                Result.success(user)
            } else {
                val errorMessage = "Failed to activate user"
                Log.e(TAG, "Activate user failed: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Activate user error", e)
            Result.failure(e)
        }
    }
    
    override suspend fun deactivateUser(userId: String): Result<User> {
        return try {
            val token = getAuthToken() ?: return Result.failure(Exception("Not authenticated"))
            
            val response = userApi.deactivateUser("Bearer $token", userId)
            
            if (response.isSuccessful && response.body() != null) {
                val userData = response.body()!!
                val user = mapUserResponseToDomain(userData)
                
                // Update cached user in local database
                userDao.updateUser(user.toEntity())
                
                Result.success(user)
            } else {
                val errorMessage = "Failed to deactivate user"
                Log.e(TAG, "Deactivate user failed: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Deactivate user error", e)
            Result.failure(e)
        }
    }
    
    override suspend fun resetUserPassword(userId: String, newPassword: String): Result<Unit> {
        return try {
            val token = getAuthToken() ?: return Result.failure(Exception("Not authenticated"))
            
            val request = ResetPasswordRequest(newPassword)
            val response = userApi.resetUserPassword("Bearer $token", userId, request)
            
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMessage = "Failed to reset password"
                Log.e(TAG, "Reset password failed: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Reset password error", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getUsersByRole(role: String): Result<List<User>> {
        return try {
            val token = getAuthToken() ?: return Result.failure(Exception("Not authenticated"))
            
            val response = userApi.getUsersByRole("Bearer $token", role)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val users = response.body()!!.data?.users?.map { userData ->
                    mapUserDataToDomain(userData)
                } ?: emptyList()
                
                // Cache users in local database
                users.forEach { user ->
                    userDao.insertUser(user.toEntity())
                }
                
                Result.success(users)
            } else {
                val errorMessage = response.body()?.error?.message ?: "Failed to fetch users by role"
                Log.e(TAG, "Get users by role failed: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Get users by role error", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getUsersByDepartment(departmentId: String): Result<List<User>> {
        return try {
            val token = getAuthToken() ?: return Result.failure(Exception("Not authenticated"))
            
            val response = userApi.getUsersByDepartment("Bearer $token", departmentId)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val users = response.body()!!.data?.users?.map { userData ->
                    mapUserDataToDomain(userData)
                } ?: emptyList()
                
                // Cache users in local database
                users.forEach { user ->
                    userDao.insertUser(user.toEntity())
                }
                
                Result.success(users)
            } else {
                val errorMessage = response.body()?.error?.message ?: "Failed to fetch users by department"
                Log.e(TAG, "Get users by department failed: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Get users by department error", e)
            Result.failure(e)
        }
    }
    
    // Local operations (cached data)
    override fun getCachedUsers(): Flow<List<User>> {
        return userDao.getAllActiveUsers().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getCachedUsersByRole(role: UserRole): Flow<List<User>> {
        return userDao.getUsersByRole(role.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun getCachedUserById(userId: String): User? {
        return userDao.getUserById(userId)?.toDomain()
    }
    
    override suspend fun refreshUsers(): Result<Unit> {
        return try {
            // Fetch all users from API and update local cache
            val result = getUsers(page = 1, limit = 100)
            if (result.isSuccess) {
                Result.success(Unit)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Failed to refresh users"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Refresh users error", e)
            Result.failure(e)
        }
    }
    
    // Helper methods
    private suspend fun getAuthToken(): String? {
        return sessionManager.getAuthTokenSuspend()
    }
    
    private fun mapUserDataToDomain(userData: UserData): User {
        return User(
            id = userData.id,
            name = "${userData.firstName} ${userData.lastName}",
            email = userData.email,
            role = mapApiRoleToAppRole(userData.role),
            phoneNumber = null, // API doesn't provide phone number in list response
            profilePictureUrl = null, // API doesn't provide profile picture in list response
            departmentId = userData.schoolId,
            isActive = userData.isActive,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }
    
    private fun mapUserResponseToDomain(userResponse: UserResponse): User {
        return User(
            id = userResponse.id,
            name = "${userResponse.firstName} ${userResponse.lastName}",
            email = userResponse.email,
            role = mapApiRoleToAppRole(userResponse.role),
            phoneNumber = null, // API doesn't provide phone number
            profilePictureUrl = null, // API doesn't provide profile picture
            departmentId = userResponse.organizationId,
            isActive = userResponse.isActive,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
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
    
    private fun UserEntity.toDomain(): User {
        return User(
            id = id,
            name = name,
            email = email,
            role = role,
            phoneNumber = phoneNumber,
            profilePictureUrl = profilePictureUrl,
            departmentId = departmentId,
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
            isActive = isActive,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}