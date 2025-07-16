# API Testing Plan - Cambodia Education Attendance System

## Test Environment Setup

### Base URLs
- Development: `https://api-dev.plp-attendance.gov.kh`
- Staging: `https://api-staging.plp-attendance.gov.kh`
- Production: `https://api.plp-attendance.gov.kh`

### Authentication
- Type: Bearer Token (JWT)
- Header: `Authorization: Bearer {token}`
- Token Expiry: 24 hours
- Refresh Token Expiry: 30 days

## API Endpoints Testing

### 1. Authentication Module

#### 1.1 Login
```
POST /api/v1/auth/login
```
**Request:**
```json
{
  "username": "teacher001",
  "password": "SecurePass123!",
  "deviceId": "android-device-uuid"
}
```
**Success Response (200):**
```json
{
  "status": "success",
  "data": {
    "user": {
      "id": "usr_123",
      "username": "teacher001",
      "role": "TEACHER",
      "firstName": "Sophea",
      "lastName": "Kim"
    },
    "tokens": {
      "accessToken": "eyJhbGc...",
      "refreshToken": "eyJhbGc...",
      "expiresIn": 86400
    }
  }
}
```
**Error Cases:**
- 401: Invalid credentials
- 422: Validation errors
- 429: Too many attempts

#### 1.2 Refresh Token
```
POST /api/v1/auth/refresh
```
**Request:**
```json
{
  "refreshToken": "eyJhbGc..."
}
```

#### 1.3 Logout
```
POST /api/v1/auth/logout
```

### 2. Attendance Module

#### 2.1 Check In
```
POST /api/v1/attendance/check-in
```
**Request:**
```json
{
  "latitude": 11.5564,
  "longitude": 104.9282,
  "accuracy": 10.5,
  "photoBase64": "data:image/jpeg;base64,..."
}
```
**Validations:**
- Must be within school geofence
- Time between 06:00-09:00
- No existing check-in for today

#### 2.2 Check Out
```
POST /api/v1/attendance/check-out
```
**Request:**
```json
{
  "latitude": 11.5564,
  "longitude": 104.9282,
  "accuracy": 10.5
}
```

#### 2.3 Get Attendance History
```
GET /api/v1/attendance/history?startDate=2024-01-01&endDate=2024-01-31&page=1&limit=20
```

#### 2.4 Get វត្ថមានសម្រាប់ថ្ងៃនេះ
```
GET /api/v1/attendance/today
```

### 3. Mission Module

#### 3.1 Create Mission
```
POST /api/v1/missions
```
**Request:**
```json
{
  "title": "Provincial Training Meeting",
  "type": "TRAINING",
  "startDate": "2024-02-01T08:00:00Z",
  "endDate": "2024-02-03T17:00:00Z",
  "startLocation": {
    "name": "School Office",
    "latitude": 11.5564,
    "longitude": 104.9282
  },
  "destinationLocation": {
    "name": "Provincial Education Office",
    "latitude": 11.5624,
    "longitude": 104.9312
  },
  "purpose": "Annual teacher training workshop",
  "description": "Attending mandatory training on new curriculum"
}
```

#### 3.2 Start Mission
```
PUT /api/v1/missions/{missionId}/start
```
**Request:**
```json
{
  "actualStartLocation": {
    "latitude": 11.5564,
    "longitude": 104.9282
  }
}
```

#### 3.3 Complete Mission
```
PUT /api/v1/missions/{missionId}/complete
```

#### 3.4 Get Missions
```
GET /api/v1/missions?status=APPROVED&page=1&limit=10
```

### 4. Leave Module

#### 4.1 Request Leave
```
POST /api/v1/leave/request
```
**Request:**
```json
{
  "type": "SICK",
  "startDate": "2024-02-05",
  "endDate": "2024-02-07",
  "reason": "Medical treatment required",
  "attachments": ["doc_url_1", "doc_url_2"]
}
```

#### 4.2 Get Leave History
```
GET /api/v1/leave/history
```

#### 4.3 Get Leave Balance
```
GET /api/v1/leave/balance
```

### 5. Approval Module (Director+)

#### 5.1 Get Pending Approvals
```
GET /api/v1/approvals/pending?type=LEAVE&page=1
```

#### 5.2 Approve Request
```
PUT /api/v1/approvals/{requestId}/approve
```
**Request:**
```json
{
  "comments": "Approved for medical emergency",
  "conditions": []
}
```

#### 5.3 Reject Request
```
PUT /api/v1/approvals/{requestId}/reject
```
**Request:**
```json
{
  "reason": "Insufficient documentation",
  "comments": "Please attach medical certificate"
}
```

### 6. Reports Module

#### 6.1 Attendance Summary
```
GET /api/v1/reports/attendance/summary?level=SCHOOL&entityId=sch_123&startDate=2024-01-01&endDate=2024-01-31
```

#### 6.2 Export Report
```
POST /api/v1/reports/export
```
**Request:**
```json
{
  "reportType": "MONTHLY_ATTENDANCE",
  "format": "EXCEL",
  "filters": {
    "schoolId": "sch_123",
    "month": "2024-01"
  }
}
```

### 7. User Management (Admin)

#### 7.1 Create User
```
POST /api/v1/users
```

#### 7.2 Update User
```
PUT /api/v1/users/{userId}
```

#### 7.3 Bulk Import
```
POST /api/v1/users/import
Content-Type: multipart/form-data
```

## Error Response Format

```json
{
  "status": "error",
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed",
    "details": [
      {
        "field": "startDate",
        "message": "Start date cannot be in the past"
      }
    ]
  },
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/v1/leave/request"
}
```

## Test Scenarios

### Positive Test Cases
1. ✅ Successful login with valid credentials
2. ✅ Check-in within geofence and time window
3. ✅ Create and complete mission workflow
4. ✅ Leave request approval chain
5. ✅ Report generation with filters

### Negative Test Cases
1. ❌ Login with invalid credentials
2. ❌ Check-in outside geofence
3. ❌ Check-in outside time window
4. ❌ Mission with overlapping dates
5. ❌ Leave request without required documents
6. ❌ Unauthorized access to higher-level data

### Edge Cases
1. 🔄 Concurrent check-in attempts
2. 🔄 Network failure during check-in
3. 🔄 Token expiry during mission
4. 🔄 Conflicting approvals
5. 🔄 Large data exports (>10k records)

## Performance Requirements
- API Response Time: < 500ms (95th percentile)
- Bulk Operations: < 5 seconds for 1000 records
- Report Generation: < 10 seconds
- Concurrent Users: Support 10,000 simultaneous users