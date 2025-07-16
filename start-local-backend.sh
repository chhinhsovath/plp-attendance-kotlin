#!/bin/bash

echo "Starting PLP Attendance Backend Server locally..."
echo "================================================"

cd backend

# Check if node_modules exists
if [ ! -d "node_modules" ]; then
    echo "Installing dependencies..."
    npm install
fi

echo ""
echo "Starting server on http://localhost:3000"
echo "The server will connect to remote database at 157.10.73.52"
echo ""
echo "Android emulator should connect to http://10.0.2.2:3000"
echo ""

# Start the server
npm start