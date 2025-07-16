package com.plp.attendance.data.remote.dto

data class RefreshTokenResponse(
    val success: Boolean,
    val message: String?,
    val data: RefreshTokenData?,
    val error: ErrorData?
)

data class RefreshTokenData(
    val accessToken: String,
    val expiresIn: String
)