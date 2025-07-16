package com.plp.attendance.data.remote.dto

data class LoginRequest(
    val identifier: String, // Email or username
    val password: String
)