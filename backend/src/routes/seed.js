const express = require('express');
const { query, transaction } = require('../config/database');
const { hashPassword } = require('../middleware/auth');
const { v4: uuidv4 } = require('uuid');
const logger = require('../utils/logger');

const router = express.Router();

/**
 * @swagger
 * /api/seed/users:
 *   post:
 *     summary: Seed the database with test users
 *     tags: [Development]
 *     responses:
 *       200:
 *         description: Database seeded successfully
 *       500:
 *         description: Database seeding failed
 */
router.post('/users', async (req, res) => {
  try {
    // Check if users already exist
    const existingUsers = await query('SELECT COUNT(*) as count FROM users');
    if (parseInt(existingUsers.rows[0].count) > 0) {
      return res.json({
        success: true,
        message: 'Users already exist in database',
        data: { userCount: existingUsers.rows[0].count }
      });
    }

    // Hash password for all test users
    const hashedPassword = await hashPassword('password');

    // Insert sample schools first
    const schoolsResult = await query(`
      INSERT INTO schools (id, name, address, phone_number, email, principal_name, established_date, school_type, latitude, longitude) 
      VALUES 
      ($1, 'Royal University of Phnom Penh', 'Russian Federation Blvd, Phnom Penh, Cambodia', '+855-23-880-009', 'info@rupp.edu.kh', 'Dr. Chet Chealy', '1960-01-01', 'university', 11.5564, 104.9282),
      ($2, 'Hun Sen High School', 'Monivong Blvd, Phnom Penh, Cambodia', '+855-23-123-456', 'info@hunsen.edu.kh', 'Mr. Sok Panha', '1985-09-15', 'high_school', 11.5449, 104.8922),
      ($3, 'International School of Phnom Penh', 'Street 95, Phnom Penh, Cambodia', '+855-23-213-103', 'admin@ispp.edu.kh', 'Ms. Sarah Johnson', '1995-08-20', 'international', 11.5625, 104.9010)
      ON CONFLICT (id) DO NOTHING
      RETURNING id
    `, [uuidv4(), uuidv4(), uuidv4()]);

    // Get school IDs
    const schoolIds = schoolsResult.rows.map(row => row.id);
    const ruppId = schoolIds[0];
    const hunsenId = schoolIds[1];
    const isppId = schoolIds[2];

    // Insert sample users
    const testUsers = [
      {
        id: uuidv4(),
        username: 'admin',
        email: 'admin@plp.edu.kh',
        firstName: 'System',
        lastName: 'Administrator',
        role: 'admin',
        schoolId: ruppId,
        department: 'IT Department',
        employeeId: 'ADM001'
      },
      {
        id: uuidv4(),
        username: 'teacher1',
        email: 'teacher1@rupp.edu.kh',
        firstName: 'Sophea',
        lastName: 'Chan',
        role: 'teacher',
        schoolId: ruppId,
        department: 'Computer Science',
        employeeId: 'TCH001'
      },
      {
        id: uuidv4(),
        username: 'teacher2',
        email: 'teacher2@rupp.edu.kh',
        firstName: 'Dara',
        lastName: 'Kim',
        role: 'teacher',
        schoolId: ruppId,
        department: 'Mathematics',
        employeeId: 'TCH002'
      },
      {
        id: uuidv4(),
        username: 'teacher3',
        email: 'teacher3@hunsen.edu.kh',
        firstName: 'Maly',
        lastName: 'Pov',
        role: 'teacher',
        schoolId: hunsenId,
        department: 'English',
        employeeId: 'TCH003'
      },
      {
        id: uuidv4(),
        username: 'student1',
        email: 'student1@rupp.edu.kh',
        firstName: 'Pisach',
        lastName: 'Leng',
        role: 'student',
        schoolId: ruppId,
        department: 'Computer Science',
        employeeId: 'STU001'
      },
      {
        id: uuidv4(),
        username: 'student2',
        email: 'student2@rupp.edu.kh',
        firstName: 'Sreynich',
        lastName: 'Hout',
        role: 'student',
        schoolId: ruppId,
        department: 'Mathematics',
        employeeId: 'STU002'
      },
      {
        id: uuidv4(),
        username: 'staff1',
        email: 'staff1@rupp.edu.kh',
        firstName: 'Chantha',
        lastName: 'Ngoun',
        role: 'staff',
        schoolId: ruppId,
        department: 'Administration',
        employeeId: 'STF001'
      }
    ];

    // Insert users
    for (const user of testUsers) {
      await query(`
        INSERT INTO users (id, username, email, password_hash, first_name, last_name, role, school_id, department, employee_id, email_verified, is_active)
        VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)
        ON CONFLICT (username) DO NOTHING
      `, [
        user.id,
        user.username,
        user.email,
        hashedPassword,
        user.firstName,
        user.lastName,
        user.role,
        user.schoolId,
        user.department,
        user.employeeId,
        user.role === 'admin' || user.role === 'teacher', // Email verified for admins and teachers
        true // is_active
      ]);
    }

    // Count final users
    const finalCount = await query('SELECT COUNT(*) as count FROM users');
    
    logger.info('Database seeded successfully', {
      usersCreated: parseInt(finalCount.rows[0].count),
      schoolsCreated: schoolIds.length
    });

    res.json({
      success: true,
      message: 'Database seeded successfully',
      data: {
        usersCreated: parseInt(finalCount.rows[0].count),
        schoolsCreated: schoolIds.length,
        testCredentials: testUsers.map(user => ({
          username: user.username,
          email: user.email,
          password: 'password', // All test users have password 'password'
          role: user.role,
          name: `${user.firstName} ${user.lastName}`
        }))
      }
    });

  } catch (error) {
    logger.error('Database seeding failed:', error);
    res.status(500).json({
      success: false,
      error: {
        message: 'Database seeding failed',
        details: error.message
      }
    });
  }
});

/**
 * @swagger
 * /api/seed/status:
 *   get:
 *     summary: Check database seed status
 *     tags: [Development]
 *     responses:
 *       200:
 *         description: Database status retrieved successfully
 */
router.get('/status', async (req, res) => {
  try {
    const [userCount, schoolCount] = await Promise.all([
      query('SELECT COUNT(*) as count FROM users'),
      query('SELECT COUNT(*) as count FROM schools')
    ]);

    res.json({
      success: true,
      data: {
        users: parseInt(userCount.rows[0].count),
        schools: parseInt(schoolCount.rows[0].count),
        seeded: parseInt(userCount.rows[0].count) > 0
      }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      error: {
        message: 'Failed to check database status',
        details: error.message
      }
    });
  }
});

module.exports = router;