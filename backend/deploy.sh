#!/bin/bash

# Cambodia Education Attendance System API - Deployment Script
# This script automates the deployment process to Digital Ocean server

set -e  # Exit on any error

# Configuration
SERVER_HOST="157.10.73.52"
SERVER_USER="ubuntu"
SERVER_PASSWORD="en_&xdX#!N(^OqCQzc3RE0B)m6ogU!"
APP_NAME="plp-attendance-api"
APP_DIR="/var/csv/plp_attendance_api"
NODE_VERSION="18"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Helper functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if sshpass is available
check_sshpass() {
    if ! command -v sshpass &> /dev/null; then
        log_error "sshpass is required but not installed."
        log_info "Install it with: brew install hudochenkov/sshpass/sshpass (macOS) or apt-get install sshpass (Ubuntu)"
        exit 1
    fi
}

# Execute command on remote server
remote_exec() {
    sshpass -p "${SERVER_PASSWORD}" ssh -o StrictHostKeyChecking=no "${SERVER_USER}@${SERVER_HOST}" "$1"
}

# Copy files to remote server
remote_copy() {
    sshpass -p "${SERVER_PASSWORD}" scp -o StrictHostKeyChecking=no -r "$1" "${SERVER_USER}@${SERVER_HOST}:$2"
}

# Install Node.js on server
install_nodejs() {
    log_info "Installing Node.js ${NODE_VERSION} on server..."
    
    remote_exec "
        # Update system
        sudo apt update && sudo apt upgrade -y
        
        # Install Node.js
        curl -fsSL https://deb.nodesource.com/setup_${NODE_VERSION}.x | sudo bash -
        sudo apt-get install -y nodejs
        
        # Install PM2 globally
        sudo npm install -g pm2
        
        # Verify installation
        node --version
        npm --version
        pm2 --version
    "
}

# Install PostgreSQL if not present
install_postgresql() {
    log_info "Checking PostgreSQL installation..."
    
    remote_exec "
        if ! command -v psql &> /dev/null; then
            echo 'Installing PostgreSQL...'
            sudo apt install -y postgresql postgresql-contrib
            sudo systemctl start postgresql
            sudo systemctl enable postgresql
            
            # Set postgres user password
            sudo -u postgres psql -c \"ALTER USER postgres PASSWORD 'P@ssw0rd';\"
            
            # Create database
            sudo -u postgres createdb plp_attendance_kotlin
            
            # Enable UUID extension
            sudo -u postgres psql -d plp_attendance_kotlin -c 'CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";'
        else
            echo 'PostgreSQL already installed'
        fi
    "
}

# Deploy application
deploy_app() {
    log_info "Deploying application to ${SERVER_HOST}..."
    
    # Create application directory
    remote_exec "sudo mkdir -p ${APP_DIR} && sudo chown ${SERVER_USER}:${SERVER_USER} ${APP_DIR}"
    
    # Copy application files
    log_info "Copying application files..."
    remote_copy "." "${APP_DIR}/"
    
    # Install dependencies and setup
    log_info "Installing dependencies and setting up database..."
    remote_exec "
        cd ${APP_DIR}
        
        # Install production dependencies
        npm install --production
        
        # Run database migration and seeding
        npm run setup-db
        
        # Stop existing PM2 process if running
        pm2 stop ${APP_NAME} || true
        pm2 delete ${APP_NAME} || true
        
        # Start application with PM2
        pm2 start src/server.js --name '${APP_NAME}' --env production
        
        # Save PM2 configuration
        pm2 save
        
        # Setup PM2 startup script
        pm2 startup systemd -u ${SERVER_USER} --hp /home/${SERVER_USER}
    "
}

