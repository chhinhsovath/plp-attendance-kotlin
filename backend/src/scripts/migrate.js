const fs = require('fs').promises;
const path = require('path');
const { query, connectDatabase, closeDatabase } = require('../config/database');
const logger = require('../utils/logger');

async function runMigration () {
  try {
    logger.info('Starting database migration...');

    // Connect to database
    await connectDatabase();

    // Read schema file
    const schemaPath = path.join(__dirname, '../../database/schema.sql');
    const schemaSQL = await fs.readFile(schemaPath, 'utf8');

    // Split schema into individual statements
    const statements = schemaSQL
      .split(';')
      .map(stmt => stmt.trim())
      .filter(stmt => stmt.length > 0);

    logger.info(`Executing ${statements.length} SQL statements...`);

    // Execute each statement
    for (let i = 0; i < statements.length; i++) {
      const statement = statements[i];
      if (statement.trim()) {
        try {
          await query(statement);
          logger.debug(`Statement ${i + 1}/${statements.length} executed successfully`);
        } catch (error) {
          // Some statements might fail if they already exist (like CREATE EXTENSION)
          if (error.code !== '42710' && error.code !== '42P07') { // Skip "already exists" errors
            logger.error(`Error executing statement ${i + 1}:`, error.message);
            throw error;
          } else {
            logger.debug(`Statement ${i + 1} skipped (already exists)`);
          }
        }
      }
    }

    logger.info('Database migration completed successfully!');
  } catch (error) {
    logger.error('Database migration failed:', error);
    process.exit(1);
  } finally {
    await closeDatabase();
  }
}

// Run migration if this file is executed directly
if (require.main === module) {
  runMigration();
}

module.exports = { runMigration };
