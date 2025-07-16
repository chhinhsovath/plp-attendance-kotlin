const express = require('express');
const { body, query: queryValidator, validationResult } = require('express-validator');
const { v4: uuidv4 } = require('uuid');

const { query, transaction } = require('../config/database');
const { authenticate, authorize } = require('../middleware/auth');
const { asyncHandler, createError } = require('../middleware/errorHandler');
const logger = require('../utils/logger');

const router = express.Router();

// Apply authentication to all routes
router.use(authenticate);

// Validation rules
const leaveRequestValidation = [
  body('leaveType')
    .isIn(['sick', 'personal', 'emergency', 'maternity', 'paternity', 'bereavement', 'study'])
    .withMessage('Invalid leave type'),
  body('startDate')
    .isISO8601()
    .withMessage('Valid start date is required'),
  body('endDate')
    .isISO8601()
    .withMessage('Valid end date is required')
    .custom((endDate, { req }) => {
      return new Date(endDate) >= new Date(req.body.startDate);
    })
    .withMessage('End date must be after or equal to start date'),
  body('reason')
    .isLength({ min: 5, max: 1000 })
    .withMessage('Reason must be between 5 and 1000 characters')
];

const approvalValidation = [
  body('action')
    .isIn(['approve', 'reject'])
    .withMessage('Action must be approve or reject'),
  body('comment')
    .optional()
    .isLength({ max: 500 })
    .withMessage('Comment too long')
];

/**
 * @swagger
 * /api/leave/request:
 *   post:
 *     summary: Submit a new leave request
 *     tags: [Leave Management]
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required:
 *               - leaveType
 *               - startDate
 *               - endDate
 *               - reason
 *             properties:
 *               leaveType:
 *                 type: string
 *                 enum: [sick, personal, emergency, maternity, paternity, bereavement, study]
 *               startDate:
 *                 type: string
 *                 format: date
 *               endDate:
 *                 type: string
 *                 format: date
 *               reason:
 *                 type: string
 *     responses:
 *       201:
 *         description: Leave request submitted successfully
 *       400:
 *         description: Validation error
 */
router.post('/request', leaveRequestValidation, asyncHandler(async (req, res) => {
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

  const { leaveType, startDate, endDate, reason } = req.body;
  const userId = req.user.id;

  await transaction(async (client) => {
    // Calculate leave days
    const start = new Date(startDate);
    const end = new Date(endDate);
    const days = Math.floor((end - start) / (1000 * 60 * 60 * 24)) + 1;

    // Create leave request
    const leaveResult = await client.query(
      `INSERT INTO leave_requests 
       (id, user_id, leave_type, start_date, end_date, days_requested, reason, status, created_at, updated_at)
       VALUES ($1, $2, $3, $4, $5, $6, $7, 'pending', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
       RETURNING *`,
      [uuidv4(), userId, leaveType, startDate, endDate, days, reason]
    );

    const leave = leaveResult.rows[0];

    // Get approver based on user's role/hierarchy
    const approverResult = await client.query(
      `SELECT manager_id FROM users WHERE id = $1`,
      [userId]
    );

    if (approverResult.rows.length > 0 && approverResult.rows[0].manager_id) {
      // Create approval record
      await client.query(
        `INSERT INTO leave_approvals 
         (id, leave_request_id, approver_id, status, created_at, updated_at)
         VALUES ($1, $2, $3, 'pending', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)`,
        [uuidv4(), leave.id, approverResult.rows[0].manager_id]
      );
    }

    logger.info(`Leave request created: ${leave.id} by user: ${userId}`);

    res.status(201).json({
      success: true,
      message: 'Leave request submitted successfully',
      data: leave
    });
  });
}));

/**
 * @swagger
 * /api/leave/requests:
 *   get:
 *     summary: Get leave requests
 *     tags: [Leave Management]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: query
 *         name: status
 *         schema:
 *           type: string
 *           enum: [pending, approved, rejected, cancelled]
 *         description: Filter by status
 *       - in: query
 *         name: page
 *         schema:
 *           type: integer
 *           default: 1
 *       - in: query
 *         name: limit
 *         schema:
 *           type: integer
 *           default: 20
 *     responses:
 *       200:
 *         description: Leave requests retrieved successfully
 */
