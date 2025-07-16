-- Sample data for Cambodia Education Attendance System
-- This file contains sample data for testing and development

-- Insert sample schools
INSERT INTO schools (id, name, address, phone_number, email, principal_name, established_date, school_type, latitude, longitude) VALUES
(uuid_generate_v4(), 'Royal University of Phnom Penh', 'Russian Federation Blvd, Phnom Penh, Cambodia', '+855-23-880-009', 'info@rupp.edu.kh', 'Dr. Chet Chealy', '1960-01-01', 'university', 11.5564, 104.9282),
(uuid_generate_v4(), 'Hun Sen High School', 'Monivong Blvd, Phnom Penh, Cambodia', '+855-23-123-456', 'info@hunsen.edu.kh', 'Mr. Sok Panha', '1985-09-15', 'high_school', 11.5449, 104.8922),
(uuid_generate_v4(), 'International School of Phnom Penh', 'Street 95, Phnom Penh, Cambodia', '+855-23-213-103', 'admin@ispp.edu.kh', 'Ms. Sarah Johnson', '1995-08-20', 'international', 11.5625, 104.9010);

-- Get school IDs for reference
DO $$
DECLARE
    rupp_id UUID;
    hunsen_id UUID;
    ispp_id UUID;
BEGIN
    SELECT id INTO rupp_id FROM schools WHERE name = 'Royal University of Phnom Penh';
    SELECT id INTO hunsen_id FROM schools WHERE name = 'Hun Sen High School';
    SELECT id INTO ispp_id FROM schools WHERE name = 'International School of Phnom Penh';
    
    -- Insert sample users
    INSERT INTO users (id, username, email, password_hash, first_name, last_name, phone_number, role, school_id, department, employee_id) VALUES
    -- Admins
    (uuid_generate_v4(), 'admin', 'admin@plp.edu.kh', '$2b$12$LQv3/X/0W6Y9GbCi.bhdUu4eKjR5BjMPpiqEFG0AQS8pAP7u9H5jK', 'System', 'Administrator', '+855-12-345-678', 'admin', rupp_id, 'IT Department', 'ADM001'),
    
    -- Teachers
    (uuid_generate_v4(), 'teacher1', 'teacher1@rupp.edu.kh', '$2b$12$LQv3/X/0W6Y9GbCi.bhdUu4eKjR5BjMPpiqEFG0AQS8pAP7u9H5jK', 'Sophea', 'Chan', '+855-12-111-222', 'teacher', rupp_id, 'Computer Science', 'TCH001'),
    (uuid_generate_v4(), 'teacher2', 'teacher2@rupp.edu.kh', '$2b$12$LQv3/X/0W6Y9GbCi.bhdUu4eKjR5BjMPpiqEFG0AQS8pAP7u9H5jK', 'Dara', 'Kim', '+855-12-333-444', 'teacher', rupp_id, 'Mathematics', 'TCH002'),
    (uuid_generate_v4(), 'teacher3', 'teacher3@hunsen.edu.kh', '$2b$12$LQv3/X/0W6Y9GbCi.bhdUu4eKjR5BjMPpiqEFG0AQS8pAP7u9H5jK', 'Maly', 'Pov', '+855-12-555-666', 'teacher', hunsen_id, 'English', 'TCH003'),
    
    -- Students
    (uuid_generate_v4(), 'student1', 'student1@rupp.edu.kh', '$2b$12$LQv3/X/0W6Y9GbCi.bhdUu4eKjR5BjMPpiqEFG0AQS8pAP7u9H5jK', 'Pisach', 'Leng', '+855-12-777-888', 'student', rupp_id, 'Computer Science', 'STU001'),
    (uuid_generate_v4(), 'student2', 'student2@rupp.edu.kh', '$2b$12$LQv3/X/0W6Y9GbCi.bhdUu4eKjR5BjMPpiqEFG0AQS8pAP7u9H5jK', 'Sreynich', 'Hout', '+855-12-999-000', 'student', rupp_id, 'Mathematics', 'STU002'),
    (uuid_generate_v4(), 'student3', 'student3@hunsen.edu.kh', '$2b$12$LQv3/X/0W6Y9GbCi.bhdUu4eKjR5BjMPpiqEFG0AQS8pAP7u9H5jK', 'Vicheka', 'Sor', '+855-12-111-333', 'student', hunsen_id, 'Grade 12', 'STU003'),
    
    -- Staff
    (uuid_generate_v4(), 'staff1', 'staff1@rupp.edu.kh', '$2b$12$LQv3/X/0W6Y9GbCi.bhdUu4eKjR5BjMPpiqEFG0AQS8pAP7u9H5jK', 'Chantha', 'Ngoun', '+855-12-222-444', 'staff', rupp_id, 'Administration', 'STF001'),
    (uuid_generate_v4(), 'staff2', 'staff2@ispp.edu.kh', '$2b$12$LQv3/X/0W6Y9GbCi.bhdUu4eKjR5BjMPpiqEFG0AQS8pAP7u9H5jK', 'Bopha', 'Tan', '+855-12-555-777', 'staff', ispp_id, 'Library', 'STF002');

    -- Insert sample system settings
    INSERT INTO system_settings (school_id, setting_key, setting_value, category, description) VALUES
    (rupp_id, 'working_hours_start', '"08:00"', 'attendance', 'Default start time for working hours'),
    (rupp_id, 'working_hours_end', '"17:00"', 'attendance', 'Default end time for working hours'),
    (rupp_id, 'late_threshold_minutes', '15', 'attendance', 'Minutes after start time to mark as late'),
    (rupp_id, 'geofence_radius_meters', '100', 'attendance', 'Radius around school for valid check-ins'),
    (rupp_id, 'leave_approval_required', 'true', 'leave', 'Whether leave requests require approval'),
    (rupp_id, 'max_leave_days_per_month', '5', 'leave', 'Maximum leave days allowed per month'),
    (hunsen_id, 'working_hours_start', '"07:30"', 'attendance', 'Default start time for working hours'),
    (hunsen_id, 'working_hours_end', '"16:30"', 'attendance', 'Default end time for working hours'),
    (ispp_id, 'working_hours_start', '"08:30"', 'attendance', 'Default start time for working hours'),
    (ispp_id, 'working_hours_end', '"17:30"', 'attendance', 'Default end time for working hours');

