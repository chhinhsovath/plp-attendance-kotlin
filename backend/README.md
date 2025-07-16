# Cambodia Education Attendance System API

A comprehensive RESTful API for the Cambodia Education Attendance System, providing endpoints for attendance tracking, leave management, mission coordination, and analytics.

## Features

- **Authentication & Authorization**: JWT-based authentication with role-based access control
- **Attendance Management**: Check-in/check-out with GPS validation and geofencing
- **Leave Management**: Leave request submission, approval workflow
- **Mission Tracking**: Field trip and mission coordination with GPS tracking
- **Real-time Notifications**: WebSocket-based real-time communication
- **Analytics & Reporting**: Comprehensive attendance analytics and report generation
- **Security**: Rate limiting, input validation, SQL injection prevention
- **Documentation**: Swagger/OpenAPI documentation

## Technology Stack

- **Runtime**: Node.js 18+
- **Framework**: Express.js
- **Database**: PostgreSQL with UUID support
- **Authentication**: JWT (jsonwebtoken)
- **Real-time**: Socket.IO
- **Validation**: express-validator, Joi
- **Security**: Helmet, CORS, Rate limiting
- **Documentation**: Swagger UI
- **Logging**: Winston
- **Testing**: Jest, Supertest

## Prerequisites

- Node.js 18.0.0 or higher
- npm 8.0.0 or higher
- PostgreSQL 12+ with UUID extension
- Redis (optional, for caching and sessions)

## Installation

### Local Development

1. **Clone the repository**
   ```bash
   cd backend
   ```

2. **Install dependencies**
   ```bash
   npm install
   ```

3. **Environment Configuration**
   ```bash
   cp .env.example .env
   ```
   
   Update the `.env` file with your configuration:
   ```env
   # Database Configuration
   DB_HOST=137.184.109.21
   DB_PORT=5432
   DB_NAME=plp_attendance
   DB_USER=postgres
   DB_PASSWORD=P@ssw0rd
   
   # JWT Configuration
   JWT_SECRET=your-super-secret-jwt-key-change-this-in-production
   JWT_REFRESH_SECRET=your-super-secret-refresh-key-change-this-in-production
   ```

4. **Database Setup**
   ```bash
   # Run database migration
   npm run migrate
   
   # Seed with sample data
   npm run seed
   
   # Or run both commands
   npm run setup-db
   ```

5. **Start Development Server**
   ```bash
   npm run dev
   ```

The API will be available at:
- **Server**: http://localhost:3000
- **API Documentation**: http://localhost:3000/api-docs
- **Health Check**: http://localhost:3000/health

### Production Deployment (Digital Ocean)

1. **Connect to Digital Ocean Server**
   ```bash
   ssh root@137.184.109.21
   # Password: 6UYNIx4uWaVzkBy
   ```

2. **Install Node.js and npm**
   ```bash
   # Update system
   apt update && apt upgrade -y
   
   # Install Node.js 18
   curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
   apt-get install -y nodejs
   
   # Verify installation
   node --version
   npm --version
   ```

3. **Install PM2 (Process Manager)**
   ```bash
   npm install -g pm2
   ```

4. **Clone and Setup Application**
   ```bash
   # Create application directory
   mkdir -p /var/www/plp-attendance-api
   cd /var/www/plp-attendance-api
   
   # Copy backend files (use scp or git)
   # For example:
   # scp -r /path/to/backend/* root@137.184.109.21:/var/www/plp-attendance-api/
   ```

5. **Install Dependencies and Setup**
   ```bash
   npm install --production
   npm run setup-db
   ```

6. **Start with PM2**
   ```bash
   # Start the application
   pm2 start src/server.js --name "plp-attendance-api"
   
   # Save PM2 configuration
   pm2 save
   
   # Setup PM2 startup script
   pm2 startup
   ```

7. **Setup Nginx (Optional - for reverse proxy)**
   ```bash
   apt install nginx -y
   
   # Create Nginx configuration
   cat > /etc/nginx/sites-available/plp-attendance-api << 'EOF'
   server {
       listen 80;
       server_name 137.184.109.21;
   
       location / {
           proxy_pass http://localhost:3000;
           proxy_http_version 1.1;
           proxy_set_header Upgrade $http_upgrade;
           proxy_set_header Connection 'upgrade';
           proxy_set_header Host $host;
           proxy_set_header X-Real-IP $remote_addr;
           proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
           proxy_set_header X-Forwarded-Proto $scheme;
           proxy_cache_bypass $http_upgrade;
       }
   }
   EOF
   
   # Enable the site
   ln -s /etc/nginx/sites-available/plp-attendance-api /etc/nginx/sites-enabled/
   nginx -t
   systemctl restart nginx
   ```

## API Endpoints

