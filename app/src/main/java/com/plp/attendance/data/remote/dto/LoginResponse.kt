package com.plp.attendance.data.remote.dto

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    val success: Boolean,
    val message: String?,
    val data: LoginData?,
    val error: ErrorData?,
    val timestamp: String?,
    val path: String?,
    val method: String?
)

data class LoginData(
    val user: UserData,
    val tokens: TokenData
)

data class TokenData(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: String
)

data class UserData(
    val id: String,
    val username: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: String,
    val schoolId: String?,
    val schoolName: String?,
    val schoolLatitude: Double?,
    val schoolLongitude: Double?,
    val department: String?,
    val employeeId: String?,
    val emailVerified: Boolean = false,
    val phoneVerified: Boolean = false,
    val lastLoginAt: String?,
    val isActive: Boolean = true
)

data class ErrorData(
    val message: String,
    val code: String? = null,
    val details: List<ValidationError>? = null
)

data class ValidationError(
    val type: String,
    val msg: String,
    val path: String,
    val location: String
)