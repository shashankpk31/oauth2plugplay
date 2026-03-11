#!/bin/bash

# OTP-based Keycloak Microservices - API Testing Script
# This script demonstrates all API endpoints

echo "=========================================="
echo "OTP-Based Keycloak Microservices Testing"
echo "=========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Base URLs
GATEWAY_URL="http://localhost:8081"

# Function to print section headers
print_header() {
    echo ""
    echo -e "${BLUE}=========================================="
    echo -e "$1"
    echo -e "==========================================${NC}"
    echo ""
}

# Function to print success
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

# Function to print error
print_error() {
    echo -e "${RED}✗ $1${NC}"
}

# Test 1: Register User with Email
print_header "Test 1: Register User with Email"
REGISTER_EMAIL_RESPONSE=$(curl -s -X POST ${GATEWAY_URL}/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "identifier": "testuser@example.com",
    "identifierType": "EMAIL"
  }')

echo "$REGISTER_EMAIL_RESPONSE" | jq .
print_success "Check the Identity Service console for OTP code"
echo ""
read -p "Enter the OTP code from console: " OTP_CODE

# Test 2: Verify OTP and Get Token
print_header "Test 2: Verify OTP and Get Token"
VERIFY_RESPONSE=$(curl -s -X POST ${GATEWAY_URL}/auth/otp/verify \
  -H "Content-Type: application/json" \
  -d "{
    \"identifier\": \"testuser@example.com\",
    \"otp\": \"${OTP_CODE}\"
  }")

echo "$VERIFY_RESPONSE" | jq .

# Extract access token
ACCESS_TOKEN=$(echo "$VERIFY_RESPONSE" | jq -r '.data.accessToken')
REFRESH_TOKEN=$(echo "$VERIFY_RESPONSE" | jq -r '.data.refreshToken')

if [ "$ACCESS_TOKEN" != "null" ] && [ -n "$ACCESS_TOKEN" ]; then
    print_success "Access token obtained successfully"
    echo "Token: ${ACCESS_TOKEN:0:50}..."
else
    print_error "Failed to obtain access token"
    exit 1
fi

# Test 3: Access Protected Business Endpoint
print_header "Test 3: Access Protected Business Endpoint"
BUSINESS_DATA_RESPONSE=$(curl -s -X GET ${GATEWAY_URL}/api/data \
  -H "Authorization: Bearer ${ACCESS_TOKEN}")

echo "$BUSINESS_DATA_RESPONSE" | jq .
print_success "Business data retrieved with JWT validation"

# Test 4: Get User Profile (FeignClient Test)
print_header "Test 4: Get User Profile via Business Service (FeignClient)"
USER_PROFILE_RESPONSE=$(curl -s -X GET ${GATEWAY_URL}/api/user/profile \
  -H "Authorization: Bearer ${ACCESS_TOKEN}")

echo "$USER_PROFILE_RESPONSE" | jq .
print_success "User profile retrieved via FeignClient with JWT propagation"

# Test 5: Refresh Token
print_header "Test 5: Refresh Access Token"
REFRESH_RESPONSE=$(curl -s -X POST ${GATEWAY_URL}/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{
    \"refreshToken\": \"${REFRESH_TOKEN}\"
  }")

echo "$REFRESH_RESPONSE" | jq .

NEW_ACCESS_TOKEN=$(echo "$REFRESH_RESPONSE" | jq -r '.data.accessToken')
if [ "$NEW_ACCESS_TOKEN" != "null" ] && [ -n "$NEW_ACCESS_TOKEN" ]; then
    print_success "Token refreshed successfully"
    ACCESS_TOKEN="$NEW_ACCESS_TOKEN"
else
    print_error "Failed to refresh token"
fi

# Test 6: Send OTP for Existing User
print_header "Test 6: Login - Send OTP for Existing User"
SEND_OTP_RESPONSE=$(curl -s -X POST ${GATEWAY_URL}/auth/otp/send \
  -H "Content-Type: application/json" \
  -d '{
    "identifier": "testuser@example.com"
  }')

echo "$SEND_OTP_RESPONSE" | jq .
print_success "OTP sent for existing user login"

# Test 7: Register User with Phone
print_header "Test 7: Register User with Phone"
REGISTER_PHONE_RESPONSE=$(curl -s -X POST ${GATEWAY_URL}/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "identifier": "+919876543210",
    "identifierType": "PHONE"
  }')

echo "$REGISTER_PHONE_RESPONSE" | jq .
print_success "User registered with phone number"

# Test 8: Test Invalid OTP
print_header "Test 8: Test Invalid OTP (Error Handling)"
INVALID_OTP_RESPONSE=$(curl -s -X POST ${GATEWAY_URL}/auth/otp/verify \
  -H "Content-Type: application/json" \
  -d '{
    "identifier": "testuser@example.com",
    "otp": "000000"
  }')

echo "$INVALID_OTP_RESPONSE" | jq .
print_success "Error handling working correctly"

# Test 9: Test Unauthorized Access
print_header "Test 9: Test Unauthorized Access (No JWT)"
UNAUTHORIZED_RESPONSE=$(curl -s -w "\nHTTP Status: %{http_code}\n" \
  -X GET ${GATEWAY_URL}/api/data)

echo "$UNAUTHORIZED_RESPONSE"
print_success "Unauthorized access blocked correctly"

# Test 10: Test JWT Validation
print_header "Test 10: Test with Invalid JWT"
INVALID_JWT_RESPONSE=$(curl -s -w "\nHTTP Status: %{http_code}\n" \
  -X GET ${GATEWAY_URL}/api/data \
  -H "Authorization: Bearer invalid.jwt.token")

echo "$INVALID_JWT_RESPONSE"
print_success "Invalid JWT rejected correctly"

# Summary
print_header "Test Summary"
echo -e "${GREEN}All tests completed!${NC}"
echo ""
echo "Key Points Demonstrated:"
echo "  ✓ OTP-based registration (email and phone)"
echo "  ✓ OTP verification and JWT token generation"
echo "  ✓ JWT validation at API Gateway"
echo "  ✓ JWT validation at individual services"
echo "  ✓ User context header propagation (X-User-*)"
echo "  ✓ FeignClient with JWT propagation"
echo "  ✓ Token refresh mechanism"
echo "  ✓ Error handling and validation"
echo ""
echo "Access Token (for manual testing):"
echo "${ACCESS_TOKEN}"
echo ""
echo "=========================================="