END $$;

-- Insert sample attendance records (for the last 7 days)
DO $$
DECLARE
    user_record RECORD;
    day_offset INTEGER;
    check_in_time TIMESTAMP WITH TIME ZONE;
    check_out_time TIMESTAMP WITH TIME ZONE;
BEGIN
    FOR user_record IN SELECT id, school_id FROM users WHERE role IN ('teacher', 'student', 'staff') LOOP
        FOR day_offset IN 0..6 LOOP
            -- Generate attendance for the last 7 days
            check_in_time := (CURRENT_DATE - day_offset) + INTERVAL '8 hours' + (random() * INTERVAL '2 hours');
            check_out_time := check_in_time + INTERVAL '8 hours' + (random() * INTERVAL '2 hours');
            
            -- Skip weekend attendance (Saturday = 6, Sunday = 0)
            IF EXTRACT(dow FROM check_in_time::date) NOT IN (0, 6) THEN
                INSERT INTO attendance_records (
                    user_id, 
                    school_id, 
                    check_in_time, 
                    check_out_time,
                    check_in_latitude,
                    check_in_longitude,
                    check_out_latitude,
                    check_out_longitude,
                    working_hours,
                    status
                ) VALUES (
                    user_record.id,
                    user_record.school_id,
                    check_in_time,
                    check_out_time,
                    11.5564 + (random() - 0.5) * 0.01, -- Slight variation around school location
                    104.9282 + (random() - 0.5) * 0.01,
                    11.5564 + (random() - 0.5) * 0.01,
                    104.9282 + (random() - 0.5) * 0.01,
                    EXTRACT(EPOCH FROM (check_out_time - check_in_time))/3600,
                    CASE 
                        WHEN random() > 0.9 THEN 'late'
                        WHEN random() > 0.95 THEN 'early_departure'
                        ELSE 'present'
                    END
                );
            END IF;
        END LOOP;
    END LOOP;
