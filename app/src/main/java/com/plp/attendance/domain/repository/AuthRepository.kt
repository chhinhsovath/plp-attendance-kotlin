package com.plp.attendance.domain.repository

import com.plp.attendance.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<User>
    suspend fun logout(): Result<Unit>
    suspend fun refreshToken(): Result<String>
    fun getCurrentUser(): Flow<User?>
    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit>
    suspend fun forgotPassword(email: String): Result<Unit>
    suspend fun validateSession(): Result<Boolean>
}