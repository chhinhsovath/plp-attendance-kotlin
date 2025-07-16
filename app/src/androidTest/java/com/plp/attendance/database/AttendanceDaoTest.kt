package com.plp.attendance.database

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.plp.attendance.data.local.database.AttendanceDatabase
import com.plp.attendance.data.local.dao.AttendanceDao
import com.plp.attendance.data.local.entities.AttendanceEntity
import com.plp.attendance.data.local.entities.SyncStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class AttendanceDaoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: AttendanceDatabase
    private lateinit var attendanceDao: AttendanceDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AttendanceDatabase::class.java
        ).allowMainThreadQueries().build()
        
        attendanceDao = database.attendanceDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndGetAttendance() = runTest {
        // Given
        val attendance = createTestAttendanceEntity()

        // When
        attendanceDao.insert(attendance)
        val retrievedAttendance = attendanceDao.getById(attendance.id)

        // Then
        assertNotNull(retrievedAttendance)
        assertEquals(attendance.id, retrievedAttendance?.id)
        assertEquals(attendance.userId, retrievedAttendance?.userId)
        assertEquals(attendance.location, retrievedAttendance?.location)
    }

    @Test
    fun insertMultipleAttendanceRecords() = runTest {
        // Given
        val attendanceList = listOf(
            createTestAttendanceEntity(id = "1", userId = "user1"),
            createTestAttendanceEntity(id = "2", userId = "user2"),
            createTestAttendanceEntity(id = "3", userId = "user1")
        )

        // When
        attendanceDao.insertAll(attendanceList)
        val allAttendance = attendanceDao.getAllAttendance().first()

        // Then
        assertEquals(3, allAttendance.size)
        assertTrue(allAttendance.any { it.id == "1" })
        assertTrue(allAttendance.any { it.id == "2" })
        assertTrue(allAttendance.any { it.id == "3" })
    }

    @Test
    fun updateAttendance() = runTest {
        // Given
        val attendance = createTestAttendanceEntity()
        attendanceDao.insert(attendance)

        // When
        val updatedAttendance = attendance.copy(
            location = "Updated Location",
            checkOutTime = System.currentTimeMillis()
        )
        attendanceDao.update(updatedAttendance)
        val retrieved = attendanceDao.getById(attendance.id)

        // Then
        assertNotNull(retrieved)
        assertEquals("Updated Location", retrieved?.location)
        assertNotNull(retrieved?.checkOutTime)
    }

    @Test
    fun deleteAttendance() = runTest {
        // Given
        val attendance = createTestAttendanceEntity()
        attendanceDao.insert(attendance)

        // When
        attendanceDao.deleteById(attendance.id)
        val retrieved = attendanceDao.getById(attendance.id)

        // Then
        assertNull(retrieved)
    }

    @Test
    fun getAttendanceByUserId() = runTest {
        // Given
        val userId = "testUser"
        val userAttendance = listOf(
            createTestAttendanceEntity(id = "1", userId = userId),
            createTestAttendanceEntity(id = "2", userId = userId)
        )
        val otherUserAttendance = createTestAttendanceEntity(id = "3", userId = "otherUser")

        // When
        attendanceDao.insertAll(userAttendance + otherUserAttendance)
        val result = attendanceDao.getAttendanceByUserId(userId).first()

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.userId == userId })
    }

    @Test
    fun getTodayAttendance() = runTest {
        // Given
        val userId = "testUser"
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        val yesterday = "2023-12-31"

        val todayAttendance = createTestAttendanceEntity(id = "today", userId = userId, date = today)
        val yesterdayAttendance = createTestAttendanceEntity(id = "yesterday", userId = userId, date = yesterday)

        // When
        attendanceDao.insertAll(listOf(todayAttendance, yesterdayAttendance))
        val result = attendanceDao.getTodayAttendance(userId).first()

        // Then
        assertEquals(1, result.size)
        assertEquals("today", result[0].id)
        assertEquals(today, result[0].date)
    }

    @Test
    fun getAttendanceByDateRange() = runTest {
        // Given
        val userId = "testUser"
        val attendanceList = listOf(
            createTestAttendanceEntity(id = "1", userId = userId, date = "2024-01-15"),
            createTestAttendanceEntity(id = "2", userId = userId, date = "2024-01-20"),
            createTestAttendanceEntity(id = "3", userId = userId, date = "2024-02-01")
        )

        // When
        attendanceDao.insertAll(attendanceList)
        val result = attendanceDao.getAttendanceByDateRange(
            userId = userId,
            startDate = "2024-01-01",
            endDate = "2024-01-31"
        ).first()

        // Then
        assertEquals(2, result.size)
        assertTrue(result.any { it.id == "1" })
        assertTrue(result.any { it.id == "2" })
        assertFalse(result.any { it.id == "3" })
    }

    @Test
    fun getUnsyncedAttendance() = runTest {
        // Given
        val syncedAttendance = createTestAttendanceEntity(id = "1", syncStatus = SyncStatus.SYNCED)
        val pendingAttendance = createTestAttendanceEntity(id = "2", syncStatus = SyncStatus.PENDING)
        val failedAttendance = createTestAttendanceEntity(id = "3", syncStatus = SyncStatus.FAILED)

        // When
        attendanceDao.insertAll(listOf(syncedAttendance, pendingAttendance, failedAttendance))
        val result = attendanceDao.getUnsyncedAttendance()

        // Then
        assertEquals(2, result.size)
        assertTrue(result.any { it.id == "2" && it.syncStatus == SyncStatus.PENDING })
        assertTrue(result.any { it.id == "3" && it.syncStatus == SyncStatus.FAILED })
        assertFalse(result.any { it.syncStatus == SyncStatus.SYNCED })
    }

    @Test
    fun updateSyncStatus() = runTest {
        // Given
        val attendance = createTestAttendanceEntity(syncStatus = SyncStatus.PENDING)
        attendanceDao.insert(attendance)

        // When
        attendanceDao.updateSyncStatus(attendance.id, SyncStatus.SYNCED)
        val updated = attendanceDao.getById(attendance.id)

        // Then
        assertNotNull(updated)
        assertEquals(SyncStatus.SYNCED, updated?.syncStatus)
    }

    @Test
    fun getAttendanceByStatus() = runTest {
        // Given
        val attendanceList = listOf(
            createTestAttendanceEntity(id = "1", status = "present"),
            createTestAttendanceEntity(id = "2", status = "absent"),
            createTestAttendanceEntity(id = "3", status = "present")
        )

        // When
        attendanceDao.insertAll(attendanceList)
        val presentAttendance = attendanceDao.getAttendanceByStatus("present").first()

        // Then
        assertEquals(2, presentAttendance.size)
        assertTrue(presentAttendance.all { it.status == "present" })
    }

    @Test
    fun getAttendanceCount() = runTest {
        // Given
        val userId = "testUser"
        val attendanceList = listOf(
            createTestAttendanceEntity(id = "1", userId = userId),
            createTestAttendanceEntity(id = "2", userId = userId),
            createTestAttendanceEntity(id = "3", userId = "otherUser")
        )

        // When
        attendanceDao.insertAll(attendanceList)
        val count = attendanceDao.getAttendanceCount(userId)

        // Then
        assertEquals(2, count)
    }

    @Test
    fun deleteAllAttendance() = runTest {
        // Given
        val attendanceList = listOf(
            createTestAttendanceEntity(id = "1"),
            createTestAttendanceEntity(id = "2"),
            createTestAttendanceEntity(id = "3")
        )

        // When
        attendanceDao.insertAll(attendanceList)
        attendanceDao.deleteAll()
        val allAttendance = attendanceDao.getAllAttendance().first()

        // Then
        assertTrue(allAttendance.isEmpty())
    }

    @Test
    fun getAttendanceWithPagination() = runTest {
        // Given
        val userId = "testUser"
        val attendanceList = (1..20).map { 
            createTestAttendanceEntity(id = it.toString(), userId = userId)
        }

        // When
        attendanceDao.insertAll(attendanceList)
        val firstPage = attendanceDao.getAttendanceWithPagination(userId, limit = 10, offset = 0).first()
        val secondPage = attendanceDao.getAttendanceWithPagination(userId, limit = 10, offset = 10).first()

        // Then
        assertEquals(10, firstPage.size)
        assertEquals(10, secondPage.size)
        // Verify no overlap between pages
        val firstPageIds = firstPage.map { it.id }.toSet()
        val secondPageIds = secondPage.map { it.id }.toSet()
        assertTrue(firstPageIds.intersect(secondPageIds).isEmpty())
    }

    @Test
    fun searchAttendanceByLocation() = runTest {
        // Given
        val attendanceList = listOf(
            createTestAttendanceEntity(id = "1", location = "Main Office"),
            createTestAttendanceEntity(id = "2", location = "Branch Office"),
            createTestAttendanceEntity(id = "3", location = "Home Office")
        )

        // When
        attendanceDao.insertAll(attendanceList)
        val result = attendanceDao.searchAttendanceByLocation("Office").first()

        // Then
        assertEquals(3, result.size)
        assertTrue(result.all { it.location.contains("Office") })
    }

    @Test
    fun getAttendanceStatistics() = runTest {
        // Given
        val userId = "testUser"
        val startDate = "2024-01-01"
        val endDate = "2024-01-31"
        val attendanceList = listOf(
            createTestAttendanceEntity(id = "1", userId = userId, date = "2024-01-15", status = "present"),
            createTestAttendanceEntity(id = "2", userId = userId, date = "2024-01-16", status = "absent"),
            createTestAttendanceEntity(id = "3", userId = userId, date = "2024-01-17", status = "present"),
            createTestAttendanceEntity(id = "4", userId = userId, date = "2024-01-18", status = "late")
        )

        // When
        attendanceDao.insertAll(attendanceList)
        val stats = attendanceDao.getAttendanceStatistics(userId, startDate, endDate)

        // Then
        assertEquals(4, stats.totalDays)
        assertEquals(2, stats.presentDays)
        assertEquals(1, stats.absentDays)
        assertEquals(1, stats.lateDays)
    }

    private fun createTestAttendanceEntity(
        id: String = "test_attendance_id",
        userId: String = "test_user_id",
        date: String = "2024-01-15",
        checkInTime: Long = System.currentTimeMillis(),
        checkOutTime: Long? = null,
        location: String = "Test Location",
        status: String = "present",
        syncStatus: SyncStatus = SyncStatus.SYNCED
    ) = AttendanceEntity(
        id = id,
        userId = userId,
        date = date,
        checkInTime = checkInTime,
        checkOutTime = checkOutTime,
        location = location,
        status = status,
        workingHours = 8.0,
        overtime = 0.0,
        notes = "Test notes",
        ipAddress = "192.168.1.1",
        deviceInfo = "Test Device",
        gpsLatitude = 11.5564,
        gpsLongitude = 104.9282,
        isManualEntry = false,
        approvedBy = null,
        createdAt = System.currentTimeMillis(),
        lastModified = System.currentTimeMillis(),
        syncStatus = syncStatus
    )
}