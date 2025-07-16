package com.plp.attendance.data.repository

import com.plp.attendance.data.local.SessionManager
import com.plp.attendance.data.local.dao.LeaveDao
import com.plp.attendance.data.local.entities.LeaveEntity
import com.plp.attendance.data.remote.api.LeaveApi
import com.plp.attendance.data.remote.dto.*
import com.plp.attendance.domain.model.Leave
import com.plp.attendance.domain.model.LeaveStatus
import com.plp.attendance.domain.model.LeaveType
import com.plp.attendance.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LeaveRepositoryImpl @Inject constructor(
    private val leaveApi: LeaveApi,
    private val leaveDao: LeaveDao,
    private val sessionManager: SessionManager
) : LeaveRepository {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override suspend fun getLeaveRequests(
        status: String?,
        page: Int,
        limit: Int
    ): Result<List<Leave>> {
        return try {
            val token = getAuthToken()
            val response = leaveApi.getLeaveRequests(token, status, page, limit)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val leaves = response.body()!!.data.leaves.map { it.toDomain() }
                
                // Cache the leaves
                leaves.forEach { leave ->
                    leaveDao.insertLeave(leave.toEntity())
                }
                
                Result.success(leaves)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to fetch leave requests"))
            }
        } catch (e: Exception) {
            // Fallback to cached data
            val cachedLeaves = leaveDao.getAllLeaves().first().map { it.toDomain() }
            if (cachedLeaves.isNotEmpty()) {
                Result.success(cachedLeaves)
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun getLeaveRequestById(leaveId: String): Result<Leave> {
        return try {
            val token = getAuthToken()
            val response = leaveApi.getLeaveRequestById(token, leaveId)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val leave = response.body()!!.data.toDomain()
                
                // Cache the leave
                leaveDao.insertLeave(leave.toEntity())
                
                Result.success(leave)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to fetch leave request"))
            }
        } catch (e: Exception) {
            // Fallback to cached data
            val cachedLeave = leaveDao.getLeaveById(leaveId)?.toDomain()
            if (cachedLeave != null) {
                Result.success(cachedLeave)
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun submitLeaveRequest(
        leaveType: LeaveType,
        startDate: Long,
        endDate: Long,
        reason: String,
        attachmentFile: File?,
        isHalfDay: Boolean,
        halfDayPeriod: String?,
        emergencyContact: String?,
        substituteTeacherId: String?
    ): Result<Leave> {
        return try {
            val token = getAuthToken()
            val userId = sessionManager.getUserIdSuspend() ?: throw Exception("User not logged in")
            
            // Create local entity first for offline support
            val localLeave = LeaveEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                leaveType = leaveType,
                startDate = startDate,
                endDate = endDate,
                reason = reason,
                status = LeaveStatus.PENDING,
                attachmentUrl = attachmentFile?.path,
                isSynced = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            
            // Save to local database
            leaveDao.insertLeave(localLeave)
            
            try {
                // Prepare multipart request
                val attachmentPart = attachmentFile?.let { file ->
                    val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("attachment", file.name, requestFile)
                }
                
                // Make API call
                val response = leaveApi.submitLeaveRequest(
                    token = token,
                    leaveType = leaveType.name,
                    startDate = dateFormat.format(Date(startDate)),
                    endDate = dateFormat.format(Date(endDate)),
                    reason = reason,
                    attachment = attachmentPart
                )
                
                if (response.isSuccessful && response.body()?.success == true) {
                    val remoteLeave = response.body()!!.data.toDomain()
                    
                    // Update local entity with server data
                    val syncedLeave = localLeave.copy(
                        id = remoteLeave.id,
                        isSynced = true,
                        updatedAt = System.currentTimeMillis()
                    )
                    leaveDao.updateLeave(syncedLeave)
                    
                    Result.success(remoteLeave)
                } else {
                    Result.failure(Exception(response.body()?.message ?: "Failed to submit leave request"))
                }
            } catch (e: Exception) {
                // Return local leave if network fails
                Result.success(localLeave.toDomain())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateLeaveRequest(
        leaveId: String,
        leaveType: LeaveType?,
        startDate: Long?,
        endDate: Long?,
        reason: String?,
        attachmentFile: File?
    ): Result<Leave> {
        return try {
            val token = getAuthToken()
            
            val request = UpdateLeaveRequest(
                leaveType = leaveType?.name,
                startDate = startDate?.let { dateFormat.format(Date(it)) },
                endDate = endDate?.let { dateFormat.format(Date(it)) },
                reason = reason,
                attachment = null // TODO: Handle file upload for update
            )
            
            val response = leaveApi.updateLeaveRequest(token, leaveId, request)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val leave = response.body()!!.data.toDomain()
                
                // Update cache
                leaveDao.updateLeave(leave.toEntity())
                
                Result.success(leave)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to update leave request"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteLeaveRequest(leaveId: String): Result<Unit> {
        return try {
            val token = getAuthToken()
            val response = leaveApi.deleteLeaveRequest(token, leaveId)
            
            if (response.isSuccessful) {
                // Remove from cache
                leaveDao.deleteLeaveById(leaveId)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete leave request"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun approveLeaveRequest(
        leaveId: String,
        comments: String?
    ): Result<Leave> {
        return try {
            val token = getAuthToken()
            val approverId = sessionManager.getUserIdSuspend() ?: throw Exception("User not logged in")
            
            val request = LeaveApprovalRequest(
                action = "approve",
                comment = comments,
                approverId = approverId
            )
            
            val response = leaveApi.approveLeaveRequest(token, leaveId, request)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val leave = response.body()!!.data.toDomain()
                
                // Update cache
                leaveDao.updateLeave(leave.toEntity())
                
                Result.success(leave)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to approve leave request"))
            }
        } catch (e: Exception) {
            // Handle offline approval
            val existingLeave = leaveDao.getLeaveById(leaveId)
            if (existingLeave != null) {
                val approverId = sessionManager.getUserIdSuspend() ?: ""
                val updatedLeave = existingLeave.copy(
                    status = LeaveStatus.APPROVED,
                    approverId = approverId,
                    approverComments = comments,
                    approvedAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    isSynced = false
                )
                leaveDao.updateLeave(updatedLeave)
                Result.success(updatedLeave.toDomain())
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun rejectLeaveRequest(
        leaveId: String,
        comments: String?
    ): Result<Leave> {
        return try {
            val token = getAuthToken()
            val approverId = sessionManager.getUserIdSuspend() ?: throw Exception("User not logged in")
            
            val request = LeaveApprovalRequest(
                action = "reject",
                comment = comments,
                approverId = approverId
            )
            
            val response = leaveApi.rejectLeaveRequest(token, leaveId, request)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val leave = response.body()!!.data.toDomain()
                
                // Update cache
                leaveDao.updateLeave(leave.toEntity())
                
                Result.success(leave)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to reject leave request"))
            }
        } catch (e: Exception) {
            // Handle offline rejection
            val existingLeave = leaveDao.getLeaveById(leaveId)
            if (existingLeave != null) {
                val approverId = sessionManager.getUserIdSuspend() ?: ""
                val updatedLeave = existingLeave.copy(
                    status = LeaveStatus.REJECTED,
                    approverId = approverId,
                    approverComments = comments,
                    approvedAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    isSynced = false
                )
                leaveDao.updateLeave(updatedLeave)
                Result.success(updatedLeave.toDomain())
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun cancelLeaveRequest(leaveId: String): Result<Leave> {
        return try {
            val token = getAuthToken()
            val response = leaveApi.cancelLeaveRequest(token, leaveId)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val leave = response.body()!!.data.toDomain()
                
                // Update cache
                leaveDao.updateLeave(leave.toEntity())
                
                Result.success(leave)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to cancel leave request"))
            }
        } catch (e: Exception) {
            // Handle offline cancellation
            val existingLeave = leaveDao.getLeaveById(leaveId)
            if (existingLeave != null && existingLeave.status == LeaveStatus.PENDING) {
                val updatedLeave = existingLeave.copy(
                    status = LeaveStatus.CANCELLED,
                    updatedAt = System.currentTimeMillis(),
                    isSynced = false
                )
                leaveDao.updateLeave(updatedLeave)
                Result.success(updatedLeave.toDomain())
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun getLeaveBalance(userId: String?): Result<LeaveBalance> {
        return try {
            val token = getAuthToken()
            val targetUserId = userId ?: sessionManager.getUserIdSuspend() 
                ?: throw Exception("User not logged in")
            
            val response = leaveApi.getLeaveBalance(token, targetUserId)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val balanceData = response.body()!!.data
                Result.success(balanceData.toDomain())
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to fetch leave balance"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getLeaveCalendar(
        startDate: String,
        endDate: String,
        departmentId: String?
    ): Result<LeaveCalendar> {
        return try {
            val token = getAuthToken()
            val response = leaveApi.getLeaveCalendar(token, startDate, endDate, departmentId)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val calendarData = response.body()!!.data
                Result.success(calendarData.toDomain())
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to fetch leave calendar"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPendingApprovals(): Result<List<Leave>> {
        return try {
            val token = getAuthToken()
            val response = leaveApi.getPendingApprovals(token)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val leaves = response.body()!!.data.leaves.map { it.toDomain() }
                Result.success(leaves)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to fetch pending approvals"))
            }
        } catch (e: Exception) {
            // Fallback to cached pending leaves
            val cachedLeaves = leaveDao.getLeavesByStatus(LeaveStatus.PENDING).first()
                .map { it.toDomain() }
            if (cachedLeaves.isNotEmpty()) {
                Result.success(cachedLeaves)
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun getLeaveStatistics(year: Int): Result<LeaveStatistics> {
        return try {
            val token = getAuthToken()
            val response = leaveApi.getLeaveStatistics(token, year)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val statsData = response.body()!!.data
                Result.success(statsData.toDomain())
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to fetch leave statistics"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getCachedLeaves(): Flow<List<Leave>> {
        return leaveDao.getAllLeaves().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getCachedLeavesByStatus(status: LeaveStatus): Flow<List<Leave>> {
        return leaveDao.getLeavesByStatus(status).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getCachedLeavesByDateRange(startDate: Long, endDate: Long): Flow<List<Leave>> {
        return leaveDao.getLeavesByDateRange(startDate, endDate).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getCachedLeaveById(leaveId: String): Leave? {
        return leaveDao.getLeaveById(leaveId)?.toDomain()
    }

    override suspend fun refreshLeaves(): Result<Unit> {
        return try {
            val result = getLeaveRequests(status = null, page = 1, limit = 100)
            if (result.isSuccess) {
                Result.success(Unit)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Failed to refresh leaves"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncPendingLeaves(): Result<Unit> {
        return try {
            val unsyncedLeaves = leaveDao.getUnsyncedLeaves().first()
            
            for (leave in unsyncedLeaves) {
                try {
                    when (leave.status) {
                        LeaveStatus.PENDING -> {
                            // Sync new leave request
                            val result = submitLeaveRequest(
                                leaveType = leave.leaveType,
                                startDate = leave.startDate,
                                endDate = leave.endDate,
                                reason = leave.reason,
                                attachmentFile = leave.attachmentUrl?.let { File(it) }
                            )
                            if (result.isSuccess) {
                                val syncedLeave = leave.copy(
                                    id = result.getOrNull()?.id ?: leave.id,
                                    isSynced = true
                                )
                                leaveDao.updateLeave(syncedLeave)
                            }
                        }
                        LeaveStatus.APPROVED -> {
                            // Sync approval
                            approveLeaveRequest(leave.id, leave.approverComments)
                        }
                        LeaveStatus.REJECTED -> {
                            // Sync rejection
                            rejectLeaveRequest(leave.id, leave.approverComments)
                        }
                        LeaveStatus.CANCELLED -> {
                            // Sync cancellation
                            cancelLeaveRequest(leave.id)
                        }
                    }
                } catch (e: Exception) {
                    // Continue with other leaves even if one fails
                    continue
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun getAuthToken(): String {
        return sessionManager.getAuthTokenSuspend()?.let { "Bearer $it" }
            ?: throw Exception("No authentication token found")
    }

    // Extension functions to convert between DTOs and domain models
    private fun LeaveDto.toDomain(): Leave {
        return Leave(
            id = id,
            userId = userId,
            leaveType = LeaveType.valueOf(leaveType.uppercase()),
            startDate = dateFormat.parse(startDate)?.time ?: 0L,
            endDate = dateFormat.parse(endDate)?.time ?: 0L,
            reason = reason,
            status = LeaveStatus.valueOf(status.uppercase()),
            approverId = approverId,
            approverComments = approverComment,
            attachmentUrl = attachmentUrl,
            requestedAt = dateFormat.parse(createdAt)?.time ?: 0L,
            approvedAt = approvedAt?.let { dateFormat.parse(it)?.time },
            isSynced = true,
            createdAt = dateFormat.parse(createdAt)?.time ?: 0L,
            updatedAt = dateFormat.parse(updatedAt)?.time ?: 0L
        )
    }

    private fun Leave.toEntity(): LeaveEntity {
        return LeaveEntity(
            id = id,
            userId = userId,
            leaveType = leaveType,
            startDate = startDate,
            endDate = endDate,
            reason = reason,
            status = status,
            approverId = approverId,
            approverComments = approverComments,
            attachmentUrl = attachmentUrl,
            requestedAt = requestedAt,
            approvedAt = approvedAt,
            isSynced = isSynced,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun LeaveEntity.toDomain(): Leave {
        return Leave(
            id = id,
            userId = userId,
            leaveType = leaveType,
            startDate = startDate,
            endDate = endDate,
            reason = reason,
            status = status,
            approverId = approverId,
            approverComments = approverComments,
            attachmentUrl = attachmentUrl,
            requestedAt = requestedAt,
            approvedAt = approvedAt,
            isSynced = isSynced,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun LeaveBalanceData.toDomain(): LeaveBalance {
        return LeaveBalance(
            userId = userId,
            year = year,
            balances = balances.map { it.toDomain() },
            summary = summary.toDomain()
        )
    }

    private fun LeaveBalanceDto.toDomain(): LeaveTypeBalance {
        return LeaveTypeBalance(
            leaveType = LeaveType.valueOf(leaveType.uppercase()),
            entitledDays = entitledDays,
            usedDays = usedDays,
            pendingDays = pendingDays,
            remainingDays = remainingDays,
            carryForwardDays = carryForwardDays,
            expiresOn = expiresOn?.let { dateFormat.parse(it)?.time }
        )
    }

    private fun LeaveBalanceSummaryDto.toDomain(): LeaveBalanceSummary {
        return LeaveBalanceSummary(
            totalEntitled = totalEntitled,
            totalUsed = totalUsed,
            totalPending = totalPending,
            totalRemaining = totalRemaining
        )
    }

    private fun LeaveCalendarData.toDomain(): LeaveCalendar {
        return LeaveCalendar(
            month = month,
            year = year,
            leaves = leaves.map { it.toDomain() },
            holidays = holidays.map { it.toDomain() },
            summary = summary.toDomain()
        )
    }

    private fun LeaveCalendarEntryDto.toDomain(): LeaveCalendarEntry {
        return LeaveCalendarEntry(
            date = dateFormat.parse(date)?.time ?: 0L,
            leaves = leaves.map { it.toDomain() }
        )
    }

    private fun LeaveCalendarItemDto.toDomain(): LeaveCalendarItem {
        return LeaveCalendarItem(
            id = id,
            userId = userId,
            userName = userName,
            leaveType = LeaveType.valueOf(leaveType.uppercase()),
            status = LeaveStatus.valueOf(status.uppercase()),
            isHalfDay = isHalfDay,
            halfDayPeriod = halfDayPeriod
        )
    }

    private fun HolidayDto.toDomain(): Holiday {
        return Holiday(
            date = dateFormat.parse(date)?.time ?: 0L,
            name = name,
            type = type
        )
    }

    private fun LeaveCalendarSummaryDto.toDomain(): LeaveCalendarSummary {
        return LeaveCalendarSummary(
            totalLeaveDays = totalLeaveDays,
            approvedLeaves = approvedLeaves,
            pendingLeaves = pendingLeaves,
            holidays = holidays,
            workingDays = workingDays
        )
    }

    private fun LeaveStatisticsData.toDomain(): LeaveStatistics {
        return LeaveStatistics(
            period = period.toDomain(),
            overall = overall.toDomain(),
            byType = byType.map { it.toDomain() },
            byDepartment = byDepartment.map { it.toDomain() },
            trends = trends.map { it.toDomain() },
            topReasons = topReasons.map { it.toDomain() }
        )
    }

    private fun StatisticsPeriodDto.toDomain(): StatisticsPeriod {
        return StatisticsPeriod(
            startDate = dateFormat.parse(startDate)?.time ?: 0L,
            endDate = dateFormat.parse(endDate)?.time ?: 0L,
            type = type
        )
    }

    private fun OverallLeaveStatisticsDto.toDomain(): OverallLeaveStatistics {
        return OverallLeaveStatistics(
            totalLeaves = totalLeaves,
            totalDays = totalDays,
            approvedLeaves = approvedLeaves,
            rejectedLeaves = rejectedLeaves,
            pendingLeaves = pendingLeaves,
            cancelledLeaves = cancelledLeaves,
            averageDaysPerLeave = averageDaysPerLeave,
            approvalRate = approvalRate,
            averageApprovalTimeHours = averageApprovalTimeHours
        )
    }

    private fun LeaveTypeStatisticsDto.toDomain(): LeaveTypeStatistics {
        return LeaveTypeStatistics(
            leaveType = LeaveType.valueOf(leaveType.uppercase()),
            count = count,
            totalDays = totalDays,
            percentage = percentage,
            averageDuration = averageDuration
        )
    }

    private fun DepartmentLeaveStatisticsDto.toDomain(): DepartmentLeaveStatistics {
        return DepartmentLeaveStatistics(
            departmentId = departmentId,
            departmentName = departmentName,
            totalEmployees = totalEmployees,
            totalLeaves = totalLeaves,
            totalDays = totalDays,
            averagePerEmployee = averagePerEmployee,
            absenteeismRate = absenteeismRate
        )
    }

    private fun LeaveTrendDto.toDomain(): LeaveTrend {
        return LeaveTrend(
            period = period,
            totalLeaves = totalLeaves,
            totalDays = totalDays,
            growthPercentage = growthPercentage
        )
    }

    private fun LeaveReasonDto.toDomain(): LeaveReason {
        return LeaveReason(
            reasonCategory = reasonCategory,
            count = count,
            percentage = percentage
        )
    }
}