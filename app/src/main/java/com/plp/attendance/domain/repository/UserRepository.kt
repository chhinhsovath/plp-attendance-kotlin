package com.plp.attendance.domain.repository

import com.plp.attendance.domain.model.User
import com.plp.attendance.domain.model.UserRole
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    // Remote operations
    suspend fun getUsers(
        page: Int = 1,
        limit: Int = 20,
        role: String? = null,
        search: String? = null
    ): Result<List<User>>
    
    suspend fun getUserById(userId: String): Result<User>
    
    suspend fun createUser(
        email: String,
        username: String,
        password: String,
        firstName: String,
        lastName: String,
        role: String,
        phoneNumber: String? = null,
        organizationId: String? = null
    ): Result<User>
    
    suspend fun updateUser(
        userId: String,
        email: String? = null,
        username: String? = null,
        firstName: String? = null,
        lastName: String? = null,
        role: String? = null,
        phoneNumber: String? = null,
        organizationId: String? = null,
        isActive: Boolean? = null
    ): Result<User>
    
    suspend fun deleteUser(userId: String): Result<Unit>
    
    suspend fun activateUser(userId: String): Result<User>
    
    suspend fun deactivateUser(userId: String): Result<User>
    
    suspend fun resetUserPassword(userId: String, newPassword: String): Result<Unit>
    
    suspend fun getUsersByRole(role: String): Result<List<User>>
    
    suspend fun getUsersByDepartment(departmentId: String): Result<List<User>>
    
    // Local operations (cached data)
    fun getCachedUsers(): Flow<List<User>>
    
    fun getCachedUsersByRole(role: UserRole): Flow<List<User>>
    
    suspend fun getCachedUserById(userId: String): User?
    
    suspend fun refreshUsers(): Result<Unit>
}