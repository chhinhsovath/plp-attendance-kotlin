#!/usr/bin/env node

const { Pool } = require('pg');
const bcrypt = require('bcryptjs');

const dbConfig = {
    user: 'postgres',
    host: '137.184.109.21',
    database: 'plp_attendance',
    password: 'P@ssw0rd',
    port: 5432,
    max: 20,
    idleTimeoutMillis: 30000,
    connectionTimeoutMillis: 2000,
};

const pool = new Pool(dbConfig);

// Test credentials from LoginScreen.kt
const testCredentials = [
    { email: 'admin@plp.gov.kh', password: 'password', name: 'សុខ សុភាព (Sok Sopheap)', role: 'Administrator' },
    { email: 'zone.north@plp.gov.kh', password: 'password', name: 'ចាន់ ដារ៉ា (Chan Dara)', role: 'Zone Manager' },
    { email: 'zone.south@plp.gov.kh', password: 'password', name: 'លី សុខា (Ly Sokha)', role: 'Zone Manager' },
    { email: 'provincial.pp@plp.gov.kh', password: 'password', name: 'ហេង សម៉ាត (Heng Samat)', role: 'Provincial Manager' },
    { email: 'provincial.sr@plp.gov.kh', password: 'password', name: 'ពៅ វិសាល (Pov Visal)', role: 'Provincial Manager' },
    { email: 'department.pp@plp.gov.kh', password: 'password', name: 'មាស សុផល (Meas Sophal)', role: 'Department Manager' },
    { email: 'department.sr@plp.gov.kh', password: 'password', name: 'សុខ ពិសិទ្ធ (Sok Piseth)', role: 'Department Manager' },
    { email: 'cluster.pp01@plp.gov.kh', password: 'password', name: 'រស់ បុប្ផា (Ros Bopha)', role: 'Cluster Head' },
    { email: 'cluster.sr01@plp.gov.kh', password: 'password', name: 'នួន សុខលី (Nuon Sokhly)', role: 'Cluster Head' },
    { email: 'director.pp001@plp.gov.kh', password: 'password', name: 'ខៀវ សំណាង (Khiev Samnang)', role: 'Director' },
    { email: 'director.pp002@plp.gov.kh', password: 'password', name: 'ទេព មករា (Tep Makara)', role: 'Director' },
    { email: 'director.sr001@plp.gov.kh', password: 'password', name: 'សាន់ វណ្ណា (San Vanna)', role: 'Director' },
    { email: 'teacher.pp001@plp.gov.kh', password: 'password', name: 'លឹម សុភាព (Lim Sopheap)', role: 'Teacher' },
    { email: 'teacher.pp002@plp.gov.kh', password: 'password', name: 'ឈួន លីដា (Chhoun Lida)', role: 'Teacher' },
    { email: 'teacher.pp003@plp.gov.kh', password: 'password', name: 'គឹម សុខហេង (Kim Sokhheng)', role: 'Teacher' },
    { email: 'teacher.sr001@plp.gov.kh', password: 'password', name: 'យិន សុវណ្ណាវ (Yin Sovannarv)', role: 'Teacher' }
];

async function testLoginFunctionality() {
    console.log('🔐 Testing login functionality for all accounts...');
    
    const client = await pool.connect();
    
    try {
        let successCount = 0;
        let failureCount = 0;
        const results = [];
        
        for (const credential of testCredentials) {
            try {
                // Simulate login by checking database
                const userResult = await client.query(
                    'SELECT id, email, password_hash, full_name, position, is_active FROM users WHERE email = $1',
                    [credential.email]
                );
                
                if (userResult.rows.length === 0) {
                    results.push({
                        email: credential.email,
                        status: '❌ NOT FOUND',
                        message: 'User not found in database'
                    });
                    failureCount++;
                    continue;
                }
                
                const user = userResult.rows[0];
                
                if (!user.is_active) {
                    results.push({
                        email: credential.email,
                        status: '❌ INACTIVE',
                        message: 'User account is inactive'
                    });
                    failureCount++;
                    continue;
                }
                
                // Check password
                const isValidPassword = await bcrypt.compare(credential.password, user.password_hash);
                
                if (!isValidPassword) {
                    results.push({
                        email: credential.email,
                        status: '❌ WRONG PASSWORD',
                        message: 'Password does not match'
                    });
                    failureCount++;
                    continue;
                }
                
                // Success
                results.push({
                    email: credential.email,
                    status: '✅ SUCCESS',
                    message: `Login successful for ${user.full_name} (${user.position})`
                });
                successCount++;
                
            } catch (error) {
                results.push({
                    email: credential.email,
                    status: '❌ ERROR',
                    message: error.message
                });
                failureCount++;
            }
        }
        
        console.log('\n📊 LOGIN TEST RESULTS:');
        console.log('=' .repeat(80));
        
        results.forEach((result, index) => {
            console.log(`${index + 1}. ${result.email}`);
            console.log(`   Status: ${result.status}`);
            console.log(`   Message: ${result.message}`);
            console.log('');
        });
        
        console.log('📈 SUMMARY:');
        console.log(`✅ Successful logins: ${successCount}/${testCredentials.length}`);
        console.log(`❌ Failed logins: ${failureCount}/${testCredentials.length}`);
        console.log(`📊 Success rate: ${(successCount / testCredentials.length * 100).toFixed(1)}%`);
        
        if (successCount === testCredentials.length) {
            console.log('\n🎉 ALL LOGIN TESTS PASSED! 🎉');
            console.log('✅ All user accounts from LoginScreen.kt can successfully authenticate');
        } else {
            console.log('\n⚠️  Some login tests failed. Please check the failed accounts above.');
        }
        
    } catch (error) {
        console.error('❌ Test execution error:', error.message);
        throw error;
    } finally {
        client.release();
    }
}

async function main() {
    console.log('🚀 PLP Attendance System - Login Functionality Test');
    console.log('=' .repeat(60));
    
    try {
        await testLoginFunctionality();
        console.log('\n✅ Login functionality test completed!');
    } catch (error) {
        console.error('\n❌ Login test failed:', error.message);
        process.exit(1);
    } finally {
        await pool.end();
    }
}

// Check if bcrypt is available
try {
    require('bcryptjs');
} catch (error) {
    console.error('❌ bcryptjs is not installed. Installing...');
    console.log('Please run: npm install bcryptjs');
    process.exit(1);
}

// Run the script
if (require.main === module) {
    main();
}

module.exports = { testLoginFunctionality };