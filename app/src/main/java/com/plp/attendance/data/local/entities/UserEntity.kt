package com.plp.attendance.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.plp.attendance.domain.model.UserRole

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val role: UserRole,
    val phoneNumber: String? = null,
    val profilePictureUrl: String? = null,
    val departmentId: String? = null,
    val schoolId: String? = null,
    val schoolName: String? = null,
    val schoolLatitude: Double? = null,
    val schoolLongitude: Double? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)