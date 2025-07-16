const express = require('express');
const { body, query: queryValidator, validationResult } = require('express-validator');
const { v4: uuidv4 } = require('uuid');
const geolib = require('geolib');

const { query, transaction } = require('../config/database');
const { authenticate } = require('../middleware/auth');
const { asyncHandler, createError } = require('../middleware/errorHandler');
const logger = require('../utils/logger');

const router = express.Router();

// Apply authentication to all routes
router.use(authenticate);

// Validation rules
const checkInValidation = [
  body('latitude')
    .isFloat({ min: -90, max: 90 })
    .withMessage('Valid latitude is required'),
  body('longitude')
    .isFloat({ min: -180, max: 180 })
    .withMessage('Valid longitude is required'),
  body('address')
    .optional()
    .isLength({ max: 500 })
    .withMessage('Address too long'),
  body('notes')
    .optional()
    .isLength({ max: 1000 })
    .withMessage('Notes too long')
];

const checkOutValidation = [
  body('latitude')
    .isFloat({ min: -90, max: 90 })
    .withMessage('Valid latitude is required'),
  body('longitude')
    .isFloat({ min: -180, max: 180 })
    .withMessage('Valid longitude is required'),
  body('address')
    .optional()
    .isLength({ max: 500 })
    .withMessage('Address too long'),
  body('notes')
    .optional()
    .isLength({ max: 1000 })
    .withMessage('Notes too long')
];

/**
 * Helper function to validate location within geofence
 */
async function validateGeofence (latitude, longitude, schoolId) {
  const schoolResult = await query(
    'SELECT latitude, longitude FROM schools WHERE id = $1',
    [schoolId]
  );

  if (schoolResult.rows.length === 0) {
    throw createError(400, 'School not found');
  }

  const school = schoolResult.rows[0];

  if (!school.latitude || !school.longitude) {
    // If school doesn't have coordinates, skip geofence validation
    return true;
  }

  const distance = geolib.getDistance(
    { latitude, longitude },
    { latitude: school.latitude, longitude: school.longitude }
  );

  const maxDistance = parseInt(process.env.GEOFENCE_RADIUS_METERS) || 100;

  return distance <= maxDistance;
}

/**
 * Helper function to calculate working hours
 */
function calculateWorkingHours (checkInTime, checkOutTime) {
  if (!checkInTime || !checkOutTime) return 0;

  const diffMs = new Date(checkOutTime) - new Date(checkInTime);
  return Math.max(0, diffMs / (1000 * 60 * 60)); // Convert to hours
}

/**
 * @swagger
 * /api/attendance/check-in:
 *   post:
 *     summary: Check in for attendance
 *     tags: [Attendance]
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required:
 *               - latitude
 *               - longitude
 *             properties:
 *               latitude:
 *                 type: number
 *                 format: double
 *               longitude:
 *                 type: number
 *                 format: double
 *               address:
 *                 type: string
 *               notes:
 *                 type: string
 *     responses:
 *       201:
 *         description: Check-in successful
 *       400:
 *         description: Validation error or location outside geofence
 *       409:
 *         description: Already checked in today
 */
