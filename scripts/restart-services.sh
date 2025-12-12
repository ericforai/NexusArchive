#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Get script directory and project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
RUN_DIR="$PROJECT_ROOT/.run"
LOGS_DIR="$PROJECT_ROOT/logs"

# Ensure runtime directories exist
mkdir -p "$RUN_DIR" "$LOGS_DIR"

echo -e "${YELLOW}=========================================${NC}"
echo -e "${YELLOW}   NexusArchive Service Restarter        ${NC}"
echo -e "${YELLOW}=========================================${NC}"

# Function to kill process on a specific port
kill_port() {
    local port=$1
    local pid=$(lsof -t -i:$port)
    
    if [ -n "$pid" ]; then
        echo -e "${YELLOW}Port $port is in use by PID $pid. Killing...${NC}"
        kill -9 $pid
        echo -e "${GREEN}Port $port freed.${NC}"
    else
        echo -e "${GREEN}Port $port is free.${NC}"
    fi
}

# 1. Clean up ports
echo -e "\n${YELLOW}[1/3] Cleaning up ports...${NC}"
kill_port 8080
kill_port 5173

# Load environment variables from .env files
if [ -f "$PROJECT_ROOT/.env" ]; then
    echo -e "${GREEN}Loading environment from .env${NC}"
    set -a
    source "$PROJECT_ROOT/.env"
    set +a
fi

if [ -f "$PROJECT_ROOT/.env.local" ]; then
    echo -e "${GREEN}Loading environment from .env.local${NC}"
    set -a
    source "$PROJECT_ROOT/.env.local"
    set +a
fi

# 2. Start Backend
echo -e "\n${YELLOW}[2/3] Starting Backend (Port 8080)...${NC}"
cd "$PROJECT_ROOT/nexusarchive-java"
mvn spring-boot:run > "$LOGS_DIR/backend.log" 2>&1 &
BACKEND_PID=$!
echo "$BACKEND_PID" > "$RUN_DIR/backend.pid"
echo -e "${GREEN}Backend started with PID $BACKEND_PID. Logs: logs/backend.log${NC}"

# 3. Start Frontend
echo -e "\n${YELLOW}[3/3] Starting Frontend (Port 5173)...${NC}"
cd "$PROJECT_ROOT"
npm run dev > "$LOGS_DIR/frontend.log" 2>&1 &
FRONTEND_PID=$!
echo "$FRONTEND_PID" > "$RUN_DIR/frontend.pid"
echo -e "${GREEN}Frontend started with PID $FRONTEND_PID. Logs: logs/frontend.log${NC}"

echo -e "\n${YELLOW}=========================================${NC}"
echo -e "${GREEN}Services are starting in the background.${NC}"
echo -e "Backend: http://localhost:8080/api"
echo -e "Frontend: http://localhost:5173"
echo -e "${YELLOW}=========================================${NC}"
echo -e "\nPID files saved to: $RUN_DIR"
echo -e "Log files saved to: $LOGS_DIR"