END $$;

-- Insert sample leave requests
DO $$
DECLARE
    user_record RECORD;
    approver_id UUID;
BEGIN
    SELECT id INTO approver_id FROM users WHERE role = 'admin' LIMIT 1;
    
    FOR user_record IN SELECT id FROM users WHERE role IN ('teacher', 'student', 'staff') LIMIT 5 LOOP
        INSERT INTO leave_requests (
            user_id,
            leave_type,
            start_date,
            end_date,
            total_days,
            reason,
            status,
            approved_by,
            approval_date
        ) VALUES 
        (user_record.id, 'sick', CURRENT_DATE + 7, CURRENT_DATE + 7, 1, 'Medical appointment', 'approved', approver_id, CURRENT_TIMESTAMP),
        (user_record.id, 'vacation', CURRENT_DATE + 14, CURRENT_DATE + 16, 3, 'Family vacation', 'pending', NULL, NULL);
    END LOOP;
END $$;

-- Insert sample missions
DO $$
DECLARE
    school_record RECORD;
    creator_id UUID;
    mission_id UUID;
    participant_record RECORD;
BEGIN
    FOR school_record IN SELECT id FROM schools LOOP
        SELECT id INTO creator_id FROM users WHERE school_id = school_record.id AND role = 'teacher' LIMIT 1;
        
        INSERT INTO missions (
            id,
            title,
            description,
            mission_type,
            school_id,
            created_by,
            start_date,
            end_date,
            start_time,
            end_time,
            destination_name,
            destination_address,
            destination_latitude,
            destination_longitude,
            status
        ) VALUES (
            uuid_generate_v4(),
            'Educational Field Trip to Angkor Wat',
            'A cultural and historical education trip to learn about Khmer heritage',
            'field_trip',
            school_record.id,
            creator_id,
            CURRENT_DATE + 21,
            CURRENT_DATE + 23,
            '08:00',
            '17:00',
            'Angkor Archaeological Park',
            'Angkor Wat, Krong Siem Reap, Cambodia',
            13.4125,
            103.8670,
            'planned'
        ) RETURNING id INTO mission_id;
        
        -- Add participants to the mission
        FOR participant_record IN SELECT id FROM users WHERE school_id = school_record.id AND role IN ('teacher', 'student') LIMIT 5 LOOP
            INSERT INTO mission_participants (
                mission_id,
                user_id,
                role,
                status
            ) VALUES (
                mission_id,
                participant_record.id,
                CASE WHEN (SELECT role FROM users WHERE id = participant_record.id) = 'teacher' THEN 'leader' ELSE 'participant' END,
                'confirmed'
            );
        END LOOP;
    END LOOP;
END $$;

-- Insert sample notifications
DO $$
DECLARE
    user_record RECORD;
BEGIN
    FOR user_record IN SELECT id FROM users LIMIT 10 LOOP
        INSERT INTO notifications (
            user_id,
            title,
            message,
            type,
            priority,
            data
        ) VALUES 
        (user_record.id, 'Welcome to PLP Attendance System', 'Thank you for joining our attendance tracking system. Please complete your profile setup.', 'system', 'normal', '{"action": "complete_profile"}'),
        (user_record.id, 'Attendance Reminder', 'Don''t forget to check in when you arrive at school today.', 'reminder', 'normal', '{"type": "check_in_reminder"}'),
        (user_record.id, 'Leave Request Update', 'Your leave request has been approved.', 'leave', 'high', '{"leave_request_id": "sample_id", "status": "approved"}');
    END LOOP;
END $$;

-- Update some users as email verified
UPDATE users SET email_verified = true WHERE role = 'admin';
UPDATE users SET email_verified = true WHERE username LIKE 'teacher%';

COMMIT;