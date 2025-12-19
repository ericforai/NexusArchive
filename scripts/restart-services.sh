#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

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
kill_port 9090
kill_port 5175

# 2. Start Backend
echo -e "\n${YELLOW}[2/3] Starting Backend (Port 9090)...${NC}"
cd nexusarchive-java
mvn spring-boot:run > ../backend.log 2>&1 &
BACKEND_PID=$!
echo -e "${GREEN}Backend started with PID $BACKEND_PID. Logs: backend.log${NC}"
cd ..

# 3. Start Frontend
echo -e "\n${YELLOW}[3/3] Starting Frontend (Port 5175)...${NC}"
npm run dev > frontend.log 2>&1 &
FRONTEND_PID=$!
echo -e "${GREEN}Frontend started with PID $FRONTEND_PID. Logs: frontend.log${NC}"

echo -e "\n${YELLOW}=========================================${NC}"
echo -e "${GREEN}Services are starting in the background.${NC}"
echo -e "Backend: http://localhost:9090/api"
echo -e "Frontend: http://localhost:5175"
echo -e "${YELLOW}=========================================${NC}"
