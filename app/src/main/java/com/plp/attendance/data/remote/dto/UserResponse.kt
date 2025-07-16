package com.plp.attendance.data.remote.dto

import com.google.gson.annotations.SerializedName

data class UserResponse(
    @SerializedName("_id")
    val id: String,
    val email: String,
    val username: String,
    val firstName: String,
    val lastName: String,
    val role: String,
    val permissions: List<String>?,
    val isActive: Boolean,
    val organizationId: String?,
    val schoolId: String?,
    val schoolName: String?,
    val schoolLatitude: Double?,
    val schoolLongitude: Double?,
    val createdAt: String,
    val updatedAt: String
)