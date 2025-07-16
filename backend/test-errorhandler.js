// Test script to verify errorHandler export
try {
  const { errorHandler } = require('./src/middleware/errorHandler');
  console.log('✅ errorHandler imported successfully');
  console.log('Type of errorHandler:', typeof errorHandler);
  console.log('Is errorHandler a function?', typeof errorHandler === 'function');
  
  // Check function signature
  console.log('Function length (params):', errorHandler.length);
  console.log('Function name:', errorHandler.name);
  
  // Test if it's the correct middleware signature (err, req, res, next)
  if (errorHandler.length === 4) {
    console.log('✅ errorHandler has correct middleware signature (4 parameters)');
  } else {
    console.log('❌ errorHandler does not have correct middleware signature');
  }
} catch (error) {
  console.error('❌ Error importing errorHandler:', error.message);
  console.error('Stack:', error.stack);
}

// Also test default export
try {
  const errorHandlerDefault = require('./src/middleware/errorHandler');
  console.log('\n--- Testing default export ---');
  console.log('Default export type:', typeof errorHandlerDefault);
  console.log('Default export keys:', Object.keys(errorHandlerDefault));
} catch (error) {
  console.error('Error with default export:', error.message);
}