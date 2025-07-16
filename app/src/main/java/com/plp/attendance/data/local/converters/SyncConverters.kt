package com.plp.attendance.data.local.converters

import androidx.room.TypeConverter
import com.plp.attendance.data.local.entities.SyncOperation
import com.plp.attendance.data.local.entities.SyncStatus

class SyncOperationConverter {
    @TypeConverter
    fun fromSyncOperation(operation: SyncOperation): String {
        return operation.name
    }

    @TypeConverter
    fun toSyncOperation(operation: String): SyncOperation {
        return SyncOperation.valueOf(operation)
    }
}

class SyncStatusConverter {
    @TypeConverter
    fun fromSyncStatus(status: SyncStatus): String {
        return status.name
    }

    @TypeConverter
    fun toSyncStatus(status: String): SyncStatus {
        return SyncStatus.valueOf(status)
    }
}