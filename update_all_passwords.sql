-- Update all user passwords to 'password123'
-- The bcrypt hash below is for 'password123' with cost factor 12

-- First, let's see which users exist
SELECT id, email, username FROM users;

-- Update all user passwords
UPDATE users 
SET password_hash = '$2b$12$LQv3/X/0W6Y9GbCi.bhdUu4eKjR5BjMPpiqEFG0AQS8pAP7u9H5jK',
    updated_at = CURRENT_TIMESTAMP;

-- Verify the update
SELECT id, email, username, 
       CASE 
         WHEN password_hash = '$2b$12$LQv3/X/0W6Y9GbCi.bhdUu4eKjR5BjMPpiqEFG0AQS8pAP7u9H5jK' 
         THEN '✓ Updated to password123' 
         ELSE '✗ Not updated' 
       END as password_status
FROM users 
ORDER BY role, email;

-- Show count of updated users
SELECT COUNT(*) as total_users_updated 
FROM users 
WHERE password_hash = '$2b$12$LQv3/X/0W6Y9GbCi.bhdUu4eKjR5BjMPpiqEFG0AQS8pAP7u9H5jK';

-- Summary message
SELECT 'All users can now login with password: password123' as message;