#!/bin/bash

# Script to update all user passwords on the remote PostgreSQL server
# This updates all passwords to 'password123'

echo "Remote Password Update Script for PLP Attendance System"
echo "======================================================"
echo ""
echo "This script will update ALL user passwords to 'password123'"
echo "Server: 157.10.73.52"
echo "Database: plp_attendance_kotlin"
echo ""

# PostgreSQL connection parameters
PGHOST="157.10.73.52"
PGPORT="5432"
PGDATABASE="plp_attendance_kotlin"
PGUSER="admin"

# The bcrypt hash for 'password123'
PASSWORD_HASH='$2b$12$LQv3/X/0W6Y9GbCi.bhdUu4eKjR5BjMPpiqEFG0AQS8pAP7u9H5jK'

# Check if psql is installed
if ! command -v psql &> /dev/null; then
    echo "Error: PostgreSQL client (psql) is not installed."
    echo "Install it with: brew install postgresql (on macOS)"
    exit 1
fi

echo "You will be prompted for the database password (P@ssw0rd)"
echo ""

# Execute the password update
PGPASSWORD="P@ssw0rd" psql -h "$PGHOST" -p "$PGPORT" -d "$PGDATABASE" -U "$PGUSER" <<EOF
-- Show current users
\echo 'Current users in the database:'
SELECT email, username, role FROM users ORDER BY role, email;

-- Update all passwords
\echo ''
\echo 'Updating all user passwords...'
UPDATE users 
SET password_hash = '$PASSWORD_HASH',
    updated_at = CURRENT_TIMESTAMP;

-- Show results
\echo ''
\echo 'Password update results:'
SELECT 
    email,
    username,
    role,
    CASE 
        WHEN password_hash = '$PASSWORD_HASH' THEN '✓ Updated' 
        ELSE '✗ Failed' 
    END as status
FROM users 
ORDER BY role, email;

-- Summary
\echo ''
SELECT 'Updated ' || COUNT(*) || ' user passwords to: password123' as summary
FROM users 
WHERE password_hash = '$PASSWORD_HASH';
EOF

echo ""
echo "Password update complete!"
echo ""
echo "All users can now login with:"
echo "  Email: [their email]"
echo "  Password: password123"
echo ""

# Test one login to verify
echo "Testing login with admin@plp.gov.kh..."
curl -s -X POST "http://$PGHOST:3000/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"identifier":"admin@plp.gov.kh","password":"password123"}' | python3 -m json.tool 2>/dev/null || echo "Login test failed"