# Setup Guide

This guide will walk you through setting up the entire OTP-based Keycloak microservices architecture.

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Docker and Docker Compose
- Git (optional)

## Step 1: Start Infrastructure

### Start Keycloak and PostgreSQL

```bash
# Navigate to project directory
cd otp-keycloak-microservices

# Start Docker containers
docker-compose up -d

# Wait for services to be ready (about 30-60 seconds)
docker-compose logs -f keycloak
```

Wait until you see: `Keycloak <version> started`

Press `Ctrl+C` to exit logs.

### Verify Services

```bash
# Check if all containers are running
docker-compose ps

# You should see:
# - postgres (port 5432)
# - postgres-auth (port 5433)
# - postgres-business (port 5434)
# - keycloak (port 8180)
```

## Step 2: Configure Keycloak

### Access Keycloak Admin Console

Open browser: http://localhost:8180/admin

**Login credentials:**
- Username: `admin`
- Password: `admin`

### Create Realm

1. Click on dropdown "master" (top left)
2. Click "Create Realm"
3. **Realm name:** `myrealm`
4. Click "Create"

### Create Client

1. Go to **Clients** → Click "Create client"
2. **General Settings:**
   - Client type: `OpenID Connect`
   - Client ID: `backend-service`
   - Click "Next"

3. **Capability config:**
   - Client authentication: `ON`
   - Authorization: `OFF`
   - Authentication flow:
     - ✅ Standard flow
     - ✅ Direct access grants (IMPORTANT!)
     - ✅ Service accounts roles
   - Click "Next"

4. **Login settings:**
   - Valid redirect URIs: `*`
   - Valid post logout redirect URIs: `*`
   - Web origins: `*`
   - Click "Save"

### Get Client Secret

1. Go to **Clients** → `backend-service` → **Credentials** tab
2. Copy the **Client secret** value
3. **Save this secret** - you'll need it in the next step

Example: `9xK7vM2pR8qL3nH5jW9fD1cA4bE6gT0s`

### Enable Direct Access Grants (Verify)

1. Go to **Clients** → `backend-service` → **Settings** tab
2. Scroll to **Capability config**
3. Ensure "Direct access grants" is **enabled**
4. Click "Save" if you made changes

## Step 3: Update Configuration

### Update Identity Auth Service Config

Edit: `identity-auth-service/src/main/resources/application.yml`

Find the line:
```yaml
keycloak:
  client-secret: REPLACE_WITH_YOUR_CLIENT_SECRET
```

Replace with your actual client secret:
```yaml
keycloak:
  client-secret: 9xK7vM2pR8qL3nH5jW9fD1cA4bE6gT0s
```

Save the file.

## Step 4: Build All Services

### Build API Gateway

```bash
cd api-gateway
mvn clean package -DskipTests
cd ..
```

### Build Identity Auth Service

```bash
cd identity-auth-service
mvn clean package -DskipTests
cd ..
```

### Build Business Service

```bash
cd business-service
mvn clean package -DskipTests
cd ..
```

**Note:** If you encounter build errors, ensure Java 17 is being used:
```bash
java -version
# Should show Java 17

# Set JAVA_HOME if needed
export JAVA_HOME=/path/to/java17
```

## Step 5: Run Services

Open **3 separate terminal windows**:

### Terminal 1: Identity Auth Service

```bash
cd identity-auth-service
mvn spring-boot:run
```

Wait until you see: `Started IdentityAuthServiceApplication`

### Terminal 2: Business Service

```bash
cd business-service
mvn spring-boot:run
```

Wait until you see: `Started BusinessServiceApplication`

### Terminal 3: API Gateway

```bash
cd api-gateway
mvn spring-boot:run
```

Wait until you see: `Started ApiGatewayApplication`

## Step 6: Verify Setup

### Check Service Health

```bash
# API Gateway
curl http://localhost:8081/actuator/health

# Identity Auth Service
curl http://localhost:8082/actuator/health

# Business Service
curl http://localhost:8083/actuator/health
```

All should return: `{"status":"UP"}`

### Check Keycloak Connectivity

```bash
curl http://localhost:8180/realms/myrealm/.well-known/openid-configuration
```

Should return JSON with Keycloak configuration.

## Step 7: Test the System

### Test 1: Register User with Email