router.get('/requests', asyncHandler(async (req, res) => {
  const { status, page = 1, limit = 20 } = req.query;
  const offset = (page - 1) * limit;

  let queryText = `
    SELECT lr.*, 
           u.first_name || ' ' || u.last_name as requester_name,
           u.email as requester_email
    FROM leave_requests lr
    JOIN users u ON lr.user_id = u.id
    WHERE 1=1
  `;
  const params = [];

  // For regular users, show only their requests
  if (req.user.role === 'teacher') {
    queryText += ` AND lr.user_id = $${params.length + 1}`;
    params.push(req.user.id);
  }

  if (status) {
    queryText += ` AND lr.status = $${params.length + 1}`;
    params.push(status);
  }

  queryText += ` ORDER BY lr.created_at DESC`;
  queryText += ` LIMIT $${params.length + 1} OFFSET $${params.length + 2}`;
  params.push(limit, offset);

  const result = await query(queryText, params);

  // Get total count
  let countQuery = `
    SELECT COUNT(*) FROM leave_requests lr
    WHERE 1=1
  `;
  const countParams = [];

  if (req.user.role === 'teacher') {
    countQuery += ` AND lr.user_id = $${countParams.length + 1}`;
    countParams.push(req.user.id);
  }

  if (status) {
    countQuery += ` AND lr.status = $${countParams.length + 1}`;
    countParams.push(status);
  }

  const countResult = await query(countQuery, countParams);
  const total = parseInt(countResult.rows[0].count);

  res.json({
    success: true,
    data: {
      leaves: result.rows,
      pagination: {
        page,
        limit,
        total,
        pages: Math.ceil(total / limit)
      }
    }
  });
}));

/**
 * @swagger
 * /api/leave/pending-approvals:
 *   get:
 *     summary: Get pending leave approvals (for managers)
 *     tags: [Leave Management]
 *     security:
 *       - bearerAuth: []
 *     responses:
 *       200:
 *         description: Pending approvals retrieved successfully
 */
router.get('/pending-approvals', asyncHandler(async (req, res) => {
  // Get leave requests pending approval for this user
  const result = await query(
    `SELECT lr.*, 
            u.first_name || ' ' || u.last_name as requester_name,
            u.email as requester_email,
            la.id as approval_id
     FROM leave_requests lr
     JOIN users u ON lr.user_id = u.id
     JOIN leave_approvals la ON lr.id = la.leave_request_id
     WHERE la.approver_id = $1 
       AND la.status = 'pending'
       AND lr.status = 'pending'
     ORDER BY lr.created_at DESC`,
    [req.user.id]
  );

  res.json({
    success: true,
    data: result.rows
  });
}));

/**
 * @swagger
 * /api/leave/request/{id}/approve:
 *   put:
 *     summary: Approve a leave request
 *     tags: [Leave Management]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: string
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             properties:
 *               comment:
 *                 type: string
 *     responses:
 *       200:
 *         description: Leave request approved successfully
 */
router.put('/request/:id/approve', asyncHandler(async (req, res) => {
  const { id } = req.params;
  const { comment } = req.body;

  await transaction(async (client) => {
    // Check if user is authorized to approve this request
    const approvalResult = await client.query(
      `SELECT la.*, lr.user_id 
       FROM leave_approvals la
       JOIN leave_requests lr ON la.leave_request_id = lr.id
       WHERE la.leave_request_id = $1 
         AND la.approver_id = $2 
         AND la.status = 'pending'`,
      [id, req.user.id]
    );

    if (approvalResult.rows.length === 0) {
      throw createError(403, 'Not authorized to approve this request');
    }

    // Update approval record
    await client.query(
      `UPDATE leave_approvals 
       SET status = 'approved', 
           comment = $1,
           approved_at = CURRENT_TIMESTAMP,
           updated_at = CURRENT_TIMESTAMP
       WHERE leave_request_id = $2 AND approver_id = $3`,
      [comment, id, req.user.id]
    );

    // Update leave request status
    await client.query(
      `UPDATE leave_requests 
       SET status = 'approved', 
           updated_at = CURRENT_TIMESTAMP
       WHERE id = $1`,
      [id]
    );

    logger.info(`Leave request ${id} approved by user ${req.user.id}`);

    res.json({
      success: true,
      message: 'Leave request approved successfully'
    });
  });
}));

/**
 * @swagger
 * /api/leave/request/{id}/reject:
 *   put:
 *     summary: Reject a leave request
 *     tags: [Leave Management]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: string
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required:
 *               - comment
 *             properties:
 *               comment:
 *                 type: string
 *     responses:
 *       200:
 *         description: Leave request rejected successfully
 */
