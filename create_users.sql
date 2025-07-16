-- Create test users directly with SQL
-- Password hash for 'password123' using bcrypt with 10 rounds
-- This is the hash for 'password123': $2b$12$LQv3/X/0W6Y9GbCi.bhdUu4eKjR5BjMPpiqEFG0AQS8pAP7u9H5jK

-- First create a school if it doesn't exist
INSERT INTO schools (id, name, address, phone_number, email, principal_name, established_date, school_type, latitude, longitude) 
VALUES 
('550e8400-e29b-41d4-a716-446655440000', 'Test School', 'Phnom Penh, Cambodia', '+855-23-123-456', 'test@school.kh', 'Test Principal', '2020-01-01', 'high_school', 11.5564, 104.9282)
ON CONFLICT (id) DO NOTHING;

-- Insert test users
INSERT INTO users (id, username, email, password_hash, first_name, last_name, role, school_id, department, employee_id, email_verified, phone_verified, is_active, created_at, updated_at) 
VALUES 
('550e8400-e29b-41d4-a716-446655440001', 'admin', 'admin@plp.edu.kh', '$2b$12$LQv3/X/0W6Y9GbCi.bhdUu4eKjR5BjMPpiqEFG0AQS8pAP7u9H5jK', 'System', 'Administrator', 'admin', '550e8400-e29b-41d4-a716-446655440000', 'IT Department', 'ADM001', true, false, true, NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440002', 'teacher1', 'teacher1@plp.edu.kh', '$2b$12$LQv3/X/0W6Y9GbCi.bhdUu4eKjR5BjMPpiqEFG0AQS8pAP7u9H5jK', 'Sophea', 'Chan', 'teacher', '550e8400-e29b-41d4-a716-446655440000', 'Computer Science', 'TCH001', true, false, true, NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440003', 'teacher2', 'teacher2@plp.edu.kh', '$2b$12$LQv3/X/0W6Y9GbCi.bhdUu4eKjR5BjMPpiqEFG0AQS8pAP7u9H5jK', 'Dara', 'Kim', 'teacher', '550e8400-e29b-41d4-a716-446655440000', 'Mathematics', 'TCH002', true, false, true, NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440004', 'student1', 'student1@plp.edu.kh', '$2b$12$LQv3/X/0W6Y9GbCi.bhdUu4eKjR5BjMPpiqEFG0AQS8pAP7u9H5jK', 'Pisach', 'Leng', 'student', '550e8400-e29b-41d4-a716-446655440000', 'Computer Science', 'STU001', false, false, true, NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440005', 'staff1', 'staff1@plp.edu.kh', '$2b$12$LQv3/X/0W6Y9GbCi.bhdUu4eKjR5BjMPpiqEFG0AQS8pAP7u9H5jK', 'Chantha', 'Ngoun', 'staff', '550e8400-e29b-41d4-a716-446655440000', 'Administration', 'STF001', false, false, true, NOW(), NOW())
ON CONFLICT (username) DO NOTHING;

-- Verify the users were created
SELECT username, email, first_name, last_name, role, is_active FROM users;