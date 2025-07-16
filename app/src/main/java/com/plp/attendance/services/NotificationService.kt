package com.plp.attendance.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.plp.attendance.MainActivity
import com.plp.attendance.R
import com.plp.attendance.data.local.PLPDatabase
import com.plp.attendance.data.local.entities.NotificationEntity
import com.plp.attendance.data.local.entities.NotificationPriority
import com.plp.attendance.data.local.entities.NotificationType
import com.plp.attendance.data.preferences.AppPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: PLPDatabase,
    private val appPreferences: AppPreferences
) {
    
    private val notificationManager = NotificationManagerCompat.from(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    companion object {
        private const val ATTENDANCE_CHANNEL_ID = "attendance_channel"
        private const val APPROVAL_CHANNEL_ID = "approval_channel"
        private const val MISSION_CHANNEL_ID = "mission_channel"
        private const val SYSTEM_CHANNEL_ID = "system_channel"
        private const val SYNC_CHANNEL_ID = "sync_channel"
        
        private const val ATTENDANCE_NOTIFICATION_ID = 1001
        private const val APPROVAL_NOTIFICATION_ID = 1002
        private const val MISSION_NOTIFICATION_ID = 1003
        private const val SYSTEM_NOTIFICATION_ID = 1004
        private const val SYNC_NOTIFICATION_ID = 1005
    }
    
    init {
        createNotificationChannels()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    ATTENDANCE_CHANNEL_ID,
                    "Attendance Notifications",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications for attendance reminders and updates"
                },
                NotificationChannel(
                    APPROVAL_CHANNEL_ID,
                    "Approval Notifications",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications for approval requests and responses"
                },
                NotificationChannel(
                    MISSION_CHANNEL_ID,
                    "Mission Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications for mission tracking and updates"
                },
                NotificationChannel(
                    SYSTEM_CHANNEL_ID,
                    "System Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "General system notifications and alerts"
                },
                NotificationChannel(
                    SYNC_CHANNEL_ID,
                    "Sync Notifications",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Data synchronization status notifications"
                }
            )
            
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            channels.forEach { channel ->
                manager.createNotificationChannel(channel)
            }
        }
    }
    
    suspend fun showAttendanceReminder(
        title: String = "Attendance Reminder",
        message: String = "Don't forget to mark your attendance",
        actionText: String = "Mark Attendance"
    ) {
        if (!appPreferences.isNotificationsEnabled.first()) return
        
        val pendingIntent = createPendingIntent()
        
        val notification = NotificationCompat.Builder(context, ATTENDANCE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_notification,
                actionText,
                pendingIntent
            )
            .build()
        
        notificationManager.notify(ATTENDANCE_NOTIFICATION_ID, notification)
        
        // Store in database
        storeNotification(
            type = NotificationType.ATTENDANCE,
            title = title,
            message = message,
            priority = NotificationPriority.HIGH
        )
    }
    
    suspend fun showApprovalRequest(
        type: String,
        requesterName: String,
        details: String,
        requestId: String
    ) {
        if (!appPreferences.isNotificationsEnabled.first()) return
        
        val title = "New ${type.capitalize()} Request"
        val message = "$requesterName has submitted a $type request: $details"
        
        val pendingIntent = createPendingIntent()
        
        val notification = NotificationCompat.Builder(context, APPROVAL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_notification,
                "Review",
                pendingIntent
            )
            .build()
        
        notificationManager.notify(APPROVAL_NOTIFICATION_ID + requestId.hashCode(), notification)
        
        // Store in database
        storeNotification(
            type = NotificationType.APPROVAL,
            title = title,
            message = message,
            priority = NotificationPriority.HIGH,
            actionData = requestId
        )
    }
    
    suspend fun showMissionUpdate(
        title: String,
        message: String,
        missionId: String,
        actionText: String = "View Mission"
    ) {
        if (!appPreferences.isNotificationsEnabled.first()) return
        
        val pendingIntent = createPendingIntent()
        
        val notification = NotificationCompat.Builder(context, MISSION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_notification,
                actionText,
                pendingIntent
            )
            .build()
        
        notificationManager.notify(MISSION_NOTIFICATION_ID + missionId.hashCode(), notification)
        
        // Store in database
        storeNotification(
            type = NotificationType.MISSION,
            title = title,
            message = message,
            priority = NotificationPriority.NORMAL,
            actionData = missionId
        )
    }
    
    suspend fun showSystemAlert(
        title: String,
        message: String,
        priority: NotificationPriority = NotificationPriority.NORMAL
    ) {
        if (!appPreferences.isNotificationsEnabled.first()) return
        
        val pendingIntent = createPendingIntent()
        
        val notificationPriority = when (priority) {
            NotificationPriority.URGENT -> NotificationCompat.PRIORITY_MAX
            NotificationPriority.HIGH -> NotificationCompat.PRIORITY_HIGH
            NotificationPriority.NORMAL -> NotificationCompat.PRIORITY_DEFAULT
            NotificationPriority.LOW -> NotificationCompat.PRIORITY_LOW
        }
        
        val notification = NotificationCompat.Builder(context, SYSTEM_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(notificationPriority)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        
        notificationManager.notify(SYSTEM_NOTIFICATION_ID + title.hashCode(), notification)
        
        // Store in database
        storeNotification(
            type = NotificationType.SYSTEM,
            title = title,
            message = message,
            priority = priority
        )
    }
    
    suspend fun showSyncStatus(
        isSuccess: Boolean,
        itemCount: Int,
        details: String? = null
    ) {
        if (!appPreferences.isNotificationsEnabled.first()) return
        
        val title = if (isSuccess) "Sync Completed" else "Sync Failed"
        val message = if (isSuccess) {
            "Successfully synced $itemCount items"
        } else {
            "Failed to sync $itemCount items${details?.let { ": $it" } ?: ""}"
        }
        
        val pendingIntent = createPendingIntent()
        
        val notification = NotificationCompat.Builder(context, SYNC_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        
        notificationManager.notify(SYNC_NOTIFICATION_ID, notification)
        
        // Store in database
        storeNotification(
            type = NotificationType.SYNC,
            title = title,
            message = message,
            priority = NotificationPriority.LOW
        )
    }
    
    suspend fun showProgressNotification(
        title: String,
        message: String,
        progress: Int,
        maxProgress: Int = 100
    ) {
        val pendingIntent = createPendingIntent()
        
        val notification = NotificationCompat.Builder(context, SYNC_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setProgress(maxProgress, progress, false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
        
        notificationManager.notify(SYNC_NOTIFICATION_ID, notification)
    }
    
    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }
    
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }
    
    private suspend fun storeNotification(
        type: NotificationType,
        title: String,
        message: String,
        priority: NotificationPriority,
        actionData: String? = null
    ) {
        scope.launch {
            try {
                val notification = NotificationEntity(
                    type = type,
                    title = title,
                    message = message,
                    priority = priority,
                    actionData = actionData,
                    createdAt = System.currentTimeMillis()
                )
                
                database.notificationDao().insert(notification)
            } catch (e: Exception) {
                // Log error but don't crash
                android.util.Log.e("NotificationService", "Failed to store notification", e)
            }
        }
    }
    
    private fun createPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}