router.put('/request/:id/reject', asyncHandler(async (req, res) => {
  const { id } = req.params;
  const { comment } = req.body;

  if (!comment) {
    return res.status(400).json({
      success: false,
      error: {
        message: 'Comment is required when rejecting a leave request'
      }
    });
  }

  await transaction(async (client) => {
    // Check if user is authorized to reject this request
    const approvalResult = await client.query(
      `SELECT la.*, lr.user_id 
       FROM leave_approvals la
       JOIN leave_requests lr ON la.leave_request_id = lr.id
       WHERE la.leave_request_id = $1 
         AND la.approver_id = $2 
         AND la.status = 'pending'`,
      [id, req.user.id]
    );

    if (approvalResult.rows.length === 0) {
      throw createError(403, 'Not authorized to reject this request');
    }

    // Update approval record
    await client.query(
      `UPDATE leave_approvals 
       SET status = 'rejected', 
           comment = $1,
           approved_at = CURRENT_TIMESTAMP,
           updated_at = CURRENT_TIMESTAMP
       WHERE leave_request_id = $2 AND approver_id = $3`,
      [comment, id, req.user.id]
    );

    // Update leave request status
    await client.query(
      `UPDATE leave_requests 
       SET status = 'rejected', 
           updated_at = CURRENT_TIMESTAMP
       WHERE id = $1`,
      [id]
    );

    logger.info(`Leave request ${id} rejected by user ${req.user.id}`);

    res.json({
      success: true,
      message: 'Leave request rejected successfully'
    });
  });
}));

/**
 * @swagger
 * /api/leave/request/{id}/cancel:
 *   put:
 *     summary: Cancel a leave request
 *     tags: [Leave Management]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: string
 *     responses:
 *       200:
 *         description: Leave request cancelled successfully
 */
router.put('/request/:id/cancel', asyncHandler(async (req, res) => {
  const { id } = req.params;

  await transaction(async (client) => {
    // Check if user owns this request
    const leaveResult = await client.query(
      `SELECT * FROM leave_requests 
       WHERE id = $1 AND user_id = $2 AND status = 'pending'`,
      [id, req.user.id]
    );

    if (leaveResult.rows.length === 0) {
      throw createError(403, 'Cannot cancel this leave request');
    }

    // Update leave request status
    await client.query(
      `UPDATE leave_requests 
       SET status = 'cancelled', 
           updated_at = CURRENT_TIMESTAMP
       WHERE id = $1`,
      [id]
    );

    // Update any pending approvals
    await client.query(
      `UPDATE leave_approvals 
       SET status = 'cancelled', 
           updated_at = CURRENT_TIMESTAMP
       WHERE leave_request_id = $1 AND status = 'pending'`,
      [id]
    );

    logger.info(`Leave request ${id} cancelled by user ${req.user.id}`);

    res.json({
      success: true,
      message: 'Leave request cancelled successfully'
    });
  });
}));

/**
 * @swagger
 * /api/leave/balance/{userId}:
 *   get:
 *     summary: Get leave balance for a user
 *     tags: [Leave Management]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: userId
 *         required: true
 *         schema:
 *           type: string
 *     responses:
 *       200:
 *         description: Leave balance retrieved successfully
 */
router.get('/balance/:userId', asyncHandler(async (req, res) => {
  const { userId } = req.params;

  // Check authorization
  if (req.user.id !== userId && !['admin', 'director', 'cluster_head'].includes(req.user.role)) {
    throw createError(403, 'Not authorized to view this leave balance');
  }

  // Get leave balance
  const balanceResult = await query(
    `SELECT 
       SUM(CASE WHEN leave_type = 'sick' THEN days_requested ELSE 0 END) as sick_days_used,
       SUM(CASE WHEN leave_type = 'personal' THEN days_requested ELSE 0 END) as personal_days_used,
       SUM(CASE WHEN leave_type = 'emergency' THEN days_requested ELSE 0 END) as emergency_days_used
     FROM leave_requests
     WHERE user_id = $1 
       AND status = 'approved'
       AND EXTRACT(YEAR FROM start_date) = EXTRACT(YEAR FROM CURRENT_DATE)`,
    [userId]
  );

  const balance = balanceResult.rows[0];

  // Default allocations (should come from settings/contracts)
  const allocations = {
    sick_days_total: 10,
    personal_days_total: 5,
    emergency_days_total: 3
  };

  res.json({
    success: true,
    data: {
      allocations,
      used: {
        sick_days: parseInt(balance.sick_days_used) || 0,
        personal_days: parseInt(balance.personal_days_used) || 0,
        emergency_days: parseInt(balance.emergency_days_used) || 0
      },
      remaining: {
        sick_days: allocations.sick_days_total - (parseInt(balance.sick_days_used) || 0),
        personal_days: allocations.personal_days_total - (parseInt(balance.personal_days_used) || 0),
        emergency_days: allocations.emergency_days_total - (parseInt(balance.emergency_days_used) || 0)
      }
    }
  });
}));

module.exports = router;