const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
const { query } = require('../config/database');
const logger = require('../utils/logger');
const { createError } = require('./errorHandler');

/**
 * Generate JWT token
 */
const generateToken = (payload) => {
  return jwt.sign(payload, process.env.JWT_SECRET, {
    expiresIn: process.env.JWT_EXPIRES_IN || '24h'
  });
};

/**
 * Generate refresh token
 */
const generateRefreshToken = (payload) => {
  return jwt.sign(payload, process.env.JWT_REFRESH_SECRET, {
    expiresIn: process.env.JWT_REFRESH_EXPIRES_IN || '7d'
  });
};

/**
 * Verify JWT token
 */
const verifyToken = (token) => {
  return jwt.verify(token, process.env.JWT_SECRET);
};

/**
 * Verify refresh token
 */
const verifyRefreshToken = (token) => {
  return jwt.verify(token, process.env.JWT_REFRESH_SECRET);
};

/**
 * Hash password
 */
const hashPassword = async (password) => {
  const saltRounds = 12;
  return await bcrypt.hash(password, saltRounds);
};

/**
 * Compare password
 */
const comparePassword = async (password, hashedPassword) => {
  return await bcrypt.compare(password, hashedPassword);
};

/**
 * Authentication middleware
 */
const authenticate = async (req, res, next) => {
  try {
    const authHeader = req.headers.authorization;

    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      logger.logAuth('Token Missing', null, req.ip, req.get('User-Agent'), false);
      return next(createError(401, 'Access token is required'));
    }

    const token = authHeader.substring(7); // Remove 'Bearer ' prefix

    let decoded;
    try {
      decoded = verifyToken(token);
    } catch (error) {
      logger.logAuth('Token Invalid', null, req.ip, req.get('User-Agent'), false, error);

      if (error.name === 'TokenExpiredError') {
        return next(createError(401, 'Token expired'));
      } else if (error.name === 'JsonWebTokenError') {
        return next(createError(401, 'Invalid token'));
      } else {
        return next(createError(401, 'Token verification failed'));
      }
    }

    // Get user from database
    const userResult = await query(
      `SELECT u.*, s.name as school_name, s.timezone as school_timezone,
              s.latitude as school_latitude, s.longitude as school_longitude
       FROM users u 
       LEFT JOIN schools s ON u.school_id = s.id 
       WHERE u.id = $1 AND u.is_active = true`,
      [decoded.userId]
    );

    if (userResult.rows.length === 0) {
      logger.logAuth('User Not Found', decoded.userId, req.ip, req.get('User-Agent'), false);
      return next(createError(401, 'User not found or inactive'));
    }

    const user = userResult.rows[0];

    // Update last login time
    await query(
      'UPDATE users SET last_login_at = CURRENT_TIMESTAMP WHERE id = $1',
      [user.id]
    );

    // Attach user to request
    req.user = {
      id: user.id,
      username: user.username,
      email: user.email,
      firstName: user.first_name,
      lastName: user.last_name,
      role: user.role,
      schoolId: user.school_id,
      schoolName: user.school_name,
      schoolTimezone: user.school_timezone,
      department: user.department,
      employeeId: user.employee_id,
      isActive: user.is_active,
      emailVerified: user.email_verified,
      phoneVerified: user.phone_verified
    };

    logger.logAuth('Token Verified', user.id, req.ip, req.get('User-Agent'), true);
    next();
  } catch (error) {
    logger.error('Authentication error:', error);
    next(createError(500, 'Authentication failed'));
  }
};

/**
 * Role-based authorization middleware
 */
const authorize = (...roles) => {
  return (req, res, next) => {
    if (!req.user) {
      return next(createError(401, 'Authentication required'));
    }

    if (!roles.includes(req.user.role)) {
      logger.logSecurity('Unauthorized Role Access', {
        userId: req.user.id,
        userRole: req.user.role,
        requiredRoles: roles,
        ip: req.ip,
        url: req.originalUrl,
        method: req.method
      });

      return next(createError(403, 'Insufficient permissions'));
    }

    next();
  };
};

/**
 * School-based authorization middleware
 */
const authorizeSchool = (req, res, next) => {
  if (!req.user) {
    return next(createError(401, 'Authentication required'));
  }

  // Admins can access any school
  if (req.user.role === 'admin') {
    return next();
  }

  // Check if user belongs to the requested school
  const requestedSchoolId = req.params.schoolId || req.body.schoolId || req.query.schoolId;

  if (requestedSchoolId && requestedSchoolId !== req.user.schoolId) {
    logger.logSecurity('Cross-School Access Attempt', {
      userId: req.user.id,
      userSchoolId: req.user.schoolId,
      requestedSchoolId,
      ip: req.ip,
      url: req.originalUrl
    });

    return next(createError(403, 'Access denied to this school'));
  }

  next();
};