router.post('/check-in', checkInValidation, asyncHandler(async (req, res) => {
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

  const { latitude, longitude, address, notes } = req.body;
  const userId = req.user.id;
  const schoolId = req.user.schoolId;

  await transaction(async (client) => {
    // Check if user already checked in today
    const today = new Date().toISOString().split('T')[0];
    const existingRecord = await client.query(
      `SELECT id, check_in_time FROM attendance_records 
       WHERE user_id = $1 AND DATE(check_in_time) = $2`,
      [userId, today]
    );

    if (existingRecord.rows.length > 0) {
      throw createError(409, 'Already checked in today');
    }

    // Validate geofence
    const isWithinGeofence = await validateGeofence(latitude, longitude, schoolId);
    if (!isWithinGeofence) {
      logger.logSecurity('Geofence Violation', {
        userId,
        schoolId,
        latitude,
        longitude,
        ip: req.ip
      });
      throw createError(400, 'Location is outside the allowed area');
    }

    // Determine status based on time
    const now = new Date();
    const schoolSettings = await client.query(
      `SELECT setting_value FROM system_settings 
       WHERE school_id = $1 AND setting_key = 'working_hours_start'`,
      [schoolId]
    );

    let status = 'present';
    if (schoolSettings.rows.length > 0) {
      const workStartTime = JSON.parse(schoolSettings.rows[0].setting_value);
      const [startHour, startMinute] = workStartTime.split(':');

      const todayStart = new Date();
      todayStart.setHours(parseInt(startHour), parseInt(startMinute), 0, 0);

      // Check late threshold
      const lateThreshold = await client.query(
        `SELECT setting_value FROM system_settings 
         WHERE school_id = $1 AND setting_key = 'late_threshold_minutes'`,
        [schoolId]
      );

      const thresholdMinutes = lateThreshold.rows.length > 0
        ? parseInt(lateThreshold.rows[0].setting_value)
        : 15;

      const lateTime = new Date(todayStart.getTime() + thresholdMinutes * 60000);

      if (now > lateTime) {
        status = 'late';
      }
    }

    // Create attendance record
    const attendanceResult = await client.query(
      `INSERT INTO attendance_records (
        id, user_id, school_id, check_in_time, check_in_latitude, check_in_longitude,
        check_in_address, status, notes
      ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
      RETURNING *`,
      [
        uuidv4(),
        userId,
        schoolId,
        now,
        latitude,
        longitude,
        address,
        status,
        notes
      ]
    );

    const record = attendanceResult.rows[0];

    // Log attendance
    logger.info('Attendance Check-in', {
      userId,
      recordId: record.id,
      status,
      location: { latitude, longitude },
      timestamp: now
    });

    res.status(201).json({
      success: true,
      message: 'Check-in successful',
      data: {
        record: {
          id: record.id,
          checkInTime: record.check_in_time,
          status: record.status,
          latitude: record.check_in_latitude,
          longitude: record.check_in_longitude,
          address: record.check_in_address,
          notes: record.notes
        }
      }
    });
  });
}));

/**
 * @swagger
 * /api/attendance/check-out:
 *   post:
 *     summary: Check out for attendance
 *     tags: [Attendance]
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required:
 *               - latitude
 *               - longitude
 *             properties:
 *               latitude:
 *                 type: number
 *                 format: double
 *               longitude:
 *                 type: number
 *                 format: double
 *               address:
 *                 type: string
 *               notes:
 *                 type: string
 *     responses:
 *       200:
 *         description: Check-out successful
 *       400:
 *         description: No check-in found for today
 */
router.post('/check-out', checkOutValidation, asyncHandler(async (req, res) => {
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

  const { latitude, longitude, address, notes } = req.body;
  const userId = req.user.id;
  const schoolId = req.user.schoolId;

  await transaction(async (client) => {
    // Find today's attendance record
    const today = new Date().toISOString().split('T')[0];
    const recordResult = await client.query(
      `SELECT * FROM attendance_records 
       WHERE user_id = $1 AND DATE(check_in_time) = $2 AND check_out_time IS NULL`,
      [userId, today]
    );

    if (recordResult.rows.length === 0) {
      throw createError(400, 'No check-in found for today or already checked out');
    }

    const record = recordResult.rows[0];
    const now = new Date();

    // Validate geofence
    const isWithinGeofence = await validateGeofence(latitude, longitude, schoolId);
    if (!isWithinGeofence) {
      logger.logSecurity('Geofence Violation - Check Out', {
        userId,
        schoolId,
        latitude,
        longitude,
        ip: req.ip
      });
      throw createError(400, 'Location is outside the allowed area');
    }

    // Calculate working hours
    const workingHours = calculateWorkingHours(record.check_in_time, now);

    // Determine if early departure
    let status = record.status;
    const schoolSettings = await client.query(
      `SELECT setting_value FROM system_settings 
       WHERE school_id = $1 AND setting_key = 'working_hours_end'`,
      [schoolId]
    );

    if (schoolSettings.rows.length > 0) {
      const workEndTime = JSON.parse(schoolSettings.rows[0].setting_value);
      const [endHour, endMinute] = workEndTime.split(':');

      const todayEnd = new Date();
      todayEnd.setHours(parseInt(endHour), parseInt(endMinute), 0, 0);

      if (now < todayEnd && workingHours < 6) { // Assuming 6 hours minimum
        status = 'early_departure';
      }
    }

    // Update attendance record
    const updatedRecord = await client.query(
      `UPDATE attendance_records SET
        check_out_time = $1,
        check_out_latitude = $2,
        check_out_longitude = $3,
        check_out_address = $4,
        working_hours = $5,
        status = $6,
        notes = COALESCE(notes, '') || CASE WHEN $7 IS NOT NULL THEN '\n' || $7 ELSE '' END,
        updated_at = CURRENT_TIMESTAMP
       WHERE id = $8
       RETURNING *`,
      [
        now,
        latitude,
        longitude,
        address,
        workingHours,
        status,
        notes,
        record.id
      ]
    );

    const finalRecord = updatedRecord.rows[0];

    // Log attendance
    logger.info('Attendance Check-out', {
      userId,
      recordId: record.id,
      workingHours,
      status,
      location: { latitude, longitude },
      timestamp: now
    });

    res.json({
      success: true,
      message: 'Check-out successful',
      data: {
        record: {
          id: finalRecord.id,
          checkInTime: finalRecord.check_in_time,
          checkOutTime: finalRecord.check_out_time,
          workingHours: finalRecord.working_hours,
          status: finalRecord.status,
          checkOutLatitude: finalRecord.check_out_latitude,
          checkOutLongitude: finalRecord.check_out_longitude,
          checkOutAddress: finalRecord.check_out_address,
          notes: finalRecord.notes
        }
      }
    });
  });
}));

