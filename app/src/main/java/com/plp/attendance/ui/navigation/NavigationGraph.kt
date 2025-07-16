package com.plp.attendance.ui.navigation

import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
// Temporarily commented out for basic build
// import com.plp.attendance.services.DemoModeManager
import com.plp.attendance.ui.auth.LoginScreen
import com.plp.attendance.ui.auth.RealLoginScreen
import com.plp.attendance.ui.auth.AuthViewModel
import com.plp.attendance.domain.model.User
import com.plp.attendance.ui.MainScreen
import com.plp.attendance.ui.demo.DemoSetupScreen
import com.plp.attendance.ui.config.AppConfigScreen
import com.plp.attendance.ui.settings.DeveloperSettingsScreen
import com.plp.attendance.ui.management.UserManagementScreen
import com.plp.attendance.ui.management.UserDetailScreen
import com.plp.attendance.ui.management.CreateUserScreen
import com.plp.attendance.ui.management.EditUserScreen
import com.plp.attendance.data.preferences.AppPreferences
// import com.plp.attendance.presentation.screens.documents.*
// import com.plp.attendance.presentation.screens.audit.*
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Config : Screen("config")
    object Login : Screen("login")
    object DemoSetup : Screen("demo_setup")
    object Main : Screen("main")
    
    // Core Feature Screens
    object Dashboard : Screen("dashboard")
    object Missions : Screen("missions")
    object Schools : Screen("schools")
    object Notifications : Screen("notifications")
    object Profile : Screen("profile")
    object Language : Screen("language")
    object LeaveRequest : Screen("leave_request")
    object LeaveHistory : Screen("leave_history")
    object LeaveCalendar : Screen("leave_calendar")
    object LeaveBalance : Screen("leave_balance")
    object LeaveApproval : Screen("leave_approval")
    object AttendanceCheckIn : Screen("attendance_checkin")
    
    // Developer Settings
    object DeveloperSettings : Screen("developer_settings")
    
    // Document Management Screens
    object DocumentLibrary : Screen("document_library")
    object DocumentUpload : Screen("document_upload")
    object DocumentViewer : Screen("document_viewer/{documentId}") {
        fun createRoute(documentId: String) = "document_viewer/$documentId"
    }
    object DocumentSearch : Screen("document_search")
    object DocumentPermissions : Screen("document_permissions/{documentId}") {
        fun createRoute(documentId: String) = "document_permissions/$documentId"
    }
    
    // Audit Trail Screens
    object AuditLog : Screen("audit_log")
    object AuditLogDetail : Screen("audit_log/{logId}") {
        fun createRoute(logId: String) = "audit_log/$logId"
    }
    object SecurityEvents : Screen("security_events")
    object ComplianceReport : Screen("compliance_report")
    object ComplianceReportDetail : Screen("compliance_report/{reportId}") {
        fun createRoute(reportId: String) = "compliance_report/$reportId"
    }
    object SystemActivity : Screen("system_activity")
    object UserActivity : Screen("user_activity/{userId}") {
        fun createRoute(userId: String) = "user_activity/$userId"
    }
    
    // User Management Screens
    object UserManagement : Screen("user_management")
    object UserDetail : Screen("user_detail/{userId}") {
        fun createRoute(userId: String) = "user_detail/$userId"
    }
    object CreateUser : Screen("create_user")
    object EditUser : Screen("edit_user/{userId}") {
        fun createRoute(userId: String) = "edit_user/$userId"
    }
}

@Composable
fun NavigationGraph(
    navController: NavHostController = rememberNavController(),
    initialRoute: String? = null
) {
    val context = LocalContext.current
    // val demoModeManager = remember { DemoModeManager(context) }
    val appPreferences = remember { AppPreferences(context) }
    val scope = rememberCoroutineScope()
    
    // Start directly with login screen
    val startDestination = Screen.Login.route
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // UserSelection screen removed - using direct login instead
        
        composable(Screen.Login.route) {
            RealLoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Main.route) {
            MainScreen(
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.DeveloperSettings.route) {
            DeveloperSettingsScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToTestLogin = {
                    // Remove TestCredentials - go directly to login
                    navController.navigate(Screen.Login.route)
                }
            )
        }
        
        
        // User Management Navigation
        composable(Screen.UserManagement.route) {
            UserManagementScreen(navController = navController)
        }
        
        composable(Screen.UserDetail.route) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            UserDetailScreen(
                userId = userId,
                navController = navController
            )
        }
        
        composable(Screen.CreateUser.route) {
            CreateUserScreen(navController = navController)
        }
        
        composable(Screen.EditUser.route) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            EditUserScreen(
                userId = userId,
                navController = navController
            )
        }
        
        // TODO: Re-enable complex navigation when all screens are implemented
        // Document Management Navigation and Audit Trail screens
        // temporarily disabled for basic build
    }
}
