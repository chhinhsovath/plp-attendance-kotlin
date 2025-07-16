package com.plp.attendance.domain.usecases

import com.plp.attendance.domain.entities.Attendance
import com.plp.attendance.domain.repositories.AttendanceRepository
import com.plp.attendance.data.local.entities.SyncStatus
import com.plp.attendance.utils.Result
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class AttendanceUseCaseTest {

    @MockK
    lateinit var attendanceRepository: AttendanceRepository

    private lateinit var attendanceUseCase: AttendanceUseCase

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        attendanceUseCase = AttendanceUseCase(attendanceRepository)
    }

    @Test
    fun `checkIn should create attendance record successfully`() = runTest {
        // Given
        val userId = "user123"
        val location = "Office"
        val expectedAttendance = createMockAttendance(userId = userId, location = location)
        
        coEvery { attendanceRepository.checkIn(userId, location) } returns Result.Success(expectedAttendance)

        // When
        val result = attendanceUseCase.checkIn(userId, location)

        // Then
        assertTrue(result is Result.Success)
        assertEquals(expectedAttendance, (result as Result.Success).data)
        coVerify { attendanceRepository.checkIn(userId, location) }
    }

    @Test
    fun `checkIn should handle repository error`() = runTest {
        // Given
        val userId = "user123"
        val location = "Office"
        val errorMessage = "Network error"
        
        coEvery { attendanceRepository.checkIn(userId, location) } returns Result.Error(errorMessage)

        // When
        val result = attendanceUseCase.checkIn(userId, location)

        // Then
        assertTrue(result is Result.Error)
        assertEquals(errorMessage, (result as Result.Error).message)
    }

    @Test
    fun `checkOut should update attendance record`() = runTest {
        // Given
        val attendanceId = "attendance123"
        val location = "Office"
        val updatedAttendance = createMockAttendance(id = attendanceId, checkOutTime = System.currentTimeMillis())
        
        coEvery { attendanceRepository.checkOut(attendanceId, location) } returns Result.Success(updatedAttendance)

        // When
        val result = attendanceUseCase.checkOut(attendanceId, location)

        // Then
        assertTrue(result is Result.Success)
        assertEquals(updatedAttendance, (result as Result.Success).data)
        coVerify { attendanceRepository.checkOut(attendanceId, location) }
    }

    @Test
    fun `getTodayAttendance should return today's attendance`() = runTest {
        // Given
        val userId = "user123"
        val todayAttendance = listOf(createMockAttendance(userId = userId))
        
        coEvery { attendanceRepository.getTodayAttendance(userId) } returns flowOf(todayAttendance)

        // When
        attendanceUseCase.getTodayAttendance(userId).collect { result ->
            // Then
            assertEquals(todayAttendance, result)
        }

        coVerify { attendanceRepository.getTodayAttendance(userId) }
    }

    @Test
    fun `getAttendanceByDateRange should return filtered attendance`() = runTest {
        // Given
        val userId = "user123"
        val startDate = "2024-01-01"
        val endDate = "2024-01-31"
        val attendanceList = listOf(
            createMockAttendance(userId = userId, date = "2024-01-15"),
            createMockAttendance(userId = userId, date = "2024-01-20")
        )
        
        coEvery { 
            attendanceRepository.getAttendanceByDateRange(userId, startDate, endDate) 
        } returns flowOf(attendanceList)

        // When
        attendanceUseCase.getAttendanceByDateRange(userId, startDate, endDate).collect { result ->
            // Then
            assertEquals(attendanceList, result)
            assertEquals(2, result.size)
        }

        coVerify { attendanceRepository.getAttendanceByDateRange(userId, startDate, endDate) }
    }

    @Test
    fun `validateAttendance should return validation errors for invalid data`() = runTest {
        // Given
        val invalidAttendance = createMockAttendance(
            checkInTime = 0L, // Invalid check-in time
            location = "" // Empty location
        )

        // When
        val result = attendanceUseCase.validateAttendance(invalidAttendance)

        // Then
        assertTrue(result is Result.Error)
        val errorMessage = (result as Result.Error).message
        assertTrue(errorMessage.contains("Invalid check-in time") || errorMessage.contains("Location is required"))
    }

    @Test
    fun `validateAttendance should return success for valid data`() = runTest {
        // Given
        val validAttendance = createMockAttendance()

        // When
        val result = attendanceUseCase.validateAttendance(validAttendance)

        // Then
        assertTrue(result is Result.Success)
    }

    @Test
    fun `calculateWorkingHours should return correct duration`() = runTest {
        // Given
        val checkInTime = 1640995200000L // 9:00 AM
        val checkOutTime = 1641024000000L // 5:00 PM (8 hours later)
        val attendance = createMockAttendance(
            checkInTime = checkInTime,
            checkOutTime = checkOutTime
        )

        // When
        val workingHours = attendanceUseCase.calculateWorkingHours(attendance)

        // Then
        assertEquals(8.0, workingHours, 0.1) // 8 hours with 0.1 tolerance
    }

    @Test
    fun `isLateArrival should return true for late check-in`() = runTest {
        // Given
        val lateCheckInTime = 1641002400000L // 11:00 AM (assuming 9:00 AM is standard)
        val attendance = createMockAttendance(checkInTime = lateCheckInTime)

        // When
        val isLate = attendanceUseCase.isLateArrival(attendance, standardStartTime = "09:00")

        // Then
        assertTrue(isLate)
    }

    @Test
    fun `isLateArrival should return false for on-time check-in`() = runTest {
        // Given
        val onTimeCheckIn = 1640995200000L // 9:00 AM
        val attendance = createMockAttendance(checkInTime = onTimeCheckIn)

        // When
        val isLate = attendanceUseCase.isLateArrival(attendance, standardStartTime = "09:00")

        // Then
        assertFalse(isLate)
    }

    private fun createMockAttendance(
        id: String = "attendance123",
        userId: String = "user123",
        date: String = "2024-01-15",
        checkInTime: Long = System.currentTimeMillis(),
        checkOutTime: Long? = null,
        location: String = "Office",
        status: String = "present"
    ) = Attendance(
        id = id,
        userId = userId,
        date = date,
        checkInTime = checkInTime,
        checkOutTime = checkOutTime,
        location = location,
        status = status,
        workingHours = 0.0,
        overtime = 0.0,
        notes = "",
        ipAddress = "192.168.1.1",
        deviceInfo = "Android Device",
        gpsCoordinates = Pair(11.5564, 104.9282),
        isManualEntry = false,
        approvedBy = null,
        createdAt = System.currentTimeMillis(),
        lastModified = System.currentTimeMillis(),
        syncStatus = SyncStatus.SYNCED
    )
}