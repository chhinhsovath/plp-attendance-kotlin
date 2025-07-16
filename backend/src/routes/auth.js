const express = require('express');
const rateLimit = require('express-rate-limit');
const { body, validationResult } = require('express-validator');
const { v4: uuidv4 } = require('uuid');

const { query, transaction } = require('../config/database');
const {
  hashPassword,
  comparePassword,
  generateTokenPair,
  refreshAccessToken,
  revokeRefreshToken,
  authenticate
} = require('../middleware/auth');
const { asyncHandler, createError } = require('../middleware/errorHandler');
const logger = require('../utils/logger');

const router = express.Router();

// Rate limiting for auth endpoints
const authLimiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 1000, // Increased to 1000 attempts per IP per window for development
  message: 'Too many authentication attempts, please try again later',
  standardHeaders: true,
  legacyHeaders: false
});

// Validation rules
const registerValidation = [
  body('username')
    .isLength({ min: 3, max: 50 })
    .withMessage('Username must be between 3 and 50 characters')
    .matches(/^[a-zA-Z0-9_]+$/)
    .withMessage('Username can only contain letters, numbers, and underscores'),
  body('email')
    .isEmail()
    .normalizeEmail()
    .withMessage('Valid email is required'),
  body('password')
    .isLength({ min: 8 })
    .withMessage('Password must be at least 8 characters')
    .matches(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]/)
    .withMessage('Password must contain uppercase, lowercase, number, and special character'),
  body('firstName')
    .isLength({ min: 1, max: 100 })
    .withMessage('First name is required'),
  body('lastName')
    .isLength({ min: 1, max: 100 })
    .withMessage('Last name is required'),
  body('phoneNumber')
    .optional()
    .matches(/^(\+855|0)[0-9]{8,9}$/)
    .withMessage('Invalid Cambodia phone number format'),
  body('role')
    .isIn(['admin', 'teacher', 'student', 'staff'])
    .withMessage('Invalid role'),
  body('schoolId')
    .isUUID()
    .withMessage('Valid school ID is required')
];

const loginValidation = [
  body('identifier')
    .notEmpty()
    .withMessage('Email or username is required'),
  body('password')
    .notEmpty()
    .withMessage('Password is required')
];

/**
 * @swagger
 * /api/auth/register:
 *   post:
 *     summary: Register a new user
 *     tags: [Authentication]
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required:
 *               - username
 *               - email
 *               - password
 *               - firstName
 *               - lastName
 *               - role
 *               - schoolId
 *             properties:
 *               username:
 *                 type: string
 *               email:
 *                 type: string
 *                 format: email
 *               password:
 *                 type: string
 *                 minLength: 8
 *               firstName:
 *                 type: string
 *               lastName:
 *                 type: string
 *               phoneNumber:
 *                 type: string
 *               role:
 *                 type: string
 *                 enum: [admin, teacher, student, staff]
 *               schoolId:
 *                 type: string
 *                 format: uuid
 *               department:
 *                 type: string
 *               employeeId:
 *                 type: string
 *     responses:
 *       201:
 *         description: User registered successfully
 *       400:
 *         description: Validation error
 *       409:
 *         description: User already exists
 */
router.post('/register', authLimiter, registerValidation, asyncHandler(async (req, res) => {
  // Check validation errors
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    return res.status(400).json({
      success: false,
      error: {
        message: 'Validation failed',
        details: errors.array()
      }
    });
  }

  const {
    username,
    email,
    password,
    firstName,
    lastName,
    phoneNumber,
    role,
    schoolId,
    department,
    employeeId
  } = req.body;

  await transaction(async (client) => {
    // Check if school exists
    const schoolResult = await client.query(
      'SELECT id FROM schools WHERE id = $1 AND is_active = true',
      [schoolId]
    );

    if (schoolResult.rows.length === 0) {
      throw createError(400, 'Invalid school ID');
    }

    // Check if user already exists
    const existingUser = await client.query(
      'SELECT id FROM users WHERE username = $1 OR email = $2',
      [username, email]
    );

    if (existingUser.rows.length > 0) {
      throw createError(409, 'Username or email already exists');
    }

    // Hash password
    const passwordHash = await hashPassword(password);

    // Create user
    const userResult = await client.query(
      `INSERT INTO users (
        id, username, email, password_hash, first_name, last_name,
        phone_number, role, school_id, department, employee_id
      ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11)
      RETURNING id, username, email, first_name, last_name, role, school_id, created_at`,
      [
        uuidv4(),
        username,
        email,
        passwordHash,
        firstName,
        lastName,
        phoneNumber,
        role,
        schoolId,
        department,
        employeeId
      ]
    );

    const user = userResult.rows[0];

    // Log registration
    logger.logAuth('User Registered', user.id, req.ip, req.get('User-Agent'), true);

    res.status(201).json({
      success: true,
      message: 'User registered successfully',
      data: {
        user: {
          id: user.id,
          username: user.username,
          email: user.email,
          firstName: user.first_name,
          lastName: user.last_name,
          role: user.role,
          schoolId: user.school_id,
          createdAt: user.created_at
        }
      }
    });
  });
}));

