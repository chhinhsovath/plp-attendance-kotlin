package com.plp.attendance.data.local.dao

import androidx.room.*
import com.plp.attendance.data.local.entities.SyncQueueEntity
import com.plp.attendance.data.local.entities.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {
    
    @Query("SELECT * FROM sync_queue WHERE status = :status ORDER BY priority DESC, createdAt ASC")
    suspend fun getItemsByStatus(status: SyncStatus): List<SyncQueueEntity>
    
    @Query("SELECT * FROM sync_queue WHERE status IN (:statuses) ORDER BY priority DESC, createdAt ASC")
    suspend fun getItemsByStatuses(statuses: List<SyncStatus>): List<SyncQueueEntity>
    
    @Query("SELECT * FROM sync_queue WHERE entityType = :entityType AND entityId = :entityId")
    suspend fun getItemsByEntity(entityType: String, entityId: String): List<SyncQueueEntity>
    
    @Query("SELECT * FROM sync_queue ORDER BY priority DESC, createdAt ASC")
    fun getAllItems(): Flow<List<SyncQueueEntity>>
    
    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = :status")
    fun getCountByStatus(status: SyncStatus): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM sync_queue WHERE status IN ('PENDING', 'RETRY')")
    fun getPendingCount(): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'FAILED'")
    fun getFailedCount(): Flow<Int>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SyncQueueEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SyncQueueEntity>)
    
    @Update
    suspend fun update(item: SyncQueueEntity)
    
    @Delete
    suspend fun delete(item: SyncQueueEntity)
    
    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("DELETE FROM sync_queue WHERE status = :status")
    suspend fun deleteByStatus(status: SyncStatus)
    
    @Query("DELETE FROM sync_queue WHERE entityType = :entityType AND entityId = :entityId")
    suspend fun deleteByEntity(entityType: String, entityId: String)
    
    @Query("UPDATE sync_queue SET status = :status, lastAttemptAt = :timestamp, attemptCount = attemptCount + 1, errorMessage = :errorMessage WHERE id = :id")
    suspend fun updateSyncAttempt(id: Long, status: SyncStatus, timestamp: Long, errorMessage: String?)
    
    @Query("UPDATE sync_queue SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: SyncStatus)
    
    @Query("DELETE FROM sync_queue WHERE status = 'SUCCESS' AND createdAt < :cutoffTime")
    suspend fun cleanupOldSuccessfulItems(cutoffTime: Long)
    
    @Query("SELECT * FROM sync_queue WHERE attemptCount >= maxAttempts AND status = 'FAILED'")
    suspend fun getFailedItems(): List<SyncQueueEntity>
    
    @Query("DELETE FROM sync_queue")
    suspend fun deleteAll()
}