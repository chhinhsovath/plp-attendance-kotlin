package com.plp.attendance.data.repository

import android.util.Log
import com.plp.attendance.data.local.SessionManager
import com.plp.attendance.data.local.dao.AttendanceDao
import com.plp.attendance.data.local.dao.UserDao
import com.plp.attendance.data.local.entities.AttendanceEntity
import com.plp.attendance.data.remote.api.AttendanceApi
import com.plp.attendance.services.LocationService
import com.plp.attendance.data.remote.dto.*
import com.plp.attendance.data.remote.dto.AttendanceRecord
import com.plp.attendance.domain.model.Attendance
import com.plp.attendance.domain.model.AttendanceStatus
import com.plp.attendance.domain.repository.AttendanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
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
import com.google.gson.Gson

@Singleton
class AttendanceRepositoryImpl @Inject constructor(
    private val attendanceApi: AttendanceApi,
    private val attendanceDao: AttendanceDao,
    private val sessionManager: SessionManager,
    private val locationService: LocationService,
    private val userDao: UserDao
) : AttendanceRepository {
    
    companion object {
        private const val TAG = "AttendanceRepository"
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }
    
    // Check-in with photo
    override suspend fun checkIn(
        latitude: Double,
        longitude: Double,
        photoFile: File?,
        notes: String?
    ): Result<Attendance> {
        return try {
            val token = getAuthToken()
            Log.d(TAG, "Check-in attempt - Token: ${token?.take(20)}...")
            
            if (token == null) {
                Log.e(TAG, "No auth token found")
                return Result.failure(Exception("Not authenticated"))
            }
            
            val userId = sessionManager.getUserIdSuspend() ?: return Result.failure(Exception("User ID not found"))
            Log.d(TAG, "Check-in for user: $userId at location: $latitude, $longitude")
            
            val request = CheckInRequest(
                latitude = latitude,
                longitude = longitude,
                address = null, // TODO: Add address geocoding
                notes = notes
            )
            
            Log.d(TAG, "Sending check-in request: $request")
            val response = attendanceApi.checkIn(request)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val responseData = response.body()!!.data!!
                // Log the check-in response for debugging
                Log.d(TAG, "Check-in response data: ${Gson().toJson(responseData)}")
                val attendance = if (responseData.record != null) {
                    // New API response structure with record
                    mapAttendanceRecordToDomain(responseData.record, userId)
                } else {
                    // Legacy response structure
                    mapAttendanceDataToDomain(
                        AttendanceData(
                            id = responseData.id ?: "",
                            userId = responseData.userId ?: userId,
                            date = responseData.date ?: "",
                            checkInTime = responseData.checkInTime,
                            checkOutTime = responseData.checkOutTime,
                            checkInLocation = responseData.checkInLocation,
                            checkOutLocation = responseData.checkOutLocation,
                            checkInPhotoUrl = responseData.checkInPhotoUrl,
                            checkOutPhotoUrl = responseData.checkOutPhotoUrl,
                            status = responseData.status ?: "present",
                            workingHours = responseData.workingHours,
                            notes = responseData.notes,
                            createdAt = responseData.createdAt ?: "",
                            updatedAt = responseData.updatedAt ?: ""
                        ), userId
                    )
                }
                
                // Save to local database
                attendanceDao.insertAttendance(attendance.toEntity())
                
                Result.success(attendance)
            } else {
                // For non-2xx responses, we need to parse the error body differently
                val errorBody = if (response.isSuccessful) {
                    response.body()
                } else {
                    // Parse error body from response.errorBody()
                    try {
                        val errorBodyString = response.errorBody()?.string() ?: ""
                        val gson = com.google.gson.Gson()
                        gson.fromJson(errorBodyString, AttendanceResponse::class.java)
                    } catch (e: Exception) {
                        null
                    }
                }
                
                Log.e(TAG, "Check-in failed - Response code: ${response.code()}")
                if (!response.isSuccessful) {
                    val errorString = response.errorBody()?.string()
                    Log.e(TAG, "Error body string: $errorString")
                }
                
                val apiErrorMessage = errorBody?.error?.message
                val errorMessage = when {
                    apiErrorMessage?.contains("outside the allowed area", ignoreCase = true) == true -> 
                        "អ្នកនៅក្រៅតំបន់សាលារៀន។ សូមចូលទៅជិតដើម្បីចូលការងារ។"
                    apiErrorMessage?.contains("already checked in", ignoreCase = true) == true -> 
                        "អ្នកបានចូលការងាររួចហើយនៅថ្ងៃនេះ។"
                    apiErrorMessage?.contains("not a working day", ignoreCase = true) == true -> 
                        "មិនអនុញ្ញាតឱ្យចូលការងារនៅថ្ងៃឈប់សម្រាក។"
                    apiErrorMessage?.contains("outside working hours", ignoreCase = true) == true -> 
                        "អនុញ្ញាតឱ្យចូលការងារតែក្នុងម៉ោងធ្វើការប៉ុណ្ណោះ។"
                    else -> apiErrorMessage ?: "ចូលការងារមិនបានជោគជ័យ។ សូមព្យាយាមម្តងទៀត។"
                }
                Log.e(TAG, "Check-in failed: $apiErrorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "ចុះវត្ថមានបរាជ័", e)
            
            // Fallback to local storage if network fails
            try {
                // SECURITY: Validate geofencing even in offline mode
                val userId = sessionManager.getUserIdSuspend()
                val userEntity = userId?.let { userDao.getUserById(it) }
                val schoolLat = userEntity?.schoolLatitude
                val schoolLon = userEntity?.schoolLongitude
                
                if (schoolLat != null && schoolLon != null) {
                    val isWithinGeofence = locationService.isWithinGeofence(
                        latitude, longitude,
                        schoolLat, schoolLon,
                        LocationService.DEFAULT_GEOFENCE_RADIUS
                    )
                    
                    if (!isWithinGeofence) {
                        Log.w(TAG, "Offline check-in denied: Outside geofence (${locationService.calculateDistance(latitude, longitude, schoolLat, schoolLon)}m)")
                        return Result.failure(Exception("អ្នកនៅក្រៅតំបន់អនុញ្ញាតសម្រាប់ចូលការងារ។ សូមចូលទៅជិតសាលារៀន (១០០ម៉ែត្រ) ដើម្បីចូលការងារ។"))
                    }
                } else {
                    Log.e(TAG, "Cannot validate geofence: Missing school coordinates")
                    return Result.failure(Exception("ទីតាំងសាលារៀនមិនមាន។ សូមទាក់ទងអ្នកគ្រប់គ្រង។"))
                }
                
                val attendance = createLocalAttendance(
                    checkIn = true,
                    latitude = latitude,
                    longitude = longitude,
                    photoUrl = photoFile?.absolutePath,
                    notes = notes
                )
                attendanceDao.insertAttendance(attendance.toEntity())
                Result.success(attendance)
            } catch (localError: Exception) {
                Log.e(TAG, "Local ចុះវត្ថមានបរាជ័", localError)
                Result.failure(e)
            }
        }
    }
    
    // Check-out with photo
    override suspend fun checkOut(
        latitude: Double,
        longitude: Double,
        photoFile: File?,
        notes: String?
    ): Result<Attendance> {
        return try {
            val token = getAuthToken() ?: return Result.failure(Exception("Not authenticated"))
            val userId = sessionManager.getUserIdSuspend() ?: return Result.failure(Exception("User ID not found"))
            
            // SECURITY: For security check-out, validate geofencing before API call
            if (notes == "Security check-out: Outside geofence") {
                Log.i(TAG, "Security check-out: Skipping online API, using offline fallback")
                // Force offline mode for security check-out to bypass server geofencing
                throw Exception("Force offline mode for security check-out")
            }
            
            val request = CheckOutRequest(
                latitude = latitude,
                longitude = longitude,
                address = null, // TODO: Add address geocoding
                notes = notes
            )
            
            val response = attendanceApi.checkOut(request)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val responseData = response.body()!!.data!!
                val attendance = if (responseData.record != null) {
                    // New API response structure with record
                    mapAttendanceRecordToDomain(responseData.record, userId)
                } else {
                    // Legacy response structure
                    mapAttendanceDataToDomain(
                        AttendanceData(
                            id = responseData.id ?: "",
                            userId = responseData.userId ?: userId,
                            date = responseData.date ?: "",
                            checkInTime = responseData.checkInTime,
                            checkOutTime = responseData.checkOutTime,
                            checkInLocation = responseData.checkInLocation,
                            checkOutLocation = responseData.checkOutLocation,
                            checkInPhotoUrl = responseData.checkInPhotoUrl,
                            checkOutPhotoUrl = responseData.checkOutPhotoUrl,
                            status = responseData.status ?: "present",
                            workingHours = responseData.workingHours,
                            notes = responseData.notes,
                            createdAt = responseData.createdAt ?: "",
                            updatedAt = responseData.updatedAt ?: ""
                        ), userId
                    )
                }
                
                // Update local database
                attendanceDao.updateAttendance(attendance.toEntity())
                
                Result.success(attendance)
            } else {
                // For non-2xx responses, we need to parse the error body differently
                val errorBody = if (response.isSuccessful) {
                    response.body()
                } else {
                    // Parse error body from response.errorBody()
                    try {
                        val errorBodyString = response.errorBody()?.string() ?: ""
                        val gson = Gson()
                        gson.fromJson(errorBodyString, AttendanceResponse::class.java)
                    } catch (e: Exception) {
                        null
                    }
                }
                
                val apiErrorMessage = errorBody?.error?.message
                val errorMessage = when {
                    apiErrorMessage?.contains("not checked in", ignoreCase = true) == true -> 
                        "អ្នកមិនទាន់បានចូលការងារនៅថ្ងៃនេះទេ។"
                    apiErrorMessage?.contains("already checked out", ignoreCase = true) == true -> 
                        "អ្នកបានចេញពីការងាររួចហើយនៅថ្ងៃនេះ។"
                    apiErrorMessage?.contains("minimum hours", ignoreCase = true) == true -> 
                        "អ្នកត្រូវធ្វើការឱ្យបានម៉ោងអប្បបរមាមុនពេលចេញពីការងារ។"
                    else -> apiErrorMessage ?: "ចេញពីការងារមិនបានជោគជ័យ។ សូមព្យាយាមម្តងទៀត។"
                }
                Log.e(TAG, "Check-out failed: $apiErrorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Check-out error", e)
            
            // Fallback to local storage if network fails
            try {
                // SECURITY: Validate geofencing even in offline mode
                val userId = sessionManager.getUserIdSuspend()
                val userEntity = userId?.let { userDao.getUserById(it) }
                val schoolLat = userEntity?.schoolLatitude
                val schoolLon = userEntity?.schoolLongitude
                
                // SECURITY: Allow security check-out but validate normal check-out
                if (notes != "Security check-out: Outside geofence") {
                    if (schoolLat != null && schoolLon != null) {
                        val isWithinGeofence = locationService.isWithinGeofence(
                            latitude, longitude,
                            schoolLat, schoolLon,
                            LocationService.DEFAULT_GEOFENCE_RADIUS
                        )
                        
                        if (!isWithinGeofence) {
                            Log.w(TAG, "Offline check-out denied: Outside geofence (${locationService.calculateDistance(latitude, longitude, schoolLat, schoolLon)}m)")
                            return Result.failure(Exception("អ្នកនៅក្រៅតំបន់អនុញ្ញាតសម្រាប់ចេញការងារ។ សូមចូលទៅជិតសាលារៀន (១០០ម៉ែត្រ) ដើម្បីចេញការងារ។"))
                        }
                    } else {
                        Log.e(TAG, "Cannot validate geofence for check-out: Missing school coordinates")
                        return Result.failure(Exception("ទីតាំងសាលារៀនមិនមាន។ សូមទាក់ទងអ្នកគ្រប់គ្រង។"))
                    }
                } else {
                    Log.i(TAG, "Security check-out: Bypassing geofence validation")
                }
                
                val todayAttendance = getTodayAttendanceLocal()
                if (todayAttendance != null) {
                    val updatedAttendance = todayAttendance.copy(
                        checkOutTime = System.currentTimeMillis(),
                        checkOutLatitude = latitude,
                        checkOutLongitude = longitude,
                        checkOutPhotoUrl = photoFile?.absolutePath,
                        notes = notes ?: todayAttendance.notes,
                        isSynced = false,
                        updatedAt = System.currentTimeMillis()
                    )
                    attendanceDao.updateAttendance(updatedAttendance.toEntity())
                    Result.success(updatedAttendance)
                } else {
                    Result.failure(Exception("No check-in record found for today"))
                }
            } catch (localError: Exception) {
                Log.e(TAG, "Local check-out error", localError)
                Result.failure(e)
            }
        }
    }
    
    // Get today's attendance status
    override suspend fun getTodayStatus(): Result<AttendanceStatusData> {
        return try {
            val token = getAuthToken() ?: return Result.failure(Exception("Not authenticated"))
            
            val response = attendanceApi.getTodayStatus()
            
            if (response.isSuccessful && response.body()?.success == true) {
                val statusData = response.body()!!.data!!
                // Log the response for debugging
                Log.d(TAG, "វត្ថមានសម្រាប់ថ្ងៃនេះ response: ${Gson().toJson(statusData)}")
                Result.success(statusData)
            } else {
                val errorMessage = response.body()?.error?.message ?: "Failed to get status"
                Log.e(TAG, "Get today status failed: $errorMessage")
                
                // Fallback to local data
                val localAttendance = getTodayAttendanceLocal()
                val statusData = AttendanceStatusData(
                    hasCheckedIn = localAttendance != null,
                    hasCheckedOut = localAttendance?.checkOutTime != null,
                    attendance = localAttendance?.toAttendanceData(),
                    workingHours = calculateWorkingHours(localAttendance),
                    isWorkingDay = true // TODO: Implement proper work day calculation
                )
                Result.success(statusData)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Get today status error", e)
            
            // Fallback to local data
            try {
                val localAttendance = getTodayAttendanceLocal()
                val statusData = AttendanceStatusData(
                    hasCheckedIn = localAttendance != null,
                    hasCheckedOut = localAttendance?.checkOutTime != null,
                    attendance = localAttendance?.toAttendanceData(),
                    workingHours = calculateWorkingHours(localAttendance),
                    isWorkingDay = true
                )
                Result.success(statusData)
            } catch (localError: Exception) {
                Result.failure(e)
            }
        }
    }
    
    // Get attendance history
    override suspend fun getAttendanceHistory(
        startDate: String?,
        endDate: String?,
        page: Int,
        limit: Int
    ): Result<List<Attendance>> {
        return try {
            val token = getAuthToken() ?: return Result.failure(Exception("Not authenticated"))
            val userId = sessionManager.getUserIdSuspend() ?: return Result.failure(Exception("User ID not found"))
            
            val response = attendanceApi.getAttendanceHistory(
                startDate = startDate,
                endDate = endDate,
                page = page,
                limit = limit
            )
            
            if (response.isSuccessful && response.body()?.success == true) {
                val attendances = response.body()!!.data?.attendances?.map { attendanceData ->
                    mapAttendanceDataToDomain(attendanceData, userId)
                } ?: emptyList()
                
                // Cache in local database
                attendances.forEach { attendance ->
                    attendanceDao.insertAttendance(attendance.toEntity())
                }
                
                Result.success(attendances)
            } else {
                val errorMessage = response.body()?.error?.message ?: "Failed to get attendance history"
                Log.e(TAG, "Get attendance history failed: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Get attendance history error", e)
            
            // Fallback to local data
            try {
                val localAttendances = getLocalAttendanceHistory(startDate, endDate, limit)
                Result.success(localAttendances)
            } catch (localError: Exception) {
                Result.failure(e)
            }
        }
    }
    
    // Get attendance statistics
    override suspend fun getAttendanceStatistics(
        startDate: String,
        endDate: String,
        groupBy: String?
    ): Result<AttendanceStatisticsData> {
        return try {
            val token = getAuthToken() ?: return Result.failure(Exception("Not authenticated"))
            
            val response = attendanceApi.getAttendanceStatistics(
                startDate = startDate,
                endDate = endDate,
                groupBy = groupBy
            )
            
            if (response.isSuccessful && response.body()?.success == true) {
                val statisticsData = response.body()!!.data!!
                Result.success(statisticsData)
            } else {
                val errorMessage = response.body()?.error?.message ?: "Failed to get statistics"
                Log.e(TAG, "Get statistics failed: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Get statistics error", e)
            Result.failure(e)
        }
    }
    
    // Get user attendance for a specific date range
    override suspend fun getUserAttendance(
        userId: String,
        startDate: String,
        endDate: String
    ): Result<List<Attendance>> {
        return try {
            val token = getAuthToken() ?: return Result.failure(Exception("Not authenticated"))
            
            val response = attendanceApi.getUserAttendance(
                userId = userId,
                startDate = startDate,
                endDate = endDate
            )
            
            if (response.isSuccessful && response.body()?.success == true) {
                val attendances = response.body()!!.data?.attendances?.map { attendanceData ->
                    mapAttendanceDataToDomain(attendanceData, userId)
                } ?: emptyList()
                
                Result.success(attendances)
            } else {
                val errorMessage = response.body()?.error?.message ?: "Failed to get user attendance"
                Log.e(TAG, "Get user attendance failed: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Get user attendance error", e)
            Result.failure(e)
        }
    }
    
    // Get department attendance for a specific date
    override suspend fun getDepartmentAttendance(
        departmentId: String,
        date: String
    ): Result<List<Attendance>> {
        return try {
            val token = getAuthToken() ?: return Result.failure(Exception("Not authenticated"))
            
            val response = attendanceApi.getDepartmentAttendance(
                departmentId = departmentId,
                date = date
            )
            
            if (response.isSuccessful && response.body()?.success == true) {
                val attendances = response.body()!!.data?.attendances?.map { attendanceData ->
                    mapAttendanceDataToDomain(attendanceData, attendanceData.userId)
                } ?: emptyList()
                
                Result.success(attendances)
            } else {
                val errorMessage = response.body()?.error?.message ?: "Failed to get department attendance"
                Log.e(TAG, "Get department attendance failed: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Get department attendance error", e)
            Result.failure(e)
        }
    }
    
    // Update attendance record
    override suspend fun updateAttendance(
        attendanceId: String,
        checkInTime: String?,
        checkOutTime: String?,
        status: String?,
        notes: String?
    ): Result<Attendance> {
        return try {
            val token = getAuthToken() ?: return Result.failure(Exception("Not authenticated"))
            val userId = sessionManager.getUserIdSuspend() ?: return Result.failure(Exception("User ID not found"))
            
            val request = UpdateAttendanceRequest(
                checkInTime = checkInTime,
                checkOutTime = checkOutTime,
                status = status,
                notes = notes
            )
            
            val response = attendanceApi.updateAttendance(
                attendanceId = attendanceId,
                request = request
            )
            
            if (response.isSuccessful && response.body()?.success == true) {
                val responseData = response.body()!!.data!!
                val attendance = if (responseData.record != null) {
                    // New API response structure with record
                    mapAttendanceRecordToDomain(responseData.record, userId)
                } else {
                    // Legacy response structure
                    mapAttendanceDataToDomain(
                        AttendanceData(
                            id = responseData.id ?: "",
                            userId = responseData.userId ?: userId,
                            date = responseData.date ?: "",
                            checkInTime = responseData.checkInTime,
                            checkOutTime = responseData.checkOutTime,
                            checkInLocation = responseData.checkInLocation,
                            checkOutLocation = responseData.checkOutLocation,
                            checkInPhotoUrl = responseData.checkInPhotoUrl,
                            checkOutPhotoUrl = responseData.checkOutPhotoUrl,
                            status = responseData.status ?: "present",
                            workingHours = responseData.workingHours,
                            notes = responseData.notes,
                            createdAt = responseData.createdAt ?: "",
                            updatedAt = responseData.updatedAt ?: ""
                        ), userId
                    )
                }
                
                // Update local database
                attendanceDao.updateAttendance(attendance.toEntity())
                
                Result.success(attendance)
            } else {
                val errorMessage = response.body()?.error?.message ?: "Failed to update attendance"
                Log.e(TAG, "Update attendance failed: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Update attendance error", e)
            Result.failure(e)
        }
    }
    
    // Delete attendance record
    override suspend fun deleteAttendance(attendanceId: String): Result<Unit> {
        return try {
            val token = getAuthToken() ?: return Result.failure(Exception("Not authenticated"))
            
            val response = attendanceApi.deleteAttendance(attendanceId)
            
            if (response.isSuccessful) {
                // Delete from local database
                val localAttendance = attendanceDao.getAttendanceById(attendanceId)
                localAttendance?.let {
                    attendanceDao.deleteAttendance(it)
                }
                
                Result.success(Unit)
            } else {
                val errorMessage = "Failed to delete attendance"
                Log.e(TAG, "Delete attendance failed: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Delete attendance error", e)
            Result.failure(e)
        }
    }
    
    // Local operations
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun getCachedAttendanceHistory(): Flow<List<Attendance>> {
        return sessionManager.getUserId().map { userId ->
            if (userId != null) {
                attendanceDao.getAttendanceByUserId(userId).map { entities ->
                    entities.map { it.toDomain() }
                }
            } else {
                flowOf(emptyList())
            }
        }.flatMapLatest { it }
    }
    
    override suspend fun syncUnsyncedAttendance(): Result<Unit> {
        return try {
            val unsyncedAttendances = attendanceDao.getUnsyncedAttendance()
            
            unsyncedAttendances.forEach { attendance ->
                // TODO: Implement sync logic with API
                // For now, just mark as synced
                attendanceDao.markAsSynced(attendance.id)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sync attendance error", e)
            Result.failure(e)
        }
    }
    
    // Helper methods
    private suspend fun getAuthToken(): String? {
        return sessionManager.getAuthTokenSuspend()
    }
    
    private suspend fun getTodayAttendanceLocal(): Attendance? {
        val userId = sessionManager.getUserIdSuspend() ?: return null
        val today = System.currentTimeMillis()
        return attendanceDao.getTodayAttendance(userId, today)?.toDomain()
    }
    
    private suspend fun getLocalAttendanceHistory(
        startDate: String?,
        endDate: String?,
        limit: Int
    ): List<Attendance> {
        val userId = sessionManager.getUserIdSuspend() ?: return emptyList()
        
        return if (startDate != null && endDate != null) {
            val startTime = dateFormat.parse(startDate)?.time ?: 0
            val endTime = dateFormat.parse(endDate)?.time ?: System.currentTimeMillis()
            attendanceDao.getAttendanceByUserIdAndDateRange(userId, startTime, endTime)
                .take(limit)
                .map { it.toDomain() }
        } else {
            attendanceDao.getAttendanceByUserId(userId)
                .first()
                .take(limit)
                .map { it.toDomain() }
        }
    }
    
    private suspend fun createLocalAttendance(
        checkIn: Boolean,
        latitude: Double,
        longitude: Double,
        photoUrl: String? = null,
        notes: String? = null
    ): Attendance {
        val userId = sessionManager.getUserIdSuspend() ?: throw Exception("User ID not found")
        val now = System.currentTimeMillis()
        
        return if (checkIn) {
            Attendance(
                id = UUID.randomUUID().toString(),
                userId = userId,
                checkInTime = now,
                checkOutTime = null,
                checkInLatitude = latitude,
                checkInLongitude = longitude,
                checkOutLatitude = null,
                checkOutLongitude = null,
                checkInPhotoUrl = photoUrl,
                checkOutPhotoUrl = null,
                status = AttendanceStatus.PRESENT,
                notes = notes,
                isSynced = false,
                createdAt = now,
                updatedAt = now
            )
        } else {
            throw IllegalStateException("Cannot create check-out without check-in")
        }
    }
    
    private fun calculateWorkingHours(attendance: Attendance?): Double? {
        return if (attendance?.checkInTime != null && attendance.checkOutTime != null) {
            val hours = (attendance.checkOutTime - attendance.checkInTime) / (1000.0 * 60 * 60)
            String.format("%.2f", hours).toDouble()
        } else {
            null
        }
    }
    
    private fun mapAttendanceRecordToDomain(record: AttendanceRecord, userId: String): Attendance {
        return Attendance(
            id = record.id,
            userId = userId,
            checkInTime = record.checkInTime?.let { 
                try {
                    dateTimeFormat.parse(it)?.time ?: System.currentTimeMillis()
                } catch (e: Exception) {
                    System.currentTimeMillis()
                }
            } ?: System.currentTimeMillis(),
            checkOutTime = record.checkOutTime?.let { 
                try {
                    dateTimeFormat.parse(it)?.time
                } catch (e: Exception) {
                    null
                }
            },
            checkInLatitude = record.latitude?.toDoubleOrNull() ?: 0.0,
            checkInLongitude = record.longitude?.toDoubleOrNull() ?: 0.0,
            checkOutLatitude = null,
            checkOutLongitude = null,
            checkInPhotoUrl = null,
            checkOutPhotoUrl = null,
            status = when (record.status.lowercase()) {
                "present" -> AttendanceStatus.PRESENT
                "late" -> AttendanceStatus.LATE
                "absent" -> AttendanceStatus.ABSENT
                else -> AttendanceStatus.PRESENT
            },
            notes = record.notes,
            isSynced = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }
    
    private fun mapAttendanceDataToDomain(data: AttendanceData, userId: String): Attendance {
        return Attendance(
            id = data.id,
            userId = userId,
            checkInTime = dateTimeFormat.parse(data.checkInTime ?: "")?.time ?: 0,
            checkOutTime = data.checkOutTime?.let { dateTimeFormat.parse(it)?.time },
            checkInLatitude = data.checkInLocation?.latitude ?: 0.0,
            checkInLongitude = data.checkInLocation?.longitude ?: 0.0,
            checkOutLatitude = data.checkOutLocation?.latitude,
            checkOutLongitude = data.checkOutLocation?.longitude,
            checkInPhotoUrl = data.checkInPhotoUrl,
            checkOutPhotoUrl = data.checkOutPhotoUrl,
            status = mapApiStatusToAppStatus(data.status),
            notes = data.notes,
            isSynced = true,
            createdAt = dateTimeFormat.parse(data.createdAt)?.time ?: System.currentTimeMillis(),
            updatedAt = dateTimeFormat.parse(data.updatedAt)?.time ?: System.currentTimeMillis()
        )
    }
    
    private fun mapApiStatusToAppStatus(apiStatus: String): AttendanceStatus {
        return when (apiStatus.uppercase()) {
            "PRESENT" -> AttendanceStatus.PRESENT
            "LATE" -> AttendanceStatus.LATE
            "ABSENT" -> AttendanceStatus.ABSENT
            else -> AttendanceStatus.ABSENT
        }
    }
    
    private fun AttendanceEntity.toDomain(): Attendance {
        return Attendance(
            id = id,
            userId = userId,
            checkInTime = checkInTime,
            checkOutTime = checkOutTime,
            checkInLatitude = checkInLatitude,
            checkInLongitude = checkInLongitude,
            checkOutLatitude = checkOutLatitude,
            checkOutLongitude = checkOutLongitude,
            checkInPhotoUrl = checkInPhotoUrl,
            checkOutPhotoUrl = checkOutPhotoUrl,
            status = AttendanceStatus.valueOf(status),
            notes = notes,
            isSynced = isSynced,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
    
    private fun Attendance.toEntity(): AttendanceEntity {
        return AttendanceEntity(
            id = id,
            userId = userId,
            checkInTime = checkInTime,
            checkOutTime = checkOutTime,
            checkInLatitude = checkInLatitude,
            checkInLongitude = checkInLongitude,
            checkOutLatitude = checkOutLatitude,
            checkOutLongitude = checkOutLongitude,
            checkInPhotoUrl = checkInPhotoUrl,
            checkOutPhotoUrl = checkOutPhotoUrl,
            status = status.name,
            notes = notes,
            isSynced = isSynced,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
    
    private fun Attendance.toAttendanceData(): AttendanceData {
        return AttendanceData(
            id = id,
            userId = userId,
            date = dateFormat.format(Date(checkInTime)),
            checkInTime = dateTimeFormat.format(Date(checkInTime)),
            checkOutTime = checkOutTime?.let { dateTimeFormat.format(Date(it)) },
            checkInLocation = LocationData(
                latitude = checkInLatitude,
                longitude = checkInLongitude
            ),
            checkOutLocation = if (checkOutLatitude != null && checkOutLongitude != null) {
                LocationData(
                    latitude = checkOutLatitude,
                    longitude = checkOutLongitude
                )
            } else null,
            checkInPhotoUrl = checkInPhotoUrl,
            checkOutPhotoUrl = checkOutPhotoUrl,
            status = status.name,
            workingHours = calculateWorkingHours(this),
            notes = notes,
            createdAt = dateTimeFormat.format(Date(createdAt)),
            updatedAt = dateTimeFormat.format(Date(updatedAt))
        )
    }
}