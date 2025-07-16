package com.plp.attendance.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.plp.attendance.data.local.converters.NotificationTypeConverter
import com.plp.attendance.data.local.converters.NotificationPriorityConverter

@Entity(tableName = "notifications")
@TypeConverters(NotificationTypeConverter::class, NotificationPriorityConverter::class)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: NotificationType,
    val title: String,
    val message: String,
    val priority: NotificationPriority,
    val isRead: Boolean = false,
    val actionData: String? = null, // JSON data for actions
    val createdAt: Long = System.currentTimeMillis(),
    val readAt: Long? = null,
    val scheduledFor: Long? = null, // For scheduled notifications
    val expiresAt: Long? = null // For auto-expiring notifications
)

enum class NotificationType {
    ATTENDANCE,
    APPROVAL,
    MISSION,
    SYSTEM,
    SYNC
}

enum class NotificationPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}