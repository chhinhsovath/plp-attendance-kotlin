package com.plp.attendance.data.local.converters

import androidx.room.TypeConverter
import com.plp.attendance.data.local.entities.NotificationType
import com.plp.attendance.data.local.entities.NotificationPriority

class NotificationTypeConverter {
    @TypeConverter
    fun fromNotificationType(type: NotificationType): String {
        return type.name
    }

    @TypeConverter
    fun toNotificationType(type: String): NotificationType {
        return NotificationType.valueOf(type)
    }
}

class NotificationPriorityConverter {
    @TypeConverter
    fun fromNotificationPriority(priority: NotificationPriority): String {
        return priority.name
    }

    @TypeConverter
    fun toNotificationPriority(priority: String): NotificationPriority {
        return NotificationPriority.valueOf(priority)
    }
}