#!/bin/bash

# Quick Start Script for OTP-based Keycloak Microservices
# This script builds and runs all services

set -e

echo "=========================================="
echo "OTP-based Keycloak Microservices"
echo "Quick Start Script"
echo "=========================================="
echo ""

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}Error: Docker is not running. Please start Docker first.${NC}"
    exit 1
fi

# Step 1: Start Docker containers
echo -e "${BLUE}Step 1: Starting Docker containers...${NC}"
docker-compose up -d

echo "Waiting for Keycloak to start (this may take 60 seconds)..."
sleep 60

# Check if Keycloak is ready
echo "Checking Keycloak status..."
for i in {1..30}; do
    if curl -s http://localhost:8180/health/ready > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Keycloak is ready${NC}"
        break
    fi
    echo "Waiting for Keycloak... ($i/30)"
    sleep 2
done

echo ""

# Step 2: Build services
echo -e "${BLUE}Step 2: Building services...${NC}"

echo "Building API Gateway..."
cd api-gateway
mvn clean package -DskipTests -q
cd ..
echo -e "${GREEN}✓ API Gateway built${NC}"

echo "Building Identity Auth Service..."
cd identity-auth-service
mvn clean package -DskipTests -q
cd ..
echo -e "${GREEN}✓ Identity Auth Service built${NC}"

echo "Building Business Service..."
cd business-service
mvn clean package -DskipTests -q
cd ..
echo -e "${GREEN}✓ Business Service built${NC}"

echo ""

# Step 3: Instructions
echo -e "${YELLOW}=========================================="
echo "Build Complete!"
echo "==========================================${NC}"
echo ""
echo "Next steps:"
echo ""
echo "1. Configure Keycloak:"
echo "   - Open: http://localhost:8180/admin"
echo "   - Login: admin / admin"
echo "   - Create realm: myrealm"
echo "   - Create client: backend-service"
echo "   - Enable 'Direct access grants'"
echo "   - Copy client secret"
echo ""
echo "2. Update identity-auth-service/src/main/resources/application.yml"
echo "   with your client secret"
echo ""
echo "3. Run services (in separate terminals):"
echo ""
echo "   Terminal 1:"
echo "   cd identity-auth-service && mvn spring-boot:run"
echo ""
echo "   Terminal 2:"
echo "   cd business-service && mvn spring-boot:run"
echo ""
echo "   Terminal 3:"
echo "   cd api-gateway && mvn spring-boot:run"
echo ""
echo "4. Test the APIs:"
echo "   ./test-api.sh"
echo ""
echo -e "${GREEN}For detailed instructions, see SETUP_GUIDE.md${NC}"
echo ""
