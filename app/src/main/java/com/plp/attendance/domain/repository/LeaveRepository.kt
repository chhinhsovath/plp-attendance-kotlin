package com.plp.attendance.domain.repository

import com.plp.attendance.domain.model.Leave
import com.plp.attendance.domain.model.LeaveStatus
import com.plp.attendance.domain.model.LeaveType
import kotlinx.coroutines.flow.Flow
import java.io.File

interface LeaveRepository {
    // Remote operations
    suspend fun getLeaveRequests(
        status: String? = null,
        page: Int = 1,
        limit: Int = 20
    ): Result<List<Leave>>
    
    suspend fun getLeaveRequestById(leaveId: String): Result<Leave>
    
    suspend fun submitLeaveRequest(
        leaveType: LeaveType,
        startDate: Long,
        endDate: Long,
        reason: String,
        attachmentFile: File? = null,
        isHalfDay: Boolean = false,
        halfDayPeriod: String? = null,
        emergencyContact: String? = null,
        substituteTeacherId: String? = null
    ): Result<Leave>
    
    suspend fun updateLeaveRequest(
        leaveId: String,
        leaveType: LeaveType? = null,
        startDate: Long? = null,
        endDate: Long? = null,
        reason: String? = null,
        attachmentFile: File? = null
    ): Result<Leave>
    
    suspend fun deleteLeaveRequest(leaveId: String): Result<Unit>
    
    suspend fun approveLeaveRequest(
        leaveId: String,
        comments: String? = null
    ): Result<Leave>
    
    suspend fun rejectLeaveRequest(
        leaveId: String,
        comments: String? = null
    ): Result<Leave>
    
    suspend fun cancelLeaveRequest(leaveId: String): Result<Leave>
    
    // Leave balance operations
    suspend fun getLeaveBalance(userId: String? = null): Result<LeaveBalance>
    
    // Calendar operations
    suspend fun getLeaveCalendar(
        startDate: String,
        endDate: String,
        departmentId: String? = null
    ): Result<LeaveCalendar>
    
    // Management operations
    suspend fun getPendingApprovals(): Result<List<Leave>>
    
    suspend fun getLeaveStatistics(year: Int): Result<LeaveStatistics>
    
    // Local operations (cached data)
    fun getCachedLeaves(): Flow<List<Leave>>
    
    fun getCachedLeavesByStatus(status: LeaveStatus): Flow<List<Leave>>
    
    fun getCachedLeavesByDateRange(startDate: Long, endDate: Long): Flow<List<Leave>>
    
    suspend fun getCachedLeaveById(leaveId: String): Leave?
    
    suspend fun refreshLeaves(): Result<Unit>
    
    // Sync operations
    suspend fun syncPendingLeaves(): Result<Unit>
}

// Additional data classes for leave balance and statistics
data class LeaveBalance(
    val userId: String,
    val year: Int,
    val balances: List<LeaveTypeBalance>,
    val summary: LeaveBalanceSummary
)

data class LeaveTypeBalance(
    val leaveType: LeaveType,
    val entitledDays: Int,
    val usedDays: Float,
    val pendingDays: Float,
    val remainingDays: Float,
    val carryForwardDays: Float = 0f,
    val expiresOn: Long? = null
)

data class LeaveBalanceSummary(
    val totalEntitled: Int,
    val totalUsed: Float,
    val totalPending: Float,
    val totalRemaining: Float
)

data class LeaveCalendar(
    val month: Int,
    val year: Int,
    val leaves: List<LeaveCalendarEntry>,
    val holidays: List<Holiday>,
    val summary: LeaveCalendarSummary
)

data class LeaveCalendarEntry(
    val date: Long,
    val leaves: List<LeaveCalendarItem>
)

data class LeaveCalendarItem(
    val id: String,
    val userId: String,
    val userName: String,
    val leaveType: LeaveType,
    val status: LeaveStatus,
    val isHalfDay: Boolean,
    val halfDayPeriod: String?
)

data class Holiday(
    val date: Long,
    val name: String,
    val type: String
)

data class LeaveCalendarSummary(
    val totalLeaveDays: Int,
    val approvedLeaves: Int,
    val pendingLeaves: Int,
    val holidays: Int,
    val workingDays: Int
)

data class LeaveStatistics(
    val period: StatisticsPeriod,
    val overall: OverallLeaveStatistics,
    val byType: List<LeaveTypeStatistics>,
    val byDepartment: List<DepartmentLeaveStatistics>,
    val trends: List<LeaveTrend>,
    val topReasons: List<LeaveReason>
)

data class StatisticsPeriod(
    val startDate: Long,
    val endDate: Long,
    val type: String
)

data class OverallLeaveStatistics(
    val totalLeaves: Int,
    val totalDays: Float,
    val approvedLeaves: Int,
    val rejectedLeaves: Int,
    val pendingLeaves: Int,
    val cancelledLeaves: Int,
    val averageDaysPerLeave: Float,
    val approvalRate: Float,
    val averageApprovalTimeHours: Float
)

data class LeaveTypeStatistics(
    val leaveType: LeaveType,
    val count: Int,
    val totalDays: Float,
    val percentage: Float,
    val averageDuration: Float
)

data class DepartmentLeaveStatistics(
    val departmentId: String,
    val departmentName: String,
    val totalEmployees: Int,
    val totalLeaves: Int,
    val totalDays: Float,
    val averagePerEmployee: Float,
    val absenteeismRate: Float
)

data class LeaveTrend(
    val period: String,
    val totalLeaves: Int,
    val totalDays: Float,
    val growthPercentage: Float
)

data class LeaveReason(
    val reasonCategory: String,
    val count: Int,
    val percentage: Float
)