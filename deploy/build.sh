#!/bin/bash
set -e

# Colors for output
GREEN='\033[0;32m'
NC='\033[0m' # No Color

echo -e "${GREEN}[Build] Starting local build process...${NC}"

# 1. Build Frontend
echo -e "${GREEN}[Build] Building Frontend...${NC}"
# Ensure we are in root
cd "$(dirname "$0")/.."
npm install
npm run build

# 2. Build Backend
echo -e "${GREEN}[Build] Building Backend...${NC}"
cd nexusarchive-java
mvn clean package -DskipTests

# 3. Package Artifacts
echo -e "${GREEN}[Build] Packaging artifacts...${NC}"
cd ..
mkdir -p deploy/artifacts
cp -r dist deploy/artifacts/frontend
cp nexusarchive-java/target/nexusarchive-backend-2.0.0.jar deploy/artifacts/app.jar

# Create tarball
cd deploy/artifacts
tar -czf nexusarchive-release.tar.gz frontend app.jar
mv nexusarchive-release.tar.gz ..
cd ..
rm -rf artifacts

echo -e "${GREEN}[Build] Success! Artifact created at deploy/nexusarchive-release.tar.gz${NC}"
