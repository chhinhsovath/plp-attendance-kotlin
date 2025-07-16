const { Pool } = require('pg');
const bcrypt = require('bcrypt');

// Database configuration
const pool = new Pool({
  host: '157.10.73.52',
  port: 5432,
  database: 'plp_attendance_kotlin',
  user: 'admin',
  password: 'P@ssw0rd',
  ssl: false
});

// The password we want to set for all users
const NEW_PASSWORD = 'password123';
const PASSWORD_HASH = '$2b$12$LQv3/X/0W6Y9GbCi.bhdUu4eKjR5BjMPpiqEFG0AQS8pAP7u9H5jK';

async function updateAllPasswords() {
  let client;
  
  try {
    console.log('Connecting to database...');
    client = await pool.connect();
    
    // First, check current users
    console.log('\nCurrent users:');
    const usersResult = await client.query('SELECT id, email, username, role FROM users ORDER BY role, email');
    console.table(usersResult.rows);
    
    // Update all passwords
    console.log('\nUpdating all passwords to:', NEW_PASSWORD);
    const updateResult = await client.query(
      'UPDATE users SET password_hash = $1, updated_at = CURRENT_TIMESTAMP',
      [PASSWORD_HASH]
    );
    
    console.log(`\n✅ Updated ${updateResult.rowCount} user passwords`);
    
    // Verify update
    const verifyResult = await client.query(
      'SELECT email, username, role FROM users WHERE password_hash = $1',
      [PASSWORD_HASH]
    );
    
    console.log('\nUsers with updated passwords:');
    console.table(verifyResult.rows);
    
  } catch (error) {
    console.error('Error updating passwords:', error.message);
  } finally {
    if (client) {
      client.release();
    }
    await pool.end();
  }
}

// Test login function
async function testLogin(email, password) {
  try {
    const response = await fetch('http://157.10.73.52:3000/api/auth/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        identifier: email,
        password: password
      })
    });
    
    const data = await response.json();
    
    if (response.ok && data.success) {
      console.log(`✅ Login successful for ${email}`);
      console.log(`   User: ${data.data.user.firstName} ${data.data.user.lastName}`);
      console.log(`   Role: ${data.data.user.role}`);
    } else {
      console.log(`❌ Login failed for ${email}: ${data.error?.message || 'Unknown error'}`);
    }
  } catch (error) {
    console.log(`❌ Login test failed for ${email}: ${error.message}`);
  }
}

// Main execution
async function main() {
  console.log('PLP Attendance System - Password Update Tool');
  console.log('===========================================\n');
  
  // Update passwords
  await updateAllPasswords();
  
  // Test login with updated password
  console.log('\nTesting login with updated password...');
  await testLogin('admin@plp.gov.kh', NEW_PASSWORD);
}

// Check if running directly
if (require.main === module) {
  main().catch(console.error);
}

module.exports = { updateAllPasswords, testLogin };