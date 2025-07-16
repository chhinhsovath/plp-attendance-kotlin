package com.plp.attendance.data.repositories

import com.plp.attendance.data.local.dao.AttendanceDao
import com.plp.attendance.data.local.entities.AttendanceEntity
import com.plp.attendance.data.remote.api.AttendanceApiService
import com.plp.attendance.data.remote.dto.AttendanceDto
import com.plp.attendance.domain.entities.Attendance
import com.plp.attendance.utils.Result
import com.plp.attendance.utils.NetworkUtils
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class AttendanceRepositoryTest {

    @MockK
    lateinit var attendanceDao: AttendanceDao

    @MockK
    lateinit var attendanceApiService: AttendanceApiService

    @MockK
    lateinit var networkUtils: NetworkUtils

    private lateinit var attendanceRepository: AttendanceRepositoryImpl

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        attendanceRepository = AttendanceRepositoryImpl(
            attendanceDao = attendanceDao,
            attendanceApiService = attendanceApiService,
            networkUtils = networkUtils
        )
    }

    @Test
    fun `checkIn should save locally and sync with server when network available`() = runTest {
        // Given
        val userId = "user123"
        val location = "Office"
        val mockEntity = createMockAttendanceEntity(userId = userId, location = location)
        val mockDto = createMockAttendanceDto()

        every { networkUtils.isNetworkAvailable() } returns true
        coEvery { attendanceDao.insert(any()) } returns Unit
        coEvery { attendanceApiService.checkIn(any()) } returns mockDto
        coEvery { attendanceDao.updateSyncStatus(any(), any()) } returns Unit

        // When
        val result = attendanceRepository.checkIn(userId, location)

        // Then
        assertTrue(result is Result.Success)
        coVerify { attendanceDao.insert(any()) }
        coVerify { attendanceApiService.checkIn(any()) }
        coVerify { attendanceDao.updateSyncStatus(any(), any()) }
    }

    @Test
    fun `checkIn should save locally only when network unavailable`() = runTest {
        // Given
        val userId = "user123"
        val location = "Office"

        every { networkUtils.isNetworkAvailable() } returns false
        coEvery { attendanceDao.insert(any()) } returns Unit

        // When
        val result = attendanceRepository.checkIn(userId, location)

        // Then
        assertTrue(result is Result.Success)
        coVerify { attendanceDao.insert(any()) }
        coVerify(exactly = 0) { attendanceApiService.checkIn(any()) }
    }

    @Test
    fun `checkOut should update existing attendance record`() = runTest {
        // Given
        val attendanceId = "attendance123"
        val location = "Office"
        val existingEntity = createMockAttendanceEntity(id = attendanceId)
        val updatedDto = createMockAttendanceDto(checkOutTime = System.currentTimeMillis())

        every { networkUtils.isNetworkAvailable() } returns true
        coEvery { attendanceDao.getById(attendanceId) } returns existingEntity
        coEvery { attendanceDao.update(any()) } returns Unit
        coEvery { attendanceApiService.checkOut(attendanceId, any()) } returns updatedDto
        coEvery { attendanceDao.updateSyncStatus(any(), any()) } returns Unit

        // When
        val result = attendanceRepository.checkOut(attendanceId, location)

        // Then
        assertTrue(result is Result.Success)
        coVerify { attendanceDao.getById(attendanceId) }
        coVerify { attendanceDao.update(any()) }
        coVerify { attendanceApiService.checkOut(attendanceId, any()) }
    }

    @Test
    fun `getTodayAttendance should return local data`() = runTest {
        // Given
        val userId = "user123"
        val mockEntities = listOf(createMockAttendanceEntity(userId = userId))

        coEvery { attendanceDao.getTodayAttendance(userId) } returns flowOf(mockEntities)

        // When
        attendanceRepository.getTodayAttendance(userId).collect { result ->
            // Then
            assertEquals(1, result.size)
            assertEquals(userId, result[0].userId)
        }

        coVerify { attendanceDao.getTodayAttendance(userId) }
    }

    @Test
    fun `getAttendanceByDateRange should return filtered data`() = runTest {
        // Given
        val userId = "user123"
        val startDate = "2024-01-01"
        val endDate = "2024-01-31"
        val mockEntities = listOf(
            createMockAttendanceEntity(userId = userId, date = "2024-01-15"),
            createMockAttendanceEntity(userId = userId, date = "2024-01-20")
        )

        coEvery { 
            attendanceDao.getAttendanceByDateRange(userId, startDate, endDate) 
        } returns flowOf(mockEntities)

        // When
        attendanceRepository.getAttendanceByDateRange(userId, startDate, endDate).collect { result ->
            // Then
            assertEquals(2, result.size)
            assertTrue(result.all { it.userId == userId })
        }

        coVerify { attendanceDao.getAttendanceByDateRange(userId, startDate, endDate) }
    }

    @Test
    fun `syncPendingData should upload unsynced records`() = runTest {
        // Given
        val unsyncedEntities = listOf(
            createMockAttendanceEntity(syncStatus = com.plp.attendance.data.local.entities.SyncStatus.PENDING),
            createMockAttendanceEntity(syncStatus = com.plp.attendance.data.local.entities.SyncStatus.PENDING)
        )
        val mockDto = createMockAttendanceDto()

        every { networkUtils.isNetworkAvailable() } returns true
        coEvery { attendanceDao.getUnsyncedAttendance() } returns unsyncedEntities
        coEvery { attendanceApiService.syncAttendance(any()) } returns mockDto
        coEvery { attendanceDao.updateSyncStatus(any(), any()) } returns Unit

        // When
        val result = attendanceRepository.syncPendingData()

        // Then
        assertTrue(result is Result.Success)
        coVerify { attendanceDao.getUnsyncedAttendance() }
        coVerify(exactly = 2) { attendanceApiService.syncAttendance(any()) }
        coVerify(exactly = 2) { attendanceDao.updateSyncStatus(any(), any()) }
    }

    @Test
    fun `syncPendingData should handle network errors gracefully`() = runTest {
        // Given
        val unsyncedEntities = listOf(createMockAttendanceEntity())

        every { networkUtils.isNetworkAvailable() } returns true
        coEvery { attendanceDao.getUnsyncedAttendance() } returns unsyncedEntities
        coEvery { attendanceApiService.syncAttendance(any()) } throws Exception("Network error")

        // When
        val result = attendanceRepository.syncPendingData()

        // Then
        assertTrue(result is Result.Error)
        coVerify { attendanceDao.getUnsyncedAttendance() }
        coVerify { attendanceApiService.syncAttendance(any()) }
    }

    @Test
    fun `deleteAttendance should remove record locally and remotely`() = runTest {
        // Given
        val attendanceId = "attendance123"

        every { networkUtils.isNetworkAvailable() } returns true
        coEvery { attendanceDao.deleteById(attendanceId) } returns Unit
        coEvery { attendanceApiService.deleteAttendance(attendanceId) } returns Unit

        // When
        val result = attendanceRepository.deleteAttendance(attendanceId)

        // Then
        assertTrue(result is Result.Success)
        coVerify { attendanceDao.deleteById(attendanceId) }
        coVerify { attendanceApiService.deleteAttendance(attendanceId) }
    }

    @Test
    fun `getAttendanceStatistics should calculate correct metrics`() = runTest {
        // Given
        val userId = "user123"
        val startDate = "2024-01-01"
        val endDate = "2024-01-31"
        val mockEntities = listOf(
            createMockAttendanceEntity(userId = userId, status = "present"),
            createMockAttendanceEntity(userId = userId, status = "present"),
            createMockAttendanceEntity(userId = userId, status = "absent"),
            createMockAttendanceEntity(userId = userId, status = "late")
        )

        coEvery { 
            attendanceDao.getAttendanceByDateRange(userId, startDate, endDate) 
        } returns flowOf(mockEntities)

        // When
        attendanceRepository.getAttendanceStatistics(userId, startDate, endDate).collect { stats ->
            // Then
            assertEquals(4, stats.totalDays)
            assertEquals(2, stats.presentDays)
            assertEquals(1, stats.absentDays)
            assertEquals(1, stats.lateDays)
            assertEquals(50.0, stats.attendanceRate, 0.1)
        }
    }

    private fun createMockAttendanceEntity(
        id: String = "attendance123",
        userId: String = "user123",
        date: String = "2024-01-15",
        checkInTime: Long = System.currentTimeMillis(),
        checkOutTime: Long? = null,
        location: String = "Office",
        status: String = "present",
        syncStatus: com.plp.attendance.data.local.entities.SyncStatus = com.plp.attendance.data.local.entities.SyncStatus.SYNCED
    ) = AttendanceEntity(
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
        gpsLatitude = 11.5564,
        gpsLongitude = 104.9282,
        isManualEntry = false,
        approvedBy = null,
        createdAt = System.currentTimeMillis(),
        lastModified = System.currentTimeMillis(),
        syncStatus = syncStatus
    )

    private fun createMockAttendanceDto(
        id: String = "attendance123",
        userId: String = "user123",
        checkInTime: Long = System.currentTimeMillis(),
        checkOutTime: Long? = null
    ) = AttendanceDto(
        id = id,
        userId = userId,
        date = "2024-01-15",
        checkInTime = checkInTime,
        checkOutTime = checkOutTime,
        location = "Office",
        status = "present",
        workingHours = 0.0,
        overtime = 0.0,
        notes = "",
        ipAddress = "192.168.1.1",
        deviceInfo = "Android Device",
        gpsLatitude = 11.5564,
        gpsLongitude = 104.9282,
        isManualEntry = false,
        approvedBy = null,
        createdAt = System.currentTimeMillis(),
        lastModified = System.currentTimeMillis()
    )
}