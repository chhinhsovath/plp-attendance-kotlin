package com.plp.attendance.domain.model

data class Leave(
    val id: String,
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

enum class LeaveType(val displayName: String) {
    SICK("Sick Leave"),
    VACATION("Vacation"),
    PERSONAL("Personal Leave"),
    EMERGENCY("Emergency Leave"),
    MATERNITY("Maternity Leave"),
    PATERNITY("Paternity Leave"),
    BEREAVEMENT("Bereavement Leave"),
    STUDY("Study Leave")
}

enum class LeaveStatus(val displayName: String) {
    PENDING("Pending"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    CANCELLED("Cancelled")
}