# Install and configure Nginx
setup_nginx() {
    log_info "Setting up Nginx reverse proxy..."
    
    remote_exec "
        # Install Nginx
        sudo apt install -y nginx
        
        # Create Nginx configuration
        sudo tee /etc/nginx/sites-available/${APP_NAME} << 'EOF'
server {
    listen 80;
    server_name ${SERVER_HOST};

    # Security headers
    add_header X-Frame-Options DENY;
    add_header X-Content-Type-Options nosniff;
    add_header X-XSS-Protection \"1; mode=block\";

    # Main application
    location / {
        proxy_pass http://localhost:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_cache_bypass \$http_upgrade;
        
        # Timeout settings
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # API documentation
    location /api-docs {
        proxy_pass http://localhost:3000/api-docs;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }

    # Health check
    location /health {
        proxy_pass http://localhost:3000/health;
        access_log off;
    }
}
EOF

        # Enable the site
        sudo ln -sf /etc/nginx/sites-available/${APP_NAME} /etc/nginx/sites-enabled/
        
        # Remove default site
        sudo rm -f /etc/nginx/sites-enabled/default
        
        # Test configuration
        sudo nginx -t
        
        # Restart Nginx
        sudo systemctl restart nginx
        sudo systemctl enable nginx
    "
}

# Setup firewall
setup_firewall() {
    log_info "Configuring firewall..."
    
    remote_exec "
        # Install UFW if not present
        sudo apt install -y ufw
        
        # Reset firewall rules
        sudo ufw --force reset
        
        # Default policies
        sudo ufw default deny incoming
        sudo ufw default allow outgoing
        
        # Allow SSH
        sudo ufw allow ssh
        
        # Allow HTTP and HTTPS
        sudo ufw allow http
        sudo ufw allow https
        
        # Allow PostgreSQL (only from localhost)
        sudo ufw allow from 127.0.0.1 to any port 5432
        
        # Enable firewall
        sudo ufw --force enable
        
        # Show status
        sudo ufw status
    "
}

# Verify deployment
verify_deployment() {
    log_info "Verifying deployment..."
    
    remote_exec "
        # Check PM2 status
        pm2 status
        
        # Check Nginx status
        sudo systemctl status nginx --no-pager
        
        # Check PostgreSQL status
        sudo systemctl status postgresql --no-pager
        
        # Test API health endpoint
        sleep 5
        curl -f http://localhost:3000/health || echo 'Health check failed'
    "
    
    log_info "Testing external access..."
    if curl -f "http://${SERVER_HOST}/health" &>/dev/null; then
        log_info "✅ External API access successful!"
    else
        log_warn "⚠️ External API access failed. Check firewall and Nginx configuration."
    fi
}

# Main deployment function
main() {
    log_info "Starting deployment of Cambodia Education Attendance System API"
    log_info "Target server: ${SERVER_HOST}"
    
    # Check prerequisites
    check_sshpass
    
    # Test server connection
    log_info "Testing server connection..."
    if ! remote_exec "echo 'Connection successful'"; then
        log_error "Failed to connect to server. Check credentials and network."
        exit 1
    fi
    
    # Deployment steps
    log_info "Step 1: Installing Node.js and PM2..."
    install_nodejs
    
    log_info "Step 2: Installing PostgreSQL..."
    install_postgresql
    
    log_info "Step 3: Deploying application..."
    deploy_app
    
    log_info "Step 4: Setting up Nginx..."
    setup_nginx
    
    log_info "Step 5: Configuring firewall..."
    setup_firewall
    
    log_info "Step 6: Verifying deployment..."
    verify_deployment
    
    log_info "🎉 Deployment completed successfully!"
    log_info ""
    log_info "API endpoints:"
    log_info "  Main API: http://${SERVER_HOST}/"
    log_info "  Documentation: http://${SERVER_HOST}/api-docs"
    log_info "  Health Check: http://${SERVER_HOST}/health"
    log_info ""
    log_info "Server management:"
    log_info "  SSH: ssh ${SERVER_USER}@${SERVER_HOST}"
    log_info "  PM2 status: pm2 status"
    log_info "  PM2 logs: pm2 logs ${APP_NAME}"
    log_info "  PM2 restart: pm2 restart ${APP_NAME}"
}

# Script options
case "${1:-deploy}" in
    "deploy")
        main
        ;;
    "verify")
        verify_deployment
        ;;
    "restart")
        log_info "Restarting application..."
        remote_exec "pm2 restart ${APP_NAME}"
        ;;
    "logs")
        log_info "Showing application logs..."
        remote_exec "pm2 logs ${APP_NAME} --lines 50"
        ;;
    "status")
        log_info "Checking application status..."
        remote_exec "pm2 status"
        ;;
    *)
        echo "Usage: $0 [deploy|verify|restart|logs|status]"
        echo ""
        echo "Commands:"
        echo "  deploy  - Full deployment (default)"
        echo "  verify  - Verify existing deployment"
        echo "  restart - Restart application"
        echo "  logs    - Show application logs"
        echo "  status  - Show PM2 status"
        exit 1
        ;;
esac