/**
 * @swagger
 * /api/auth/login:
 *   post:
 *     summary: Login user
 *     tags: [Authentication]
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required:
 *               - identifier
 *               - password
 *             properties:
 *               identifier:
 *                 type: string
 *                 description: Email or username
 *               password:
 *                 type: string
 *               deviceInfo:
 *                 type: object
 *                 properties:
 *                   deviceName:
 *                     type: string
 *                   deviceType:
 *                     type: string
 *                   appVersion:
 *                     type: string
 *     responses:
 *       200:
 *         description: Login successful
 *       401:
 *         description: Invalid credentials
 */
router.post('/login', authLimiter, loginValidation, asyncHandler(async (req, res) => {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    return res.status(400).json({
      success: false,
      error: {
        message: 'Validation failed',
        details: errors.array()
      }
    });
  }

  const { identifier, password, deviceInfo = {} } = req.body;

  // Find user by email or username
  const userResult = await query(
    `SELECT u.*, s.name as school_name, s.timezone as school_timezone, 
            s.latitude as school_latitude, s.longitude as school_longitude
     FROM users u 
     LEFT JOIN schools s ON u.school_id = s.id 
     WHERE (u.email = $1 OR u.username = $1) AND u.is_active = true`,
    [identifier]
  );

  if (userResult.rows.length === 0) {
    logger.logAuth('Login Failed - User Not Found', null, req.ip, req.get('User-Agent'), false);
    throw createError(401, 'Invalid credentials');
  }

  const user = userResult.rows[0];

  // Compare password
  const isPasswordValid = await comparePassword(password, user.password_hash);
  if (!isPasswordValid) {
    logger.logAuth('Login Failed - Invalid Password', user.id, req.ip,
      req.get('User-Agent'), false);
    throw createError(401, 'Invalid credentials');
  }

  // Generate tokens
  const deviceData = {
    ...deviceInfo,
    ip: req.ip,
    userAgent: req.get('User-Agent')
  };

  const { accessToken, refreshToken } = await generateTokenPair(user, deviceData);

  // Update last login
  await query(
    'UPDATE users SET last_login_at = CURRENT_TIMESTAMP WHERE id = $1',
    [user.id]
  );

  logger.logAuth('Login Successful', user.id, req.ip, req.get('User-Agent'), true);

  res.json({
    success: true,
    message: 'Login successful',
    data: {
      user: {
        id: user.id,
        username: user.username,
        email: user.email,
        firstName: user.first_name,
        lastName: user.last_name,
        role: user.role,
        schoolId: user.school_id,
        schoolName: user.school_name,
        schoolLatitude: user.school_latitude,
        schoolLongitude: user.school_longitude,
        department: user.department,
        employeeId: user.employee_id,
        emailVerified: user.email_verified,
        phoneVerified: user.phone_verified,
        lastLoginAt: user.last_login_at
      },
      tokens: {
        accessToken,
        refreshToken,
        expiresIn: process.env.JWT_EXPIRES_IN || '24h'
      }
    }
  });
}));

/**
 * @swagger
 * /api/auth/refresh:
 *   post:
 *     summary: Refresh access token
 *     tags: [Authentication]
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required:
 *               - refreshToken
 *             properties:
 *               refreshToken:
 *                 type: string
 *     responses:
 *       200:
 *         description: Token refreshed successfully
 *       401:
 *         description: Invalid refresh token
 */
router.post('/refresh', asyncHandler(async (req, res) => {
  const { refreshToken } = req.body;

  if (!refreshToken) {
    throw createError(400, 'Refresh token is required');
  }

  const deviceInfo = {
    ip: req.ip,
    userAgent: req.get('User-Agent')
  };

  const { accessToken } = await refreshAccessToken(refreshToken, deviceInfo);

  res.json({
    success: true,
    message: 'Token refreshed successfully',
    data: {
      accessToken,
      expiresIn: process.env.JWT_EXPIRES_IN || '24h'
    }
  });
}));

/**
 * @swagger
 * /api/auth/logout:
 *   post:
 *     summary: Logout user (revoke refresh token)
 *     tags: [Authentication]
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             properties:
 *               refreshToken:
 *                 type: string
 *               logoutAll:
 *                 type: boolean
 *                 description: Logout from all devices
 *     responses:
 *       200:
 *         description: Logout successful
 */