/**
 * @swagger
 * /api/attendance/status:
 *   get:
 *     summary: Get current attendance status
 *     tags: [Attendance]
 *     security:
 *       - bearerAuth: []
 *     responses:
 *       200:
 *         description: Current attendance status
 */
router.get('/status', asyncHandler(async (req, res) => {
  const userId = req.user.id;
  const today = new Date().toISOString().split('T')[0];

  const recordResult = await query(
    `SELECT * FROM attendance_records 
     WHERE user_id = $1 AND DATE(check_in_time) = $2
     ORDER BY check_in_time DESC LIMIT 1`,
    [userId, today]
  );

  let status = {
    hasCheckedIn: false,
    hasCheckedOut: false,
    canCheckIn: true,
    canCheckOut: false,
    workingHours: 0,
    record: null
  };

  if (recordResult.rows.length > 0) {
    const record = recordResult.rows[0];
    status = {
      hasCheckedIn: true,
      hasCheckedOut: !!record.check_out_time,
      canCheckIn: false,
      canCheckOut: !record.check_out_time,
      workingHours: record.working_hours || 0,
      record: {
        id: record.id,
        checkInTime: record.check_in_time,
        checkOutTime: record.check_out_time,
        status: record.status,
        workingHours: record.working_hours,
        notes: record.notes
      }
    };
  }

  res.json({
    success: true,
    data: { status }
  });
}));

/**
 * @swagger
 * /api/attendance/records:
 *   get:
 *     summary: Get attendance records
 *     tags: [Attendance]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: query
 *         name: startDate
 *         schema:
 *           type: string
 *           format: date
 *         description: Start date for records
 *       - in: query
 *         name: endDate
 *         schema:
 *           type: string
 *           format: date
 *         description: End date for records
 *       - in: query
 *         name: status
 *         schema:
 *           type: string
 *           enum: [present, absent, late, early_departure, on_leave]
 *         description: Filter by status
 *       - in: query
 *         name: page
 *         schema:
 *           type: integer
 *           minimum: 1
 *         description: Page number
 *       - in: query
 *         name: limit
 *         schema:
 *           type: integer
 *           minimum: 1
 *           maximum: 100
 *         description: Records per page
 *     responses:
 *       200:
 *         description: Attendance records retrieved successfully
 */
