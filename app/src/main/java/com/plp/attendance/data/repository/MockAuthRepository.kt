package com.plp.attendance.data.repository

import com.plp.attendance.data.local.PLPDatabase
import com.plp.attendance.data.local.SessionManager
import com.plp.attendance.data.local.TestDataSeeder
import com.plp.attendance.domain.model.User
import com.plp.attendance.domain.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mock authentication repository for local testing
 * Validates against local SQLite database instead of API
 */
@Singleton
class MockAuthRepository @Inject constructor(
    private val database: PLPDatabase,
    private val sessionManager: SessionManager,
    private val testDataSeeder: TestDataSeeder
) : AuthRepository {
    
    override suspend fun login(email: String, password: String): Result<User> {
        return try {
            // Simulate network delay
            delay(500)
            
            // Check if database is empty and seed if needed
            if (!testDataSeeder.hasTestData()) {
                testDataSeeder.seedTestData()
            }
            
            // Check if it's a test account
            val testAccounts = testDataSeeder.getTestAccounts()
            val testAccount = testAccounts.find { it.email == email }
            
            if (testAccount == null) {
                return Result.failure(Exception("Invalid email or password"))
            }
            
            // Validate password (all test accounts use the same password)
            if (password != TestDataSeeder.DEFAULT_PASSWORD) {
                return Result.failure(Exception("Invalid email or password"))
            }
            
            // Get user from database
            val userEntity = database.userDao().getUserByEmail(email)
                ?: return Result.failure(Exception("User not found in database"))
            
            // Convert to domain model
            val user = User(
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
            )
            
            // Save session
            sessionManager.saveSession(
                userId = user.id,
                userEmail = user.email,
                userName = user.name,
                userRole = user.role.name,
                token = "mock-token-${user.id}",
                refreshToken = "mock-refresh-${user.id}"
            )
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun logout(): Result<Unit> {
        return try {
            sessionManager.clearSession()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun refreshToken(): Result<String> {
        return try {
            // Simulate token refresh
            delay(300)
            val userId = sessionManager.getUserIdSuspend() ?: throw Exception("No active session")
            val newToken = "mock-token-refreshed-$userId"
            sessionManager.updateToken(newToken)
            Result.success(newToken)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun getCurrentUser(): Flow<User?> = flow {
        val userId = sessionManager.getUserIdSuspend()
        if (userId != null) {
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
                emit(null)
            }
        } else {
            emit(null)
        }
    }
    
    override suspend fun changePassword(
        currentPassword: String,
        newPassword: String
    ): Result<Unit> {
        return try {
            // For testing, we just log the password change
            delay(500)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun forgotPassword(email: String): Result<Unit> {
        return try {
            // For testing, we just log the request
            delay(500)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun validateSession(): Result<Boolean> {
        return try {
            val userId = sessionManager.getUserIdSuspend()
            val isValid = userId != null && database.userDao().getUserById(userId) != null
            Result.success(isValid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}