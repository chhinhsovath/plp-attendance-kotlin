package com.plp.attendance.services

import android.content.Context
import androidx.work.*
import com.plp.attendance.data.local.PLPDatabase
import com.plp.attendance.data.preferences.AppPreferences
import com.plp.attendance.workers.NotificationWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: PLPDatabase,
    private val appPreferences: AppPreferences,
    private val notificationService: NotificationService
) {
    
    private val workManager = WorkManager.getInstance(context)
    
    companion object {
        private const val DAILY_REMINDER_WORK = "daily_attendance_reminder"
        private const val CHECKOUT_REMINDER_WORK = "checkout_reminder"
        private const val WEEKLY_REPORT_WORK = "weekly_report"
        private const val CLEANUP_WORK = "notification_cleanup"
        private const val MISSION_REMINDER_WORK = "mission_reminder"
    }
    
    fun scheduleDailyAttendanceReminder(
        reminderTime: LocalTime = LocalTime.of(8, 0), // 8:00 AM
        reminderMinutesBefore: Int = 15
    ) {
        val targetTime = reminderTime.minusMinutes(reminderMinutesBefore.toLong())
        val delay = calculateInitialDelay(targetTime)
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()
        
        val request = PeriodicWorkRequestBuilder<NotificationWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(
                Data.Builder()
                    .putString("notification_type", "attendance_reminder")
                    .putString("title", "Attendance Reminder")
                    .putString("message", "Don't forget to mark your attendance today!")
                    .build()
            )
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            DAILY_REMINDER_WORK,
            ExistingPeriodicWorkPolicy.REPLACE,
            request
        )
    }
    
    fun scheduleCheckoutReminder(
        reminderTime: LocalTime = LocalTime.of(17, 0), // 5:00 PM
        reminderMinutesBefore: Int = 30
    ) {
        val targetTime = reminderTime.minusMinutes(reminderMinutesBefore.toLong())
        val delay = calculateInitialDelay(targetTime)
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()
        
        val request = PeriodicWorkRequestBuilder<NotificationWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(
                Data.Builder()
                    .putString("notification_type", "checkout_reminder")
                    .putString("title", "Checkout Reminder")
                    .putString("message", "Remember to check out before leaving work")
                    .build()
            )
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            CHECKOUT_REMINDER_WORK,
            ExistingPeriodicWorkPolicy.REPLACE,
            request
        )
    }
    
    fun scheduleWeeklyReport(dayOfWeek: Int = 1) { // Monday
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val request = PeriodicWorkRequestBuilder<NotificationWorker>(7, TimeUnit.DAYS)
            .setConstraints(constraints)
            .setInputData(
                Data.Builder()
                    .putString("notification_type", "weekly_report")
                    .putString("title", "Weekly Report Available")
                    .putString("message", "Your weekly attendance report is ready for review")
                    .build()
            )
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            WEEKLY_REPORT_WORK,
            ExistingPeriodicWorkPolicy.REPLACE,
            request
        )
    }
    
    fun scheduleMissionReminder(
        missionId: String,
        reminderTime: Long, // timestamp
        title: String,
        message: String
    ) {
        val delay = reminderTime - System.currentTimeMillis()
        
        if (delay <= 0) return // Past time, don't schedule
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()
        
        val request = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setConstraints(constraints)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(
                Data.Builder()
                    .putString("notification_type", "mission_reminder")
                    .putString("title", title)
                    .putString("message", message)
                    .putString("mission_id", missionId)
                    .build()
            )
            .build()
        
        workManager.enqueueUniqueWork(
            "$MISSION_REMINDER_WORK:$missionId",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
    
    fun scheduleNotificationCleanup() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(true)
            .build()
        
        val request = PeriodicWorkRequestBuilder<NotificationWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .setInputData(
                Data.Builder()
                    .putString("notification_type", "cleanup")
                    .build()
            )
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            CLEANUP_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
    
    suspend fun scheduleApprovalReminder(
        requestId: String,
        requesterName: String,
        requestType: String,
        reminderAfterHours: Int = 24
    ) {
        val delay = TimeUnit.HOURS.toMillis(reminderAfterHours.toLong())
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()
        
        val request = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setConstraints(constraints)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(
                Data.Builder()
                    .putString("notification_type", "approval_reminder")
                    .putString("title", "Pending Approval")
                    .putString("message", "$requesterName's $requestType request is awaiting your approval")
                    .putString("request_id", requestId)
                    .build()
            )
            .build()
        
        workManager.enqueueUniqueWork(
            "approval_reminder:$requestId",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
    
    suspend fun scheduleCustomNotification(
        id: String,
        title: String,
        message: String,
        triggerTime: Long,
        isRepeating: Boolean = false,
        repeatInterval: Long = 0
    ) {
        val delay = triggerTime - System.currentTimeMillis()
        
        if (delay <= 0) return // Past time, don't schedule
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()
        
        val inputData = Data.Builder()
            .putString("notification_type", "custom")
            .putString("title", title)
            .putString("message", message)
            .putString("custom_id", id)
            .build()
        
        if (isRepeating && repeatInterval > 0) {
            val request = PeriodicWorkRequestBuilder<NotificationWorker>(
                repeatInterval,
                TimeUnit.MILLISECONDS
            )
                .setConstraints(constraints)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .build()
            
            workManager.enqueueUniquePeriodicWork(
                "custom_notification:$id",
                ExistingPeriodicWorkPolicy.REPLACE,
                request
            )
        } else {
            val request = OneTimeWorkRequestBuilder<NotificationWorker>()
                .setConstraints(constraints)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .build()
            
            workManager.enqueueUniqueWork(
                "custom_notification:$id",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
    
    fun cancelNotification(workName: String) {
        workManager.cancelUniqueWork(workName)
    }
    
    fun cancelAllNotifications() {
        workManager.cancelUniqueWork(DAILY_REMINDER_WORK)
        workManager.cancelUniqueWork(CHECKOUT_REMINDER_WORK)
        workManager.cancelUniqueWork(WEEKLY_REPORT_WORK)
        workManager.cancelUniqueWork(CLEANUP_WORK)
    }
    
    private fun calculateInitialDelay(targetTime: LocalTime): Long {
        val now = ZonedDateTime.now(ZoneId.systemDefault())
        var targetDateTime = now.toLocalDate().atTime(targetTime).atZone(ZoneId.systemDefault())
        
        // If target time has passed today, schedule for tomorrow
        if (targetDateTime.isBefore(now)) {
            targetDateTime = targetDateTime.plusDays(1)
        }
        
        return targetDateTime.toInstant().toEpochMilli() - now.toInstant().toEpochMilli()
    }
}