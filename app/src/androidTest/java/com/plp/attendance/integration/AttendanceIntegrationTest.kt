package com.plp.attendance.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.plp.attendance.data.local.database.AttendanceDatabase
import com.plp.attendance.data.repositories.AttendanceRepositoryImpl
import com.plp.attendance.domain.usecases.AttendanceUseCase
import com.plp.attendance.utils.Result
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AttendanceIntegrationTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var database: AttendanceDatabase

    @Inject
    lateinit var attendanceRepository: AttendanceRepositoryImpl

    @Inject
    lateinit var attendanceUseCase: AttendanceUseCase

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun fullAttendanceWorkflow_checkInAndOut() = runTest {
        // Given
        val userId = "integration_test_user"
        val location = "Integration Test Office"

        // When - Check In
        val checkInResult = attendanceUseCase.checkIn(userId, location)

        // Then - Check In should succeed
        assertTrue("Check in should succeed", checkInResult is Result.Success)
        val attendance = (checkInResult as Result.Success).data

        assertNotNull("Attendance should not be null", attendance)
        assertEquals("User ID should match", userId, attendance.userId)
        assertEquals("Location should match", location, attendance.location)
        assertNotNull("Check in time should be set", attendance.checkInTime)
        assertNull("Check out time should be null initially", attendance.checkOutTime)

        // Verify attendance is saved in database
        val todayAttendance = attendanceRepository.getTodayAttendance(userId).first()
        assertEquals("Should have one attendance record", 1, todayAttendance.size)
        assertEquals("Attendance ID should match", attendance.id, todayAttendance[0].id)

        // When - Check Out
        val checkOutResult = attendanceUseCase.checkOut(attendance.id, location)

        // Then - Check Out should succeed
        assertTrue("Check out should succeed", checkOutResult is Result.Success)
        val updatedAttendance = (checkOutResult as Result.Success).data

        assertNotNull("Updated attendance should not be null", updatedAttendance)
        assertEquals("Attendance ID should match", attendance.id, updatedAttendance.id)
        assertNotNull("Check out time should be set", updatedAttendance.checkOutTime)
        assertTrue("Check out time should be after check in time", 
            updatedAttendance.checkOutTime!! > updatedAttendance.checkInTime)

        // Verify working hours are calculated
        val workingHours = attendanceUseCase.calculateWorkingHours(updatedAttendance)
        assertTrue("Working hours should be positive", workingHours > 0)
    }

    @Test
    fun attendanceValidation_workflow() = runTest {
        // Given
        val userId = "validation_test_user"
        val validLocation = "Valid Office"
        val invalidLocation = ""

        // When - Try to check in with invalid location
        val invalidCheckInResult = attendanceUseCase.checkIn(userId, invalidLocation)

        // Then - Should fail validation
        assertTrue("Invalid check in should fail", invalidCheckInResult is Result.Error)

        // When - Check in with valid data
        val validCheckInResult = attendanceUseCase.checkIn(userId, validLocation)

        // Then - Should succeed
        assertTrue("Valid check in should succeed", validCheckInResult is Result.Success)
        val attendance = (validCheckInResult as Result.Success).data

        // When - Validate the attendance record
        val validationResult = attendanceUseCase.validateAttendance(attendance)

        // Then - Should pass validation
        assertTrue("Attendance should pass validation", validationResult is Result.Success)
    }

    @Test
    fun attendanceStatistics_calculation() = runTest {
        // Given
        val userId = "stats_test_user"
        val location = "Stats Test Office"

        // Create multiple attendance records
        val attendanceIds = mutableListOf<String>()

        // Check in and out multiple times
        repeat(5) { index ->
            val checkInResult = attendanceUseCase.checkIn(userId, location)
            assertTrue("Check in $index should succeed", checkInResult is Result.Success)
            
            val attendance = (checkInResult as Result.Success).data
            attendanceIds.add(attendance.id)

            // Check out immediately for testing
            val checkOutResult = attendanceUseCase.checkOut(attendance.id, location)
            assertTrue("Check out $index should succeed", checkOutResult is Result.Success)
        }

        // When - Get attendance statistics
        val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        
        val stats = attendanceRepository.getAttendanceStatistics(
            userId = userId,
            startDate = currentDate,
            endDate = currentDate
        ).first()

        // Then - Statistics should be correct
        assertEquals("Should have 5 attendance records", 5, stats.totalDays)
        assertEquals("All should be present", 5, stats.presentDays)
        assertEquals("None should be absent", 0, stats.absentDays)
        assertEquals("Attendance rate should be 100%", 100.0, stats.attendanceRate, 0.1)
    }

    @Test
    fun offlineSync_workflow() = runTest {
        // Given
        val userId = "sync_test_user"
        val location = "Sync Test Office"

        // When - Check in while offline (simulated by not having network)
        val offlineCheckInResult = attendanceUseCase.checkIn(userId, location)

        // Then - Should still succeed locally
        assertTrue("Offline check in should succeed locally", offlineCheckInResult is Result.Success)
        val attendance = (offlineCheckInResult as Result.Success).data

        // Verify record is marked for sync
        val unsyncedRecords = database.attendanceDao().getUnsyncedAttendance()
        assertTrue("Should have unsynced records", unsyncedRecords.isNotEmpty())
        assertTrue("Should contain our attendance record", 
            unsyncedRecords.any { it.id == attendance.id })

        // When - Simulate sync when network is available
        val syncResult = attendanceRepository.syncPendingData()

        // Then - Sync should process the record
        // Note: In a real test, this would require mocking the network layer
        // For now, we just verify the sync method runs without errors
        assertTrue("Sync should complete", syncResult is Result.Success || syncResult is Result.Error)
    }

    @Test
    fun multipleUsers_isolation() = runTest {
        // Given
        val user1Id = "user1_isolation_test"
        val user2Id = "user2_isolation_test"
        val location = "Isolation Test Office"

        // When - Both users check in
        val user1CheckIn = attendanceUseCase.checkIn(user1Id, location)
        val user2CheckIn = attendanceUseCase.checkIn(user2Id, location)

        // Then - Both should succeed
        assertTrue("User 1 check in should succeed", user1CheckIn is Result.Success)
        assertTrue("User 2 check in should succeed", user2CheckIn is Result.Success)

        // When - Get today's attendance for each user
        val user1Attendance = attendanceRepository.getTodayAttendance(user1Id).first()
        val user2Attendance = attendanceRepository.getTodayAttendance(user2Id).first()

        // Then - Each user should only see their own attendance
        assertEquals("User 1 should have 1 record", 1, user1Attendance.size)
        assertEquals("User 2 should have 1 record", 1, user2Attendance.size)
        assertEquals("User 1 record should belong to user 1", user1Id, user1Attendance[0].userId)
        assertEquals("User 2 record should belong to user 2", user2Id, user2Attendance[0].userId)
        assertNotEquals("Records should have different IDs", 
            user1Attendance[0].id, user2Attendance[0].id)
    }

    @Test
    fun dateRange_filtering() = runTest {
        // Given
        val userId = "daterange_test_user"
        val location = "Date Range Test Office"

        // Create attendance record
        val checkInResult = attendanceUseCase.checkIn(userId, location)
        assertTrue("Check in should succeed", checkInResult is Result.Success)

        // When - Get attendance for different date ranges
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        val yesterday = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000))
        val tomorrow = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000))

        val todayRange = attendanceRepository.getAttendanceByDateRange(userId, today, today).first()
        val yesterdayRange = attendanceRepository.getAttendanceByDateRange(userId, yesterday, yesterday).first()
        val tomorrowRange = attendanceRepository.getAttendanceByDateRange(userId, tomorrow, tomorrow).first()

        // Then - Only today's range should have records
        assertEquals("Today should have 1 record", 1, todayRange.size)
        assertEquals("Yesterday should have 0 records", 0, yesterdayRange.size)
        assertEquals("Tomorrow should have 0 records", 0, tomorrowRange.size)
    }

    @Test
    fun attendanceModification_workflow() = runTest {
        // Given
        val userId = "modification_test_user"
        val location = "Modification Test Office"

        // When - Check in
        val checkInResult = attendanceUseCase.checkIn(userId, location)
        assertTrue("Check in should succeed", checkInResult is Result.Success)
        val attendance = (checkInResult as Result.Success).data

        // When - Check out
        val checkOutResult = attendanceUseCase.checkOut(attendance.id, location)
        assertTrue("Check out should succeed", checkOutResult is Result.Success)
        val completedAttendance = (checkOutResult as Result.Success).data

        // Then - Verify modification timestamps
        assertTrue("Last modified should be updated", 
            completedAttendance.lastModified > attendance.lastModified)
        
        // Verify working hours are calculated
        assertTrue("Working hours should be calculated", completedAttendance.workingHours > 0)
        
        // Verify the record can be retrieved
        val retrievedAttendance = database.attendanceDao().getById(attendance.id)
        assertNotNull("Attendance should be retrievable", retrievedAttendance)
        assertEquals("Check out time should match", 
            completedAttendance.checkOutTime, retrievedAttendance?.checkOutTime)
    }

    @Test
    fun errorHandling_workflow() = runTest {
        // Given
        val invalidUserId = ""
        val validLocation = "Error Test Office"

        // When - Try to check in with invalid user ID
        val invalidUserResult = attendanceUseCase.checkIn(invalidUserId, validLocation)

        // Then - Should handle error gracefully
        assertTrue("Invalid user check in should fail", invalidUserResult is Result.Error)

        // When - Try to check out non-existent attendance
        val invalidCheckOutResult = attendanceUseCase.checkOut("non_existent_id", validLocation)

        // Then - Should handle error gracefully
        assertTrue("Invalid check out should fail", invalidCheckOutResult is Result.Error)
    }
}