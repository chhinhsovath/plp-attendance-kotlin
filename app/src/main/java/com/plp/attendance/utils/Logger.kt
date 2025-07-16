package com.plp.attendance.utils

import android.util.Log
import com.plp.attendance.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Logger @Inject constructor() {
    
    companion object {
        private const val TAG = "PLP_ATTENDANCE"
        private const val MAX_LOG_FILE_SIZE = 5 * 1024 * 1024 // 5MB
        private const val MAX_LOG_FILES = 3
        
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        
        // Log levels
        const val VERBOSE = 0
        const val DEBUG = 1
        const val INFO = 2
        const val WARN = 3
        const val ERROR = 4
    }
    
    private val logScope = CoroutineScope(Dispatchers.IO)
    private var logFile: File? = null
    private var isInitialized = false
    
    fun initialize(logDirectory: File) {
        if (!isInitialized) {
            logFile = File(logDirectory, "plp_attendance_${System.currentTimeMillis()}.log")
            isInitialized = true
            
            // Clean up old log files
            cleanupOldLogs(logDirectory)
            
            i("Logger", "Logger initialized, log file: ${logFile?.absolutePath}")
        }
    }
    
    fun v(tag: String, message: String, throwable: Throwable? = null) {
        log(VERBOSE, tag, message, throwable)
    }
    
    fun d(tag: String, message: String, throwable: Throwable? = null) {
        log(DEBUG, tag, message, throwable)
    }
    
    fun i(tag: String, message: String, throwable: Throwable? = null) {
        log(INFO, tag, message, throwable)
    }
    
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        log(WARN, tag, message, throwable)
    }
    
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        log(ERROR, tag, message, throwable)
    }
    
    private fun log(level: Int, tag: String, message: String, throwable: Throwable?) {
        val logLevel = getLogLevelString(level)
        val timestamp = dateFormat.format(Date())
        val logMessage = "[$timestamp] [$logLevel] [$tag] $message"
        
        // Log to Android Log
        when (level) {
            VERBOSE -> Log.v(TAG, "[$tag] $message", throwable)
            DEBUG -> Log.d(TAG, "[$tag] $message", throwable)
            INFO -> Log.i(TAG, "[$tag] $message", throwable)
            WARN -> Log.w(TAG, "[$tag] $message", throwable)
            ERROR -> Log.e(TAG, "[$tag] $message", throwable)
        }
        
        // Log to file in production or debug mode
        if (BuildConfig.DEBUG || level >= WARN) {
            logToFile(logMessage, throwable)
        }
    }
    
    private fun logToFile(message: String, throwable: Throwable?) {
        if (!isInitialized || logFile == null) return
        
        logScope.launch {
            try {
                val file = logFile ?: return@launch
                
                // Check file size and rotate if necessary
                if (file.length() > MAX_LOG_FILE_SIZE) {
                    rotateLogFile()
                }
                
                FileWriter(file, true).use { writer ->
                    writer.appendLine(message)
                    
                    throwable?.let { exception ->
                        writer.appendLine("Exception: ${exception.message}")
                        exception.stackTrace.forEach { stackElement ->
                            writer.appendLine("    at $stackElement")
                        }
                    }
                    
                    writer.flush()
                }
            } catch (e: IOException) {
                Log.e(TAG, "Failed to write to log file", e)
            }
        }
    }
    
    private fun rotateLogFile() {
        val currentFile = logFile ?: return
        val directory = currentFile.parentFile ?: return
        
        try {
            // Create new log file
            val newLogFile = File(directory, "plp_attendance_${System.currentTimeMillis()}.log")
            logFile = newLogFile
            
            Log.i(TAG, "Log file rotated: ${newLogFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to rotate log file", e)
        }
    }
    
    private fun cleanupOldLogs(logDirectory: File) {
        try {
            val logFiles = logDirectory.listFiles { _, name ->
                name.startsWith("plp_attendance_") && name.endsWith(".log")
            }?.sortedByDescending { it.lastModified() }
            
            if (logFiles != null && logFiles.size > MAX_LOG_FILES) {
                logFiles.drop(MAX_LOG_FILES).forEach { file ->
                    if (file.delete()) {
                        Log.d(TAG, "Deleted old log file: ${file.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup old logs", e)
        }
    }
    
    private fun getLogLevelString(level: Int): String {
        return when (level) {
            VERBOSE -> "V"
            DEBUG -> "D"
            INFO -> "I"
            WARN -> "W"
            ERROR -> "E"
            else -> "U"
        }
    }
    
    fun logUserAction(action: String, details: Map<String, Any> = emptyMap()) {
        val detailsString = details.entries.joinToString(", ") { "${it.key}=${it.value}" }
        i("UserAction", "$action: $detailsString")
    }
    
    fun logPerformance(operation: String, duration: Long, details: Map<String, Any> = emptyMap()) {
        val detailsString = details.entries.joinToString(", ") { "${it.key}=${it.value}" }
        i("Performance", "$operation completed in ${duration}ms: $detailsString")
    }
    
    fun logError(component: String, error: Throwable, context: Map<String, Any> = emptyMap()) {
        val contextString = context.entries.joinToString(", ") { "${it.key}=${it.value}" }
        e(component, "Error occurred: ${error.message}. Context: $contextString", error)
    }
    
    fun logNetworkRequest(url: String, method: String, responseCode: Int, duration: Long) {
        i("Network", "$method $url -> $responseCode (${duration}ms)")
    }
    
    fun logDatabaseOperation(operation: String, table: String, duration: Long, rowCount: Int = 0) {
        d("Database", "$operation on $table completed in ${duration}ms (${rowCount} rows)")
    }
    
    fun logSyncOperation(operation: String, entityType: String, count: Int, success: Boolean) {
        val status = if (success) "SUCCESS" else "FAILED"
        i("Sync", "$operation $count $entityType records: $status")
    }
    
    fun logNotification(type: String, title: String, delivered: Boolean) {
        val status = if (delivered) "DELIVERED" else "FAILED"
        i("Notification", "$type notification '$title': $status")
    }
    
    fun logBiometricAuth(action: String, success: Boolean, errorMessage: String? = null) {
        val status = if (success) "SUCCESS" else "FAILED"
        val message = "Biometric $action: $status${errorMessage?.let { " - $it" } ?: ""}"
        i("Biometric", message)
    }
    
    fun logLocationUpdate(latitude: Double, longitude: Double, accuracy: Float) {
        d("Location", "Location update: lat=$latitude, lng=$longitude, accuracy=${accuracy}m")
    }
    
    fun getLogFiles(): List<File> {
        val directory = logFile?.parentFile ?: return emptyList()
        return directory.listFiles { _, name ->
            name.startsWith("plp_attendance_") && name.endsWith(".log")
        }?.toList() ?: emptyList()
    }
    
    fun clearLogs() {
        logScope.launch {
            try {
                val directory = logFile?.parentFile ?: return@launch
                val logFiles = directory.listFiles { _, name ->
                    name.startsWith("plp_attendance_") && name.endsWith(".log")
                }
                
                logFiles?.forEach { file ->
                    if (file.delete()) {
                        Log.d(TAG, "Deleted log file: ${file.name}")
                    }
                }
                
                // Create new log file
                logFile = File(directory, "plp_attendance_${System.currentTimeMillis()}.log")
                i("Logger", "Logs cleared, new log file: ${logFile?.absolutePath}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear logs", e)
            }
        }
    }
}

// Extension functions for easier logging
inline fun <reified T> T.logDebug(message: String, throwable: Throwable? = null) {
    // In a real implementation, you'd inject Logger
    Log.d(T::class.java.simpleName, message, throwable)
}

inline fun <reified T> T.logInfo(message: String, throwable: Throwable? = null) {
    Log.i(T::class.java.simpleName, message, throwable)
}

inline fun <reified T> T.logWarn(message: String, throwable: Throwable? = null) {
    Log.w(T::class.java.simpleName, message, throwable)
}

inline fun <reified T> T.logError(message: String, throwable: Throwable? = null) {
    Log.e(T::class.java.simpleName, message, throwable)
}