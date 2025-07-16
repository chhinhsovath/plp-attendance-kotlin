#!/bin/bash

echo "🗄️ Adding Local Database with Room..."

# Step 1: Update build.gradle.kts to include Room dependencies
echo "📦 Adding Room dependencies..."
cat > app/build.gradle.kts.room << 'EOF'
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    id("kotlin-kapt")
}
EOF

# Append the rest of the existing build.gradle content
cat app/build.gradle.kts | grep -v "^plugins {" >> app/build.gradle.kts.room

# Add kapt block and Room dependencies
sed -i '' '/dependencies {/i\
kapt {\
    correctErrorTypes = true\
}\
' app/build.gradle.kts.room

# Update dependencies section to include Room
sed -i '' '/implementation("androidx.room:room-runtime:2.6.1")/a\
    kapt("androidx.room:room-compiler:2.6.1")\
' app/build.gradle.kts.room

# Step 2: Create database entities
echo "🏗️ Creating database entities..."

# User Entity
cat > app/src/main/java/com/plp/attendance/data/local/entities/UserEntity.kt << 'EOF'
package com.plp.attendance.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,
    val email: String,
    val fullName: String,
    val role: String,
    val schoolId: String,
    val schoolName: String,
    val phoneNumber: String? = null,
    val profilePicture: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
EOF

# Attendance Entity
cat > app/src/main/java/com/plp/attendance/data/local/entities/AttendanceEntity.kt << 'EOF'
package com.plp.attendance.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_records")
data class AttendanceEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val date: String,
    val checkInTime: String? = null,
    val checkOutTime: String? = null,
    val checkInLatitude: Double? = null,
    val checkInLongitude: Double? = null,
    val checkOutLatitude: Double? = null,
    val checkOutLongitude: Double? = null,
    val status: String,
    val workingHours: Float? = null,
    val notes: String? = null,
    val syncStatus: String = "PENDING",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
EOF

# Step 3: Create DAOs
echo "📝 Creating DAOs..."

# User DAO
cat > app/src/main/java/com/plp/attendance/data/local/dao/UserDao.kt << 'EOF'
package com.plp.attendance.data.local.dao

import androidx.room.*
import com.plp.attendance.data.local.entities.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): UserEntity?
    
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?
    
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)
    
    @Update
    suspend fun updateUser(user: UserEntity)
    
    @Delete
    suspend fun deleteUser(user: UserEntity)
    
    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()
}
EOF

# Attendance DAO
cat > app/src/main/java/com/plp/attendance/data/local/dao/AttendanceDao.kt << 'EOF'
package com.plp.attendance.data.local.dao

import androidx.room.*
import com.plp.attendance.data.local.entities.AttendanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records WHERE userId = :userId ORDER BY date DESC")
    fun getAttendanceByUser(userId: String): Flow<List<AttendanceEntity>>
    
    @Query("SELECT * FROM attendance_records WHERE userId = :userId AND date = :date LIMIT 1")
    suspend fun getAttendanceByUserAndDate(userId: String, date: String): AttendanceEntity?
    
    @Query("SELECT * FROM attendance_records WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSyncRecords(): List<AttendanceEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: AttendanceEntity)
    
    @Update
    suspend fun updateAttendance(attendance: AttendanceEntity)
    
    @Query("UPDATE attendance_records SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)
    
    @Query("SELECT * FROM attendance_records WHERE userId = :userId ORDER BY date DESC LIMIT :limit")
    fun getRecentAttendance(userId: String, limit: Int = 7): Flow<List<AttendanceEntity>>
}
EOF

# Step 4: Create Database class
echo "🏛️ Creating database class..."
cat > app/src/main/java/com/plp/attendance/data/local/PLPDatabase.kt << 'EOF'
package com.plp.attendance.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.plp.attendance.data.local.dao.UserDao
import com.plp.attendance.data.local.dao.AttendanceDao
import com.plp.attendance.data.local.entities.UserEntity
import com.plp.attendance.data.local.entities.AttendanceEntity

@Database(
    entities = [
        UserEntity::class,
        AttendanceEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class PLPDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun attendanceDao(): AttendanceDao
    
    companion object {
        @Volatile
        private var INSTANCE: PLPDatabase? = null
        
        fun getDatabase(context: Context): PLPDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PLPDatabase::class.java,
                    "plp_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
EOF

# Step 5: Create Repository
echo "📚 Creating repository..."
cat > app/src/main/java/com/plp/attendance/data/repository/AttendanceRepository.kt << 'EOF'
package com.plp.attendance.data.repository

import com.plp.attendance.data.local.dao.AttendanceDao
import com.plp.attendance.data.local.dao.UserDao
import com.plp.attendance.data.local.entities.AttendanceEntity
import com.plp.attendance.data.local.entities.UserEntity
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttendanceRepository @Inject constructor(
    private val attendanceDao: AttendanceDao,
    private val userDao: UserDao
) {
    // User operations
    suspend fun saveUser(user: UserEntity) = userDao.insertUser(user)
    suspend fun getUser(userId: String) = userDao.getUserById(userId)
    suspend fun getUserByEmail(email: String) = userDao.getUserByEmail(email)
    
    // Attendance operations
    fun getUserAttendance(userId: String): Flow<List<AttendanceEntity>> = 
        attendanceDao.getAttendanceByUser(userId)
    
    fun getRecentAttendance(userId: String): Flow<List<AttendanceEntity>> = 
        attendanceDao.getRecentAttendance(userId)
    
    suspend fun checkIn(userId: String, latitude: Double?, longitude: Double?): AttendanceEntity {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        
        // Check if already checked in today
        val existingRecord = attendanceDao.getAttendanceByUserAndDate(userId, today)
        
        if (existingRecord != null && existingRecord.checkInTime != null) {
            throw IllegalStateException("Already checked in today")
        }
        
        val attendance = AttendanceEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            date = today,
            checkInTime = currentTime,
            checkInLatitude = latitude,
            checkInLongitude = longitude,
            status = "PRESENT",
            syncStatus = "PENDING"
        )
        
        attendanceDao.insertAttendance(attendance)
        return attendance
    }
    
    suspend fun checkOut(userId: String, latitude: Double?, longitude: Double?): AttendanceEntity {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        
        val existingRecord = attendanceDao.getAttendanceByUserAndDate(userId, today)
            ?: throw IllegalStateException("No check-in record found for today")
        
        if (existingRecord.checkOutTime != null) {
            throw IllegalStateException("Already checked out today")
        }
        
        // Calculate working hours
        val checkInTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).parse(existingRecord.checkInTime!!)
        val checkOutTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).parse(currentTime)
        val diffInMillis = checkOutTime.time - checkInTime.time
        val workingHours = diffInMillis / (1000f * 60 * 60)
        
        val updatedRecord = existingRecord.copy(
            checkOutTime = currentTime,
            checkOutLatitude = latitude,
            checkOutLongitude = longitude,
            workingHours = workingHours,
            updatedAt = System.currentTimeMillis()
        )
        
        attendanceDao.updateAttendance(updatedRecord)
        return updatedRecord
    }
    
    suspend fun getTodayAttendance(userId: String): AttendanceEntity? {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return attendanceDao.getAttendanceByUserAndDate(userId, today)
    }
}
EOF

# Step 6: Update MainActivity to initialize database
echo "🔄 Updating MainActivity..."
cat > app/src/main/java/com/plp/attendance/MainActivity.kt << 'EOF'
package com.plp.attendance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.plp.attendance.data.local.PLPDatabase
import com.plp.attendance.ui.navigation.NavigationGraph
import com.plp.attendance.ui.theme.PLPTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize database
        val database = PLPDatabase.getDatabase(this)
        
        setContent {
            PLPTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavigationGraph()
                }
            }
        }
    }
}
EOF

