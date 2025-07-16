package com.plp.attendance.data.remote.dto

data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)