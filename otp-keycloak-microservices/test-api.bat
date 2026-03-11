@echo off
REM OTP-based Keycloak Microservices - API Testing Script (Windows)
REM This script demonstrates all API endpoints

echo ==========================================
echo OTP-Based Keycloak Microservices Testing
echo ==========================================
echo.

set GATEWAY_URL=http://localhost:8081

REM Test 1: Register User with Email
echo ==========================================
echo Test 1: Register User with Email
echo ==========================================
echo.

curl -X POST %GATEWAY_URL%/auth/register ^
  -H "Content-Type: application/json" ^
  -d "{\"identifier\": \"testuser@example.com\", \"identifierType\": \"EMAIL\"}"

echo.
echo Check the Identity Service console for OTP code
echo.
set /p OTP_CODE="Enter the OTP code from console: "

REM Test 2: Verify OTP and Get Token
echo.
echo ==========================================
echo Test 2: Verify OTP and Get Token
echo ==========================================
echo.

curl -X POST %GATEWAY_URL%/auth/otp/verify ^
  -H "Content-Type: application/json" ^
  -d "{\"identifier\": \"testuser@example.com\", \"otp\": \"%OTP_CODE%\"}" > response.json

type response.json
echo.

REM Note: Manual token extraction needed for Windows batch
echo.
echo Please copy the access token from above response for next tests
set /p ACCESS_TOKEN="Enter the access token: "

REM Test 3: Access Protected Business Endpoint
echo.
echo ==========================================
echo Test 3: Access Protected Business Endpoint
echo ==========================================
echo.

curl -X GET %GATEWAY_URL%/api/data ^
  -H "Authorization: Bearer %ACCESS_TOKEN%"

echo.

REM Test 4: Get User Profile
echo.
echo ==========================================
echo Test 4: Get User Profile (FeignClient Test)
echo ==========================================
echo.

curl -X GET %GATEWAY_URL%/api/user/profile ^
  -H "Authorization: Bearer %ACCESS_TOKEN%"

echo.

REM Test 5: Register with Phone
echo.
echo ==========================================
echo Test 5: Register User with Phone
echo ==========================================
echo.

curl -X POST %GATEWAY_URL%/auth/register ^
  -H "Content-Type: application/json" ^
  -d "{\"identifier\": \"+919876543210\", \"identifierType\": \"PHONE\"}"

echo.
echo.
echo ==========================================
echo All tests completed!
echo ==========================================
echo.

REM Cleanup
del response.json 2>nul

pause
