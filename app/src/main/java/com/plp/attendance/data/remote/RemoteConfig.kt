package com.plp.attendance.data.remote

import com.plp.attendance.BuildConfig

/**
 * Remote server configuration for Cambodia Education Attendance System
 */
object RemoteConfig {
    // Server Configuration
    // Use 10.0.2.2 to connect to localhost from Android emulator
    const val SERVER_HOST = "10.0.2.2"
    const val API_PORT = 3000
    const val API_BASE_URL = "http://$SERVER_HOST:$API_PORT"
    
    // Database Configuration (for reference - actual connection is handled by backend)
    const val DB_HOST = "157.10.73.52"
    const val DB_PORT = 5432
    const val DB_NAME = "plp_attendance_kotlin"
    const val DB_USER = "admin"
    const val DB_PASSWORD = "P@ssw0rd"
    const val DB_SSL = false
    
    // API Endpoints
    object Endpoints {
        const val AUTH_LOGIN = "/api/auth/login"
        const val AUTH_LOGOUT = "/api/auth/logout"
        const val AUTH_REFRESH = "/api/auth/refresh"
        const val AUTH_VALIDATE = "/api/auth/validate"
        
        const val USERS = "/api/users"
        const val USER_BY_ID = "/api/users/{id}"
        const val USER_BY_ROLE = "/api/users/role/{role}"
        const val USER_BY_DEPARTMENT = "/api/users/department/{departmentId}"
        
        const val ATTENDANCE = "/api/attendance"
        const val ATTENDANCE_CHECK_IN = "/api/attendance/check-in"
        const val ATTENDANCE_CHECK_OUT = "/api/attendance/check-out"
        const val ATTENDANCE_BY_USER = "/api/attendance/user/{userId}"
        const val ATTENDANCE_BY_DATE = "/api/attendance/date/{date}"
        
        const val LEAVE = "/api/leave"
        const val LEAVE_BY_ID = "/api/leave/{id}"
        const val LEAVE_BY_USER = "/api/leave/user/{userId}"
        const val LEAVE_APPROVE = "/api/leave/{id}/approve"
        const val LEAVE_REJECT = "/api/leave/{id}/reject"
        
        const val SCHOOLS = "/api/schools"
        const val SCHOOL_BY_ID = "/api/schools/{id}"
        
        const val REPORTS = "/api/reports"
        const val REPORT_ATTENDANCE = "/api/reports/attendance"
        const val REPORT_LEAVE = "/api/reports/leave"
        
        const val NOTIFICATIONS = "/api/notifications"
        const val NOTIFICATION_MARK_READ = "/api/notifications/{id}/read"
    }
    
    // Sync Configuration
    object Sync {
        const val SYNC_INTERVAL_MINUTES = 15L
        const val MAX_RETRY_ATTEMPTS = 3
        const val RETRY_DELAY_SECONDS = 30L
        const val BATCH_SIZE = 50
    }
    
    // Security Configuration
    object Security {
        const val TOKEN_EXPIRY_DAYS = 7
        const val REFRESH_TOKEN_EXPIRY_DAYS = 30
        const val SESSION_TIMEOUT_MINUTES = 30
        const val MAX_LOGIN_ATTEMPTS = 5
        const val LOCKOUT_DURATION_MINUTES = 15
    }
    
    // Feature Flags
    object Features {
        val USE_REMOTE_API = BuildConfig.USE_REMOTE_API
        val ENABLE_OFFLINE_MODE = true
        val ENABLE_BIOMETRIC_AUTH = true
        val ENABLE_LOCATION_TRACKING = true
        val ENABLE_PHOTO_CAPTURE = true
        val ENABLE_NOTIFICATIONS = true
    }
}