```bash
curl -X POST http://localhost:8081/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "identifier": "john.doe@example.com",
    "identifierType": "EMAIL"
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "OTP sent successfully",
  "data": {
    "identifier": "john.doe@example.com",
    "expiresIn": 300
  },
  "timestamp": "2024-03-11T10:30:00"
}
```

**Check Terminal 1 (Identity Service) for OTP:**
```
==========================================
[OTP] Code for john.doe@example.com: 123456
[OTP] Expires at: 2024-03-11T10:35:00
==========================================
```

### Test 2: Verify OTP

```bash
curl -X POST http://localhost:8081/auth/otp/verify \
  -H "Content-Type: application/json" \
  -d '{
    "identifier": "john.doe@example.com",
    "otp": "123456"
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Authentication successful",
  "data": {
    "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 300,
    "tokenType": "Bearer",
    "user": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "keycloakId": "keycloak-uuid",
      "email": "john.doe@example.com",
      "phone": null
    }
  }
}
```

**Save the `accessToken` for next steps!**

### Test 3: Access Protected Business Endpoint

```bash
# Replace YOUR_ACCESS_TOKEN with the token from Test 2
curl -X GET http://localhost:8081/api/data \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

**Expected Response:**
```json
{
  "success": true,
  "data": {
    "message": "This is protected business data",
    "timestamp": "2024-03-11T10:31:00",
    "userId": "keycloak-user-id",
    "dataType": "business-metrics"
  },
  "userContext": {
    "userId": "keycloak-user-id",
    "email": "john.doe@example.com",
    "phone": ""
  }
}
```

### Test 4: Get User Profile (FeignClient Test)

```bash
curl -X GET http://localhost:8081/api/user/profile \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

This tests JWT propagation from Business Service to Identity Service via FeignClient.

### Test 5: Register with Phone

```bash
curl -X POST http://localhost:8081/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "identifier": "+919876543210",
    "identifierType": "PHONE"
  }'
```

Check console for OTP and verify same as Test 2.

## Troubleshooting

### Issue: Keycloak not accessible

**Solution:**
```bash
# Check Keycloak logs
docker-compose logs keycloak

# Restart Keycloak
docker-compose restart keycloak
```

### Issue: Database connection error

**Solution:**
```bash
# Check if PostgreSQL containers are running
docker-compose ps

# Restart all containers
docker-compose restart
```

### Issue: Port already in use

**Solution:**
```bash
# Find process using port
netstat -ano | findstr :8081

# Kill process or change port in application.yml
```

### Issue: JWT validation failed

**Solution:**
- Verify Keycloak is running: http://localhost:8180
- Check `issuer-uri` in application.yml matches realm
- Verify client secret is correct

### Issue: OTP not showing in console

**Solution:**
- Check Identity Auth Service terminal
- Look for `[OTP]` prefix in logs
- Ensure service is running and not crashed

### Issue: FeignClient connection refused

**Solution:**
- Verify Identity Service is running on port 8082
- Check `identity-service.url` in Business Service config
- Verify JWT is being propagated (check logs)

## Next Steps

1. **Explore the APIs** using the examples in README.md
2. **Add React Native frontend** integration
3. **Implement MPIN** feature (optional)
4. **Add email service** for real OTP delivery
5. **Set up monitoring** with ELK stack or Prometheus

## Stopping Services

### Stop Spring Boot Services
Press `Ctrl+C` in each terminal window

### Stop Docker Containers
```bash
docker-compose down

# To also remove volumes (WARNING: deletes all data)
docker-compose down -v
```

## Cleaning Up

```bash
# Remove all Docker containers and volumes
docker-compose down -v

# Remove built JAR files
cd api-gateway && mvn clean && cd ..
cd identity-auth-service && mvn clean && cd ..
cd business-service && mvn clean && cd ..
```

## Production Considerations

1. **Use HTTPS** in production
2. **Secure client secrets** using environment variables or secret management
3. **Set up proper CORS** configuration
4. **Implement rate limiting** for OTP requests
5. **Add monitoring and alerting**
6. **Use production-grade PostgreSQL** setup
7. **Implement proper logging** strategy
8. **Add API documentation** (Swagger/OpenAPI)
9. **Set up CI/CD** pipelines
10. **Implement backup and recovery** procedures

---

**Congratulations!** Your OTP-based Keycloak microservices architecture is now set up and running.
