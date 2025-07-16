package com.plp.attendance.domain.usecase.attendance

import com.plp.attendance.domain.model.*
import com.plp.attendance.domain.repository.AttendanceRepository
import com.plp.attendance.domain.repository.UserRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
import java.time.LocalTime

class CheckInUseCaseTest {
    
    private lateinit var attendanceRepository: AttendanceRepository
    private lateinit var userRepository: UserRepository
    private lateinit var checkInUseCase: CheckInUseCase
    
    private val testUser = User(
        id = "user-123",
        username = "teacher@school.edu.kh",
        name = "Test Teacher",
        email = "teacher@school.edu.kh",
        phone = "+855123456789",
        role = "TEACHER",
        department = "Primary",
        schoolId = "school-1",
        schoolName = "Demo School",
        profilePhotoPath = null,
        isActive = true
    )
    
    @Before
    fun setup() {
        attendanceRepository = mockk()
        userRepository = mockk()
        checkInUseCase = CheckInUseCase(attendanceRepository, userRepository)
    }
    
    @Test
    fun `check in with valid data returns success`() = runTest {
        // Given
        val latitude = 11.5449
        val longitude = 104.8922
        val photoPath = "/path/to/photo.jpg"
        val expectedAttendance = Attendance(
            id = "att-123",
            userId = testUser.id,
            date = LocalDate.now(),
            checkInTime = LocalTime.now(),
            checkOutTime = null,
            checkInLocation = Location(latitude, longitude),
            checkOutLocation = null,
            checkInPhotoPath = photoPath,
            checkOutPhotoPath = null,
            status = AttendanceStatus.PRESENT,
            workingHours = null,
            isLate = false,
            syncStatus = SyncStatus.PENDING
        )
        
        coEvery { userRepository.getCurrentUser() } returns testUser
        coEvery { 
            attendanceRepository.checkIn(testUser.id, latitude, longitude, photoPath) 
        } returns Result.success(expectedAttendance)
        
        // When
        val result = checkInUseCase(latitude, longitude, photoPath)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedAttendance, result.getOrNull())
        
        coVerify(exactly = 1) { userRepository.getCurrentUser() }
        coVerify(exactly = 1) { 
            attendanceRepository.checkIn(testUser.id, latitude, longitude, photoPath) 
        }
    }
    
    @Test
    fun `check in without location returns success`() = runTest {
        // Given
        val expectedAttendance = Attendance(
            id = "att-123",
            userId = testUser.id,
            date = LocalDate.now(),
            checkInTime = LocalTime.now(),
            checkOutTime = null,
            checkInLocation = null,
            checkOutLocation = null,
            checkInPhotoPath = null,
            checkOutPhotoPath = null,
            status = AttendanceStatus.PRESENT,
            workingHours = null,
            isLate = false,
            syncStatus = SyncStatus.PENDING
        )
        
        coEvery { userRepository.getCurrentUser() } returns testUser
        coEvery { 
            attendanceRepository.checkIn(testUser.id, null, null, null) 
        } returns Result.success(expectedAttendance)
        
        // When
        val result = checkInUseCase(null, null, null)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedAttendance, result.getOrNull())
    }
    
    @Test
    fun `check in with no logged in user returns error`() = runTest {
        // Given
        coEvery { userRepository.getCurrentUser() } returns null
        
        // When
        val result = checkInUseCase(11.5449, 104.8922, null)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals("No user logged in", result.exceptionOrNull()?.message)
        
        coVerify(exactly = 1) { userRepository.getCurrentUser() }
        coVerify(exactly = 0) { attendanceRepository.checkIn(any(), any(), any(), any()) }
    }
    
    @Test
    fun `check in propagates repository error`() = runTest {
        // Given
        val latitude = 11.5449
        val longitude = 104.8922
        val expectedError = AppError.Business.AlreadyCheckedIn
        
        coEvery { userRepository.getCurrentUser() } returns testUser
        coEvery { 
            attendanceRepository.checkIn(testUser.id, latitude, longitude, null) 
        } returns Result.failure(expectedError)
        
        // When
        val result = checkInUseCase(latitude, longitude, null)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals(expectedError, result.exceptionOrNull())
    }
}