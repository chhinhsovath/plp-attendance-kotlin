package com.plp.attendance.data.remote.api

import com.plp.attendance.data.remote.dto.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface AttendanceApi {
    
    @POST("api/attendance/check-in")
    suspend fun checkIn(
        @Body request: CheckInRequest
    ): Response<AttendanceResponse>
    
    @POST("api/attendance/check-out")
    suspend fun checkOut(
        @Body request: CheckOutRequest
    ): Response<AttendanceResponse>
    
    @GET("api/attendance/status")
    suspend fun getTodayStatus(): Response<AttendanceStatusResponse>
    
    @GET("api/attendance/history")
    suspend fun getAttendanceHistory(
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<AttendanceListResponse>
    
    @GET("api/attendance/user/{userId}")
    suspend fun getUserAttendance(
        @Path("userId") userId: String,
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): Response<AttendanceListResponse>
    
    @GET("api/attendance/department/{departmentId}")
    suspend fun getDepartmentAttendance(
        @Path("departmentId") departmentId: String,
        @Query("date") date: String
    ): Response<AttendanceListResponse>
    
    @GET("api/attendance/statistics")
    suspend fun getAttendanceStatistics(
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String,
        @Query("groupBy") groupBy: String? = "day" // day, week, month
    ): Response<AttendanceStatisticsResponse>
    
    @PUT("api/attendance/{id}")
    suspend fun updateAttendance(
        @Path("id") attendanceId: String,
        @Body request: UpdateAttendanceRequest
    ): Response<AttendanceResponse>
    
    @DELETE("api/attendance/{id}")
    suspend fun deleteAttendance(
        @Path("id") attendanceId: String
    ): Response<Unit>
}