// Minimal test to reproduce the server error
console.log('Testing minimal server setup...\n');

// Mock express
const express = () => {
  const app = {
    middlewares: [],
    use: function(middleware) {
      console.log('app.use() called with:', typeof middleware);
      if (typeof middleware !== 'function') {
        throw new TypeError('app.use() requires a middleware function');
      }
      this.middlewares.push(middleware);
      console.log('✅ Middleware added successfully');
    }
  };
  return app;
};

// Test 1: Correct import (destructured)
console.log('Test 1: Destructured import');
const errorHandlerModule = {
  errorHandler: (err, req, res, next) => { console.log('Error handler called'); },
  notFound: () => {},
  asyncHandler: () => {},
  createError: () => {},
  ApiError: class {}
};

const { errorHandler } = errorHandlerModule;
const app1 = express();
try {
  app1.use(errorHandler);
  console.log('✅ Test 1 passed\n');
} catch (error) {
  console.log('❌ Test 1 failed:', error.message, '\n');
}

// Test 2: Wrong import (default instead of destructured)
console.log('Test 2: Default import (wrong)');
const errorHandler2 = errorHandlerModule; // This is the whole object, not the function
const app2 = express();
try {
  app2.use(errorHandler2);
  console.log('✅ Test 2 passed\n');
} catch (error) {
  console.log('❌ Test 2 failed:', error.message);
  console.log('This is likely what\'s happening on your server!\n');
}

// Test 3: Check what happens at line 112
console.log('Test 3: Simulating server.js line 112');
console.log('If errorHandler is:', typeof errorHandlerModule);
console.log('Then app.use(errorHandler) will fail because it\'s not a function\n');

console.log('SOLUTION: Make sure the server has the correct import:');
console.log('const { errorHandler } = require(\'./middleware/errorHandler\');');
console.log('NOT: const errorHandler = require(\'./middleware/errorHandler\');');