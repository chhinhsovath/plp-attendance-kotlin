# User Interaction Scenarios - Cambodia Education Attendance System

## 1. Authentication Flow

### 1.1 Login
**User Story**: As a user, I want to login with my credentials to access the system
- **Input**: Username/Email, Password
- **Validation**: 
  - Username: Required, min 3 characters
  - Password: Required, min 6 characters
- **API**: POST /api/auth/login
- **Success**: Navigate to role-specific dashboard
- **Error**: Display error message, remain on login screen

### 1.2 Role-Based Access
**User Story**: As a logged-in user, I see features based on my role
- **Roles**: Administrator, Zone, Provincial, Department, Cluster, Director, Teacher
- **Navigation**: Dynamic menu based on role permissions
- **Data Access**: Filtered based on hierarchical position

## 2. Teacher Workflows

### 2.1 Daily Check-In/Out
**User Story**: As a teacher, I want to record my daily attendance
- **Input**: GPS location auto-captured, optional photo
- **Validation**: 
  - Must be within school geofence (500m radius)
  - Check-in between 6:00 AM - 9:00 AM
  - Check-out after check-in time
- **API**: 
  - POST /api/attendance/check-in
  - POST /api/attendance/check-out
- **Success**: Confirmation with timestamp
- **Error**: Location error, time restriction error

### 2.2 Mission Management
**User Story**: As a teacher, I want to record official travel/missions
- **Input**: 
  - Mission type (Training, Meeting, Official duty)
  - Start/End locations with GPS
  - Purpose description
  - Expected duration
- **Validation**: 
  - Both locations required
  - Description min 10 characters
  - Cannot overlap with existing missions
- **API**: 
  - POST /api/missions/create
  - PUT /api/missions/{id}/start
  - PUT /api/missions/{id}/complete
- **States**: Planned → In Progress → Completed

### 2.3 Leave Request
**User Story**: As a teacher, I want to request leave
- **Input**: 
  - Leave type (Sick, Personal, Emergency)
  - Start/End dates
  - Reason
  - Supporting documents (optional)
- **Validation**: 
  - Cannot request past dates
  - Reason required for personal leave
  - Medical certificate for sick leave > 3 days
- **API**: POST /api/leave/request
- **Approval Flow**: Teacher → Director → Department (based on duration)

## 3. Director Workflows

### 3.1 Staff Management
**User Story**: As a director, I want to manage my school's teachers
- **View**: List of all teachers with current status
- **Actions**: 
  - View attendance history
  - Approve/Reject leave requests
  - View mission status
- **Filters**: By status, date range, attendance percentage

### 3.2 Attendance Monitoring
**User Story**: As a director, I want to monitor daily attendance
- **Dashboard**: 
  - Present/Absent/On Mission/On Leave counts
  - Real-time updates
  - Attendance trends (weekly/monthly)
- **Alerts**: Late check-ins, missing check-outs
- **Export**: Daily/Monthly reports (PDF/Excel)

### 3.3 Mission Approval
**User Story**: As a director, I want to approve teacher missions
- **View**: Pending mission requests
- **Actions**: Approve/Reject with comments
- **Validation**: Check for scheduling conflicts
- **API**: PUT /api/missions/{id}/approve

## 4. Cluster Level Workflows

### 4.1 Multi-School Overview
**User Story**: As a cluster head, I want to monitor all schools in my cluster
- **Dashboard**: 
  - School-wise attendance summary
  - Comparative analytics
  - Red flags (schools with <80% attendance)
- **Drill-down**: Click school → See director/teacher details

### 4.2 Director Performance
**User Story**: As a cluster head, I want to track director attendance
- **Metrics**: 
  - Director check-in consistency
  - Report submission timeliness
  - Teacher management effectiveness

## 5. Higher Level Workflows (Department/Provincial/Zone/Admin)

### 5.1 Hierarchical Data View
**User Story**: As a higher-level user, I want to see aggregated data
- **View Levels**: 
  - Zone → Provinces
  - Provincial → Departments  
  - Department → Clusters
  - Cluster → Schools
- **Metrics**: 
  - Attendance percentages
  - Active missions
  - Leave patterns
  - Geographical distribution

### 5.2 Report Generation
**User Story**: As an administrator, I want comprehensive reports
- **Report Types**:
  - Daily attendance summary
  - Monthly trend analysis
  - Leave pattern analysis
  - Mission expense reports
- **Formats**: PDF, Excel, CSV
- **Scheduling**: Automated daily/weekly/monthly

### 5.3 System Configuration
**User Story**: As an administrator, I want to configure system settings
- **Settings**:
  - School geofence boundaries
  - Working hours
  - Leave policies
  - Approval hierarchies
- **User Management**:
  - Create/Update/Deactivate users
  - Role assignments
  - Bulk imports (CSV)

## 6. Common Features

### 6.1 Notifications
- Push notifications for approvals needed
- Daily check-in reminders
- Mission start/end reminders
- Leave request status updates

### 6.2 Offline Support
- Cache last 7 days of data
- Queue actions when offline
- Auto-sync when connected
- Conflict resolution for overlapping edits

### 6.3 Multi-Language Support
- Khmer (primary)
- English (secondary)
- Language toggle in settings
- RTL/LTR layout support

## API Response Standards

### Success Response
```json
{
  "status": "success",
  "data": {},
  "message": "Operation completed successfully"
}
```

### Error Response
```json
{
  "status": "error",
  "error": {
    "code": "ERROR_CODE",
    "message": "Human readable error message",
    "field": "field_name" // for validation errors
  }
}
```

### Pagination Response
```json
{
  "status": "success",
  "data": {
    "items": [],
    "total": 100,
    "page": 1,
    "pageSize": 20,
    "hasNext": true
  }
}
```