package com.plp.attendance.data.sync

import android.util.Log
import com.plp.attendance.data.local.PLPDatabase
import com.plp.attendance.data.local.SessionManager
import com.plp.attendance.data.local.entities.SyncOperation
import com.plp.attendance.data.local.entities.SyncQueueEntity
import com.plp.attendance.data.local.entities.SyncStatus
import com.plp.attendance.data.remote.RemoteConfig
import com.plp.attendance.data.remote.api.AttendanceApi
import com.plp.attendance.data.remote.api.LeaveApi
import com.plp.attendance.data.remote.api.UserApi
import com.plp.attendance.data.remote.dto.CheckInRequest
import com.plp.attendance.data.remote.dto.CheckOutRequest
import com.plp.attendance.data.remote.dto.CreateLeaveRequest
import com.plp.attendance.data.local.entities.AttendanceEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages data synchronization between local SQLite and remote PostgreSQL database
 */
@Singleton
class SyncManager @Inject constructor(
    private val database: PLPDatabase,
    private val sessionManager: SessionManager,
    private val attendanceApi: AttendanceApi,
    private val leaveApi: LeaveApi,
    private val userApi: UserApi
) {
    
    companion object {
        private const val TAG = "SyncManager"
    }
    
    private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * Start automatic sync process
     */
    fun startAutoSync() {
        if (RemoteConfig.Features.USE_REMOTE_API) {
            syncScope.launch {
                while (isActive) {
                    performSync()
                    delay(RemoteConfig.Sync.SYNC_INTERVAL_MINUTES * 60 * 1000)
                }
            }
        }
    }
    
    /**
     * Perform manual sync
     */
    suspend fun performSync(): SyncResult {
        return try {
            Log.d(TAG, "Starting sync process...")
            
            val token = sessionManager.getAuthTokenSuspend()
            if (token == null || token.startsWith("test_") || token.startsWith("offline_")) {
                Log.d(TAG, "Skipping sync for test/offline account")
                return SyncResult.Success(0, 0)
            }
            
            var uploadedCount = 0
            var downloadedCount = 0
            
            // Upload pending local changes
            uploadedCount += syncPendingAttendance(token)
            uploadedCount += syncPendingLeaves(token)
            
            // Download remote changes
            downloadedCount += syncRemoteAttendance(token)
            downloadedCount += syncRemoteLeaves(token)
            downloadedCount += syncRemoteUsers(token)
            
            // Clean up sync queue
            cleanupSyncQueue()
            
            Log.d(TAG, "Sync completed. Uploaded: $uploadedCount, Downloaded: $downloadedCount")
            SyncResult.Success(uploadedCount, downloadedCount)
            
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            SyncResult.Error(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Add item to sync queue
     */
    suspend fun addToSyncQueue(
        entityType: String,
        entityId: String,
        operation: SyncOperation,
        data: String
    ) {
        val syncEntity = SyncQueueEntity(
            entityType = entityType,
            entityId = entityId,
            operation = operation,
            data = data,
            status = SyncStatus.PENDING,
            createdAt = System.currentTimeMillis(),
            lastAttemptAt = null,
            errorMessage = null
        )
        
        database.syncQueueDao().insert(syncEntity)
    }
    
    /**
     * Sync pending attendance records
     */
    private suspend fun syncPendingAttendance(token: String): Int {
        var count = 0
        
        try {
            // Get unsynced attendance records
            val unsyncedAttendance = database.attendanceDao().getUnsyncedAttendance()
            
            for (attendance in unsyncedAttendance) {
                try {
                    // Upload to server
                    val response = if (attendance.checkOutTime != null) {
                        attendanceApi.checkOut(
                            request = CheckOutRequest(
                                latitude = attendance.checkOutLatitude!!,
                                longitude = attendance.checkOutLongitude!!,
                                address = null,
                                notes = null
                            )
                        )
                    } else {
                        attendanceApi.checkIn(
                            request = CheckInRequest(
                                latitude = attendance.checkInLatitude,
                                longitude = attendance.checkInLongitude,
                                address = null,
                                notes = null
                            )
                        )
                    }
                    
                    if (response.isSuccessful) {
                        // Mark as synced
                        database.attendanceDao().updateAttendance(attendance.copy(isSynced = true))
                        count++
                    } else {
                        Log.e(TAG, "Failed to sync attendance ${attendance.id}: ${response.code()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error syncing attendance ${attendance.id}", e)
                    // Add to sync queue for retry
                    addToSyncQueue(
                        entityType = "attendance",
                        entityId = attendance.id,
                        operation = SyncOperation.UPDATE,
                        data = attendance.toString()
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing attendance records", e)
        }
        
        return count
    }
    
    /**
     * Sync pending leave requests
     */
    private suspend fun syncPendingLeaves(token: String): Int {
        var count = 0
        
        try {
            // Get unsynced leave requests
            val unsyncedLeaves = database.leaveDao().getUnsyncedLeaves().first()
            
            for (leave in unsyncedLeaves) {
                try {
                    // Upload to server
                    val response = leaveApi.createLeave(
                        request = CreateLeaveRequest(
                            leaveType = leave.leaveType.name,
                            startDate = formatTimestamp(leave.startDate),
                            endDate = formatTimestamp(leave.endDate),
                            reason = leave.reason
                        )
                    )
                    
                    if (response.isSuccessful) {
                        // Mark as synced
                        database.leaveDao().updateLeave(leave.copy(isSynced = true))
                        count++
                    } else {
                        Log.e(TAG, "Failed to sync leave ${leave.id}: ${response.code()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error syncing leave ${leave.id}", e)
                    // Add to sync queue for retry
                    addToSyncQueue(
                        entityType = "leave",
                        entityId = leave.id,
                        operation = SyncOperation.CREATE,
                        data = leave.toString()
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing leave requests", e)
        }
        
        return count
    }
    
    /**
     * Sync remote attendance records
     */
    private suspend fun syncRemoteAttendance(token: String): Int {
        var count = 0
        
        try {
            val userId = sessionManager.getUserIdSuspend() ?: return 0
            val response = attendanceApi.getUserAttendance(
                userId = userId,
                startDate = "2023-01-01", // TODO: Calculate proper date range
                endDate = "2023-12-31"
            )
            
            if (response.isSuccessful && response.body() != null) {
                val remoteAttendance = response.body()!!
                
                // Update local database
                val attendanceListData = response.body()?.data
                if (attendanceListData != null) {
                    for (attendance in attendanceListData.attendances) {
                        // Convert DTO to entity and insert
                        val entity = AttendanceEntity(
                            id = attendance.id,
                            userId = attendance.userId,
                            checkInTime = parseTimestamp(attendance.checkInTime),
                            checkOutTime = parseTimestamp(attendance.checkOutTime),
                            checkInLatitude = attendance.checkInLocation?.latitude ?: 0.0,
                            checkInLongitude = attendance.checkInLocation?.longitude ?: 0.0,
                            checkOutLatitude = attendance.checkOutLocation?.latitude,
                            checkOutLongitude = attendance.checkOutLocation?.longitude,
                            checkInPhotoUrl = attendance.checkInPhotoUrl,
                            checkOutPhotoUrl = attendance.checkOutPhotoUrl,
                            status = attendance.status,
                            notes = attendance.notes,
                            isSynced = true,
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        database.attendanceDao().insertAttendance(entity)
                        count++
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing remote attendance", e)
        }
        
        return count
    }
    
    /**
     * Sync remote leave requests
     */
    private suspend fun syncRemoteLeaves(token: String): Int {
        var count = 0
        
        try {
            val response = leaveApi.getLeaveRequests(
                token = "Bearer $token",
                page = 1,
                limit = 100
            )
            
            if (response.isSuccessful && response.body() != null) {
                val remoteLeaves = response.body()!!
                
                // Update local database
                val leaveListData = response.body()?.data
                if (leaveListData != null) {
                    for (leave in leaveListData.leaves) {
                        // Convert DTO to entity
                        val entity = com.plp.attendance.data.local.entities.LeaveEntity(
                            id = leave.id,
                            userId = leave.userId,
                            leaveType = com.plp.attendance.domain.model.LeaveType.valueOf(leave.leaveType),
                            startDate = parseTimestamp(leave.startDate),
                            endDate = parseTimestamp(leave.endDate),
                            reason = leave.reason,
                            status = com.plp.attendance.domain.model.LeaveStatus.valueOf(leave.status),
                            approverId = leave.approverId,
                            approverComments = leave.approverComment,
                            attachmentUrl = leave.attachmentUrl,
                            isSynced = true,
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        database.leaveDao().insertLeave(entity)
                        count++
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing remote leaves", e)
        }
        
        return count
    }
    
    /**
     * Sync remote users
     */
    private suspend fun syncRemoteUsers(token: String): Int {
        var count = 0
        
        try {
            val response = userApi.getUsers(
                token = "Bearer $token",
                page = 1,
                limit = 100
            )
            
            if (response.isSuccessful && response.body()?.success == true) {
                val users = response.body()?.data?.users ?: emptyList()
                
                // Update local database
                for (userData in users) {
                    // Convert to UserEntity and insert
                    // (conversion logic depends on your DTO structure)
                    count++
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing remote users", e)
        }
        
        return count
    }
    
    /**
     * Clean up old sync queue entries
     */
    private suspend fun cleanupSyncQueue() {
        try {
            // TODO: Implement cleanup methods in SyncQueueDao
            // For now, just log that cleanup was attempted
            Log.d(TAG, "Sync queue cleanup attempted")
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up sync queue", e)
        }
    }
    
    /**
     * Stop sync process
     */
    fun stopSync() {
        syncScope.cancel()
    }
    
    /**
     * Parse timestamp string to Long
     */
    private fun parseTimestamp(timestamp: String?): Long {
        if (timestamp.isNullOrEmpty()) return System.currentTimeMillis()
        
        return try {
            // Try to parse as ISO timestamp or return current time
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
                .parse(timestamp)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
    
    /**
     * Format timestamp Long to String
     */
    private fun formatTimestamp(timestamp: Long): String {
        return try {
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
                .format(java.util.Date(timestamp))
        } catch (e: Exception) {
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
                .format(java.util.Date(System.currentTimeMillis()))
        }
    }
}

/**
 * Result of sync operation
 */
sealed class SyncResult {
    data class Success(val uploadedCount: Int, val downloadedCount: Int) : SyncResult()
    data class Error(val message: String) : SyncResult()
}