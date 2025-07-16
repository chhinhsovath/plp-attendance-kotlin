package com.plp.attendance.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.plp.attendance.data.local.converters.SyncOperationConverter
import com.plp.attendance.data.local.converters.SyncStatusConverter

@Entity(tableName = "sync_queue")
@TypeConverters(SyncOperationConverter::class, SyncStatusConverter::class)
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val entityType: String, // "attendance", "leave", "user", etc.
    val entityId: String, // Local ID of the entity
    val operation: SyncOperation,
    val status: SyncStatus = SyncStatus.PENDING,
    val data: String, // JSON data to sync
    val createdAt: Long = System.currentTimeMillis(),
    val lastAttemptAt: Long? = null,
    val attemptCount: Int = 0,
    val maxAttempts: Int = 3,
    val errorMessage: String? = null,
    val priority: Int = 0 // Higher number = higher priority
)

enum class SyncOperation {
    CREATE,
    UPDATE,
    DELETE
}

enum class SyncStatus {
    PENDING,
    IN_PROGRESS,
    SUCCESS,
    FAILED,
    RETRY
}