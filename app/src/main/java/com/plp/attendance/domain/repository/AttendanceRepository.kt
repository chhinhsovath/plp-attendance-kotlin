package com.plp.attendance.domain.repository

import com.plp.attendance.data.remote.dto.AttendanceStatisticsData
import com.plp.attendance.data.remote.dto.AttendanceStatusData
import com.plp.attendance.domain.model.Attendance
import kotlinx.coroutines.flow.Flow
import java.io.File

interface AttendanceRepository {
    // Check-in/Check-out operations
    suspend fun checkIn(
        latitude: Double,
        longitude: Double,
        photoFile: File? = null,
        notes: String? = null
    ): Result<Attendance>
    
    suspend fun checkOut(
        latitude: Double,
        longitude: Double,
        photoFile: File? = null,
        notes: String? = null
    ): Result<Attendance>
    
    // Status and history
    suspend fun getTodayStatus(): Result<AttendanceStatusData>
    
    suspend fun getAttendanceHistory(
        startDate: String? = null,
        endDate: String? = null,
        page: Int = 1,
        limit: Int = 20
    ): Result<List<Attendance>>
    
    // Statistics
    suspend fun getAttendanceStatistics(
        startDate: String,
        endDate: String,
        groupBy: String? = "day"
    ): Result<AttendanceStatisticsData>
    
    // User-specific operations
    suspend fun getUserAttendance(
        userId: String,
        startDate: String,
        endDate: String
    ): Result<List<Attendance>>
    
    // Department operations
    suspend fun getDepartmentAttendance(
        departmentId: String,
        date: String
    ): Result<List<Attendance>>
    
    // Update and delete
    suspend fun updateAttendance(
        attendanceId: String,
        checkInTime: String? = null,
        checkOutTime: String? = null,
        status: String? = null,
        notes: String? = null
    ): Result<Attendance>
    
    suspend fun deleteAttendance(attendanceId: String): Result<Unit>
    
    // Local operations
    fun getCachedAttendanceHistory(): Flow<List<Attendance>>
    
    suspend fun syncUnsyncedAttendance(): Result<Unit>
}