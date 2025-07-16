const { Pool } = require('pg');
const logger = require('../utils/logger');

// Database connection configuration
const dbConfig = {
  host: process.env.DB_HOST || 'localhost',
  port: parseInt(process.env.DB_PORT) || 5432,
  database: process.env.DB_NAME || 'plp_attendance_kotlin',
  user: process.env.DB_USER || 'admin',
  password: process.env.DB_PASSWORD || 'P@ssw0rd',
  ssl: process.env.DB_SSL === 'true' ? { rejectUnauthorized: false } : false,
  max: 20, // Maximum number of clients in the pool
  idleTimeoutMillis: 30000, // Close idle clients after 30 seconds
  connectionTimeoutMillis: 10000, // Return error after 10 seconds if connection failed
  maxUses: 7500 // Close (and replace) a connection after it has been used 7500 times
};

// Create connection pool
const pool = new Pool(dbConfig);

// Pool error handling
pool.on('error', (err, client) => {
  logger.error('Unexpected error on idle client', err);
  process.exit(-1);
});

pool.on('connect', (client) => {
  logger.debug('New client connected to database');
});

pool.on('acquire', (client) => {
  logger.debug('Client acquired from pool');
});

pool.on('remove', (client) => {
  logger.debug('Client removed from pool');
});

/**
 * Connect to the database and test the connection
 */
async function connectDatabase () {
  try {
    const client = await pool.connect();

    // Test the connection
    const result = await client.query('SELECT NOW() as current_time, version() as db_version');
    logger.info('Database connection successful');
    logger.info(`Connected to: ${result.rows[0].db_version}`);
    logger.info(`Current time: ${result.rows[0].current_time}`);

    client.release();
    return true;
  } catch (error) {
    logger.error('Database connection failed:', error.message);
    throw error;
  }
}

/**
 * Execute a query with optional parameters
 * @param {string} text - SQL query text
 * @param {Array} params - Query parameters
 * @returns {Promise<Object>} Query result
 */
async function query (text, params) {
  const start = Date.now();
  try {
    const result = await pool.query(text, params);
    const duration = Date.now() - start;

    if (process.env.NODE_ENV === 'development') {
      logger.debug('Executed query', {
        text: text.substring(0, 100) + (text.length > 100 ? '...' : ''),
        duration: `${duration}ms`,
        rows: result.rowCount
      });
    }

    return result;
  } catch (error) {
    const duration = Date.now() - start;
    logger.error('Query execution failed', {
      text: text.substring(0, 100) + (text.length > 100 ? '...' : ''),
      duration: `${duration}ms`,
      error: error.message
    });
    throw error;
  }
}

/**
 * Get a client from the pool for transaction handling
 * @returns {Promise<Object>} Database client
 */
async function getClient () {
  return await pool.connect();
}

/**
 * Execute multiple queries in a transaction
 * @param {Function} callback - Function containing queries to execute
 * @returns {Promise<any>} Transaction result
 */
async function transaction (callback) {
  const client = await pool.connect();

  try {
    await client.query('BEGIN');
    const result = await callback(client);
    await client.query('COMMIT');
    return result;
  } catch (error) {
    await client.query('ROLLBACK');
    throw error;
  } finally {
    client.release();
  }
}

/**
 * Close all database connections
 */
async function closeDatabase () {
  try {
    await pool.end();
    logger.info('Database pool closed');
  } catch (error) {
    logger.error('Error closing database pool:', error);
  }
}

/**
 * Check if a table exists in the database
 * @param {string} tableName - Name of the table to check
 * @returns {Promise<boolean>} True if table exists
 */
async function tableExists (tableName) {
  try {
    const result = await query(
      'SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = $1)',
      [tableName]
    );
    return result.rows[0].exists;
  } catch (error) {
    logger.error(`Error checking if table ${tableName} exists:`, error);
    return false;
  }
}

/**
 * Get database statistics and health information
 * @returns {Promise<Object>} Database health info
 */
async function getDatabaseHealth () {
  try {
    const [
      connectionInfo,
      dbSize,
      activeConnections
    ] = await Promise.all([
      query('SELECT version() as version, current_database() as database, current_user as user'),
      query('SELECT pg_size_pretty(pg_database_size(current_database())) as size'),
      query('SELECT count(*) as active_connections FROM pg_stat_activity WHERE state = \'active\'')
    ]);

    return {
      status: 'healthy',
      version: connectionInfo.rows[0].version,
      database: connectionInfo.rows[0].database,
      user: connectionInfo.rows[0].user,
      size: dbSize.rows[0].size,
      activeConnections: parseInt(activeConnections.rows[0].active_connections),
      poolInfo: {
        total: pool.totalCount,
        idle: pool.idleCount,
        waiting: pool.waitingCount
      }
    };
  } catch (error) {
    return {
      status: 'unhealthy',
      error: error.message
    };
  }
}

module.exports = {
  pool,
  query,
  getClient,
  transaction,
  connectDatabase,
  closeDatabase,
  tableExists,
  getDatabaseHealth
};