### Authentication
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - User login
- `POST /api/auth/refresh` - Refresh access token
- `POST /api/auth/logout` - User logout
- `GET /api/auth/profile` - Get user profile
- `POST /api/auth/change-password` - Change password

### Attendance
- `POST /api/attendance/check-in` - Check in attendance
- `POST /api/attendance/check-out` - Check out attendance
- `GET /api/attendance/status` - Get current attendance status
- `GET /api/attendance/records` - Get attendance records
- `GET /api/attendance/summary` - Get attendance summary

### Leave Management
- `GET /api/leave` - Get leave requests
- `POST /api/leave` - Create leave request
- `PUT /api/leave/:id` - Update leave request
- `DELETE /api/leave/:id` - Cancel leave request

### Mission Tracking
- `GET /api/missions` - Get missions
- `POST /api/missions` - Create mission
- `GET /api/missions/:id` - Get mission details
- `PUT /api/missions/:id` - Update mission
- `POST /api/missions/:id/track` - Add GPS tracking point

### Analytics
- `GET /api/analytics/attendance` - Attendance analytics
- `GET /api/analytics/reports` - Generated reports
- `POST /api/analytics/reports` - Generate new report

## Database Schema

### Key Tables
- **users** - User accounts and profiles
- **schools** - School information
- **attendance_records** - Daily attendance records
- **leave_requests** - Leave applications and approvals
- **missions** - Field trips and missions
- **mission_tracking** - GPS tracking data
- **notifications** - System notifications
- **audit_logs** - System audit trail

### Database Commands

```bash
# Connect to PostgreSQL
psql -h 137.184.109.21 -U postgres -d plp_attendance

# Common queries
SELECT COUNT(*) FROM users;
SELECT COUNT(*) FROM attendance_records;
SELECT COUNT(*) FROM schools;

# View recent attendance
SELECT u.first_name, u.last_name, ar.check_in_time, ar.status 
FROM attendance_records ar 
JOIN users u ON ar.user_id = u.id 
ORDER BY ar.check_in_time DESC LIMIT 10;
```

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `NODE_ENV` | Environment (development/production) | development |
| `PORT` | Server port | 3000 |
| `DB_HOST` | Database host | localhost |
| `DB_PORT` | Database port | 5432 |
| `DB_NAME` | Database name | plp_attendance |
| `DB_USER` | Database username | postgres |
| `DB_PASSWORD` | Database password | - |
| `JWT_SECRET` | JWT signing secret | - |
| `JWT_EXPIRES_IN` | JWT expiration time | 24h |
| `GEOFENCE_RADIUS_METERS` | Attendance geofence radius | 100 |

### Security Configuration

- **Rate Limiting**: 100 requests per 15 minutes per IP
- **CORS**: Configurable origins
- **Helmet**: Security headers enabled
- **Input Validation**: All inputs validated and sanitized
- **SQL Injection**: Parameterized queries used

## Testing

```bash
# Run all tests
npm test

# Run tests with coverage
npm run test:coverage

# Run tests in watch mode
npm run test:watch

# Linting
npm run lint
npm run lint:fix
```

## Monitoring and Logs

### PM2 Commands
```bash
# Check application status
pm2 status

# View logs
pm2 logs plp-attendance-api

# Restart application
pm2 restart plp-attendance-api

# Stop application
pm2 stop plp-attendance-api

# Monitor in real-time
pm2 monit
```

### Log Files
- Application logs: `logs/app.log`
- Error logs: `logs/error.log`
- Exception logs: `logs/exceptions.log`

## API Documentation

Once the server is running, visit:
- **Swagger UI**: http://localhost:3000/api-docs
- **JSON Schema**: http://localhost:3000/api-docs.json

## Troubleshooting

### Common Issues

1. **Database Connection Error**
   ```bash
   # Check PostgreSQL status
   systemctl status postgresql
   
   # Test connection
   psql -h 137.184.109.21 -U postgres -d plp_attendance
   ```

2. **Port Already in Use**
   ```bash
   # Find process using port 3000
   lsof -i :3000
   
   # Kill process
   kill -9 <PID>
   ```

3. **Permission Errors**
   ```bash
   # Fix file permissions
   chown -R www-data:www-data /var/www/plp-attendance-api
   chmod -R 755 /var/www/plp-attendance-api
   ```

### Debugging

Enable debug logging:
```bash
# Set log level to debug
export LOG_LEVEL=debug

# Or in .env file
LOG_LEVEL=debug
```

## Security Considerations

1. **Change default passwords** in production
2. **Use strong JWT secrets** (32+ characters)
3. **Enable HTTPS** in production
4. **Configure firewall** to restrict database access
5. **Regular security updates** for dependencies
6. **Monitor logs** for suspicious activity

## Support

For technical support or questions:
- **Email**: dev@plp.edu.kh
- **Documentation**: http://localhost:3000/api-docs
- **Health Check**: http://localhost:3000/health

## License

MIT License - see LICENSE file for details.