package com.plp.attendance.data.local.dao

import androidx.room.*
import com.plp.attendance.data.local.entities.NotificationEntity
import com.plp.attendance.data.local.entities.NotificationType
import com.plp.attendance.data.local.entities.NotificationPriority
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    
    @Query("SELECT * FROM notifications ORDER BY createdAt DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>
    
    @Query("SELECT * FROM notifications WHERE isRead = 0 ORDER BY createdAt DESC")
    fun getUnreadNotifications(): Flow<List<NotificationEntity>>
    
    @Query("SELECT * FROM notifications WHERE type = :type ORDER BY createdAt DESC")
    fun getNotificationsByType(type: NotificationType): Flow<List<NotificationEntity>>
    
    @Query("SELECT * FROM notifications WHERE priority = :priority ORDER BY createdAt DESC")
    fun getNotificationsByPriority(priority: NotificationPriority): Flow<List<NotificationEntity>>
    
    @Query("SELECT * FROM notifications WHERE title LIKE '%' || :query || '%' OR message LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchNotifications(query: String): Flow<List<NotificationEntity>>
    
    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM notifications WHERE type = :type AND isRead = 0")
    fun getUnreadCountByType(type: NotificationType): Flow<Int>
    
    @Query("SELECT * FROM notifications WHERE scheduledFor <= :currentTime AND scheduledFor IS NOT NULL")
    suspend fun getScheduledNotifications(currentTime: Long): List<NotificationEntity>
    
    @Query("SELECT * FROM notifications WHERE expiresAt <= :currentTime AND expiresAt IS NOT NULL")
    suspend fun getExpiredNotifications(currentTime: Long): List<NotificationEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notifications: List<NotificationEntity>)
    
    @Update
    suspend fun update(notification: NotificationEntity)
    
    @Delete
    suspend fun delete(notification: NotificationEntity)
    
    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("DELETE FROM notifications WHERE type = :type")
    suspend fun deleteByType(type: NotificationType)
    
    @Query("DELETE FROM notifications WHERE isRead = 1")
    suspend fun deleteReadNotifications()
    
    @Query("DELETE FROM notifications WHERE createdAt < :cutoffTime")
    suspend fun deleteOldNotifications(cutoffTime: Long)
    
    @Query("UPDATE notifications SET isRead = 1, readAt = :readTime WHERE id = :id")
    suspend fun markAsRead(id: Long, readTime: Long = System.currentTimeMillis())
    
    @Query("UPDATE notifications SET isRead = 1, readAt = :readTime WHERE type = :type")
    suspend fun markTypeAsRead(type: NotificationType, readTime: Long = System.currentTimeMillis())
    
    @Query("UPDATE notifications SET isRead = 1, readAt = :readTime")
    suspend fun markAllAsRead(readTime: Long = System.currentTimeMillis())
    
    @Query("UPDATE notifications SET isRead = 0, readAt = NULL WHERE id = :id")
    suspend fun markAsUnread(id: Long)
    
    @Query("SELECT * FROM notifications WHERE actionData = :actionData AND type = :type")
    suspend fun getNotificationsByAction(actionData: String, type: NotificationType): List<NotificationEntity>
    
    @Query("DELETE FROM notifications WHERE actionData = :actionData AND type = :type")
    suspend fun deleteByAction(actionData: String, type: NotificationType)
    
    @Query("DELETE FROM notifications")
    suspend fun deleteAllNotifications()
}