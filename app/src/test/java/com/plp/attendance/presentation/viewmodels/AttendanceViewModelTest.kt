package com.plp.attendance.presentation.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.plp.attendance.domain.entities.Attendance
import com.plp.attendance.domain.usecases.AttendanceUseCase
import com.plp.attendance.presentation.screens.attendance.AttendanceScreenViewModel
import com.plp.attendance.presentation.screens.attendance.AttendanceScreenState
import com.plp.attendance.presentation.screens.attendance.AttendanceScreenEvent
import com.plp.attendance.utils.Result
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

@ExperimentalCoroutinesApi
class AttendanceViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @MockK
    lateinit var attendanceUseCase: AttendanceUseCase

    private lateinit var viewModel: AttendanceScreenViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = AttendanceScreenViewModel(attendanceUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be correct`() {
        // Given & When
        val initialState = viewModel.uiState.value

        // Then
        assertFalse(initialState.isLoading)
        assertFalse(initialState.isCheckedIn)
        assertNull(initialState.todayAttendance)
        assertNull(initialState.currentLocation)
        assertTrue(initialState.recentAttendance.isEmpty())
    }

    @Test
    fun `checkIn should update state correctly on success`() = runTest {
        // Given
        val userId = "user123"
        val location = "Office"
        val mockAttendance = createMockAttendance(userId = userId, location = location)

        coEvery { attendanceUseCase.checkIn(userId, location) } returns Result.Success(mockAttendance)
        coEvery { attendanceUseCase.getTodayAttendance(userId) } returns flowOf(listOf(mockAttendance))

        // When
        viewModel.setEvent(AttendanceScreenEvent.CheckIn(userId, location))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.isCheckedIn)
        assertEquals(mockAttendance, state.todayAttendance)
        assertFalse(state.isLoading)

        coVerify { attendanceUseCase.checkIn(userId, location) }
    }

    @Test
    fun `checkIn should handle error correctly`() = runTest {
        // Given
        val userId = "user123"
        val location = "Office"
        val errorMessage = "Network error"

        coEvery { attendanceUseCase.checkIn(userId, location) } returns Result.Error(errorMessage)

        // When
        viewModel.setEvent(AttendanceScreenEvent.CheckIn(userId, location))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isCheckedIn)
        assertFalse(state.isLoading)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains(errorMessage))

        coVerify { attendanceUseCase.checkIn(userId, location) }
    }

    @Test
    fun `checkOut should update state correctly`() = runTest {
        // Given
        val attendanceId = "attendance123"
        val location = "Office"
        val checkOutTime = System.currentTimeMillis()
        val updatedAttendance = createMockAttendance(
            id = attendanceId,
            checkOutTime = checkOutTime
        )

        coEvery { attendanceUseCase.checkOut(attendanceId, location) } returns Result.Success(updatedAttendance)

        // When
        viewModel.setEvent(AttendanceScreenEvent.CheckOut(attendanceId, location))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isCheckedIn)
        assertEquals(updatedAttendance, state.todayAttendance)

        coVerify { attendanceUseCase.checkOut(attendanceId, location) }
    }

    @Test
    fun `loadTodayAttendance should update state with attendance data`() = runTest {
        // Given
        val userId = "user123"
        val mockAttendance = createMockAttendance(userId = userId)

        coEvery { attendanceUseCase.getTodayAttendance(userId) } returns flowOf(listOf(mockAttendance))

        // When
        viewModel.setEvent(AttendanceScreenEvent.LoadTodayAttendance(userId))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(mockAttendance, state.todayAttendance)
        assertTrue(state.isCheckedIn) // Since check-out time is null
        assertFalse(state.isLoading)

        coVerify { attendanceUseCase.getTodayAttendance(userId) }
    }

    @Test
    fun `loadRecentAttendance should update recent attendance list`() = runTest {
        // Given
        val userId = "user123"
        val startDate = "2024-01-01"
        val endDate = "2024-01-31"
        val mockAttendanceList = listOf(
            createMockAttendance(userId = userId, date = "2024-01-15"),
            createMockAttendance(userId = userId, date = "2024-01-20")
        )

        coEvery { 
            attendanceUseCase.getAttendanceByDateRange(userId, startDate, endDate) 
        } returns flowOf(mockAttendanceList)

        // When
        viewModel.setEvent(AttendanceScreenEvent.LoadRecentAttendance(userId, startDate, endDate))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(mockAttendanceList, state.recentAttendance)
        assertEquals(2, state.recentAttendance.size)

        coVerify { attendanceUseCase.getAttendanceByDateRange(userId, startDate, endDate) }
    }

    @Test
    fun `updateLocation should update current location`() = runTest {
        // Given
        val location = "New Office Location"

        // When
        viewModel.setEvent(AttendanceScreenEvent.UpdateLocation(location))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(location, state.currentLocation)
    }

    @Test
    fun `validateAttendance should handle validation errors`() = runTest {
        // Given
        val invalidAttendance = createMockAttendance(
            checkInTime = 0L, // Invalid time
            location = "" // Empty location
        )

        coEvery { attendanceUseCase.validateAttendance(invalidAttendance) } returns Result.Error("Validation failed")

        // When
        viewModel.setEvent(AttendanceScreenEvent.ValidateAttendance(invalidAttendance))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("Validation failed"))

        coVerify { attendanceUseCase.validateAttendance(invalidAttendance) }
    }

    @Test
    fun `refreshData should reload all attendance data`() = runTest {
        // Given
        val userId = "user123"
        val todayAttendance = createMockAttendance(userId = userId)
        val recentAttendance = listOf(todayAttendance)

        coEvery { attendanceUseCase.getTodayAttendance(userId) } returns flowOf(listOf(todayAttendance))
        coEvery { 
            attendanceUseCase.getAttendanceByDateRange(any(), any(), any()) 
        } returns flowOf(recentAttendance)

        // When
        viewModel.setEvent(AttendanceScreenEvent.RefreshData(userId))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(todayAttendance, state.todayAttendance)
        assertEquals(recentAttendance, state.recentAttendance)
        assertFalse(state.isLoading)

        coVerify { attendanceUseCase.getTodayAttendance(userId) }
        coVerify { attendanceUseCase.getAttendanceByDateRange(any(), any(), any()) }
    }

    @Test
    fun `loading state should be managed correctly during operations`() = runTest {
        // Given
        val userId = "user123"
        val location = "Office"

        coEvery { attendanceUseCase.checkIn(userId, location) } coAnswers {
            // Simulate delay
            kotlinx.coroutines.delay(100)
            Result.Success(createMockAttendance())
        }
        coEvery { attendanceUseCase.getTodayAttendance(userId) } returns flowOf(emptyList())

        // When
        viewModel.setEvent(AttendanceScreenEvent.CheckIn(userId, location))

        // Then - Should be loading initially
        assertTrue(viewModel.uiState.value.isLoading)

        // Wait for completion
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - Should not be loading after completion
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `clearError should reset error state`() = runTest {
        // Given - Set an error first
        viewModel.setEvent(AttendanceScreenEvent.CheckIn("user123", "Office"))
        coEvery { attendanceUseCase.checkIn(any(), any()) } returns Result.Error("Test error")
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify error is set
        assertNotNull(viewModel.uiState.value.errorMessage)

        // When
        viewModel.setEvent(AttendanceScreenEvent.ClearError)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertNull(viewModel.uiState.value.errorMessage)
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
        syncStatus = com.plp.attendance.data.local.entities.SyncStatus.SYNCED
    )
}