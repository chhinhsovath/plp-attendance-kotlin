package com.plp.attendance.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PerformanceMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: Logger
) : DefaultLifecycleObserver {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val handler = Handler(Looper.getMainLooper())
    
    private val operationStartTimes = ConcurrentHashMap<String, Long>()
    private val performanceMetrics = ConcurrentHashMap<String, PerformanceMetric>()
    
    private var isMonitoring = false
    private var memoryMonitoringJob: kotlinx.coroutines.Job? = null
    
    companion object {
        private const val TAG = "PerformanceMonitor"
        private const val MEMORY_CHECK_INTERVAL = 30000L // 30 seconds
        private const val MEMORY_WARNING_THRESHOLD = 0.85f // 85% of max heap
        private const val ANR_DETECTION_INTERVAL = 5000L // 5 seconds
    }
    
    // Initialize monitoring when needed
    
    override fun onStart(owner: LifecycleOwner) {
        startMonitoring()
    }
    
    override fun onStop(owner: LifecycleOwner) {
        stopMonitoring()
    }
    
    fun startMonitoring() {
        if (isMonitoring) return
        
        isMonitoring = true
        logger.i(TAG, "Performance monitoring started")
        
        // Start memory monitoring
        memoryMonitoringJob = scope.launch {
            while (isMonitoring) {
                checkMemoryUsage()
                delay(MEMORY_CHECK_INTERVAL)
            }
        }
        
        // Start ANR detection
        startAnrDetection()
    }
    
    fun stopMonitoring() {
        if (!isMonitoring) return
        
        isMonitoring = false
        memoryMonitoringJob?.cancel()
        logger.i(TAG, "Performance monitoring stopped")
    }
    
    fun startTiming(operation: String) {
        operationStartTimes[operation] = System.currentTimeMillis()
        logger.d(TAG, "Started timing: $operation")
    }
    
    fun endTiming(operation: String, metadata: Map<String, Any> = emptyMap()) {
        val startTime = operationStartTimes.remove(operation)
        if (startTime != null) {
            val duration = System.currentTimeMillis() - startTime
            recordPerformanceMetric(operation, duration, metadata)
            logger.logPerformance(operation, duration, metadata)
        }
    }
    
    fun recordPerformanceMetric(operation: String, duration: Long, metadata: Map<String, Any> = emptyMap()) {
        val metric = performanceMetrics.getOrPut(operation) {
            PerformanceMetric(operation)
        }
        
        metric.record(duration, metadata)
        
        // Log slow operations
        if (duration > getThresholdForOperation(operation)) {
            logger.w(TAG, "Slow operation detected: $operation took ${duration}ms")
        }
    }
    
    fun measureOperation(operation: String, block: () -> Unit) {
        val startTime = System.currentTimeMillis()
        try {
            block()
        } finally {
            val duration = System.currentTimeMillis() - startTime
            recordPerformanceMetric(operation, duration)
        }
    }
    
    suspend fun measureSuspendOperation(operation: String, block: suspend () -> Unit) {
        val startTime = System.currentTimeMillis()
        try {
            block()
        } finally {
            val duration = System.currentTimeMillis() - startTime
            recordPerformanceMetric(operation, duration)
        }
    }
    
    fun getPerformanceMetrics(): Map<String, PerformanceMetric> {
        return performanceMetrics.toMap()
    }
    
    fun getPerformanceReport(): String {
        val report = StringBuilder()
        report.appendLine("=== Performance Report ===")
        report.appendLine("Memory Usage: ${getCurrentMemoryUsage()}")
        report.appendLine("Max Heap Size: ${getMaxHeapSize()}")
        report.appendLine()
        
        report.appendLine("Operation Performance:")
        performanceMetrics.values.sortedByDescending { it.averageDuration }.forEach { metric ->
            report.appendLine("${metric.operation}:")
            report.appendLine("  Average: ${metric.averageDuration}ms")
            report.appendLine("  Min: ${metric.minDuration}ms")
            report.appendLine("  Max: ${metric.maxDuration}ms")
            report.appendLine("  Count: ${metric.count}")
            report.appendLine()
        }
        
        return report.toString()
    }
    
    private fun checkMemoryUsage() {
        val currentMemory = getCurrentMemoryUsage()
        val maxMemory = getMaxHeapSize()
        val usagePercentage = currentMemory.toFloat() / maxMemory
        
        logger.d(TAG, "Memory usage: ${currentMemory / 1024 / 1024}MB / ${maxMemory / 1024 / 1024}MB (${(usagePercentage * 100).toInt()}%)")
        
        if (usagePercentage > MEMORY_WARNING_THRESHOLD) {
            logger.w(TAG, "High memory usage detected: ${(usagePercentage * 100).toInt()}%")
            
            // Suggest garbage collection
            System.gc()
            
            // Log memory warning
            logger.logError(TAG, RuntimeException("High memory usage: ${(usagePercentage * 100).toInt()}%"))
        }
    }
    
    private fun getCurrentMemoryUsage(): Long {
        val memoryInfo = Debug.MemoryInfo()
        Debug.getMemoryInfo(memoryInfo)
        return memoryInfo.totalPss * 1024L // Convert KB to bytes
    }
    
    private fun getMaxHeapSize(): Long {
        return Runtime.getRuntime().maxMemory()
    }
    
    private fun startAnrDetection() {
        if (!isMonitoring) return
        
        val anrDetectionRunnable = object : Runnable {
            override fun run() {
                val startTime = System.currentTimeMillis()
                
                handler.post {
                    val duration = System.currentTimeMillis() - startTime
                    if (duration > ANR_DETECTION_INTERVAL) {
                        logger.w(TAG, "Potential ANR detected: Main thread blocked for ${duration}ms")
                    }
                }
                
                if (isMonitoring) {
                    handler.postDelayed(this, ANR_DETECTION_INTERVAL)
                }
            }
        }
        
        handler.postDelayed(anrDetectionRunnable, ANR_DETECTION_INTERVAL)
    }
    
    private fun getThresholdForOperation(operation: String): Long {
        return when {
            operation.contains("database", ignoreCase = true) -> 100L
            operation.contains("network", ignoreCase = true) -> 3000L
            operation.contains("sync", ignoreCase = true) -> 5000L
            operation.contains("ui", ignoreCase = true) -> 16L // 60 FPS
            operation.contains("startup", ignoreCase = true) -> 2000L
            else -> 1000L
        }
    }
    
    fun logAppLaunchTime(launchType: String, duration: Long) {
        logger.logPerformance("app_launch_$launchType", duration)
        recordPerformanceMetric("app_launch_$launchType", duration)
    }
    
    fun logScreenTransition(fromScreen: String, toScreen: String, duration: Long) {
        val operation = "screen_transition_${fromScreen}_to_${toScreen}"
        logger.logPerformance(operation, duration)
        recordPerformanceMetric(operation, duration)
    }
    
    fun logDatabaseQuery(query: String, duration: Long, rowCount: Int) {
        val operation = "database_query_${query.hashCode()}"
        val metadata = mapOf(
            "query" to query,
            "row_count" to rowCount
        )
        logger.logDatabaseOperation(query, "query", duration, rowCount)
        recordPerformanceMetric(operation, duration, metadata)
    }
    
    fun logNetworkRequest(url: String, method: String, duration: Long, responseCode: Int) {
        val operation = "network_${method}_${url.hashCode()}"
        val metadata = mapOf(
            "url" to url,
            "method" to method,
            "response_code" to responseCode
        )
        logger.logNetworkRequest(url, method, responseCode, duration)
        recordPerformanceMetric(operation, duration, metadata)
    }
    
    fun clearMetrics() {
        performanceMetrics.clear()
        operationStartTimes.clear()
        logger.i(TAG, "Performance metrics cleared")
    }
}

data class PerformanceMetric(
    val operation: String,
    var count: Int = 0,
    var totalDuration: Long = 0L,
    var minDuration: Long = Long.MAX_VALUE,
    var maxDuration: Long = 0L,
    var lastExecutionTime: Long = 0L,
    val metadata: MutableMap<String, Any> = mutableMapOf()
) {
    val averageDuration: Long
        get() = if (count > 0) totalDuration / count else 0L
    
    fun record(duration: Long, additionalMetadata: Map<String, Any> = emptyMap()) {
        count++
        totalDuration += duration
        minDuration = minOf(minDuration, duration)
        maxDuration = maxOf(maxDuration, duration)
        lastExecutionTime = System.currentTimeMillis()
        metadata.putAll(additionalMetadata)
    }
}

// Extension functions for easier performance monitoring
inline fun <T> PerformanceMonitor.measure(operation: String, block: () -> T): T {
    startTiming(operation)
    return try {
        block()
    } finally {
        endTiming(operation)
    }
}

suspend inline fun <T> PerformanceMonitor.measureSuspend(operation: String, block: suspend () -> T): T {
    startTiming(operation)
    return try {
        block()
    } finally {
        endTiming(operation)
    }
}