/**
 * Resource ownership authorization
 */
const authorizeOwnership = (resourceField = 'userId') => {
  return async (req, res, next) => {
    if (!req.user) {
      return next(createError(401, 'Authentication required'));
    }

    // Admins can access any resource
    if (req.user.role === 'admin') {
      return next();
    }

    const resourceUserId = req.params[resourceField] || req.body[resourceField];

    if (resourceUserId && resourceUserId !== req.user.id) {
      logger.logSecurity('Resource Ownership Violation', {
        userId: req.user.id,
        resourceUserId,
        resourceField,
        ip: req.ip,
        url: req.originalUrl
      });

      return next(createError(403, 'Access denied to this resource'));
    }

    next();
  };
};

/**
 * Rate limiting for authentication endpoints
 */
const authRateLimit = {
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 5, // 5 attempts per window
  message: 'Too many authentication attempts, please try again later',
  standardHeaders: true,
  legacyHeaders: false,
  keyGenerator: (req) => {
    return `${req.ip}:${req.body.email || req.body.username || 'unknown'}`;
  },
  handler: (req, res) => {
    logger.logSecurity('Rate Limit Exceeded', {
      ip: req.ip,
      userAgent: req.get('User-Agent'),
      url: req.originalUrl,
      identifier: req.body.email || req.body.username
    });

    res.status(429).json({
      success: false,
      error: {
        message: 'Too many authentication attempts, please try again later'
      }
    });
  }
};

/**
 * Generate token pair (access + refresh)
 */
const generateTokenPair = async (user, deviceInfo = {}) => {
  const payload = {
    userId: user.id,
    role: user.role,
    schoolId: user.school_id
  };

  const accessToken = generateToken(payload);
  const refreshToken = generateRefreshToken(payload);

  // Store refresh token in database
  const tokenHash = await hashPassword(refreshToken);
  await query(
    `INSERT INTO refresh_tokens (user_id, token_hash, device_info, ip_address, expires_at)
     VALUES ($1, $2, $3, $4, $5)`,
    [
      user.id,
      tokenHash,
      JSON.stringify(deviceInfo),
      deviceInfo.ip,
      new Date(Date.now() + 7 * 24 * 60 * 60 * 1000) // 7 days
    ]
  );

  return { accessToken, refreshToken };
};

/**
 * Validate refresh token and generate new access token
 */
const refreshAccessToken = async (refreshToken, deviceInfo = {}) => {
  try {
    const decoded = verifyRefreshToken(refreshToken);

    // Get stored refresh token from database
    const tokenResult = await query(
      `SELECT rt.*, u.id, u.role, u.school_id, u.is_active
       FROM refresh_tokens rt
       JOIN users u ON rt.user_id = u.id
       WHERE rt.user_id = $1 AND rt.revoked_at IS NULL AND rt.expires_at > CURRENT_TIMESTAMP
       ORDER BY rt.created_at DESC`,
      [decoded.userId]
    );

    if (tokenResult.rows.length === 0) {
      throw new Error('Refresh token not found or expired');
    }

    // Verify refresh token hash
    let validToken = false;
    for (const storedToken of tokenResult.rows) {
      if (await comparePassword(refreshToken, storedToken.token_hash)) {
        validToken = true;
        break;
      }
    }

    if (!validToken) {
      throw new Error('Invalid refresh token');
    }

    const user = tokenResult.rows[0];

    if (!user.is_active) {
      throw new Error('User account is inactive');
    }

    // Generate new access token
    const newAccessToken = generateToken({
      userId: user.id,
      role: user.role,
      schoolId: user.school_id
    });

    return { accessToken: newAccessToken };
  } catch (error) {
    logger.logAuth('Refresh Token Failed', null, deviceInfo.ip, deviceInfo.userAgent, false, error);
    throw error;
  }
};

/**
 * Revoke refresh token
 */
const revokeRefreshToken = async (userId, tokenHash = null) => {
  let queryText, params;

  if (tokenHash) {
    queryText = 'UPDATE refresh_tokens SET revoked_at = CURRENT_TIMESTAMP ' +
      'WHERE user_id = $1 AND token_hash = $2';
    params = [userId, tokenHash];
  } else {
    queryText = 'UPDATE refresh_tokens SET revoked_at = CURRENT_TIMESTAMP ' +
      'WHERE user_id = $1 AND revoked_at IS NULL';
    params = [userId];
  }

  await query(queryText, params);
};

module.exports = {
  authenticate,
  authorize,
  authorizeSchool,
  authorizeOwnership,
  authRateLimit,
  generateToken,
  generateRefreshToken,
  verifyToken,
  verifyRefreshToken,
  hashPassword,
  comparePassword,
  generateTokenPair,
  refreshAccessToken,
  revokeRefreshToken
};
