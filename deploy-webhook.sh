#!/bin/bash
# Deployment script for PLP Attendance API

# Configuration
PROJECT_DIR="/var/csv/plp_attendance_api"
BRANCH="main"
LOG_FILE="/var/log/plp_attendance_deploy.log"

# Function to log messages
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" >> "$LOG_FILE"
}

log "Starting deployment..."

# Navigate to project directory
cd "$PROJECT_DIR" || exit 1

# Fetch latest changes
git fetch origin

# Check if there are changes
LOCAL=$(git rev-parse HEAD)
REMOTE=$(git rev-parse origin/"$BRANCH")

if [ "$LOCAL" = "$REMOTE" ]; then
    log "No changes detected. Deployment skipped."
    exit 0
fi

# Pull latest changes
git pull origin "$BRANCH" >> "$LOG_FILE" 2>&1

# Install/update dependencies if package.json changed
if git diff HEAD@{1} --name-only | grep -q "package.json"; then
    log "Installing dependencies..."
    npm install >> "$LOG_FILE" 2>&1
fi

# Run database migrations if needed
if [ -f "backend/src/scripts/migrate.js" ]; then
    log "Running migrations..."
    cd backend && npm run migrate >> "$LOG_FILE" 2>&1
    cd ..
fi

# Restart the application (adjust based on your setup)
# Option 1: If using PM2
if command -v pm2 &> /dev/null; then
    log "Restarting application with PM2..."
    pm2 restart plp-api >> "$LOG_FILE" 2>&1
fi

# Option 2: If using systemd
# systemctl restart plp-attendance-api

# Option 3: If using Docker
# docker-compose restart

log "Deployment completed successfully!"