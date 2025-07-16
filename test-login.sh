#!/bin/bash

# Test login script for PLP Attendance System
# This script tests various user credentials against the API

API_URL="http://157.10.73.52:3000/api/auth/login"

echo "Testing PLP Attendance System Login API"
echo "======================================="

# Function to test login
test_login() {
    local email=$1
    local password=$2
    echo -e "\nTesting login for: $email"
    
    response=$(curl -s -w "\n%{http_code}" -X POST "$API_URL" \
        -H "Content-Type: application/json" \
        -d "{\"identifier\":\"$email\",\"password\":\"$password\"}")
    
    # Extract HTTP status code (last line)
    http_code=$(echo "$response" | tail -n1)
    # Extract JSON response (everything except last line)
    json_response=$(echo "$response" | sed '$d')
    
    if [ "$http_code" -eq 200 ]; then
        echo "✅ Login successful!"
        echo "$json_response" | python3 -m json.tool 2>/dev/null || echo "$json_response"
    elif [ "$http_code" -eq 429 ]; then
        echo "❌ Rate limit exceeded (429). Please wait before trying again."
        echo "$json_response"
    elif [ "$http_code" -eq 401 ]; then
        echo "❌ Invalid credentials (401)"
        echo "$json_response" | python3 -m json.tool 2>/dev/null || echo "$json_response"
    else
        echo "❌ Login failed with status: $http_code"
        echo "$json_response"
    fi
}

# Test different user credentials
echo -e "\n1. Testing admin@plp.gov.kh with password:"
test_login "admin@plp.gov.kh" "password"

echo -e "\n2. Testing admin@plp.edu.kh with password:"
test_login "admin@plp.edu.kh" "password"

echo -e "\n3. Testing teacher1@rupp.edu.kh with password:"
test_login "teacher1@rupp.edu.kh" "password"

echo -e "\n4. Testing with wrong password:"
test_login "admin@plp.gov.kh" "wrongpassword"

echo -e "\n\nNote: If you're getting 429 errors, the rate limit is active."
echo "The server allows 5 attempts per 15 minutes per IP address."
echo "To remove this limit, the backend server needs to be restarted after the code change."