router.post('/logout', authenticate, asyncHandler(async (req, res) => {
  const { refreshToken, logoutAll = false } = req.body;

  if (logoutAll) {
    // Revoke all refresh tokens for the user
    await revokeRefreshToken(req.user.id);
    logger.logAuth('Logout All Devices', req.user.id, req.ip, req.get('User-Agent'), true);
  } else if (refreshToken) {
    // Revoke specific refresh token
    const tokenHash = await hashPassword(refreshToken);
    await revokeRefreshToken(req.user.id, tokenHash);
    logger.logAuth('Logout Single Device', req.user.id, req.ip, req.get('User-Agent'), true);
  }

  res.json({
    success: true,
    message: 'Logout successful'
  });
}));

/**
 * @swagger
 * /api/auth/profile:
 *   get:
 *     summary: Get current user profile
 *     tags: [Authentication]
 *     security:
 *       - bearerAuth: []
 *     responses:
 *       200:
 *         description: User profile retrieved successfully
 */
router.get('/profile', authenticate, asyncHandler(async (req, res) => {
  const userResult = await query(
    `SELECT u.*, s.name as school_name, s.address as school_address,
            s.latitude as school_latitude, s.longitude as school_longitude
     FROM users u 
     LEFT JOIN schools s ON u.school_id = s.id 
     WHERE u.id = $1`,
    [req.user.id]
  );

  const user = userResult.rows[0];

  res.json({
    success: true,
    data: {
      user: {
        id: user.id,
        username: user.username,
        email: user.email,
        firstName: user.first_name,
        lastName: user.last_name,
        phoneNumber: user.phone_number,
        role: user.role,
        schoolId: user.school_id,
        schoolName: user.school_name,
        schoolAddress: user.school_address,
        department: user.department,
        employeeId: user.employee_id,
        profileImageUrl: user.profile_image_url,
        isActive: user.is_active,
        emailVerified: user.email_verified,
        phoneVerified: user.phone_verified,
        lastLoginAt: user.last_login_at,
        createdAt: user.created_at,
        updatedAt: user.updated_at
      }
    }
  });
}));

/**
 * @swagger
 * /api/auth/change-password:
 *   post:
 *     summary: Change user password
 *     tags: [Authentication]
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required:
 *               - currentPassword
 *               - newPassword
 *             properties:
 *               currentPassword:
 *                 type: string
 *               newPassword:
 *                 type: string
 *                 minLength: 8
 *     responses:
 *       200:
 *         description: Password changed successfully
 *       400:
 *         description: Invalid current password
 */
router.get('/me', authenticate, asyncHandler(async (req, res) => {
  // Get full user information including school details
  const userResult = await query(
    `SELECT u.*, 
            s.name as school_name,
            s.latitude as school_latitude,
            s.longitude as school_longitude,
            s.address as school_address
     FROM users u
     LEFT JOIN schools s ON u.school_id = s.id
     WHERE u.id = $1`,
    [req.user.id]
  );

  if (userResult.rows.length === 0) {
    throw createError(404, 'User not found');
  }

  const user = userResult.rows[0];

  res.json({
    id: user.id,
    username: user.username,
    email: user.email,
    firstName: user.first_name,
    lastName: user.last_name,
    role: user.role,
    organizationId: user.school_id,
    schoolId: user.school_id,
    schoolName: user.school_name,
    schoolLatitude: user.school_latitude,
    schoolLongitude: user.school_longitude,
    schoolAddress: user.school_address,
    department: user.department,
    employeeId: user.employee_id,
    isActive: user.is_active,
    emailVerified: user.email_verified,
    phoneVerified: user.phone_verified,
    lastLoginAt: user.last_login_at
  });
}));

router.post('/change-password', authenticate, [
  body('currentPassword').notEmpty().withMessage('Current password is required'),
  body('newPassword')
    .isLength({ min: 8 })
    .withMessage('New password must be at least 8 characters')
    .matches(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]/)
    .withMessage('New password must contain uppercase, lowercase, number, and special character')
], asyncHandler(async (req, res) => {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    return res.status(400).json({
      success: false,
      error: {
        message: 'Validation failed',
        details: errors.array()
      }
    });
  }

  const { currentPassword, newPassword } = req.body;

  // Get current password hash
  const userResult = await query(
    'SELECT password_hash FROM users WHERE id = $1',
    [req.user.id]
  );

  const user = userResult.rows[0];

  // Verify current password
  const isCurrentPasswordValid = await comparePassword(currentPassword, user.password_hash);
  if (!isCurrentPasswordValid) {
    throw createError(400, 'Current password is incorrect');
  }

  // Hash new password
  const newPasswordHash = await hashPassword(newPassword);

  // Update password
  await query(
    'UPDATE users SET password_hash = $1, updated_at = CURRENT_TIMESTAMP WHERE id = $2',
    [newPasswordHash, req.user.id]
  );

  // Revoke all refresh tokens to force re-login on all devices
  await revokeRefreshToken(req.user.id);

  logger.logAuth('Password Changed', req.user.id, req.ip, req.get('User-Agent'), true);

  res.json({
    success: true,
    message: 'Password changed successfully. Please log in again on all devices.'
  });
}));

module.exports = router;
