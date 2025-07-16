package com.plp.attendance.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plp.attendance.data.local.PLPDatabase
import com.plp.attendance.data.local.entities.NotificationEntity
import com.plp.attendance.data.local.entities.NotificationPriority
import com.plp.attendance.data.local.entities.NotificationType
import com.plp.attendance.services.NotificationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val database: PLPDatabase,
    private val notificationService: NotificationService
) : ViewModel() {
    
    private val notificationDao = database.notificationDao()
    
    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()
    
    private val _notifications = MutableStateFlow<List<NotificationEntity>>(emptyList())
    val notifications: StateFlow<List<NotificationEntity>> = _notifications.asStateFlow()
    
    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()
    
    private val _currentFilter = MutableStateFlow<NotificationFilter>(NotificationFilter.All)
    private val _searchQuery = MutableStateFlow("")
    
    init {
        observeNotifications()
        observeUnreadCount()
    }
    
    private fun observeNotifications() {
        viewModelScope.launch {
            combine(
                _currentFilter,
                _searchQuery
            ) { filter, query ->
                Pair(filter, query)
            }.collect { (filter, query) ->
                try {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                    
                    val notificationFlow = when {
                        query.isNotEmpty() -> notificationDao.searchNotifications(query)
                        filter is NotificationFilter.ByType -> notificationDao.getNotificationsByType(filter.type)
                        filter is NotificationFilter.ByPriority -> notificationDao.getNotificationsByPriority(filter.priority)
                        filter is NotificationFilter.Unread -> notificationDao.getUnreadNotifications()
                        else -> notificationDao.getAllNotifications()
                    }
                    
                    notificationFlow
                        .catch { exception ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = exception.message ?: "Failed to load notifications"
                            )
                        }
                        .collect { notifications ->
                            _notifications.value = notifications
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = null
                            )
                        }
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load notifications"
                    )
                }
            }
        }
    }
    
    private fun observeUnreadCount() {
        viewModelScope.launch {
            notificationDao.getUnreadCount()
                .catch { exception ->
                    // Log error but don't crash
                    android.util.Log.e("NotificationViewModel", "Failed to get unread count", exception)
                }
                .collect { count ->
                    _unreadCount.value = count
                }
        }
    }
    
    fun filterNotifications(type: NotificationType?) {
        _currentFilter.value = when (type) {
            null -> NotificationFilter.All
            else -> NotificationFilter.ByType(type)
        }
    }
    
    fun filterByPriorityAndType(priority: NotificationPriority?, type: NotificationType?) {
        _currentFilter.value = when {
            priority != null && type != null -> NotificationFilter.Combined(priority, type)
            priority != null -> NotificationFilter.ByPriority(priority)
            type != null -> NotificationFilter.ByType(type)
            else -> NotificationFilter.All
        }
    }
    
    fun searchNotifications(query: String) {
        _searchQuery.value = query
    }
    
    fun markAsRead(notificationId: Long) {
        viewModelScope.launch {
            try {
                notificationDao.markAsRead(notificationId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to mark notification as read"
                )
            }
        }
    }
    
    fun markAllAsRead() {
        viewModelScope.launch {
            try {
                notificationDao.markAllAsRead()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to mark all notifications as read"
                )
            }
        }
    }
    
    fun markAsUnread(notificationId: Long) {
        viewModelScope.launch {
            try {
                notificationDao.markAsUnread(notificationId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to mark notification as unread"
                )
            }
        }
    }
    
    fun deleteNotification(notificationId: Long) {
        viewModelScope.launch {
            try {
                notificationDao.deleteById(notificationId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to delete notification"
                )
            }
        }
    }
    
    fun deleteAllReadNotifications() {
        viewModelScope.launch {
            try {
                notificationDao.deleteReadNotifications()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to delete read notifications"
                )
            }
        }
    }
    
    fun refreshNotifications() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                // Refresh logic here - for now just clear the error
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to refresh notifications"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun getUnreadCountByType(type: NotificationType): StateFlow<Int> {
        val countFlow = MutableStateFlow(0)
        viewModelScope.launch {
            notificationDao.getUnreadCountByType(type)
                .catch { exception ->
                    android.util.Log.e("NotificationViewModel", "Failed to get unread count for type", exception)
                }
                .collect { count ->
                    countFlow.value = count
                }
        }
        return countFlow.asStateFlow()
    }
    
    // Test notification methods (for development)
    fun sendTestAttendanceNotification() {
        viewModelScope.launch {
            notificationService.showAttendanceReminder(
                title = "Test Attendance",
                message = "This is a test attendance notification",
                actionText = "Test Action"
            )
        }
    }
    
    fun sendTestApprovalNotification() {
        viewModelScope.launch {
            notificationService.showApprovalRequest(
                type = "leave",
                requesterName = "John Doe",
                details = "Test leave request for development",
                requestId = "test-123"
            )
        }
    }
    
    fun sendTestSystemNotification() {
        viewModelScope.launch {
            notificationService.showSystemAlert(
                title = "Test System Alert",
                message = "This is a test system notification for development",
                priority = NotificationPriority.HIGH
            )
        }
    }
}

data class NotificationUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class NotificationFilter {
    object All : NotificationFilter()
    object Unread : NotificationFilter()
    data class ByType(val type: NotificationType) : NotificationFilter()
    data class ByPriority(val priority: NotificationPriority) : NotificationFilter()
    data class Combined(val priority: NotificationPriority, val type: NotificationType) : NotificationFilter()
}