router.get('/records', [
  queryValidator('startDate').optional().isDate().withMessage('Invalid start date'),
  queryValidator('endDate').optional().isDate().withMessage('Invalid end date'),
  queryValidator('status').optional().isIn([
    'present', 'absent', 'late', 'early_departure', 'on_leave'
  ]),
  queryValidator('page').optional().isInt({ min: 1 })
    .withMessage('Page must be a positive integer'),
  queryValidator('limit').optional().isInt({ min: 1, max: 100 })
    .withMessage('Limit must be between 1 and 100')
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

  const {
    startDate,
    endDate,
    status,
    page = 1,
    limit = 20
  } = req.query;

  const userId = req.user.id;
  const offset = (page - 1) * limit;

  // Build query conditions
  const whereConditions = ['user_id = $1'];
  const queryParams = [userId];
  let paramIndex = 2;

  if (startDate) {
    whereConditions.push(`DATE(check_in_time) >= $${paramIndex}`);
    queryParams.push(startDate);
    paramIndex++;
  }

  if (endDate) {
    whereConditions.push(`DATE(check_in_time) <= $${paramIndex}`);
    queryParams.push(endDate);
    paramIndex++;
  }

  if (status) {
    whereConditions.push(`status = $${paramIndex}`);
    queryParams.push(status);
    paramIndex++;
  }

  const whereClause = whereConditions.join(' AND ');

  // Get total count
  const countResult = await query(
    `SELECT COUNT(*) as total FROM attendance_records WHERE ${whereClause}`,
    queryParams
  );

  const total = parseInt(countResult.rows[0].total);

  // Get records
  const recordsResult = await query(
    `SELECT ar.*, u.first_name, u.last_name, u.employee_id
     FROM attendance_records ar
     JOIN users u ON ar.user_id = u.id
     WHERE ${whereClause}
     ORDER BY ar.check_in_time DESC
     LIMIT $${paramIndex} OFFSET $${paramIndex + 1}`,
    [...queryParams, limit, offset]
  );

  const records = recordsResult.rows.map(record => ({
    id: record.id,
    userId: record.user_id,
    userName: `${record.first_name} ${record.last_name}`,
    employeeId: record.employee_id,
    checkInTime: record.check_in_time,
    checkOutTime: record.check_out_time,
    workingHours: record.working_hours,
    status: record.status,
    notes: record.notes,
    checkInLocation: {
      latitude: record.check_in_latitude,
      longitude: record.check_in_longitude,
      address: record.check_in_address
    },
    checkOutLocation: record.check_out_latitude
      ? {
        latitude: record.check_out_latitude,
        longitude: record.check_out_longitude,
        address: record.check_out_address
      }
      : null,
    createdAt: record.created_at,
    updatedAt: record.updated_at
  }));

  res.json({
    success: true,
    data: {
      records,
      pagination: {
        currentPage: parseInt(page),
        totalPages: Math.ceil(total / limit),
        totalRecords: total,
        recordsPerPage: parseInt(limit),
        hasNextPage: page * limit < total,
        hasPreviousPage: page > 1
      }
    }
  });
}));

/**
 * @swagger
 * /api/attendance/summary:
 *   get:
 *     summary: Get attendance summary statistics
 *     tags: [Attendance]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: query
 *         name: period
 *         schema:
 *           type: string
 *           enum: [week, month, year]
 *         description: Summary period
 *     responses:
 *       200:
 *         description: Attendance summary retrieved successfully
 */
router.get('/summary', [
  queryValidator('period').optional().isIn(['week', 'month', 'year']).withMessage('Invalid period')
], asyncHandler(async (req, res) => {
  const { period = 'month' } = req.query;
  const userId = req.user.id;

  let dateCondition;
  switch (period) {
  case 'week':
    dateCondition = 'DATE(check_in_time) >= DATE_TRUNC(\'week\', CURRENT_DATE)';
    break;
  case 'year':
    dateCondition = 'DATE(check_in_time) >= DATE_TRUNC(\'year\', CURRENT_DATE)';
    break;
  default: // month
    dateCondition = 'DATE(check_in_time) >= DATE_TRUNC(\'month\', CURRENT_DATE)';
  }

  const summaryResult = await query(
    `SELECT 
      COUNT(*) as total_days,
      COUNT(CASE WHEN status = 'present' THEN 1 END) as present_days,
      COUNT(CASE WHEN status = 'late' THEN 1 END) as late_days,
      COUNT(CASE WHEN status = 'early_departure' THEN 1 END) as early_departure_days,
      COUNT(CASE WHEN status = 'absent' THEN 1 END) as absent_days,
      COALESCE(SUM(working_hours), 0) as total_working_hours,
      COALESCE(AVG(working_hours), 0) as average_working_hours,
      MAX(working_hours) as max_working_hours,
      MIN(working_hours) as min_working_hours
     FROM attendance_records 
     WHERE user_id = $1 AND ${dateCondition}`,
    [userId]
  );

  const summary = summaryResult.rows[0];

  // Calculate attendance rate
  const attendanceRate = summary.total_days > 0
    ? ((summary.present_days + summary.late_days) / summary.total_days * 100).toFixed(1)
    : 0;

  res.json({
    success: true,
    data: {
      summary: {
        period,
        totalDays: parseInt(summary.total_days),
        presentDays: parseInt(summary.present_days),
        lateDays: parseInt(summary.late_days),
        earlyDepartureDays: parseInt(summary.early_departure_days),
        absentDays: parseInt(summary.absent_days),
        attendanceRate: parseFloat(attendanceRate),
        totalWorkingHours: parseFloat(summary.total_working_hours),
        averageWorkingHours: parseFloat(summary.average_working_hours),
        maxWorkingHours: parseFloat(summary.max_working_hours),
        minWorkingHours: parseFloat(summary.min_working_hours)
      }
    }
  });
}));

module.exports = router;
