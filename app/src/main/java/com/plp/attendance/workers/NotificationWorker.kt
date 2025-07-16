package com.plp.attendance.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.plp.attendance.data.local.PLPDatabase
import com.plp.attendance.data.local.entities.NotificationPriority
import com.plp.attendance.services.NotificationService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class NotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val notificationService: NotificationService,
    private val database: PLPDatabase
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "NotificationWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting notification work")
        
        return try {
            val notificationType = inputData.getString("notification_type") ?: return Result.failure()
            
            when (notificationType) {
                "attendance_reminder" -> handleAttendanceReminder()
                "checkout_reminder" -> handleCheckoutReminder()
                "weekly_report" -> handleWeeklyReport()
                "mission_reminder" -> handleMissionReminder()
                "approval_reminder" -> handleApprovalReminder()
                "custom" -> handleCustomNotification()
                "cleanup" -> handleCleanup()
                else -> {
                    Log.w(TAG, "Unknown notification type: $notificationType")
                    Result.failure()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Notification worker failed", e)
            Result.retry()
        }
    }
    
    private suspend fun handleAttendanceReminder(): Result {
        val title = inputData.getString("title") ?: "Attendance Reminder"
        val message = inputData.getString("message") ?: "Don't forget to mark your attendance"
        
        notificationService.showAttendanceReminder(
            title = title,
            message = message,
            actionText = "Mark Attendance"
        )
        
        Log.d(TAG, "Attendance reminder sent")
        return Result.success()
    }
    
    private suspend fun handleCheckoutReminder(): Result {
        val title = inputData.getString("title") ?: "Checkout Reminder"
        val message = inputData.getString("message") ?: "Remember to check out"
        
        notificationService.showAttendanceReminder(
            title = title,
            message = message,
            actionText = "Check Out"
        )
        
        Log.d(TAG, "Checkout reminder sent")
        return Result.success()
    }
    
    private suspend fun handleWeeklyReport(): Result {
        val title = inputData.getString("title") ?: "Weekly Report Available"
        val message = inputData.getString("message") ?: "Your weekly report is ready"
        
        notificationService.showSystemAlert(
            title = title,
            message = message,
            priority = NotificationPriority.NORMAL
        )
        
        Log.d(TAG, "Weekly report notification sent")
        return Result.success()
    }
    
    private suspend fun handleMissionReminder(): Result {
        val title = inputData.getString("title") ?: "Mission Reminder"
        val message = inputData.getString("message") ?: "You have a scheduled mission"
        val missionId = inputData.getString("mission_id") ?: "unknown"
        
        notificationService.showMissionUpdate(
            title = title,
            message = message,
            missionId = missionId,
            actionText = "View Mission"
        )
        
        Log.d(TAG, "Mission reminder sent for mission: $missionId")
        return Result.success()
    }
    
    private suspend fun handleApprovalReminder(): Result {
        val title = inputData.getString("title") ?: "Pending Approval"
        val message = inputData.getString("message") ?: "You have a pending approval request"
        val requestId = inputData.getString("request_id") ?: "unknown"
        
        notificationService.showApprovalRequest(
            type = "reminder",
            requesterName = "System",
            details = message,
            requestId = requestId
        )
        
        Log.d(TAG, "Approval reminder sent for request: $requestId")
        return Result.success()
    }
    
    private suspend fun handleCustomNotification(): Result {
        val title = inputData.getString("title") ?: "Custom Notification"
        val message = inputData.getString("message") ?: "Custom notification message"
        val customId = inputData.getString("custom_id") ?: "unknown"
        
        notificationService.showSystemAlert(
            title = title,
            message = message,
            priority = NotificationPriority.NORMAL
        )
        
        Log.d(TAG, "Custom notification sent: $customId")
        return Result.success()
    }
    
    private suspend fun handleCleanup(): Result {
        try {
            val notificationDao = database.notificationDao()
            val currentTime = System.currentTimeMillis()
            
            // Delete notifications older than 30 days
            val cutoffTime = currentTime - TimeUnit.DAYS.toMillis(30)
            notificationDao.deleteOldNotifications(cutoffTime)
            
            // Delete expired notifications
            val expiredNotifications = notificationDao.getExpiredNotifications(currentTime)
            expiredNotifications.forEach { notification ->
                notificationDao.delete(notification)
            }
            
            Log.d(TAG, "Notification cleanup completed")
            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Cleanup failed", e)
            return Result.retry()
        }
    }
}