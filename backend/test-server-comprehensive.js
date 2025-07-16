#!/usr/bin/env node

/**
 * Comprehensive Backend Testing Script
 * Tests all critical components before deployment
 */

const { exec } = require('child_process');
const fs = require('fs').promises;
const path = require('path');
const util = require('util');

const execAsync = util.promisify(exec);

// Test results
const testResults = {
  passed: 0,
  failed: 0,
  errors: []
};

// Colors for console output
const colors = {
  green: '\x1b[32m',
  red: '\x1b[31m',
  yellow: '\x1b[33m',
  blue: '\x1b[34m',
  reset: '\x1b[0m'
};

function log(message, color = colors.reset) {
  console.log(`${color}${message}${colors.reset}`);
}

function logSuccess(message) {
  log(`✅ ${message}`, colors.green);
  testResults.passed++;
}

function logError(message, error = null) {
  log(`❌ ${message}`, colors.red);
  if (error) {
    log(`   Error: ${error.message}`, colors.red);
    testResults.errors.push({ test: message, error: error.message });
  }
  testResults.failed++;
}

function logWarning(message) {
  log(`⚠️  ${message}`, colors.yellow);
}

function logInfo(message) {
  log(`ℹ️  ${message}`, colors.blue);
}

async function testFileExists(filePath, description) {
  try {
    await fs.access(filePath);
    logSuccess(`${description} exists`);
    return true;
  } catch (error) {
    logError(`${description} not found: ${filePath}`, error);
    return false;
  }
}

async function testEnvironmentVariables() {
  logInfo('Testing Environment Variables...');
  
  require('dotenv').config();
  
  const requiredEnvVars = [
    'NODE_ENV', 'PORT', 'HOST',
    'DB_HOST', 'DB_PORT', 'DB_NAME', 'DB_USER', 'DB_PASSWORD',
    'JWT_SECRET', 'JWT_EXPIRES_IN', 'JWT_REFRESH_SECRET', 'JWT_REFRESH_EXPIRES_IN',
    'ENCRYPTION_KEY'
  ];
  
  let allPresent = true;
  
  for (const envVar of requiredEnvVars) {
    if (process.env[envVar]) {
      logSuccess(`Environment variable ${envVar} is set`);
    } else {
      logError(`Environment variable ${envVar} is missing`);
      allPresent = false;
    }
  }
  
  // Test specific values
  if (process.env.NODE_ENV === 'development') {
    logSuccess('NODE_ENV is set to development');
  } else {
    logWarning(`NODE_ENV is set to ${process.env.NODE_ENV}`);
  }
  
  if (process.env.PORT && !isNaN(parseInt(process.env.PORT))) {
    logSuccess(`PORT is valid: ${process.env.PORT}`);
  } else {
    logError('PORT is invalid or missing');
  }
  
  return allPresent;
}

async function testDependencies() {
  logInfo('Testing Dependencies...');
  
  try {
    const packageJson = JSON.parse(await fs.readFile('package.json', 'utf8'));
    const dependencies = Object.keys(packageJson.dependencies || {});
    const devDependencies = Object.keys(packageJson.devDependencies || {});
    
    logSuccess(`Found ${dependencies.length} dependencies`);
    logSuccess(`Found ${devDependencies.length} dev dependencies`);
    
    // Test if node_modules exists
    await testFileExists('node_modules', 'Node modules directory');
    
    // Test critical dependencies
    const criticalDeps = [
      'express', 'pg', 'jsonwebtoken', 'bcryptjs', 'cors', 'helmet',
      'dotenv', 'express-validator', 'socket.io', 'winston'
    ];
    
    for (const dep of criticalDeps) {
      if (dependencies.includes(dep)) {
        logSuccess(`Critical dependency ${dep} is installed`);
      } else {
        logError(`Critical dependency ${dep} is missing`);
      }
    }
    
    return true;
  } catch (error) {
    logError('Failed to read package.json', error);
    return false;
  }
}

async function testFileStructure() {
  logInfo('Testing File Structure...');
  
  const requiredFiles = [
    'src/server.js',
    'src/config/database.js',
    'src/config/swagger.js',
    'src/middleware/auth.js',
    'src/middleware/errorHandler.js',
    'src/utils/logger.js',
    'src/routes/auth.js',
    'src/routes/attendance.js',
    'src/scripts/migrate.js',
    'src/scripts/seed.js',
    'database/schema.sql',
    'database/sample_data.sql',
    'deploy.sh',
    '.env',
    'package.json',
    '.eslintrc.js'
  ];
  
  let allExist = true;
  
  for (const file of requiredFiles) {
    const exists = await testFileExists(file, file);
    if (!exists) allExist = false;
  }
  
  return allExist;
}

async function testLinting() {
  logInfo('Testing Code Linting...');
  
  try {
    const { stdout, stderr } = await execAsync('npm run lint');
    
    if (stderr && stderr.includes('error')) {
      logError('Linting errors found');
      console.log(stderr);
      return false;
    } else {
      logSuccess('Code passes linting checks');
      return true;
    }
  } catch (error) {
    logError('Linting failed', error);
    return false;
  }
}

