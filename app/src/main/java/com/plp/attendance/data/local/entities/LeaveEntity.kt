package com.plp.attendance.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import com.plp.attendance.domain.model.LeaveType
import com.plp.attendance.domain.model.LeaveStatus

@Entity(
    tableName = "leaves",
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
        androidx.room.Index(value = ["status"]),
        androidx.room.Index(value = ["startDate", "endDate"])
    ]
)
data class LeaveEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val leaveType: LeaveType,
    val startDate: Long,
    val endDate: Long,
    val reason: String,
    val status: LeaveStatus,
    val approverId: String? = null,
    val approverComments: String? = null,
    val attachmentUrl: String? = null,
    val requestedAt: Long = System.currentTimeMillis(),
    val approvedAt: Long? = null,
    val isSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)