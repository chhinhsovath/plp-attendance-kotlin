package com.plp.attendance.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey

@Entity(
    tableName = "attendance",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        androidx.room.Index(value = ["userId"]),
        androidx.room.Index(value = ["checkInTime"])
    ]
)
data class AttendanceEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val checkInTime: Long,
    val checkOutTime: Long? = null,
    val checkInLatitude: Double,
    val checkInLongitude: Double,
    val checkOutLatitude: Double? = null,
    val checkOutLongitude: Double? = null,
    val checkInPhotoUrl: String? = null,
    val checkOutPhotoUrl: String? = null,
    val status: String, // PRESENT, LATE, ABSENT
    val notes: String? = null,
    val isSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)