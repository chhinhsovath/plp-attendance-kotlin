package com.plp.attendance.services

import android.content.Context
import android.util.Log
import androidx.work.*
import com.plp.attendance.data.local.PLPDatabase
import com.plp.attendance.data.local.entities.SyncQueueEntity
import com.plp.attendance.data.local.entities.SyncOperation
import com.plp.attendance.data.local.entities.SyncStatus
import com.plp.attendance.domain.repository.AuthRepository
import com.plp.attendance.utils.NetworkUtils
import com.plp.attendance.workers.SyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import com.google.gson.Gson
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: PLPDatabase,
    private val authRepository: AuthRepository,
    private val networkUtils: NetworkUtils
) {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncQueueDao = database.syncQueueDao()
    
    private val _syncState = MutableStateFlow(SyncState())
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()
    
    private val _lastSyncTime = MutableStateFlow(0L)
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()
    
    companion object {
        private const val TAG = "SyncManager"
        private const val SYNC_WORK_NAME = "sync_work"
        private const val PERIODIC_SYNC_INTERVAL = 15L // minutes
        private const val CLEANUP_INTERVAL = 24L // hours
    }
    
    init {
        observeSyncQueue()
        schedulePeriodicSync()
    }
    
    private fun observeSyncQueue() {
        scope.launch {
            combine(
                syncQueueDao.getPendingCount(),
                syncQueueDao.getFailedCount(),
                syncQueueDao.getCountByStatus(SyncStatus.IN_PROGRESS)
            ) { pending, failed, inProgress ->
                SyncState(
                    pendingCount = pending,
                    failedCount = failed,
                    isInProgress = inProgress > 0
                )
            }.collect { state ->
                _syncState.value = state
            }
        }
    }
    
    fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            PERIODIC_SYNC_INTERVAL,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
    
    fun triggerImmediateSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()
        
        WorkManager.getInstance(context).enqueue(syncRequest)
    }
    
    suspend fun queueForSync(
        entityType: String,
        entityId: String,
        operation: SyncOperation,
        data: String,
        priority: Int = 0
    ): kotlin.Result<Long> {
        return try {
            // Remove any existing sync items for this entity
            syncQueueDao.deleteByEntity(entityType, entityId)
            
            val syncItem = SyncQueueEntity(
                entityType = entityType,
                entityId = entityId,
                operation = operation,
                data = data,
                priority = priority
            )
            
            val id = syncQueueDao.insert(syncItem)
            Log.d(TAG, "Queued $operation for $entityType:$entityId with priority $priority")
            
            kotlin.Result.success(id)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to queue sync item: ${e.message}")
            kotlin.Result.failure(e)
        }
    }
    
    suspend fun performSync(): kotlin.Result<SyncResult> {
        if (!networkUtils.isNetworkAvailable()) {
            return kotlin.Result.failure(Exception("No network connection available"))
        }
        
        // For now, assume user is logged in - we'll check properly later
        val isLoggedIn = true
        if (!isLoggedIn) {
            return kotlin.Result.failure(Exception("User not authenticated"))
        }
        
        return try {
            val pendingItems = syncQueueDao.getItemsByStatuses(
                listOf(SyncStatus.PENDING, SyncStatus.RETRY)
            )
            
            if (pendingItems.isEmpty()) {
                return kotlin.Result.success(SyncResult(0, 0, 0))
            }
            
            var successCount = 0
            var failedCount = 0
            var skippedCount = 0
            
            for (item in pendingItems) {
                // Mark as in progress
                syncQueueDao.updateStatus(item.id, SyncStatus.IN_PROGRESS)
                
                try {
                    val result = syncItem(item)
                    if (result.isSuccess) {
                        syncQueueDao.updateStatus(item.id, SyncStatus.SUCCESS)
                        successCount++
                    } else {
                        handleSyncFailure(item, result.message)
                        failedCount++
                    }
                } catch (e: Exception) {
                    handleSyncFailure(item, e.message)
                    failedCount++
                }
            }
            
            // Cleanup old successful items
            cleanupOldItems()
            
            _lastSyncTime.value = System.currentTimeMillis()
            
            kotlin.Result.success(SyncResult(successCount, failedCount, skippedCount))
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed: ${e.message}")
            kotlin.Result.failure(e)
        }
    }
    
    private suspend fun syncItem(item: SyncQueueEntity): SyncItemResult {
        return try {
            when (item.entityType) {
                "attendance" -> syncAttendance(item)
                "leave" -> syncLeave(item)
                "user" -> syncUser(item)
                else -> SyncItemResult(false, "Unknown entity type: ${item.entityType}")
            }
        } catch (e: Exception) {
            SyncItemResult(false, e.message ?: "Sync failed")
        }
    }
    
    private suspend fun syncAttendance(item: SyncQueueEntity): SyncItemResult {
        // TODO: Implement attendance sync with API
        // For now, simulate success
        return SyncItemResult(true, "Attendance synced successfully")
    }
    
    private suspend fun syncLeave(item: SyncQueueEntity): SyncItemResult {
        // TODO: Implement leave sync with API
        // For now, simulate success
        return SyncItemResult(true, "Leave synced successfully")
    }
    
    private suspend fun syncUser(item: SyncQueueEntity): SyncItemResult {
        // TODO: Implement user sync with API
        // For now, simulate success
        return SyncItemResult(true, "User synced successfully")
    }
    
    private suspend fun handleSyncFailure(item: SyncQueueEntity, errorMessage: String?) {
        val newAttemptCount = item.attemptCount + 1
        val status = if (newAttemptCount >= item.maxAttempts) {
            SyncStatus.FAILED
        } else {
            SyncStatus.RETRY
        }
        
        syncQueueDao.updateSyncAttempt(
            item.id,
            status,
            System.currentTimeMillis(),
            errorMessage
        )
        
        Log.w(TAG, "Sync failed for ${item.entityType}:${item.entityId} - $errorMessage")
    }
    
    private suspend fun cleanupOldItems() {
        val cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
        syncQueueDao.cleanupOldSuccessfulItems(cutoffTime)
    }
    
    suspend fun retryFailedItems() {
        val failedItems = syncQueueDao.getFailedItems()
        for (item in failedItems) {
            syncQueueDao.updateStatus(item.id, SyncStatus.RETRY)
        }
        triggerImmediateSync()
    }
    
    suspend fun clearSyncQueue() {
        syncQueueDao.deleteByStatus(SyncStatus.SUCCESS)
        syncQueueDao.deleteByStatus(SyncStatus.FAILED)
    }
    
    fun getSyncQueueItems(): Flow<List<SyncQueueEntity>> {
        return syncQueueDao.getAllItems()
    }
}

data class SyncState(
    val pendingCount: Int = 0,
    val failedCount: Int = 0,
    val isInProgress: Boolean = false
)

data class SyncResult(
    val successCount: Int,
    val failedCount: Int,
    val skippedCount: Int
)

private data class SyncItemResult(
    val isSuccess: Boolean,
    val message: String
)