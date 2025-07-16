package com.plp.attendance.data.local

import com.plp.attendance.data.local.entities.*
import com.plp.attendance.domain.model.UserRole
import com.plp.attendance.domain.model.LeaveType
import com.plp.attendance.domain.model.LeaveStatus
import com.plp.attendance.data.local.entities.NotificationType
import com.plp.attendance.data.local.entities.NotificationPriority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enhanced Test Data Seeder with complete organizational hierarchy
 * and proper GPS coordinates for Cambodian schools
 */
@Singleton
class TestDataSeederV2 @Inject constructor(
    private val database: PLPDatabase
) {
    
    companion object {
        // Default password for all test accounts
        const val DEFAULT_PASSWORD = "password123"
        
        // Organization IDs - National Level
        const val MINISTRY_ID = "ministry-education"
        
        // Zone IDs
        const val ZONE_CENTRAL_ID = "zone-central"
        const val ZONE_NORTHWEST_ID = "zone-northwest"
        
        // Province IDs
        const val PROVINCE_PHNOM_PENH_ID = "province-phnom-penh"
        const val PROVINCE_KANDAL_ID = "province-kandal"
        const val PROVINCE_SIEM_REAP_ID = "province-siem-reap"
        const val PROVINCE_BATTAMBANG_ID = "province-battambang"
        
        // Department/District IDs
        const val DEPT_CHAMKAR_MON_ID = "dept-chamkar-mon"
        const val DEPT_DAUN_PENH_ID = "dept-daun-penh"
        const val DEPT_TAKHMAU_ID = "dept-takhmau"
        const val DEPT_SIEM_REAP_CITY_ID = "dept-siem-reap-city"
        
        // Cluster IDs
        const val CLUSTER_CHAMKAR_MON_1_ID = "cluster-chamkar-mon-1"
        const val CLUSTER_CHAMKAR_MON_2_ID = "cluster-chamkar-mon-2"
        const val CLUSTER_DAUN_PENH_1_ID = "cluster-daun-penh-1"
        const val CLUSTER_TAKHMAU_1_ID = "cluster-takhmau-1"
        const val CLUSTER_SIEM_REAP_1_ID = "cluster-siem-reap-1"
        const val CLUSTER_SIEM_REAP_2_ID = "cluster-siem-reap-2"
        
        // School IDs with proper naming
        const val SCHOOL_HUN_SEN_CHAMKAR_MON_ID = "school-hun-sen-chamkar-mon"
        const val SCHOOL_PREAH_SISOWATH_ID = "school-preah-sisowath"
        const val SCHOOL_CHEA_SIM_TAKHMAU_ID = "school-chea-sim-takhmau"
        const val SCHOOL_ANGKOR_HIGH_ID = "school-angkor-high"
        const val SCHOOL_WAT_BO_PRIMARY_ID = "school-wat-bo-primary"
        const val SCHOOL_BAKONG_SECONDARY_ID = "school-bakong-secondary"
    }
    
    // School data with actual GPS coordinates
    private val schoolsData = mapOf(
        // Phnom Penh Schools
        SCHOOL_HUN_SEN_CHAMKAR_MON_ID to SchoolInfo(
            name = "វិទ្យាល័យ ហ៊ុន សែន ចំការមន (Hun Sen Chamkar Mon High School)",
            address = "St. 114, Chamkar Mon, Phnom Penh",
            latitude = 11.5449,
            longitude = 104.9160
        ),
        SCHOOL_PREAH_SISOWATH_ID to SchoolInfo(
            name = "វិទ្យាល័យ ព្រះស៊ីសុវត្ថិ (Preah Sisowath High School)",
            address = "St. 106, Daun Penh, Phnom Penh",
            latitude = 11.5667,
            longitude = 104.9283
        ),
        
        // Kandal Schools
        SCHOOL_CHEA_SIM_TAKHMAU_ID to SchoolInfo(
            name = "វិទ្យាល័យ ជា ស៊ីម តាខ្មៅ (Chea Sim Takhmau High School)",
            address = "National Road 2, Takhmau, Kandal",
            latitude = 11.4833,
            longitude = 104.9500
        ),
        
        // Siem Reap Schools
        SCHOOL_ANGKOR_HIGH_ID to SchoolInfo(
            name = "វិទ្យាល័យ អង្គរ (Angkor High School)",
            address = "Charles de Gaulle Ave, Siem Reap",
            latitude = 13.3633,
            longitude = 103.8564
        ),
        SCHOOL_WAT_BO_PRIMARY_ID to SchoolInfo(
            name = "បឋមសិក្សា វត្តបូ (Wat Bo Primary School)",
            address = "Wat Bo Village, Siem Reap",
            latitude = 13.3544,
            longitude = 103.8589
        ),
        SCHOOL_BAKONG_SECONDARY_ID to SchoolInfo(
            name = "អនុវិទ្យាល័យ បាគង (Bakong Secondary School)",
            address = "Bakong District, Siem Reap",
            latitude = 13.3456,
            longitude = 103.9742
        )
    )
    
    private val testUsers = listOf(
        // === ADMINISTRATORS (2) - National Level ===
        TestUser(
            id = "admin-001",
            email = "admin.national@moeyes.gov.kh",
            name = "ឯកឧត្តម សុខ សុភា (H.E. Sok Sophea)",
            role = UserRole.ADMINISTRATOR,
            phoneNumber = "+855 12 345 001",
            departmentId = MINISTRY_ID,
            description = "National Administrator - Ministry of Education",
            schoolId = null,
            schoolName = null,
            schoolLatitude = null,
            schoolLongitude = null
        ),
        TestUser(
            id = "admin-002",
            email = "admin.deputy@moeyes.gov.kh",
            name = "លោកជំទាវ ចាន់ ចន្ទ្រា (Lok Chumteav Chan Chantra)",
            role = UserRole.ADMINISTRATOR,
            phoneNumber = "+855 12 345 002",
            departmentId = MINISTRY_ID,
            description = "Deputy National Administrator - Ministry of Education",
            schoolId = null,
            schoolName = null,
            schoolLatitude = null,
            schoolLongitude = null
        ),
        
        // === ZONE MANAGERS (2) - Multi-Province Level ===
        TestUser(
            id = "zone-001",
            email = "zone.central@moeyes.gov.kh",
            name = "លោក ហេង សំរិន (Mr. Heng Samrin)",
            role = UserRole.ZONE_MANAGER,
            phoneNumber = "+855 12 345 101",
            departmentId = ZONE_CENTRAL_ID,
            description = "Zone Manager - Central Cambodia (PP, Kandal, Kampong Speu)",
            schoolId = null,
            schoolName = null,
            schoolLatitude = null,
            schoolLongitude = null
        ),
        TestUser(
            id = "zone-002",
            email = "zone.northwest@moeyes.gov.kh",
            name = "លោក ប៊ុន រ៉ានី (Mr. Bun Rany)",
            role = UserRole.ZONE_MANAGER,
            phoneNumber = "+855 12 345 102",
            departmentId = ZONE_NORTHWEST_ID,
            description = "Zone Manager - Northwest Cambodia (Siem Reap, Battambang, Banteay Meanchey)",
            schoolId = null,
            schoolName = null,
            schoolLatitude = null,
            schoolLongitude = null
        ),
        
        // === PROVINCIAL MANAGERS (2) - Province Level ===
        TestUser(
            id = "provincial-001",
            email = "provincial.pp@moeyes.gov.kh",
            name = "លោក កែវ សុវណ្ណារ៉ា (Mr. Keo Sovannara)",
            role = UserRole.PROVINCIAL_MANAGER,
            phoneNumber = "+855 12 345 201",
            departmentId = PROVINCE_PHNOM_PENH_ID,
            description = "Provincial Manager - Phnom Penh",
            schoolId = null,
            schoolName = null,
            schoolLatitude = null,
            schoolLongitude = null
        ),
        TestUser(
            id = "provincial-002",
            email = "provincial.sr@moeyes.gov.kh",
            name = "លោកស្រី ម៉ៅ សុភាព (Ms. Mao Sopheap)",
            role = UserRole.PROVINCIAL_MANAGER,
            phoneNumber = "+855 12 345 202",
            departmentId = PROVINCE_SIEM_REAP_ID,
            description = "Provincial Manager - Siem Reap Province",
            schoolId = null,
            schoolName = null,
            schoolLatitude = null,
            schoolLongitude = null
        ),
        
        // === DEPARTMENT MANAGERS (2) - District Level ===
        TestUser(
            id = "dept-001",
            email = "dept.chamkarmon@moeyes.gov.kh",
            name = "លោក ពៅ វុទ្ធី (Mr. Pov Vuthy)",
            role = UserRole.DEPARTMENT_MANAGER,
            phoneNumber = "+855 12 345 301",
            departmentId = DEPT_CHAMKAR_MON_ID,
            description = "Department Manager - Chamkar Mon District",
            schoolId = null,
            schoolName = null,
            schoolLatitude = null,
            schoolLongitude = null
        ),
        TestUser(
            id = "dept-002",
            email = "dept.siemreap@moeyes.gov.kh",
            name = "លោកស្រី សៀង លីណា (Ms. Seang Lina)",
            role = UserRole.DEPARTMENT_MANAGER,
            phoneNumber = "+855 12 345 302",
            departmentId = DEPT_SIEM_REAP_CITY_ID,
            description = "Department Manager - Siem Reap City",
            schoolId = null,
            schoolName = null,
            schoolLatitude = null,
            schoolLongitude = null
        ),
        
        // === CLUSTER HEADS (2) - Multi-School Level ===
        TestUser(
            id = "cluster-001",
            email = "cluster.chamkarmon1@moeyes.gov.kh",
            name = "លោក រស់ បុប្ផា (Mr. Ros Bopha)",
            role = UserRole.CLUSTER_HEAD,
            phoneNumber = "+855 12 345 401",
            departmentId = CLUSTER_CHAMKAR_MON_1_ID,
            description = "Cluster Head - Chamkar Mon Zone 1",
            schoolId = null,
            schoolName = null,
            schoolLatitude = null,
            schoolLongitude = null
        ),
        TestUser(
            id = "cluster-002",
            email = "cluster.siemreap1@moeyes.gov.kh",
            name = "លោកស្រី នួន សុខលី (Ms. Nuon Sokhly)",
            role = UserRole.CLUSTER_HEAD,
            phoneNumber = "+855 12 345 402",
            departmentId = CLUSTER_SIEM_REAP_1_ID,
            description = "Cluster Head - Siem Reap City Zone 1",
            schoolId = null,
            schoolName = null,
            schoolLatitude = null,
            schoolLongitude = null
        ),
        
        // === DIRECTORS (2) - School Level ===
        TestUser(
            id = "director-001",
            email = "director.hunsen@moeyes.gov.kh",
            name = "លោក ខៀវ សំណាង (Mr. Khiev Samnang)",
            role = UserRole.DIRECTOR,
            phoneNumber = "+855 12 345 501",
            departmentId = SCHOOL_HUN_SEN_CHAMKAR_MON_ID,
            description = "School Director - Hun Sen Chamkar Mon High School",
            schoolId = SCHOOL_HUN_SEN_CHAMKAR_MON_ID,
            schoolName = schoolsData[SCHOOL_HUN_SEN_CHAMKAR_MON_ID]?.name,
            schoolLatitude = schoolsData[SCHOOL_HUN_SEN_CHAMKAR_MON_ID]?.latitude,
            schoolLongitude = schoolsData[SCHOOL_HUN_SEN_CHAMKAR_MON_ID]?.longitude
        ),
        TestUser(
            id = "director-002",
            email = "director.angkor@moeyes.gov.kh",
            name = "លោកស្រី សាន់ វណ្ណា (Ms. San Vanna)",
            role = UserRole.DIRECTOR,
            phoneNumber = "+855 12 345 502",
            departmentId = SCHOOL_ANGKOR_HIGH_ID,
            description = "School Director - Angkor High School",
            schoolId = SCHOOL_ANGKOR_HIGH_ID,
            schoolName = schoolsData[SCHOOL_ANGKOR_HIGH_ID]?.name,
            schoolLatitude = schoolsData[SCHOOL_ANGKOR_HIGH_ID]?.latitude,
            schoolLongitude = schoolsData[SCHOOL_ANGKOR_HIGH_ID]?.longitude
        ),
        
        // === TEACHERS (2) - Classroom Level ===
        TestUser(
            id = "teacher-001",
            email = "teacher.math.hunsen@moeyes.gov.kh",
            name = "លោក លឹម សុភាព (Mr. Lim Sopheap)",
            role = UserRole.TEACHER,
            phoneNumber = "+855 12 345 601",
            departmentId = SCHOOL_HUN_SEN_CHAMKAR_MON_ID,
            description = "Mathematics Teacher - Hun Sen Chamkar Mon High School",
            schoolId = SCHOOL_HUN_SEN_CHAMKAR_MON_ID,
            schoolName = schoolsData[SCHOOL_HUN_SEN_CHAMKAR_MON_ID]?.name,
            schoolLatitude = schoolsData[SCHOOL_HUN_SEN_CHAMKAR_MON_ID]?.latitude,
            schoolLongitude = schoolsData[SCHOOL_HUN_SEN_CHAMKAR_MON_ID]?.longitude
        ),
        TestUser(
            id = "teacher-002",
            email = "teacher.khmer.angkor@moeyes.gov.kh",
            name = "លោកស្រី យិន សុវណ្ណារី (Ms. Yin Sovannary)",
            role = UserRole.TEACHER,
            phoneNumber = "+855 12 345 602",
            departmentId = SCHOOL_ANGKOR_HIGH_ID,
            description = "Khmer Literature Teacher - Angkor High School",
            schoolId = SCHOOL_ANGKOR_HIGH_ID,
            schoolName = schoolsData[SCHOOL_ANGKOR_HIGH_ID]?.name,
            schoolLatitude = schoolsData[SCHOOL_ANGKOR_HIGH_ID]?.latitude,
            schoolLongitude = schoolsData[SCHOOL_ANGKOR_HIGH_ID]?.longitude
        )
    )
    
    /**
     * Main entry point to seed test data
     */
    suspend fun seedTestData() = withContext(Dispatchers.IO) {
        try {
            // Clear existing data
            clearAllData()
            
            // Insert users
            insertTestUsers()
            
            // Create sample attendance records
            createSampleAttendanceRecords()
            
            // Create sample leave requests
            createSampleLeaveRequests()
            
            // Create sample notifications
            createSampleNotifications()
            
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
    
    /**
     * Clear all existing data
     */
    private suspend fun clearAllData() {
        database.notificationDao().deleteAllNotifications()
        database.syncQueueDao().deleteAll()
        database.leaveDao().deleteAll()
        database.attendanceDao().deleteAll()
        database.userDao().deleteAll()
    }
    
    /**
     * Insert test users
     */
    private suspend fun insertTestUsers() {
        val userEntities = testUsers.map { testUser ->
            UserEntity(
                id = testUser.id,
                name = testUser.name,
                email = testUser.email,
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
    
    /**
     * Create sample attendance records for teachers
     */
    private suspend fun createSampleAttendanceRecords() {
        val teachers = testUsers.filter { it.role == UserRole.TEACHER }
        val currentTime = System.currentTimeMillis()
        val oneDayInMillis = 24 * 60 * 60 * 1000L
        
        teachers.forEach { teacher ->
            // Create attendance for last 7 days
            for (i in 0..6) {
                val date = currentTime - (i * oneDayInMillis)
                val calendar = java.util.Calendar.getInstance().apply {
                    timeInMillis = date
                }
                
                // Skip weekends
                if (calendar.get(java.util.Calendar.DAY_OF_WEEK) in listOf(
                    java.util.Calendar.SATURDAY,
                    java.util.Calendar.SUNDAY
                )) {
                    continue
                }
                
                // Morning check-in (7:30 AM + random minutes)
                val checkInTime = calendar.apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 7)
                    set(java.util.Calendar.MINUTE, 30 + (0..15).random())
                    set(java.util.Calendar.SECOND, 0)
                }.timeInMillis
                
                // Afternoon check-out (4:30 PM + random minutes)
                val checkOutTime = calendar.apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 16)
                    set(java.util.Calendar.MINUTE, 30 + (0..30).random())
                }.timeInMillis
                
                val attendance = AttendanceEntity(
                    id = UUID.randomUUID().toString(),
                    userId = teacher.id,
                    checkInTime = checkInTime,
                    checkOutTime = checkOutTime,
                    checkInLatitude = teacher.schoolLatitude ?: 0.0,
                    checkInLongitude = teacher.schoolLongitude ?: 0.0,
                    checkOutLatitude = teacher.schoolLatitude ?: 0.0,
                    checkOutLongitude = teacher.schoolLongitude ?: 0.0,
                    checkInPhotoUrl = null,
                    checkOutPhotoUrl = null,
                    status = if (calendar.get(java.util.Calendar.MINUTE) > 45) "LATE" else "PRESENT",
                    notes = null,
                    isSynced = true,
                    createdAt = checkInTime,
                    updatedAt = checkOutTime
                )
                
                database.attendanceDao().insertAttendance(attendance)
            }
        }
    }
    
    /**
     * Create sample leave requests
     */
    private suspend fun createSampleLeaveRequests() {
        val currentTime = System.currentTimeMillis()
        val oneDayInMillis = 24 * 60 * 60 * 1000L
        
        // Create pending leave request for teacher-001
        val pendingLeave = LeaveEntity(
            id = UUID.randomUUID().toString(),
            userId = "teacher-001",
            leaveType = LeaveType.SICK,
            startDate = currentTime + (2 * oneDayInMillis),
            endDate = currentTime + (3 * oneDayInMillis),
            reason = "ឈឺក្បាល និង គ្រុនក្តៅ (Headache and fever)",
            status = LeaveStatus.PENDING,
            approverId = null,
            approverComments = null,
            attachmentUrl = null,
            createdAt = currentTime,
            updatedAt = currentTime,
            isSynced = true
        )
        
        // Create approved leave request for teacher-002
        val approvedLeave = LeaveEntity(
            id = UUID.randomUUID().toString(),
            userId = "teacher-002",
            leaveType = LeaveType.PERSONAL,
            startDate = currentTime - (5 * oneDayInMillis),
            endDate = currentTime - (3 * oneDayInMillis),
            reason = "ធ្វើបុណ្យភូមិ (Village ceremony)",
            status = LeaveStatus.APPROVED,
            approverId = "director-002",
            approverComments = "Approved for cultural ceremony",
            attachmentUrl = null,
            createdAt = currentTime - (7 * oneDayInMillis),
            updatedAt = currentTime - (6 * oneDayInMillis),
            isSynced = true
        )
        
        database.leaveDao().insertLeave(pendingLeave)
        database.leaveDao().insertLeave(approvedLeave)
    }
    
    /**
     * Create sample notifications
     */
    private suspend fun createSampleNotifications() {
        val currentTime = System.currentTimeMillis()
        
        val notifications = listOf(
            // Attendance reminder
            NotificationEntity(
                type = NotificationType.ATTENDANCE,
                title = "ពេលវេលាចូលធ្វើការ (Time to Check In)",
                message = "សូមចុច Check In មុនម៉ោង ៨:០០ ព្រឹក",
                priority = NotificationPriority.HIGH,
                isRead = false,
                actionData = """{"userId":"teacher-001","action":"check_in"}""",
                createdAt = currentTime,
                readAt = null,
                scheduledFor = null,
                expiresAt = null
            ),
            
            // Leave approval notification
            NotificationEntity(
                type = NotificationType.APPROVAL,
                title = "ច្បាប់ឈប់សម្រាកត្រូវបានអនុម័ត (Leave Approved)",
                message = "ច្បាប់ឈប់សម្រាករបស់អ្នកត្រូវបានអនុម័ត",
                priority = NotificationPriority.NORMAL,
                isRead = true,
                actionData = """{"userId":"teacher-002","leaveId":"leave-001","action":"view_leave"}""",
                createdAt = currentTime - (2 * 24 * 60 * 60 * 1000L),
                readAt = currentTime - (2 * 24 * 60 * 60 * 1000L),
                scheduledFor = null,
                expiresAt = null
            )
        )
        
        database.notificationDao().insertAll(notifications)
    }
    
    /**
     * Check if test data exists
     */
    suspend fun hasTestData(): Boolean = withContext(Dispatchers.IO) {
        try {
            val userCount = database.userDao().getUserCount()
            userCount > 0
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get test account credentials
     */
    fun getTestAccounts(): List<TestAccount> {
        return testUsers.map { user ->
            TestAccount(
                email = user.email,
                password = DEFAULT_PASSWORD,
                role = user.role.name,
                description = user.description
            )
        }
    }
    
    // Helper data classes
    data class TestUser(
        val id: String,
        val email: String,
        val name: String,
        val role: UserRole,
        val phoneNumber: String?,
        val departmentId: String?,
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
        val description: String
    )
    
    data class SchoolInfo(
        val name: String,
        val address: String,
        val latitude: Double,
        val longitude: Double
    )
}