package com.plp.attendance.data.remote.dto

import com.google.gson.annotations.SerializedName
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * DTOs for Leave Management API operations
 */

// List Response DTOs
data class LeaveListResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: LeaveListData,
    @SerializedName("message") val message: String? = null
)

data class LeaveListData(
    @SerializedName("leaves") val leaves: List<LeaveDto>,
    @SerializedName("pagination") val pagination: PaginationDto
)

// Single Leave Response
data class LeaveResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: LeaveDto,
    @SerializedName("message") val message: String? = null
)

// Core Leave DTO
data class LeaveDto(
    @SerializedName("id") val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("user_name") val userName: String,
    @SerializedName("user_role") val userRole: String,
    @SerializedName("department") val department: String?,
    @SerializedName("school") val school: String?,
    @SerializedName("leave_type") val leaveType: String,
    @SerializedName("start_date") val startDate: String,
    @SerializedName("end_date") val endDate: String,
    @SerializedName("total_days") val totalDays: Int,
    @SerializedName("reason") val reason: String,
    @SerializedName("status") val status: String,
    @SerializedName("approver_id") val approverId: String?,
    @SerializedName("approver_name") val approverName: String?,
    @SerializedName("approver_comment") val approverComment: String?,
    @SerializedName("approved_at") val approvedAt: String?,
    @SerializedName("attachment_url") val attachmentUrl: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("is_half_day") val isHalfDay: Boolean = false,
    @SerializedName("half_day_period") val halfDayPeriod: String? = null, // "morning" or "afternoon"
    @SerializedName("emergency_contact") val emergencyContact: String? = null,
    @SerializedName("substitute_teacher_id") val substituteTeacherId: String? = null,
    @SerializedName("substitute_teacher_name") val substituteTeacherName: String? = null
)

// Request DTOs
data class CreateLeaveRequest(
    @SerializedName("leave_type") val leaveType: String,
    @SerializedName("start_date") val startDate: String,
    @SerializedName("end_date") val endDate: String,
    @SerializedName("reason") val reason: String,
    @SerializedName("is_half_day") val isHalfDay: Boolean = false,
    @SerializedName("half_day_period") val halfDayPeriod: String? = null,
    @SerializedName("emergency_contact") val emergencyContact: String? = null,
    @SerializedName("substitute_teacher_id") val substituteTeacherId: String? = null,
    @SerializedName("attachment") val attachment: String? = null // Base64 encoded attachment
)

data class UpdateLeaveRequest(
    @SerializedName("leave_type") val leaveType: String? = null,
    @SerializedName("start_date") val startDate: String? = null,
    @SerializedName("end_date") val endDate: String? = null,
    @SerializedName("reason") val reason: String? = null,
    @SerializedName("is_half_day") val isHalfDay: Boolean? = null,
    @SerializedName("half_day_period") val halfDayPeriod: String? = null,
    @SerializedName("emergency_contact") val emergencyContact: String? = null,
    @SerializedName("substitute_teacher_id") val substituteTeacherId: String? = null,
    @SerializedName("attachment") val attachment: String? = null
)

data class LeaveApprovalRequest(
    @SerializedName("action") val action: String, // "approve" or "reject"
    @SerializedName("comment") val comment: String? = null,
    @SerializedName("approver_id") val approverId: String
)

// Leave Balance Response
data class LeaveBalanceResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: LeaveBalanceData,
    @SerializedName("message") val message: String? = null
)

data class LeaveBalanceData(
    @SerializedName("user_id") val userId: String,
    @SerializedName("year") val year: Int,
    @SerializedName("balances") val balances: List<LeaveBalanceDto>,
    @SerializedName("summary") val summary: LeaveBalanceSummaryDto
)

data class LeaveBalanceDto(
    @SerializedName("leave_type") val leaveType: String,
    @SerializedName("entitled_days") val entitledDays: Int,
    @SerializedName("used_days") val usedDays: Float,
    @SerializedName("pending_days") val pendingDays: Float,
    @SerializedName("remaining_days") val remainingDays: Float,
    @SerializedName("carry_forward_days") val carryForwardDays: Float = 0f,
    @SerializedName("expires_on") val expiresOn: String? = null
)

data class LeaveBalanceSummaryDto(
    @SerializedName("total_entitled") val totalEntitled: Int,
    @SerializedName("total_used") val totalUsed: Float,
    @SerializedName("total_pending") val totalPending: Float,
    @SerializedName("total_remaining") val totalRemaining: Float
)

// Leave Calendar Response
data class LeaveCalendarResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: LeaveCalendarData,
    @SerializedName("message") val message: String? = null
)

data class LeaveCalendarData(
    @SerializedName("month") val month: Int,
    @SerializedName("year") val year: Int,
    @SerializedName("leaves") val leaves: List<LeaveCalendarEntryDto>,
    @SerializedName("holidays") val holidays: List<HolidayDto>,
    @SerializedName("summary") val summary: LeaveCalendarSummaryDto
)

data class LeaveCalendarEntryDto(
    @SerializedName("date") val date: String,
    @SerializedName("leaves") val leaves: List<LeaveCalendarItemDto>
)

data class LeaveCalendarItemDto(
    @SerializedName("id") val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("user_name") val userName: String,
    @SerializedName("leave_type") val leaveType: String,
    @SerializedName("status") val status: String,
    @SerializedName("is_half_day") val isHalfDay: Boolean,
    @SerializedName("half_day_period") val halfDayPeriod: String?
)

