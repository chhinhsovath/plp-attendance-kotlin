#!/bin/bash

# API Endpoint Testing Script for PLP Attendance System
# Base URL configuration
BASE_URL="http://localhost:3000"
API_URL="${BASE_URL}/api"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}=== PLP Attendance API Routes Test ===${NC}\n"

# Function to test endpoint
test_endpoint() {
    local method=$1
    local endpoint=$2
    local description=$3
    local data=$4
    local token=$5
    
    echo -e "${YELLOW}Testing:${NC} ${method} ${endpoint}"
    echo -e "${GREEN}Description:${NC} ${description}"
    
    if [ -n "$token" ]; then
        if [ -n "$data" ]; then
            response=$(curl -s -X ${method} "${BASE_URL}${endpoint}" \
                -H "Content-Type: application/json" \
                -H "Authorization: Bearer ${token}" \
                -d "${data}")
        else
            response=$(curl -s -X ${method} "${BASE_URL}${endpoint}" \
                -H "Authorization: Bearer ${token}")
        fi
    else
        if [ -n "$data" ]; then
            response=$(curl -s -X ${method} "${BASE_URL}${endpoint}" \
                -H "Content-Type: application/json" \
                -d "${data}")
        else
            response=$(curl -s -X ${method} "${BASE_URL}${endpoint}")
        fi
    fi
    
    echo -e "Response: ${response}\n"
}

# 1. Health Check
echo -e "${GREEN}=== Health Check ===${NC}"
test_endpoint "GET" "/health" "Server health status"

# 2. Authentication Routes
echo -e "${GREEN}=== Authentication Routes ===${NC}"

# Register a test user
test_endpoint "POST" "/api/auth/register" "Register a new user" '{
    "email": "test@example.com",
    "password": "Test123!",
    "firstName": "Test",
    "lastName": "User",
    "role": "teacher",
    "schoolId": 1
}'

# Login
echo -e "${YELLOW}Attempting login to get token...${NC}"
LOGIN_RESPONSE=$(curl -s -X POST "${API_URL}/auth/login" \
    -H "Content-Type: application/json" \
    -d '{
        "email": "admin@plp.gov.kh",
        "password": "Admin@123"
    }')

echo "Login Response: ${LOGIN_RESPONSE}"

# Extract token if available
TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"accessToken":"[^"]*' | grep -o '[^"]*$')

if [ -n "$TOKEN" ]; then
    echo -e "${GREEN}Token obtained successfully${NC}\n"
    
    # Test authenticated routes
    test_endpoint "GET" "/api/auth/profile" "Get current user profile" "" "$TOKEN"
    test_endpoint "GET" "/api/auth/me" "Get current user info" "" "$TOKEN"
    
    # 3. Attendance Routes
    echo -e "${GREEN}=== Attendance Routes ===${NC}"
    test_endpoint "GET" "/api/attendance/status" "Get today's attendance status" "" "$TOKEN"
    test_endpoint "GET" "/api/attendance/records?page=1&limit=10" "Get attendance records" "" "$TOKEN"
    test_endpoint "GET" "/api/attendance/summary?period=week" "Get attendance summary" "" "$TOKEN"
    
    # Check-in example (commented out to avoid creating records)
    # test_endpoint "POST" "/api/attendance/check-in" "Check in for attendance" '{
    #     "latitude": 11.5564,
    #     "longitude": 104.9282,
    #     "photoUrl": "https://example.com/photo.jpg"
    # }' "$TOKEN"
    
    # 4. Leave Routes
    echo -e "${GREEN}=== Leave Management Routes ===${NC}"
    test_endpoint "GET" "/api/leave/requests?page=1&limit=10" "Get leave requests" "" "$TOKEN"
    test_endpoint "GET" "/api/leave/pending-approvals" "Get pending approvals" "" "$TOKEN"
    test_endpoint "GET" "/api/leave/balance/1" "Get leave balance" "" "$TOKEN"
    
    # Leave request example (commented out)
    # test_endpoint "POST" "/api/leave/request" "Submit leave request" '{
    #     "leaveType": "sick",
    #     "startDate": "2024-01-15",
    #     "endDate": "2024-01-16",
    #     "reason": "Medical appointment"
    # }' "$TOKEN"
    
else
    echo -e "${RED}Failed to obtain token. Check login credentials.${NC}"
fi

# 5. Other Routes (placeholders)
echo -e "${GREEN}=== Other Routes (Placeholders) ===${NC}"
test_endpoint "GET" "/api/users" "Users endpoint"
test_endpoint "GET" "/api/missions" "Missions endpoint"
test_endpoint "GET" "/api/notifications" "Notifications endpoint"
test_endpoint "GET" "/api/analytics" "Analytics endpoint"
test_endpoint "GET" "/api/schools" "Schools endpoint"

# 6. Swagger Documentation
echo -e "${GREEN}=== API Documentation ===${NC}"
echo -e "${YELLOW}Swagger UI available at:${NC} ${BASE_URL}/api-docs"
echo -e "${YELLOW}API JSON specification at:${NC} ${BASE_URL}/api-docs/swagger.json"

echo -e "\n${GREEN}=== Test Complete ===${NC}"
echo -e "Server running at: ${BASE_URL}"
echo -e "To stop the server, run: pkill -f 'node src/server.js'"