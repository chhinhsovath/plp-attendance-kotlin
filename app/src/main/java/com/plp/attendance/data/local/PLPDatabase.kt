package com.plp.attendance.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.plp.attendance.data.local.dao.AttendanceDao
import com.plp.attendance.data.local.dao.LeaveDao
import com.plp.attendance.data.local.dao.UserDao
import com.plp.attendance.data.local.dao.SyncQueueDao
import com.plp.attendance.data.local.dao.NotificationDao
import com.plp.attendance.data.local.entities.AttendanceEntity
import com.plp.attendance.data.local.entities.LeaveEntity
import com.plp.attendance.data.local.entities.UserEntity
import com.plp.attendance.data.local.entities.SyncQueueEntity
import com.plp.attendance.data.local.entities.NotificationEntity
import com.plp.attendance.data.local.converters.UserRoleConverter
import com.plp.attendance.data.local.converters.LeaveTypeConverter
import com.plp.attendance.data.local.converters.SyncOperationConverter
import com.plp.attendance.data.local.converters.SyncStatusConverter
import com.plp.attendance.data.local.converters.NotificationTypeConverter
import com.plp.attendance.data.local.converters.NotificationPriorityConverter

@Database(
    entities = [
        UserEntity::class,
        AttendanceEntity::class,
        LeaveEntity::class,
        SyncQueueEntity::class,
        NotificationEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(
    UserRoleConverter::class, 
    LeaveTypeConverter::class,
    SyncOperationConverter::class,
    SyncStatusConverter::class,
    NotificationTypeConverter::class,
    NotificationPriorityConverter::class
)
abstract class PLPDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun leaveDao(): LeaveDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        const val DATABASE_NAME = "plp_database"
        
        @Volatile
        private var INSTANCE: PLPDatabase? = null

        fun getDatabase(context: Context): PLPDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PLPDatabase::class.java,
                    DATABASE_NAME
                ).build()
                INSTANCE = instance
                instance
            }
        }
        
        fun getInstance(context: Context): PLPDatabase {
            return getDatabase(context)
        }
    }
}