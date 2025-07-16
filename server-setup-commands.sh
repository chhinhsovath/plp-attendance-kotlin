#!/bin/bash
# Server setup commands for automatic deployment
# Run these commands after SSHing into your server

# 1. First, check if git is installed
echo "Checking git installation..."
git --version || sudo apt-get update && sudo apt-get install git -y

# 2. Navigate to the project directory
cd /var/csv/plp_attendance_api

# 3. Check if repository exists, if not clone it
if [ ! -d ".git" ]; then
    echo "Cloning repository..."
    git clone https://github.com/chhinhsovath/plp-attendance-kotlin.git .
else
    echo "Repository already exists, pulling latest changes..."
    git pull origin main
fi

# 4. Create deployment script
cat > deploy.sh << 'EOF'
#!/bin/bash
# Automated deployment script

PROJECT_DIR="/var/csv/plp_attendance_api"
LOG_FILE="/var/log/plp_deploy.log"

# Create log file if it doesn't exist
sudo touch $LOG_FILE
sudo chmod 666 $LOG_FILE

echo "[$(date)] Starting deployment..." >> $LOG_FILE

cd $PROJECT_DIR

# Pull latest changes
echo "[$(date)] Pulling latest changes..." >> $LOG_FILE
git pull origin main >> $LOG_FILE 2>&1

# Check if backend directory exists
if [ -d "backend" ]; then
    cd backend
    
    # Install dependencies if package.json exists
    if [ -f "package.json" ]; then
        echo "[$(date)] Installing dependencies..." >> $LOG_FILE
        npm install >> $LOG_FILE 2>&1
    fi
    
    # Run migrations if migrate script exists
    if [ -f "src/scripts/migrate.js" ]; then
        echo "[$(date)] Running migrations..." >> $LOG_FILE
        npm run migrate >> $LOG_FILE 2>&1
    fi
    
    # Restart application
    # Check if PM2 is being used
    if pm2 list | grep -q "plp-api"; then
        echo "[$(date)] Restarting PM2 application..." >> $LOG_FILE
        pm2 restart plp-api >> $LOG_FILE 2>&1
    else
        echo "[$(date)] PM2 process not found. Please start manually." >> $LOG_FILE
    fi
fi

echo "[$(date)] Deployment completed!" >> $LOG_FILE
EOF

# Make deployment script executable
chmod +x deploy.sh

# 5. Set up a cron job for automatic pulling (optional)
# This will check for updates every 5 minutes
(crontab -l 2>/dev/null; echo "*/5 * * * * /var/csv/plp_attendance_api/deploy.sh") | crontab -

echo "Setup completed! Deployment script created at: /var/csv/plp_attendance_api/deploy.sh"