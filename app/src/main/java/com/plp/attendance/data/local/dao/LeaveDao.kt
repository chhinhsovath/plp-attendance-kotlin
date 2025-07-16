package com.plp.attendance.data.local.dao

import androidx.room.*
import com.plp.attendance.data.local.entities.LeaveEntity
import com.plp.attendance.domain.model.LeaveStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface LeaveDao {
    @Query("SELECT * FROM leaves WHERE id = :id")
    suspend fun getLeaveById(id: String): LeaveEntity?

    @Query("SELECT * FROM leaves WHERE userId = :userId ORDER BY requestedAt DESC")
    fun getLeavesByUserId(userId: String): Flow<List<LeaveEntity>>

    @Query("SELECT * FROM leaves WHERE status = :status ORDER BY requestedAt DESC")
    fun getLeavesByStatus(status: LeaveStatus): Flow<List<LeaveEntity>>

    @Query("SELECT * FROM leaves WHERE startDate <= :date AND endDate >= :date")
    fun getLeavesByDate(date: Long): Flow<List<LeaveEntity>>

    @Query("SELECT * FROM leaves WHERE isSynced = 0")
    fun getUnsyncedLeaves(): Flow<List<LeaveEntity>>

    @Query("SELECT * FROM leaves WHERE userId = :userId AND status = 'PENDING'")
    fun getPendingLeavesByUserId(userId: String): Flow<List<LeaveEntity>>

    @Query("SELECT * FROM leaves ORDER BY requestedAt DESC")
    fun getAllLeaves(): Flow<List<LeaveEntity>>

    @Query("SELECT * FROM leaves WHERE startDate >= :startDate AND endDate <= :endDate ORDER BY startDate ASC")
    fun getLeavesByDateRange(startDate: Long, endDate: Long): Flow<List<LeaveEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeave(leave: LeaveEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeaves(leaves: List<LeaveEntity>)

    @Update
    suspend fun updateLeave(leave: LeaveEntity)

    @Delete
    suspend fun deleteLeave(leave: LeaveEntity)

    @Query("DELETE FROM leaves WHERE id = :id")
    suspend fun deleteLeaveById(id: String)

    @Query("UPDATE leaves SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)

    @Query("UPDATE leaves SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>)
    
    @Query("DELETE FROM leaves")
    suspend fun deleteAll()
}