data class HolidayDto(
    @SerializedName("date") val date: String,
    @SerializedName("name") val name: String,
    @SerializedName("type") val type: String // "national", "school", "regional"
)

data class LeaveCalendarSummaryDto(
    @SerializedName("total_leave_days") val totalLeaveDays: Int,
    @SerializedName("approved_leaves") val approvedLeaves: Int,
    @SerializedName("pending_leaves") val pendingLeaves: Int,
    @SerializedName("holidays") val holidays: Int,
    @SerializedName("working_days") val workingDays: Int
)

// Leave Statistics Response
data class LeaveStatisticsResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: LeaveStatisticsData,
    @SerializedName("message") val message: String? = null
)

data class LeaveStatisticsData(
    @SerializedName("period") val period: StatisticsPeriodDto,
    @SerializedName("overall") val overall: OverallLeaveStatisticsDto,
    @SerializedName("by_type") val byType: List<LeaveTypeStatisticsDto>,
    @SerializedName("by_department") val byDepartment: List<DepartmentLeaveStatisticsDto>,
    @SerializedName("trends") val trends: List<LeaveTrendDto>,
    @SerializedName("top_reasons") val topReasons: List<LeaveReasonDto>
)

data class StatisticsPeriodDto(
    @SerializedName("start_date") val startDate: String,
    @SerializedName("end_date") val endDate: String,
    @SerializedName("type") val type: String // "monthly", "quarterly", "yearly"
)

data class OverallLeaveStatisticsDto(
    @SerializedName("total_leaves") val totalLeaves: Int,
    @SerializedName("total_days") val totalDays: Float,
    @SerializedName("approved_leaves") val approvedLeaves: Int,
    @SerializedName("rejected_leaves") val rejectedLeaves: Int,
    @SerializedName("pending_leaves") val pendingLeaves: Int,
    @SerializedName("cancelled_leaves") val cancelledLeaves: Int,
    @SerializedName("average_days_per_leave") val averageDaysPerLeave: Float,
    @SerializedName("approval_rate") val approvalRate: Float,
    @SerializedName("average_approval_time_hours") val averageApprovalTimeHours: Float
)

data class LeaveTypeStatisticsDto(
    @SerializedName("leave_type") val leaveType: String,
    @SerializedName("count") val count: Int,
    @SerializedName("total_days") val totalDays: Float,
    @SerializedName("percentage") val percentage: Float,
    @SerializedName("average_duration") val averageDuration: Float
)

data class DepartmentLeaveStatisticsDto(
    @SerializedName("department_id") val departmentId: String,
    @SerializedName("department_name") val departmentName: String,
    @SerializedName("total_employees") val totalEmployees: Int,
    @SerializedName("total_leaves") val totalLeaves: Int,
    @SerializedName("total_days") val totalDays: Float,
    @SerializedName("average_per_employee") val averagePerEmployee: Float,
    @SerializedName("absenteeism_rate") val absenteeismRate: Float
)

data class LeaveTrendDto(
    @SerializedName("period") val period: String, // e.g., "2024-01", "Q1-2024"
    @SerializedName("total_leaves") val totalLeaves: Int,
    @SerializedName("total_days") val totalDays: Float,
    @SerializedName("growth_percentage") val growthPercentage: Float
)

data class LeaveReasonDto(
    @SerializedName("reason_category") val reasonCategory: String,
    @SerializedName("count") val count: Int,
    @SerializedName("percentage") val percentage: Float
)

// Pagination DTO (reusable)
data class PaginationDto(
    @SerializedName("current_page") val currentPage: Int,
    @SerializedName("per_page") val perPage: Int,
    @SerializedName("total_pages") val totalPages: Int,
    @SerializedName("total_items") val totalItems: Int,
    @SerializedName("has_next") val hasNext: Boolean,
    @SerializedName("has_previous") val hasPrevious: Boolean
)

// Error Response
data class LeaveErrorResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("error") val error: ErrorDto,
    @SerializedName("message") val message: String
)

data class ErrorDto(
    @SerializedName("code") val code: String,
    @SerializedName("details") val details: Map<String, List<String>>? = null
)

// Approval Chain Response
data class LeaveApprovalChainResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: List<ApprovalChainDto>,
    @SerializedName("message") val message: String? = null
)

data class ApprovalChainDto(
    @SerializedName("level") val level: Int,
    @SerializedName("approver_id") val approverId: String,
    @SerializedName("approver_name") val approverName: String,
    @SerializedName("approver_role") val approverRole: String,
    @SerializedName("status") val status: String, // "pending", "approved", "rejected"
    @SerializedName("comment") val comment: String?,
    @SerializedName("action_date") val actionDate: String?
)

// Leave Policy Response
data class LeavePolicyResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: List<LeavePolicyDto>,
    @SerializedName("message") val message: String? = null
)

data class LeavePolicyDto(
    @SerializedName("leave_type") val leaveType: String,
    @SerializedName("annual_entitlement") val annualEntitlement: Int,
    @SerializedName("carry_forward_allowed") val carryForwardAllowed: Boolean,
    @SerializedName("max_carry_forward") val maxCarryForward: Int?,
    @SerializedName("requires_approval") val requiresApproval: Boolean,
    @SerializedName("min_days_notice") val minDaysNotice: Int,
    @SerializedName("max_consecutive_days") val maxConsecutiveDays: Int?,
    @SerializedName("documentation_required") val documentationRequired: Boolean,
    @SerializedName("half_day_allowed") val halfDayAllowed: Boolean,
    @SerializedName("description") val description: String
)