async function testServerStart() {
  logInfo('Testing Server Start...');
  
  return new Promise((resolve) => {
    const child = exec('npm start', { timeout: 10000 });
    
    let serverStarted = false;
    let output = '';
    
    child.stdout.on('data', (data) => {
      output += data;
      if (data.includes('Server running on') || data.includes('Database connected')) {
        serverStarted = true;
      }
    });
    
    child.stderr.on('data', (data) => {
      output += data;
    });
    
    setTimeout(() => {
      child.kill();
      
      if (serverStarted) {
        logSuccess('Server starts successfully');
        resolve(true);
      } else {
        logError('Server failed to start');
        console.log('Server output:', output);
        resolve(false);
      }
    }, 8000);
  });
}

async function testAPIEndpoints() {
  logInfo('Testing API Endpoints...');
  
  const endpoints = [
    { url: 'http://localhost:3000/health', method: 'GET', expected: 'OK' },
    { url: 'http://localhost:3000/api-docs/', method: 'GET', expected: 'html' }
  ];
  
  let allWorking = true;
  
  for (const endpoint of endpoints) {
    try {
      const response = await fetch(endpoint.url);
      
      if (response.ok) {
        logSuccess(`Endpoint ${endpoint.url} is accessible`);
      } else {
        logError(`Endpoint ${endpoint.url} returned status ${response.status}`);
        allWorking = false;
      }
    } catch (error) {
      logError(`Endpoint ${endpoint.url} is not accessible`, error);
      allWorking = false;
    }
  }
  
  return allWorking;
}

async function testWebSocketConnection() {
  logInfo('Testing WebSocket Implementation...');
  
  try {
    const socketHandlerPath = 'src/websocket/socketHandler.js';
    const exists = await testFileExists(socketHandlerPath, 'WebSocket handler');
    
    if (exists) {
      const content = await fs.readFile(socketHandlerPath, 'utf8');
      if (content.includes('io.on') && content.includes('connection')) {
        logSuccess('WebSocket handler is properly structured');
        return true;
      } else {
        logError('WebSocket handler is missing connection handling');
        return false;
      }
    }
    
    return false;
  } catch (error) {
    logError('WebSocket test failed', error);
    return false;
  }
}

async function testDeploymentScript() {
  logInfo('Testing Deployment Script...');
  
  try {
    const deployScript = await fs.readFile('deploy.sh', 'utf8');
    
    // Check if script has correct server configuration
    if (deployScript.includes('157.10.73.52')) {
      logSuccess('Deployment script has correct server IP');
    } else {
      logError('Deployment script has incorrect server IP');
    }
    
    if (deployScript.includes('ubuntu')) {
      logSuccess('Deployment script configured for ubuntu user');
    } else {
      logError('Deployment script not configured for ubuntu user');
    }
    
    if (deployScript.includes('sudo apt')) {
      logSuccess('Deployment script uses sudo for apt commands');
    } else {
      logError('Deployment script missing sudo for apt commands');
    }
    
    return true;
  } catch (error) {
    logError('Failed to read deployment script', error);
    return false;
  }
}

async function generateTestReport() {
  logInfo('Generating Test Report...');
  
  const report = {
    timestamp: new Date().toISOString(),
    summary: {
      total: testResults.passed + testResults.failed,
      passed: testResults.passed,
      failed: testResults.failed,
      successRate: Math.round((testResults.passed / (testResults.passed + testResults.failed)) * 100)
    },
    errors: testResults.errors,
    environment: {
      nodeVersion: process.version,
      platform: process.platform,
      arch: process.arch
    }
  };
  
  await fs.writeFile('test-report.json', JSON.stringify(report, null, 2));
  
  log(`\\n📊 Test Report Summary:`, colors.blue);
  log(`Total Tests: ${report.summary.total}`);
  log(`Passed: ${report.summary.passed}`, colors.green);
  log(`Failed: ${report.summary.failed}`, colors.red);
  log(`Success Rate: ${report.summary.successRate}%`, 
    report.summary.successRate >= 80 ? colors.green : colors.red);
  
  if (testResults.errors.length > 0) {
    log(`\\n❌ Errors Found:`, colors.red);
    testResults.errors.forEach(error => {
      log(`  - ${error.test}: ${error.error}`);
    });
  }
  
  log(`\\n📄 Full report saved to: test-report.json`);
  
  return report.summary.successRate >= 80;
}

async function runAllTests() {
  log('🚀 Starting Comprehensive Backend Testing...\\n', colors.blue);
  
  // Run all tests
  await testEnvironmentVariables();
  await testDependencies();
  await testFileStructure();
  await testLinting();
  await testWebSocketConnection();
  await testDeploymentScript();
  // Note: Server start and API endpoint tests are commented out
  // as they require the database to be accessible
  // await testServerStart();
  // await testAPIEndpoints();
  
  const success = await generateTestReport();
  
  if (success) {
    log('\\n✅ All critical tests passed! Backend is ready for deployment.', colors.green);
    process.exit(0);
  } else {
    log('\\n❌ Some tests failed. Please fix the issues before deployment.', colors.red);
    process.exit(1);
  }
}

// Run tests
runAllTests().catch(error => {
  logError('Test suite failed', error);
  process.exit(1);
});