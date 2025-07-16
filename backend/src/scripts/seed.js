const fs = require('fs').promises;
const path = require('path');
const { query, connectDatabase, closeDatabase } = require('../config/database');
const logger = require('../utils/logger');

async function seedDatabase () {
  try {
    logger.info('Starting database seeding...');

    // Connect to database
    await connectDatabase();

    // Read sample data file
    const seedPath = path.join(__dirname, '../../database/sample_data.sql');
    const seedSQL = await fs.readFile(seedPath, 'utf8');

    logger.info('Executing sample data SQL...');

    // Execute the seed SQL
    await query(seedSQL);

    logger.info('Database seeding completed successfully!');

    // Verify data was inserted
    const userCount = await query('SELECT COUNT(*) as count FROM users');
    const schoolCount = await query('SELECT COUNT(*) as count FROM schools');
    const attendanceCount = await query('SELECT COUNT(*) as count FROM attendance_records');

    logger.info('Seeded data summary:', {
      users: parseInt(userCount.rows[0].count),
      schools: parseInt(schoolCount.rows[0].count),
      attendanceRecords: parseInt(attendanceCount.rows[0].count)
    });
  } catch (error) {
    logger.error('Database seeding failed:', error);
    process.exit(1);
  } finally {
    await closeDatabase();
  }
}

// Run seeding if this file is executed directly
if (require.main === module) {
  seedDatabase();
}

module.exports = { seedDatabase };
