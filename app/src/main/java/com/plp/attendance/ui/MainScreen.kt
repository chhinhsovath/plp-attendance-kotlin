package com.plp.attendance.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import com.plp.attendance.R
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
// Temporarily commented out for basic build
// import com.plp.attendance.ui.attendance.AttendanceScreen
// import com.plp.attendance.ui.leave.LeaveScreen
// import com.plp.attendance.ui.reports.ReportsScreen
// import com.plp.attendance.ui.analytics.AnalyticsScreen
// import com.plp.attendance.ui.management.UserManagementScreen
// import com.plp.attendance.ui.settings.SettingsScreen
// import com.plp.attendance.ui.navigation.RoleBasedMainScreen
import com.plp.attendance.domain.model.UserRole
// import com.plp.attendance.services.DemoModeManager
// import com.plp.attendance.services.PermissionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var showLeaveRequest by remember { mutableStateOf(false) }
    val navController = rememberNavController()
    
    if (showLeaveRequest) {
        com.plp.attendance.ui.leave.LeaveRequestScreen(
            onNavigateBack = { showLeaveRequest = false }
        )
    } else {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Main content
            Box(
                modifier = Modifier.weight(1f)
            ) {
                when (selectedTab) {
                    0 -> com.plp.attendance.ui.attendance.AttendanceScreen(
                        onLogout = onLogout
                    )
                    1 -> com.plp.attendance.ui.leave.LeaveScreen(
                        onNavigateToRequest = { showLeaveRequest = true }
                    )
                    2 -> PlaceholderScreen(stringResource(R.string.reports))
                    3 -> com.plp.attendance.ui.settings.SettingsScreen(
                        onLogout = onLogout,
                        navController = navController
                    )
                    else -> PlaceholderScreen(stringResource(R.string.coming_soon))
                }
            }
            
            // Bottom Navigation
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.CheckCircle, contentDescription = stringResource(R.string.attendance)) },
                    label = { Text(stringResource(R.string.attendance)) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.CalendarToday, contentDescription = stringResource(R.string.leave)) },
                    label = { Text(stringResource(R.string.leave)) }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = stringResource(R.string.reports)) },
                    label = { Text(stringResource(R.string.reports)) }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings)) },
                    label = { Text(stringResource(R.string.settings)) }
                )
            }
        }
    }
}

@Composable
fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = stringResource(R.string.coming_soon),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
