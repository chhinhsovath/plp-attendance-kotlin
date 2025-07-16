package com.plp.attendance.data.local.converters

import androidx.room.TypeConverter
import com.plp.attendance.domain.model.UserRole

class UserRoleConverter {
    @TypeConverter
    fun fromUserRole(role: UserRole): String {
        return role.name
    }

    @TypeConverter
    fun toUserRole(roleString: String): UserRole {
        return UserRole.valueOf(roleString)
    }
}