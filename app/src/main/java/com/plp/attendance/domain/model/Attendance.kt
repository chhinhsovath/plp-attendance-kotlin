package com.plp.attendance.domain.model

data class Attendance(
    val id: String,
    val userId: String,
    val checkInTime: Long,
    val checkOutTime: Long? = null,
    val checkInLatitude: Double,
    val checkInLongitude: Double,
    val checkOutLatitude: Double? = null,
    val checkOutLongitude: Double? = null,
    val checkInPhotoUrl: String? = null,
    val checkOutPhotoUrl: String? = null,
    val status: AttendanceStatus,
    val notes: String? = null,
    val isSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class AttendanceStatus {
    PRESENT,
    LATE,
    ABSENT
}