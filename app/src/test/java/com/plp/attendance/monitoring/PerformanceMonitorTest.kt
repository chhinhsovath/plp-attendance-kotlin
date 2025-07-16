package com.plp.attendance.monitoring

import android.content.Context
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@ExperimentalCoroutinesApi
class PerformanceMonitorTest {

    @MockK
    lateinit var context: Context

    private lateinit var performanceMonitor: PerformanceMonitor
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        
        // Mock context methods
        every { context.getSystemService(Context.ACTIVITY_SERVICE) } returns mockk(relaxed = true)
        every { context.filesDir } returns mockk(relaxed = true)
        every { context.registerReceiver(any(), any()) } returns mockk(relaxed = true)
        
        performanceMonitor = PerformanceMonitor(context)
    }

    @After
    fun tearDown() {
        performanceMonitor.cleanup()
    }

    @Test
    fun `startMonitoring should initialize monitoring correctly`() = runTest {
        // When
        performanceMonitor.startMonitoring()

        // Then
        // Verify monitoring has started by checking if metrics are being updated
        advanceTimeBy(2000) // Advance time to allow monitoring to run
        
        val memoryMetrics = performanceMonitor.memoryMetrics.first()
        // Memory metrics should be initialized (though values might be 0 in test environment)
        assertNotNull("Memory metrics should be initialized", memoryMetrics)
    }

    @Test
    fun `stopMonitoring should stop monitoring correctly`() = runTest {
        // Given
        performanceMonitor.startMonitoring()
        advanceTimeBy(1000)

        // When
        performanceMonitor.stopMonitoring()

        // Then
        // Monitoring should stop - this is verified by ensuring no exceptions are thrown
        // and the monitor can be safely cleaned up
        performanceMonitor.cleanup()
    }

    @Test
    fun `trackNetworkRequest should update network metrics`() = runTest {
        // Given
        val url = "https://api.example.com/test"
        val startTime = System.currentTimeMillis()
        val endTime = startTime + 1000 // 1 second duration
        val success = true

        // When
        performanceMonitor.trackNetworkRequest(url, startTime, endTime, success)
        advanceUntilIdle()

        // Then
        val networkMetrics = performanceMonitor.networkMetrics.first()
        assertEquals("Total requests should be 1", 1, networkMetrics.totalRequests)
        assertEquals("Successful requests should be 1", 1, networkMetrics.successfulRequests)
        assertEquals("Failed requests should be 0", 0, networkMetrics.failedRequests)
        assertEquals("Average response time should be 1000ms", 1000L, networkMetrics.averageResponseTime)
    }

    @Test
    fun `trackNetworkRequest should handle failed requests`() = runTest {
        // Given
        val url = "https://api.example.com/test"
        val startTime = System.currentTimeMillis()
        val endTime = startTime + 2000 // 2 seconds duration
        val success = false
        val error = "Network timeout"

        // When
        performanceMonitor.trackNetworkRequest(url, startTime, endTime, success, error)
        advanceUntilIdle()

        // Then
        val networkMetrics = performanceMonitor.networkMetrics.first()
        assertEquals("Total requests should be 1", 1, networkMetrics.totalRequests)
        assertEquals("Successful requests should be 0", 0, networkMetrics.successfulRequests)
        assertEquals("Failed requests should be 1", 1, networkMetrics.failedRequests)
    }

    @Test
    fun `trackDatabaseQuery should update database metrics`() = runTest {
        // Given
        val query = "SELECT * FROM attendance WHERE userId = ?"
        val startTime = System.currentTimeMillis()
        val endTime = startTime + 50 // 50ms duration
        val success = true

        // When
        performanceMonitor.trackDatabaseQuery(query, startTime, endTime, success)
        advanceUntilIdle()

        // Then
        val databaseMetrics = performanceMonitor.databaseMetrics.first()
        assertEquals("Total queries should be 1", 1, databaseMetrics.totalQueries)
        assertEquals("Successful queries should be 1", 1, databaseMetrics.successfulQueries)
        assertEquals("Failed queries should be 0", 0, databaseMetrics.failedQueries)
        assertEquals("Average query time should be 50ms", 50L, databaseMetrics.averageQueryTime)
    }

    @Test
    fun `trackDatabaseQuery should emit slow query event`() = runTest {
        // Given
        val query = "SELECT * FROM large_table"
        val startTime = System.currentTimeMillis()
        val endTime = startTime + 150 // 150ms duration (above threshold)
        val success = true

        var slowQueryEventReceived = false

        // Collect performance events
        val eventCollectionJob = launch {
            performanceMonitor.performanceEvents.collect { event ->
                if (event is PerformanceEvent.SlowDatabaseQuery) {
                    slowQueryEventReceived = true
                }
            }
        }

        // When
        performanceMonitor.trackDatabaseQuery(query, startTime, endTime, success)
        advanceUntilIdle()

        // Then
        assertTrue("Slow database query event should be emitted", slowQueryEventReceived)
        
        eventCollectionJob.cancel()
    }

    @Test
    fun `multiple network requests should calculate correct averages`() = runTest {
        // Given
        val requests = listOf(
            Triple("url1", 1000L, true),  // 1 second
            Triple("url2", 2000L, true),  // 2 seconds  
            Triple("url3", 500L, false),  // 0.5 seconds
            Triple("url4", 1500L, true)   // 1.5 seconds
        )

        // When
        requests.forEach { (url, duration, success) ->
            val startTime = System.currentTimeMillis()
            val endTime = startTime + duration
            performanceMonitor.trackNetworkRequest(url, startTime, endTime, success)
        }
        advanceUntilIdle()

        // Then
        val networkMetrics = performanceMonitor.networkMetrics.first()
        assertEquals("Total requests should be 4", 4, networkMetrics.totalRequests)
        assertEquals("Successful requests should be 3", 3, networkMetrics.successfulRequests)
        assertEquals("Failed requests should be 1", 1, networkMetrics.failedRequests)
        
        // Average should be (1000 + 2000 + 500 + 1500) / 4 = 1250ms
        assertEquals("Average response time should be 1250ms", 1250L, networkMetrics.averageResponseTime)
        assertEquals("Slowest request should be 2000ms", 2000L, networkMetrics.slowestRequest)
        assertEquals("Fastest request should be 500ms", 500L, networkMetrics.fastestRequest)
    }

    @Test
    fun `generatePerformanceReport should create comprehensive report`() = runTest {
        // Given
        performanceMonitor.startMonitoring()
        
        // Add some test data
        performanceMonitor.trackNetworkRequest("test-url", 1000, 2000, true)
        performanceMonitor.trackDatabaseQuery("SELECT * FROM test", 1000, 1050, true)
        
        advanceTimeBy(1000)

        // When
        val report = performanceMonitor.generatePerformanceReport()

        // Then
        assertNotNull("Report should not be null", report)
        assertTrue("Report timestamp should be recent", 
            report.timestamp > System.currentTimeMillis() - 5000)
        assertNotNull("Memory metrics should be included", report.memoryMetrics)
        assertNotNull("CPU metrics should be included", report.cpuMetrics)
        assertNotNull("Network metrics should be included", report.networkMetrics)
        assertNotNull("Database metrics should be included", report.databaseMetrics)
        assertNotNull("Frame metrics should be included", report.frameMetrics)
        assertTrue("Performance score should be between 0 and 100", 
            report.performanceScore in 0..100)
        assertNotNull("Recommendations should be included", report.recommendations)
    }

    @Test
    fun `performance events should be emitted for critical conditions`() = runTest {
        // Given
        val receivedEvents = mutableListOf<PerformanceEvent>()
        
        val eventCollectionJob = launch {
            performanceMonitor.performanceEvents.collect { event ->
                receivedEvents.add(event)
            }
        }

        // When - Track a very slow network request
        performanceMonitor.trackNetworkRequest(
            "slow-url", 
            System.currentTimeMillis(), 
            System.currentTimeMillis() + 6000, // 6 seconds (above threshold)
            false,
            "Timeout error"
        )
        
        // Track a slow database query
        performanceMonitor.trackDatabaseQuery(
            "SLOW SELECT", 
            System.currentTimeMillis(), 
            System.currentTimeMillis() + 200, // 200ms (above threshold)
            true
        )
        
        advanceUntilIdle()

        // Then
        assertTrue("Should have received performance events", receivedEvents.isNotEmpty())
        assertTrue("Should have slow network request event", 
            receivedEvents.any { it is PerformanceEvent.SlowNetworkRequest })
        assertTrue("Should have slow database query event", 
            receivedEvents.any { it is PerformanceEvent.SlowDatabaseQuery })
        
        eventCollectionJob.cancel()
    }

    @Test
    fun `memory metrics should be tracked`() = runTest {
        // Given
        performanceMonitor.startMonitoring()
        advanceTimeBy(2000) // Allow monitoring to collect some data

        // When
        val memoryMetrics = performanceMonitor.memoryMetrics.first()

        // Then
        assertNotNull("Memory metrics should be available", memoryMetrics)
        assertTrue("Memory usage percentage should be valid", 
            memoryMetrics.memoryUsagePercentage >= 0.0f)
        assertTrue("Total heap size should be positive", 
            memoryMetrics.totalHeapSize >= 0L)
        assertTrue("Used heap size should be positive", 
            memoryMetrics.usedHeapSize >= 0L)
    }

    @Test
    fun `cpu metrics should be tracked`() = runTest {
        // Given
        performanceMonitor.startMonitoring()
        advanceTimeBy(2000) // Allow monitoring to collect some data

        // When
        val cpuMetrics = performanceMonitor.cpuMetrics.first()

        // Then
        assertNotNull("CPU metrics should be available", cpuMetrics)
        assertTrue("CPU usage should be valid", 
            cpuMetrics.appCpuUsagePercentage >= 0.0)
        assertTrue("CPU cores should be positive", 
            cpuMetrics.cpuCores > 0)
    }

    @Test
    fun `cleanup should stop all monitoring activities`() = runTest {
        // Given
        performanceMonitor.startMonitoring()
        advanceTimeBy(1000)

        // When
        performanceMonitor.cleanup()

        // Then
        // Should not throw any exceptions and should stop gracefully
        // We can verify this by ensuring subsequent operations don't cause issues
        performanceMonitor.trackNetworkRequest("test", 1000, 2000, true)
        advanceUntilIdle()
        
        // Should complete without errors
    }
}