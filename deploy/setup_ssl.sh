#!/bin/bash
set -e

# Configuration
DOMAIN="digivoucher.cn"
EMAIL="admin@digivoucher.cn" # Used for Let's Encrypt expiration warnings

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${YELLOW}[SSL Setup] Starting SSL setup for ${DOMAIN}...${NC}"

# 1. Check for Root
if [ "$EUID" -ne 0 ]; then
  echo -e "${RED}Please run as root (sudo).${NC}"
  exit 1
fi

# 2. Install Certbot
echo -e "${GREEN}[1/3] Installing Certbot...${NC}"
if command -v apt-get &> /dev/null; then
    # Ubuntu/Debian
    apt-get update
    apt-get install -y certbot
elif command -v yum &> /dev/null; then
    # CentOS/RHEL
    yum install -y epel-release
    yum install -y certbot
else
    echo -e "${RED}Unsupported OS. Please install Certbot manually.${NC}"
    exit 1
fi

# 3. Obtain Certificate
echo -e "${GREEN}[2/3] Obtaining Certificate...${NC}"

# Check if certificate already exists
if [ -d "/etc/letsencrypt/live/${DOMAIN}" ]; then
    echo -e "${YELLOW}Certificate for ${DOMAIN} exists. Checking for expansion...${NC}"
fi

# Stop Nginx temporarily to use standalone mode (avoids config conflicts)
echo "Stopping Nginx to free up port 80..."
systemctl stop nginx || true

# Request Cert (Include www and use --expand to update existing cert)
certbot certonly --standalone \
    -d "${DOMAIN}" -d "www.${DOMAIN}" \
    --expand \
    --non-interactive \
    --agree-tos \
    -m "${EMAIL}"

echo "Starting Nginx..."
systemctl start nginx || true

# 4. Setup Auto-Renewal
echo -e "${GREEN}[3/3] Setting up Auto-Renewal...${NC}"
# Certbot usually installs a systemd timer or cron job automatically.
# We'll just test it.
certbot renew --dry-run

echo -e "${GREEN}[Success] SSL Certificates obtained!${NC}"
echo -e "Location: /etc/letsencrypt/live/${DOMAIN}/"
echo -e "Next step: Run deploy.sh to apply the Nginx SSL configuration."