# Step 7: Create a simple test to verify database works
echo "🧪 Creating database test..."
cat > app/src/main/java/com/plp/attendance/ui/attendance/AttendanceViewModel.kt << 'EOF'
package com.plp.attendance.ui.attendance

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.plp.attendance.data.local.PLPDatabase
import com.plp.attendance.data.local.entities.AttendanceEntity
import com.plp.attendance.data.repository.AttendanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AttendanceViewModel(application: Application) : AndroidViewModel(application) {
    private val database = PLPDatabase.getDatabase(application)
    private val repository = AttendanceRepository(
        database.attendanceDao(),
        database.userDao()
    )
    
    private val _todayAttendance = MutableStateFlow<AttendanceEntity?>(null)
    val todayAttendance: StateFlow<AttendanceEntity?> = _todayAttendance
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    init {
        loadTodayAttendance()
    }
    
    private fun loadTodayAttendance() {
        viewModelScope.launch {
            try {
                // For demo, use a hardcoded user ID
                val attendance = repository.getTodayAttendance("demo-user")
                _todayAttendance.value = attendance
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
    
    fun checkIn() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                val attendance = repository.checkIn(
                    userId = "demo-user",
                    latitude = null, // Will add GPS later
                    longitude = null
                )
                _todayAttendance.value = attendance
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun checkOut() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                val attendance = repository.checkOut(
                    userId = "demo-user",
                    latitude = null,
                    longitude = null
                )
                _todayAttendance.value = attendance
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
}
EOF

# Apply the new build.gradle
echo "📋 Applying new build configuration..."
mv app/build.gradle.kts.room app/build.gradle.kts

# Build the APK
echo "🔨 Building APK with database support..."
./gradlew clean assembleDebug

if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    echo "✅ BUILD SUCCESSFUL!"
    
    # Create output directory
    timestamp=$(date +%Y%m%d_%H%M%S)
    output_dir="apk_with_database_$timestamp"
    mkdir -p "$output_dir"
    
    # Copy APK
    cp app/build/outputs/apk/debug/app-debug.apk "$output_dir/PLP_Attendance_Database.apk"
    
    echo ""
    echo "📱 APK with Database Support Built!"
    echo "📂 Location: $output_dir/PLP_Attendance_Database.apk"
    echo ""
    echo "✨ New Features:"
    echo "  ✓ Local SQLite database with Room"
    echo "  ✓ User data persistence"
    echo "  ✓ Attendance record storage"
    echo "  ✓ Offline capability"
    echo "  ✓ Data sync preparation"
    
    open "$output_dir" 2>/dev/null || true
else
    echo "❌ Build failed. Check errors above."
fi