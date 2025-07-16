const swaggerJsdoc = require('swagger-jsdoc');
const swaggerUi = require('swagger-ui-express');

const options = {
  definition: {
    openapi: '3.0.0',
    info: {
      title: 'Cambodia Education Attendance System API',
      version: '1.0.0',
      description: 'RESTful API for the Cambodia Education Attendance System with ' +
        'comprehensive endpoints for attendance tracking, leave management, ' +
        'mission coordination, and analytics.',
      contact: {
        name: 'PLP Development Team',
        email: 'dev@plp.edu.kh'
      },
      license: {
        name: 'MIT',
        url: 'https://opensource.org/licenses/MIT'
      }
    },
    servers: [
      {
        url: 'http://localhost:3000',
        description: 'Development server'
      },
      {
        url: 'https://api.attendance.edu.kh',
        description: 'Production server'
      }
    ],
    components: {
      securitySchemes: {
        bearerAuth: {
          type: 'http',
          scheme: 'bearer',
          bearerFormat: 'JWT',
          description: 'Enter your JWT token'
        }
      },
      schemas: {
        User: {
          type: 'object',
          properties: {
            id: {
              type: 'string',
              format: 'uuid',
              description: 'Unique user identifier'
            },
            username: {
              type: 'string',
              description: 'User username'
            },
            email: {
              type: 'string',
              format: 'email',
              description: 'User email address'
            },
            firstName: {
              type: 'string',
              description: 'User first name'
            },
            lastName: {
              type: 'string',
              description: 'User last name'
            },
            phoneNumber: {
              type: 'string',
              description: 'User phone number'
            },
            role: {
              type: 'string',
              enum: ['admin', 'teacher', 'student', 'staff'],
              description: 'User role'
            },
            schoolId: {
              type: 'string',
              format: 'uuid',
              description: 'Associated school ID'
            },
            department: {
              type: 'string',
              description: 'User department'
            },
            employeeId: {
              type: 'string',
              description: 'Employee/Student ID'
            },
            isActive: {
              type: 'boolean',
              description: 'User account status'
            },
            emailVerified: {
              type: 'boolean',
              description: 'Email verification status'
            },
            phoneVerified: {
              type: 'boolean',
              description: 'Phone verification status'
            },
            createdAt: {
              type: 'string',
              format: 'date-time',
              description: 'Account creation timestamp'
            },
            updatedAt: {
              type: 'string',
              format: 'date-time',
              description: 'Last update timestamp'
            }
          }
        },
        School: {
          type: 'object',
          properties: {
            id: {
              type: 'string',
              format: 'uuid'
            },
            name: {
              type: 'string',
              description: 'School name'
            },
            address: {
              type: 'string',
              description: 'School address'
            },
            phoneNumber: {
              type: 'string',
              description: 'School phone number'
            },
            email: {
              type: 'string',
              format: 'email',
              description: 'School email'
            },
            principalName: {
              type: 'string',
              description: 'Principal name'
            },
            establishedDate: {
              type: 'string',
              format: 'date',
              description: 'School establishment date'
            },
            schoolType: {
              type: 'string',
              description: 'Type of school'
            },
            latitude: {
              type: 'number',
              format: 'double',
              description: 'School latitude'
            },
            longitude: {
              type: 'number',
              format: 'double',
              description: 'School longitude'
            },
            timezone: {
              type: 'string',
              description: 'School timezone'
            },
            isActive: {
              type: 'boolean',
              description: 'School status'
            }
          }
        },
        AttendanceRecord: {
          type: 'object',
          properties: {
            id: {
              type: 'string',
              format: 'uuid'
            },
            userId: {
              type: 'string',
              format: 'uuid'
            },
            schoolId: {
              type: 'string',
              format: 'uuid'
            },
            checkInTime: {
              type: 'string',
              format: 'date-time'
            },
            checkOutTime: {
              type: 'string',
              format: 'date-time'
            },
            checkInLatitude: {
              type: 'number',
              format: 'double'
            },
            checkInLongitude: {
              type: 'number',
              format: 'double'
            },
            checkOutLatitude: {
              type: 'number',
              format: 'double'
            },
            checkOutLongitude: {
              type: 'number',
              format: 'double'
            },
            workingHours: {
              type: 'number',
              format: 'double'
            },
            status: {
              type: 'string',
              enum: ['present', 'absent', 'late', 'early_departure', 'on_leave']
            },
            notes: {
              type: 'string'
            }
          }
        },
        LeaveRequest: {
          type: 'object',
          properties: {
            id: {
              type: 'string',
              format: 'uuid'
            },
            userId: {
              type: 'string',
              format: 'uuid'
            },
            leaveType: {
              type: 'string',
              enum: ['sick', 'vacation', 'personal', 'maternity', 'emergency', 'other']
            },
            startDate: {
              type: 'string',
              format: 'date'
            },
            endDate: {
              type: 'string',
              format: 'date'
            },
            totalDays: {
              type: 'integer'
            },
            reason: {
              type: 'string'
            },
            status: {
              type: 'string',
              enum: ['pending', 'approved', 'rejected', 'cancelled']
            },
            approvedBy: {
              type: 'string',
              format: 'uuid'
            },
            approvalDate: {
              type: 'string',
              format: 'date-time'
            },
            approvalNotes: {
              type: 'string'
            }
          }
        },
        Mission: {
          type: 'object',
          properties: {
            id: {
              type: 'string',
              format: 'uuid'
            },
            title: {
              type: 'string'
            },
            description: {
              type: 'string'
            },
            missionType: {
              type: 'string',
              enum: ['field_trip', 'training', 'meeting', 'conference', 'inspection', 'other']
            },
            schoolId: {
              type: 'string',
              format: 'uuid'
            },
            createdBy: {
              type: 'string',
              format: 'uuid'
            },
            startDate: {
              type: 'string',
              format: 'date'
            },
            endDate: {
              type: 'string',
              format: 'date'
            },
            startTime: {
              type: 'string',
              format: 'time'
            },
            endTime: {
              type: 'string',
              format: 'time'
            },
            destinationName: {
              type: 'string'
            },
            destinationAddress: {
              type: 'string'
            },
            destinationLatitude: {
              type: 'number',
              format: 'double'
            },
            destinationLongitude: {
              type: 'number',
              format: 'double'
            },
            status: {
              type: 'string',
              enum: ['planned', 'in_progress', 'completed', 'cancelled']
            },
            budget: {
              type: 'number',
              format: 'double'
            },
            currency: {
              type: 'string'
            }
          }
        },
        Error: {
          type: 'object',
          properties: {
            success: {
              type: 'boolean',
              example: false
            },
            error: {
              type: 'object',
              properties: {
                message: {
                  type: 'string',
                  description: 'Error message'
                },
                details: {
                  type: 'array',
                  items: {
                    type: 'object'
                  },
                  description: 'Detailed error information'
                }
              }
            },
            timestamp: {
              type: 'string',
              format: 'date-time'
            },
            path: {
              type: 'string',
              description: 'Request path'
            },
            method: {
              type: 'string',
              description: 'HTTP method'
            }
          }
        },
        SuccessResponse: {
          type: 'object',
          properties: {
            success: {
              type: 'boolean',
              example: true
            },
            message: {
              type: 'string',
              description: 'Success message'
            },
            data: {
              type: 'object',
              description: 'Response data'
            }
          }
        }
      },
      responses: {
        BadRequest: {
          description: 'Bad Request',
          content: {
            'application/json': {
              schema: {
                $ref: '#/components/schemas/Error'
              }
            }
          }
        },
        Unauthorized: {
          description: 'Unauthorized',
          content: {
            'application/json': {
              schema: {
                $ref: '#/components/schemas/Error'
              }
            }
          }
        },
        Forbidden: {
          description: 'Forbidden',
          content: {
            'application/json': {
              schema: {
                $ref: '#/components/schemas/Error'
              }
            }
          }
        },
        NotFound: {
          description: 'Not Found',
          content: {
            'application/json': {
              schema: {
                $ref: '#/components/schemas/Error'
              }
            }
          }
        },
        InternalServerError: {
          description: 'Internal Server Error',
          content: {
            'application/json': {
              schema: {
                $ref: '#/components/schemas/Error'
              }
            }
          }
        }
      }
    },
    security: [
      {
        bearerAuth: []
      }
    ],
    tags: [
      {
        name: 'Authentication',
        description: 'User authentication and authorization'
      },
      {
        name: 'Users',
        description: 'User management'
      },
      {
        name: 'Schools',
        description: 'School management'
      },
      {
        name: 'Attendance',
        description: 'Attendance tracking and management'
      },
      {
        name: 'Leave',
        description: 'Leave request management'
      },
      {
        name: 'Missions',
        description: 'Mission and field trip management'
      },
      {
        name: 'Notifications',
        description: 'Notification system'
      },
      {
        name: 'Analytics',
        description: 'Analytics and reporting'
      }
    ]
  },
  apis: ['./src/routes/*.js', './src/server.js'] // paths to files containing OpenAPI definitions
};

const specs = swaggerJsdoc(options);

function setupSwagger (app) {
  // Swagger page
  app.use('/api-docs', swaggerUi.serve, swaggerUi.setup(specs, {
    explorer: true,
    customCss: '.swagger-ui .topbar { display: none }',
    customSiteTitle: 'PLP Attendance API Documentation',
    swaggerOptions: {
      docExpansion: 'none',
      filter: true,
      showRequestDuration: true
    }
  }));

  // Docs in JSON format
  app.get('/api-docs.json', (req, res) => {
    res.setHeader('Content-Type', 'application/json');
    res.send(specs);
  });
}

module.exports = setupSwagger;
