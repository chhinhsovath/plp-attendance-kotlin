package com.plp.attendance.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.plp.attendance.services.SyncManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncManager: SyncManager
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "SyncWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting sync work")
        
        return try {
            syncManager.performSync().fold(
                onSuccess = { syncResult ->
                    Log.d(TAG, "Sync completed - Success: ${syncResult.successCount}, Failed: ${syncResult.failedCount}")
                    
                    if (syncResult.failedCount > 0) {
                        // Return success but indicate partial failure
                        Result.success()
                    } else {
                        Result.success()
                    }
                },
                onFailure = { exception ->
                    Log.e(TAG, "Sync failed: ${exception.message}")
                    Result.retry()
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Sync worker failed with exception", e)
            Result.retry()
        }
    }
}