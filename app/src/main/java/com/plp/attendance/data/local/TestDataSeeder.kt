package com.plp.attendance.data.local

import com.plp.attendance.data.local.entities.*
import com.plp.attendance.domain.model.UserRole
import com.plp.attendance.domain.model.LeaveType
import com.plp.attendance.domain.model.LeaveStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Test data seeder for local SQLite testing
 * Creates comprehensive test data for all user roles
 */
@Singleton
class TestDataSeeder @Inject constructor(
    private val database: PLPDatabase
) {
    
    companion object {
        // Test account passwords (all use "password123" for testing)
        const val DEFAULT_PASSWORD = "password123"
        
        // Test school/department IDs
        const val MINISTRY_ID = "ministry-001"
        const val ZONE_NORTH_ID = "zone-north-001"
        const val ZONE_SOUTH_ID = "zone-south-001"
        const val PROVINCE_PP_ID = "province-pp-001"
        const val PROVINCE_SR_ID = "province-sr-001"
        const val DEPT_EDU_PP_ID = "dept-edu-pp-001"
        const val DEPT_EDU_SR_ID = "dept-edu-sr-001"
        const val CLUSTER_PP_01_ID = "cluster-pp-01"
        const val CLUSTER_SR_01_ID = "cluster-sr-01"
        const val SCHOOL_PP_001_ID = "school-pp-001"
        const val SCHOOL_PP_002_ID = "school-pp-002"
        const val SCHOOL_SR_001_ID = "school-sr-001"
        const val PLP_OFFICE_ID = "plp-office-001"
    }
    
    private val testUsers = listOf(
        // Administrator (National Level)
        TestUser(
            id = "admin-001",
            email = "admin@plp.gov.kh",
            name = "សុខ សុភា (Sok Sopheap)",
            role = UserRole.ADMINISTRATOR,
            phoneNumber = "+855 12 345 678",
            departmentId = MINISTRY_ID,
            description = "National Administrator - Full system access",
            schoolId = "test-school",
            schoolName = "Test School",
            schoolLatitude = 11.551481374849613,
            schoolLongitude = 104.92816726562374
        ),
        
        // Zone Managers
        TestUser(
            id = "zone-north-001",
            email = "zone.north@plp.gov.kh",
            name = "ចាន់ ដារា (Chan Dara)",
            role = UserRole.ZONE_MANAGER,
            phoneNumber = "+855 12 345 679",
            departmentId = ZONE_NORTH_ID,
            description = "Zone Manager - Northern Provinces",
            schoolId = "test-school",
            schoolName = "Test School",
            schoolLatitude = 11.551481374849613,
            schoolLongitude = 104.92816726562374
        ),
        TestUser(
            id = "zone-south-001",
            email = "zone.south@plp.gov.kh",
            name = "លី សុខា (Ly Sokha)",
            role = UserRole.ZONE_MANAGER,
            phoneNumber = "+855 12 345 680",
            departmentId = ZONE_SOUTH_ID,
            description = "Zone Manager - Southern Provinces",
            schoolId = "test-school",
            schoolName = "Test School",
            schoolLatitude = 11.551481374849613,
            schoolLongitude = 104.92816726562374
        ),
        
        // Provincial Managers
        TestUser(
            id = "prov-pp-001",
            email = "provincial.pp@plp.gov.kh",
            name = "ហេង សំអាត (Heng Samat)",
            role = UserRole.PROVINCIAL_MANAGER,
            phoneNumber = "+855 12 345 681",
            departmentId = PROVINCE_PP_ID,
            description = "Provincial Manager - Phnom Penh",
            schoolId = "test-school",
            schoolName = "Test School",
            schoolLatitude = 11.551481374849613,
            schoolLongitude = 104.92816726562374
        ),
        TestUser(
            id = "prov-sr-001",
            email = "provincial.sr@plp.gov.kh",
            name = "ពៅ វិសាល (Pov Visal)",
            role = UserRole.PROVINCIAL_MANAGER,
            phoneNumber = "+855 12 345 682",
            departmentId = PROVINCE_SR_ID,
            description = "Provincial Manager - Siem Reap",
            schoolId = "test-school",
            schoolName = "Test School",
            schoolLatitude = 11.551481374849613,
            schoolLongitude = 104.92816726562374
        ),
        
        // Department Managers
        TestUser(
            id = "dept-pp-001",
            email = "department.pp@plp.gov.kh",
            name = "មាស សុផល (Meas Sophal)",
            role = UserRole.DEPARTMENT_MANAGER,
            phoneNumber = "+855 12 345 683",
            departmentId = DEPT_EDU_PP_ID,
            description = "Department Manager - Education Phnom Penh",
            schoolId = "test-school",
            schoolName = "Test School",
            schoolLatitude = 11.551481374849613,
            schoolLongitude = 104.92816726562374
        ),
        TestUser(
            id = "dept-sr-001",
            email = "department.sr@plp.gov.kh",
            name = "សុខ ពិសិដ្ឋ (Sok Piseth)",
            role = UserRole.DEPARTMENT_MANAGER,
            phoneNumber = "+855 12 345 684",
            departmentId = DEPT_EDU_SR_ID,
            description = "Department Manager - Education Siem Reap",
            schoolId = "test-school",
            schoolName = "Test School",
            schoolLatitude = 11.551481374849613,
            schoolLongitude = 104.92816726562374
        ),
        
        // Cluster Heads
        TestUser(
            id = "cluster-pp-001",
            email = "cluster.pp01@plp.gov.kh",
            name = "រស់ បុប្ផា (Ros Bopha)",
            role = UserRole.CLUSTER_HEAD,
            phoneNumber = "+855 12 345 685",
            departmentId = CLUSTER_PP_01_ID,
            description = "Cluster Head - Phnom Penh District 1",
            schoolId = "test-school",
            schoolName = "Test School",
            schoolLatitude = 11.551481374849613,
            schoolLongitude = 104.92816726562374
        ),
        TestUser(
            id = "cluster-sr-001",
            email = "cluster.sr01@plp.gov.kh",
            name = "នួន សុខលី (Nuon Sokhly)",
            role = UserRole.CLUSTER_HEAD,
            phoneNumber = "+855 12 345 686",
            departmentId = CLUSTER_SR_01_ID,
            description = "Cluster Head - Siem Reap District 1",
            schoolId = "test-school",
            schoolName = "Test School",
            schoolLatitude = 11.551481374849613,
            schoolLongitude = 104.92816726562374
        ),
        
        // School Directors
        TestUser(
            id = "director-pp-001",
            email = "director.pp001@plp.gov.kh",
            name = "ខៀវ សំណាង (Khiev Samnang)",
            role = UserRole.DIRECTOR,
            phoneNumber = "+855 12 345 687",
            departmentId = SCHOOL_PP_001_ID,
            description = "School Director - PP Primary School 001",
            schoolId = SCHOOL_PP_001_ID,
            schoolName = "សាលាបឋមសិក្សា ភ្នំពេញ ០០១",
            schoolLatitude = 11.551481374849613,
            schoolLongitude = 104.92816726562374
        ),
        TestUser(
            id = "director-pp-002",
            email = "director.pp002@plp.gov.kh",
            name = "ទេព មករា (Tep Makara)",
            role = UserRole.DIRECTOR,
            phoneNumber = "+855 12 345 688",
            departmentId = SCHOOL_PP_002_ID,
            description = "School Director - PP Primary School 002",
            schoolId = SCHOOL_PP_002_ID,
            schoolName = "សាលាបឋមសិក្សា ភ្នំពេញ ០០២",
            schoolLatitude = 11.558765,
            schoolLongitude = 104.935678
        ),
        TestUser(
            id = "director-sr-001",
            email = "director.sr001@plp.gov.kh",
            name = "សាន់ វណ្ណា (San Vanna)",
            role = UserRole.DIRECTOR,
            phoneNumber = "+855 12 345 689",
            departmentId = SCHOOL_SR_001_ID,
            description = "School Director - SR Primary School 001",
            schoolId = SCHOOL_SR_001_ID,
            schoolName = "សាលាបឋមសិក្សា សៀមរាប ០០១",
            schoolLatitude = 13.361134,
            schoolLongitude = 103.859828
        ),
        
        // Teachers
        TestUser(
            id = "teacher-pp-001",
            email = "teacher.pp001@plp.gov.kh",
            name = "លឹម សុភាព (Lim Sopheap)",
            role = UserRole.TEACHER,
            phoneNumber = "+855 12 345 690",
            departmentId = SCHOOL_PP_001_ID,
            description = "Teacher - PP Primary School 001",
            schoolId = SCHOOL_PP_001_ID,
            schoolName = "សាលាបឋមសិក្សា ភ្នំពេញ ០០១",
            schoolLatitude = 11.551481374849613,
            schoolLongitude = 104.92816726562374
        ),
        TestUser(
            id = "teacher-pp-002",
            email = "teacher.pp002@plp.gov.kh",
            name = "ឈួន លីដា (Chhoun Lida)",
            role = UserRole.TEACHER,
            phoneNumber = "+855 12 345 691",
            departmentId = SCHOOL_PP_001_ID,
            description = "Teacher - PP Primary School 001",
            schoolId = SCHOOL_PP_001_ID,
            schoolName = "សាលាបឋមសិក្សា ភ្នំពេញ ០០១",
            schoolLatitude = 11.551481374849613,
            schoolLongitude = 104.92816726562374
        ),
        TestUser(
            id = "teacher-pp-003",
            email = "teacher.pp003@plp.gov.kh",
            name = "គឹម សុខហេង (Kim Sokhheng)",
            role = UserRole.TEACHER,
            phoneNumber = "+855 12 345 692",
            departmentId = SCHOOL_PP_002_ID,
            description = "Teacher - PP Primary School 002"
        ),
        TestUser(
            id = "teacher-sr-001",
            email = "teacher.sr001@plp.gov.kh",
            name = "យិន សុវណ្ណារី (Yin Sovannary)",
            role = UserRole.TEACHER,
            phoneNumber = "+855 12 345 693",
            departmentId = SCHOOL_SR_001_ID,
            description = "Teacher - SR Primary School 001"
        ),
        
        // PLP Office Administrator
        TestUser(
            id = "plp-office-001",
            email = "plp@plp.gov.kh",
            name = "PLP Administrator",
            role = UserRole.ADMINISTRATOR,
            phoneNumber = "+855 12 345 694",
            departmentId = PLP_OFFICE_ID,
            description = "PLP Office Administrator - Central Office",
            schoolId = PLP_OFFICE_ID,
            schoolName = "PLP Office",
            schoolLatitude = 11.55145702067305,
            schoolLongitude = 104.92820924946557
        )
    )
    
    suspend fun seedTestData() = withContext(Dispatchers.IO) {
        try {
            // Clear existing data
            clearAllData()
            
            // Insert test users
            insertTestUsers()
            
            // Create sample attendance records
            createSampleAttendanceRecords()
            
            // Create sample leave requests
            createSampleLeaveRequests()
            
            // Create sample notifications
            createSampleNotifications()
            
            android.util.Log.d("TestDataSeeder", "Test data seeded successfully")
        } catch (e: Exception) {
            android.util.Log.e("TestDataSeeder", "Error seeding test data", e)
            throw e
        }
    }
    
    private suspend fun clearAllData() {
        database.notificationDao().deleteAllNotifications()
        database.syncQueueDao().deleteByStatus(SyncStatus.SUCCESS)
        database.syncQueueDao().deleteByStatus(SyncStatus.FAILED)
        database.syncQueueDao().deleteByStatus(SyncStatus.PENDING)
        database.leaveDao().deleteAll()
        database.attendanceDao().deleteAll()
        database.userDao().deleteAll()
    }
    
    private suspend fun insertTestUsers() {
        val userEntities = testUsers.map { testUser ->
            UserEntity(
                id = testUser.id,
                email = testUser.email,
                name = testUser.name,
                role = testUser.role,
                phoneNumber = testUser.phoneNumber,
                profilePictureUrl = null,
                departmentId = testUser.departmentId,
                schoolId = testUser.schoolId,
                schoolName = testUser.schoolName,
                schoolLatitude = testUser.schoolLatitude,
                schoolLongitude = testUser.schoolLongitude,
                isActive = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        }
        database.userDao().insertUsers(userEntities)
    }
    
    private suspend fun createSampleAttendanceRecords() {
        val attendanceRecords = mutableListOf<AttendanceEntity>()
        val currentTime = System.currentTimeMillis()
        val oneDayMillis = 24 * 60 * 60 * 1000L
        
        // Create attendance for the last 7 days for teachers
        val teachers = testUsers.filter { it.role == UserRole.TEACHER }
        
        for (teacher in teachers) {
            for (day in 0..6) {
                val date = currentTime - (day * oneDayMillis)
                val checkInTime = date - (date % oneDayMillis) + (8 * 60 * 60 * 1000L) // 8:00 AM
                val checkOutTime = checkInTime + (9 * 60 * 60 * 1000L) // 5:00 PM
                
                // Skip weekend (simplified - not accounting for actual day of week)
                if (day % 7 == 0 || day % 7 == 6) continue
                
                attendanceRecords.add(
                    AttendanceEntity(
                        id = UUID.randomUUID().toString(),
                        userId = teacher.id,
                        checkInTime = checkInTime,
                        checkOutTime = if (day == 0) null else checkOutTime, // Today not checked out yet
                        checkInLatitude = 11.551481374849613 + (Math.random() * 0.001), // Real school coordinates
                        checkInLongitude = 104.92816726562374 + (Math.random() * 0.001),
                        checkOutLatitude = if (day == 0) null else 11.551481374849613 + (Math.random() * 0.001),
                        checkOutLongitude = if (day == 0) null else 104.92816726562374 + (Math.random() * 0.001),
                        checkInPhotoUrl = null,
                        checkOutPhotoUrl = null,
                        status = if (day % 3 == 0) "LATE" else "PRESENT",
                        notes = if (day % 3 == 0) "Traffic congestion" else null,
                        isSynced = false,
                        createdAt = checkInTime,
                        updatedAt = if (day == 0) checkInTime else checkOutTime
                    )
                )
            }
        }
        
        database.attendanceDao().insertAttendances(attendanceRecords)
    }
    
    private suspend fun createSampleLeaveRequests() {
        val leaveRequests = listOf(
            // Pending leave request from teacher
            LeaveEntity(
                id = UUID.randomUUID().toString(),
                userId = "teacher-pp-001",
                leaveType = LeaveType.SICK,
                startDate = System.currentTimeMillis() + (2 * 24 * 60 * 60 * 1000L),
                endDate = System.currentTimeMillis() + (3 * 24 * 60 * 60 * 1000L),
                reason = "Medical appointment and recovery",
                status = LeaveStatus.PENDING,
                approverId = null,
                approverComments = null,
                attachmentUrl = null,
                requestedAt = System.currentTimeMillis(),
                approvedAt = null,
                isSynced = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            ),
            
            // Approved leave request
            LeaveEntity(
                id = UUID.randomUUID().toString(),
                userId = "teacher-pp-002",
                leaveType = LeaveType.PERSONAL,
                startDate = System.currentTimeMillis() - (5 * 24 * 60 * 60 * 1000L),
                endDate = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000L),
                reason = "Family emergency",
                status = LeaveStatus.APPROVED,
                approverId = "director-pp-001",
                approverComments = "Approved. Please ensure your classes are covered.",
                attachmentUrl = null,
                requestedAt = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L),
                approvedAt = System.currentTimeMillis() - (6 * 24 * 60 * 60 * 1000L),
                isSynced = false,
                createdAt = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L),
                updatedAt = System.currentTimeMillis() - (6 * 24 * 60 * 60 * 1000L)
            ),
            
            // Rejected leave request
            LeaveEntity(
                id = UUID.randomUUID().toString(),
                userId = "teacher-sr-001",
                leaveType = LeaveType.VACATION,
                startDate = System.currentTimeMillis() + (10 * 24 * 60 * 60 * 1000L),
                endDate = System.currentTimeMillis() + (15 * 24 * 60 * 60 * 1000L),
                reason = "Personal vacation",
                status = LeaveStatus.REJECTED,
                approverId = "director-sr-001",
                approverComments = "Cannot approve during examination period.",
                attachmentUrl = null,
                requestedAt = System.currentTimeMillis() - (2 * 24 * 60 * 60 * 1000L),
                approvedAt = System.currentTimeMillis() - (1 * 24 * 60 * 60 * 1000L),
                isSynced = false,
                createdAt = System.currentTimeMillis() - (2 * 24 * 60 * 60 * 1000L),
                updatedAt = System.currentTimeMillis() - (1 * 24 * 60 * 60 * 1000L)
            )
        )
        
        database.leaveDao().insertLeaves(leaveRequests)
    }
    
    private suspend fun createSampleNotifications() {
        val notifications = listOf(
            // Attendance reminder
            NotificationEntity(
                type = NotificationType.ATTENDANCE,
                title = "Morning Check-in Reminder",
                message = "Don't forget to mark your attendance",
                priority = NotificationPriority.HIGH,
                isRead = false,
                actionData = null,
                createdAt = System.currentTimeMillis() - (2 * 60 * 60 * 1000L), // 2 hours ago
                readAt = null,
                scheduledFor = null,
                expiresAt = null
            ),
            
            // Leave approval notification
            NotificationEntity(
                type = NotificationType.APPROVAL,
                title = "Leave Request Pending",
                message = "You have 1 pending leave request to review",
                priority = NotificationPriority.HIGH,
                isRead = false,
                actionData = """{"requestId": "leave-001", "type": "leave"}""",
                createdAt = System.currentTimeMillis() - (1 * 60 * 60 * 1000L), // 1 hour ago
                readAt = null,
                scheduledFor = null,
                expiresAt = null
            ),
            
            // System notification
            NotificationEntity(
                type = NotificationType.SYSTEM,
                title = "System Maintenance",
                message = "Scheduled maintenance on Sunday 2:00 AM - 4:00 AM",
                priority = NotificationPriority.NORMAL,
                isRead = true,
                actionData = null,
                createdAt = System.currentTimeMillis() - (24 * 60 * 60 * 1000L), // Yesterday
                readAt = System.currentTimeMillis() - (20 * 60 * 60 * 1000L),
                scheduledFor = null,
                expiresAt = null
            )
        )
        
        database.notificationDao().insertAll(notifications)
    }
    
    fun getTestAccounts(): List<TestAccount> {
        android.util.Log.d("TestDataSeeder", "Getting test accounts, total: ${testUsers.size}")
        return testUsers.map { user ->
            TestAccount(
                email = user.email,
                password = DEFAULT_PASSWORD,
                role = user.role.name,
                name = user.name,
                description = user.description
            )
        }
    }
    
    suspend fun resetUserPassword(email: String, newPassword: String = DEFAULT_PASSWORD) {
        // In a real app, this would hash the password
        // For testing, we're just logging it
        android.util.Log.d("TestDataSeeder", "Password reset for $email to $newPassword")
    }
    
    suspend fun hasTestData(): Boolean {
        return try {
            val userCount = database.userDao().getUserCount()
            android.util.Log.d("TestDataSeeder", "Current user count in database: $userCount")
            userCount > 0
        } catch (e: Exception) {
            android.util.Log.e("TestDataSeeder", "Error checking test data", e)
            false
        }
    }
}

data class TestUser(
    val id: String,
    val email: String,
    val name: String,
    val role: UserRole,
    val phoneNumber: String,
    val departmentId: String,
    val description: String,
    val schoolId: String? = null,
    val schoolName: String? = null,
    val schoolLatitude: Double? = null,
    val schoolLongitude: Double? = null
)

data class TestAccount(
    val email: String,
    val password: String,
    val role: String,
    val name: String,
    val description: String
)