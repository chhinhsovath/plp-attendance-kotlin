package com.plp.attendance.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.plp.attendance.data.local.entities.NotificationEntity
import com.plp.attendance.data.local.entities.NotificationPriority
import com.plp.attendance.data.local.entities.NotificationType
import com.plp.attendance.ui.components.ErrorMessage
import com.plp.attendance.ui.components.LoadingOverlay
import com.plp.attendance.ui.viewmodels.NotificationViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onBackClick: () -> Unit,
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadCount.collectAsStateWithLifecycle()
    
    var selectedTab by remember { mutableStateOf(0) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    val tabs = listOf("All", "Unread", "Attendance", "Approvals", "Missions", "System")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("Notifications")
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (unreadCount > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = unreadCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                    
                    IconButton(onClick = { viewModel.markAllAsRead() }) {
                        Icon(Icons.Default.DoneAll, contentDescription = "Mark all as read")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { 
                    searchQuery = it
                    viewModel.searchNotifications(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search notifications...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { 
                            searchQuery = ""
                            viewModel.searchNotifications("")
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                }
            )
            
            // Tab Row
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { 
                            selectedTab = index
                            viewModel.filterNotifications(getFilterType(index))
                        },
                        text = { Text(title) }
                    )
                }
            }
            
            // Content
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> {
                        LoadingOverlay(
                            isLoading = true,
                            message = "Loading notifications..."
                        )
                    }
                    
                    uiState.error != null -> {
                        uiState.error?.let { error ->
                            ErrorMessage(
                                message = error,
                                onDismiss = { viewModel.clearError() },
                                onRetry = { viewModel.refreshNotifications() },
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    
                    notifications.isEmpty() -> {
                        EmptyNotificationsState(
                            selectedTab = selectedTab,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    
                    else -> {
                        NotificationsList(
                            notifications = notifications,
                            onNotificationClick = { notification ->
                                viewModel.markAsRead(notification.id)
                                // Handle notification action
                            },
                            onNotificationDismiss = { notification ->
                                viewModel.deleteNotification(notification.id)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
    
    if (showFilterDialog) {
        FilterDialog(
            onDismiss = { showFilterDialog = false },
            onFilterApplied = { priority, type ->
                viewModel.filterByPriorityAndType(priority, type)
                showFilterDialog = false
            }
        )
    }
}

@Composable
private fun NotificationsList(
    notifications: List<NotificationEntity>,
    onNotificationClick: (NotificationEntity) -> Unit,
    onNotificationDismiss: (NotificationEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    
    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(notifications) { notification ->
            NotificationItem(
                notification = notification,
                onClick = { onNotificationClick(notification) },
                onDismiss = { onNotificationDismiss(notification) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationItem(
    notification: NotificationEntity,
    onClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.primaryContainer
            }
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NotificationTypeIcon(notification.type)
                    
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    NotificationPriorityIndicator(notification.priority)
                }
                
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = formatTimestamp(notification.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun NotificationTypeIcon(type: NotificationType) {
    val icon = when (type) {
        NotificationType.ATTENDANCE -> Icons.Default.AccessTime
        NotificationType.APPROVAL -> Icons.Default.CheckCircle
        NotificationType.MISSION -> Icons.Default.Place
        NotificationType.SYSTEM -> Icons.Default.Settings
        NotificationType.SYNC -> Icons.Default.Sync
    }
    
    val color = when (type) {
        NotificationType.ATTENDANCE -> MaterialTheme.colorScheme.primary
        NotificationType.APPROVAL -> MaterialTheme.colorScheme.tertiary
        NotificationType.MISSION -> MaterialTheme.colorScheme.secondary
        NotificationType.SYSTEM -> MaterialTheme.colorScheme.outline
        NotificationType.SYNC -> MaterialTheme.colorScheme.surfaceTint
    }
    
    Icon(
        imageVector = icon,
        contentDescription = type.name,
        tint = color,
        modifier = Modifier.size(20.dp)
    )
}

@Composable
private fun NotificationPriorityIndicator(priority: NotificationPriority) {
    val color = when (priority) {
        NotificationPriority.URGENT -> MaterialTheme.colorScheme.error
        NotificationPriority.HIGH -> MaterialTheme.colorScheme.tertiary
        NotificationPriority.NORMAL -> MaterialTheme.colorScheme.primary
        NotificationPriority.LOW -> MaterialTheme.colorScheme.outline
    }
    
    if (priority != NotificationPriority.NORMAL) {
        Surface(
            color = color,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.size(8.dp)
        ) {}
    }
}

@Composable
private fun EmptyNotificationsState(
    selectedTab: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.NotificationsNone,
            contentDescription = "No notifications",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = when (selectedTab) {
                1 -> "No unread notifications"
                2 -> "No attendance notifications"
                3 -> "No approval notifications"
                4 -> "No mission notifications"
                5 -> "No system notifications"
                else -> "No notifications"
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = "You're all caught up!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FilterDialog(
    onDismiss: () -> Unit,
    onFilterApplied: (NotificationPriority?, NotificationType?) -> Unit
) {
    var selectedPriority by remember { mutableStateOf<NotificationPriority?>(null) }
    var selectedType by remember { mutableStateOf<NotificationType?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter Notifications") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Priority filter
                Text("Priority:", style = MaterialTheme.typography.titleSmall)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NotificationPriority.values().forEach { priority ->
                        SuggestionChip(
                            onClick = { 
                                selectedPriority = if (selectedPriority == priority) null else priority
                            },
                            label = { Text(priority.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
                
                // Type filter
                Text("Type:", style = MaterialTheme.typography.titleSmall)
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    NotificationType.values().forEach { type ->
                        SuggestionChip(
                            onClick = { 
                                selectedType = if (selectedType == type) null else type
                            },
                            label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onFilterApplied(selectedPriority, selectedType) }) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun getFilterType(tabIndex: Int): NotificationType? {
    return when (tabIndex) {
        2 -> NotificationType.ATTENDANCE
        3 -> NotificationType.APPROVAL
        4 -> NotificationType.MISSION
        5 -> NotificationType.SYSTEM
        else -> null
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        diff < 604_800_000 -> "${diff / 86_400_000}d ago"
        else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}