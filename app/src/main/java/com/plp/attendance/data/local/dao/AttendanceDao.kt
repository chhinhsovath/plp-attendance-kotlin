package com.plp.attendance.data.local.dao

import androidx.room.*
import com.plp.attendance.data.local.entities.AttendanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance WHERE id = :id")
    suspend fun getAttendanceById(id: String): AttendanceEntity?

    @Query("SELECT * FROM attendance WHERE userId = :userId ORDER BY checkInTime DESC")
    fun getAttendanceByUserId(userId: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE userId = :userId AND checkInTime >= :startTime AND checkInTime < :endTime")
    suspend fun getAttendanceByUserIdAndDateRange(userId: String, startTime: Long, endTime: Long): List<AttendanceEntity>

    @Query("SELECT * FROM attendance WHERE checkInTime >= :startTime AND checkInTime < :endTime ORDER BY checkInTime DESC")
    fun getAttendanceByDateRange(startTime: Long, endTime: Long): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE isSynced = 0")
    suspend fun getUnsyncedAttendance(): List<AttendanceEntity>

    @Query("SELECT * FROM attendance WHERE userId = :userId AND DATE(checkInTime/1000, 'unixepoch') = DATE(:date/1000, 'unixepoch')")
    suspend fun getTodayAttendance(userId: String, date: Long): AttendanceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: AttendanceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendances(attendances: List<AttendanceEntity>)

    @Update
    suspend fun updateAttendance(attendance: AttendanceEntity)

    @Delete
    suspend fun deleteAttendance(attendance: AttendanceEntity)

    @Query("UPDATE attendance SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)

    @Query("UPDATE attendance SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>)
    
    @Query("DELETE FROM attendance")
    suspend fun deleteAll()
}