package com.plp.attendance.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AttendanceResponse(
    val success: Boolean,
    val data: AttendanceResponseData?,
    val error: ErrorData?,
    val message: String? = null
)

data class AttendanceResponseData(
    val record: AttendanceRecord? = null,
    // For backward compatibility
    @SerializedName("_id")
    val id: String? = null,
    val userId: String? = null,
    val date: String? = null,
    val checkInTime: String? = null,
    val checkOutTime: String? = null,
    val checkInLocation: LocationData? = null,
    val checkOutLocation: LocationData? = null,
    val checkInPhotoUrl: String? = null,
    val checkOutPhotoUrl: String? = null,
    val status: String? = null,
    val workingHours: Double? = null,
    val notes: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class AttendanceData(
    @SerializedName("_id")
    val id: String,
    val userId: String,
    val date: String,
    val checkInTime: String?,
    val checkOutTime: String?,
    val checkInLocation: LocationData?,
    val checkOutLocation: LocationData?,
    val checkInPhotoUrl: String?,
    val checkOutPhotoUrl: String?,
    val status: String, // PRESENT, LATE, ABSENT, HOLIDAY, WEEKEND
    val workingHours: Double?,
    val notes: String?,
    val createdAt: String,
    val updatedAt: String
)

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double? = null,
    val address: String? = null
)

data class AttendanceStatusResponse(
    val success: Boolean,
    val data: AttendanceStatusData?,
    val error: ErrorData?
)

data class AttendanceStatusData(
    val hasCheckedIn: Boolean,
    val hasCheckedOut: Boolean,
    val attendance: AttendanceData?,
    val workingHours: Double?,
    val isWorkingDay: Boolean,
    val status: StatusInfo? = null
)

data class StatusInfo(
    val hasCheckedIn: Boolean,
    val hasCheckedOut: Boolean,
    val canCheckIn: Boolean,
    val canCheckOut: Boolean,
    val workingHours: Double?,
    val record: AttendanceRecord?
)

data class AttendanceRecord(
    val id: String,
    val checkInTime: String?,
    val checkOutTime: String?,
    val status: String,
    val workingHours: Double?,
    val notes: String?,
    val latitude: String? = null,
    val longitude: String? = null,
    val address: String? = null
)

data class AttendanceListResponse(
    val success: Boolean,
    val data: AttendanceListData?,
    val error: ErrorData?
)

data class AttendanceListData(
    val attendances: List<AttendanceData>,
    val pagination: PaginationData?
)

data class AttendanceStatisticsResponse(
    val success: Boolean,
    val data: AttendanceStatisticsData?,
    val error: ErrorData?
)

data class AttendanceStatisticsData(
    val summary: AttendanceSummary,
    val daily: List<DailyAttendance>?,
    val weekly: List<WeeklyAttendance>?,
    val monthly: List<MonthlyAttendance>?
)

data class AttendanceSummary(
    val totalDays: Int,
    val presentDays: Int,
    val lateDays: Int,
    val absentDays: Int,
    val averageWorkingHours: Double,
    val attendanceRate: Double,
    val punctualityRate: Double
)

data class DailyAttendance(
    val date: String,
    val status: String,
    val workingHours: Double?
)

data class WeeklyAttendance(
    val week: Int,
    val year: Int,
    val presentDays: Int,
    val totalWorkingHours: Double
)

data class MonthlyAttendance(
    val month: Int,
    val year: Int,
    val presentDays: Int,
    val totalWorkingHours: Double
)

data class UpdateAttendanceRequest(
    val checkInTime: String? = null,
    val checkOutTime: String? = null,
    val status: String? = null,
    val notes: String? = null
)