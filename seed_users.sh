#!/bin/bash

# Script to seed login users for PLP Attendance System
# This ensures all users shown in LoginScreen.kt can successfully authenticate

echo "🚀 PLP Attendance System - User Seeding Script"
echo "=============================================="

# Check if Node.js is available
if ! command -v node &> /dev/null; then
    echo "❌ Node.js is not installed or not in PATH"
    echo "Please install Node.js to continue"
    exit 1
fi

# Check if pg package is available
if ! node -e "require('pg')" 2>/dev/null; then
    echo "📦 Installing PostgreSQL client..."
    npm install pg
fi

# Make the JavaScript file executable
chmod +x seed_login_users.js

# Run the seeding script
echo "🌱 Starting user seeding process..."
node seed_login_users.js

# Check exit code
if [ $? -eq 0 ]; then
    echo ""
    echo "✅ SUCCESS: All login users have been created!"
    echo "📱 Users can now login with their credentials shown in the app"
    echo "🔑 All users use password: 'password'"
    echo ""
    echo "🎯 Next steps:"
    echo "1. Start the backend server: cd backend && npm start"
    echo "2. Build and run the Android app"
    echo "3. Test login with any of the 14 user accounts"
else
    echo ""
    echo "❌ FAILED: User seeding encountered errors"
    echo "Please check the logs above for details"
    exit 1
fi