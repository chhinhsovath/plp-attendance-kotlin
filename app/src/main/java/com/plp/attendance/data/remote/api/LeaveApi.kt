package com.plp.attendance.data.remote.api

import com.plp.attendance.data.remote.dto.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface LeaveApi {
    
    @GET("api/leave/requests")
    suspend fun getLeaveRequests(
        @Header("Authorization") token: String,
        @Query("status") status: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<LeaveListResponse>
    
    @GET("api/leave/requests/{id}")
    suspend fun getLeaveRequestById(
        @Header("Authorization") token: String,
        @Path("id") requestId: String
    ): Response<LeaveResponse>
    
    @POST("api/leave/request")
    @Multipart
    suspend fun submitLeaveRequest(
        @Header("Authorization") token: String,
        @Part("leaveType") leaveType: String,
        @Part("startDate") startDate: String,
        @Part("endDate") endDate: String,
        @Part("reason") reason: String,
        @Part attachment: MultipartBody.Part? = null
    ): Response<LeaveResponse>
    
    @PUT("api/leave/request/{id}")
    suspend fun updateLeaveRequest(
        @Header("Authorization") token: String,
        @Path("id") requestId: String,
        @Body request: UpdateLeaveRequest
    ): Response<LeaveResponse>
    
    @DELETE("api/leave/request/{id}")
    suspend fun deleteLeaveRequest(
        @Header("Authorization") token: String,
        @Path("id") requestId: String
    ): Response<Unit>
    
    @PUT("api/leave/request/{id}/approve")
    suspend fun approveLeaveRequest(
        @Header("Authorization") token: String,
        @Path("id") requestId: String,
        @Body request: LeaveApprovalRequest
    ): Response<LeaveResponse>
    
    @PUT("api/leave/request/{id}/reject")
    suspend fun rejectLeaveRequest(
        @Header("Authorization") token: String,
        @Path("id") requestId: String,
        @Body request: LeaveApprovalRequest
    ): Response<LeaveResponse>
    
    @PUT("api/leave/request/{id}/cancel")
    suspend fun cancelLeaveRequest(
        @Header("Authorization") token: String,
        @Path("id") requestId: String
    ): Response<LeaveResponse>
    
    @GET("api/leave/balance/{userId}")
    suspend fun getLeaveBalance(
        @Header("Authorization") token: String,
        @Path("userId") userId: String
    ): Response<LeaveBalanceResponse>
    
    @GET("api/leave/calendar")
    suspend fun getLeaveCalendar(
        @Header("Authorization") token: String,
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String,
        @Query("departmentId") departmentId: String? = null
    ): Response<LeaveCalendarResponse>
    
    @GET("api/leave/pending-approvals")
    suspend fun getPendingApprovals(
        @Header("Authorization") token: String
    ): Response<LeaveListResponse>
    
    @GET("api/leave/statistics")
    suspend fun getLeaveStatistics(
        @Header("Authorization") token: String,
        @Query("year") year: Int
    ): Response<LeaveStatisticsResponse>
    
    @POST("api/leave")
    suspend fun createLeave(
        @Body request: CreateLeaveRequest
    ): Response<LeaveResponse>
}