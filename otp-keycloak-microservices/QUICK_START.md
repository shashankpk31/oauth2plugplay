# Quick Start Guide

Get up and running in 10 minutes!

## Prerequisites
- Java 17+
- Maven 3.6+
- Docker & Docker Compose
- PostgreSQL (via Docker)

---

## Step 1: Start Infrastructure (2 minutes)

```bash
# Start Keycloak and PostgreSQL
docker-compose up -d

# Wait for Keycloak to be ready (takes ~30 seconds)
docker-compose logs -f keycloak
# Look for: "Keycloak 23.0.0 started"
```

---

## Step 2: Configure Keycloak (3 minutes)

### A. Access Keycloak Admin Console
- URL: http://localhost:8180/admin
- Username: `admin`
- Password: `admin`

### B. Create Realm
1. Click **"Create Realm"**
2. Name: `myrealm`
3. Click **"Create"**

### C. Create Client
1. Go to **Clients** → **Create Client**
2. Client ID: `backend-service`
3. Click **"Next"**
4. Enable: **"Client authentication"**
5. Click **"Save"**

### D. Get Client Secret
1. Go to **Clients** → `backend-service` → **Credentials** tab
2. Copy the **"Client secret"**
3. Save it for next step

### E. Create Service Account (IMPORTANT!)
1. Go to **Users** → **Add User**
   - Username: `service-user-manager`
   - Email Verified: **ON**
   - Click **"Create"**

2. Go to **Credentials** tab
   - Click **"Set Password"**
   - Password: `ServiceUser@123!` (use a strong password!)
   - Temporary: **OFF**
   - Click **"Save"**

3. Go to **Role Mappings** tab
   - Client Roles: Select `realm-management`
   - Assign these roles:
     - `view-users`
     - `manage-users`
     - `query-users`

---

## Step 3: Setup Environment (2 minutes)

Create `.env` file in project root:

```bash
# Generate encryption key
openssl rand -base64 32

# Create .env file
cat > .env << 'EOF'
KEYCLOAK_SERVER_URL=http://localhost:8180
KEYCLOAK_REALM=myrealm
KEYCLOAK_CLIENT_ID=backend-service
KEYCLOAK_CLIENT_SECRET=YOUR_CLIENT_SECRET_FROM_STEP_2D
KEYCLOAK_SERVICE_USERNAME=service-user-manager
KEYCLOAK_SERVICE_PASSWORD=ServiceUser@123!
MPIN_ENCRYPTION_KEY=YOUR_GENERATED_KEY_FROM_OPENSSL
EOF
```

**Replace:**
- `YOUR_CLIENT_SECRET_FROM_STEP_2D` with actual client secret
- `YOUR_GENERATED_KEY_FROM_OPENSSL` with generated key

---

## Step 4: Build Services (2 minutes)

```bash
# Build all services
mvn clean package -DskipTests
```

---

## Step 5: Run Services (1 minute)

**Terminal 1 - Identity Service:**
```bash
cd identity-auth-service
export $(cat ../.env | xargs)  # Load environment variables
mvn spring-boot:run
```

**Terminal 2 - Business Service:**
```bash
cd business-service
export $(cat ../.env | xargs)
mvn spring-boot:run
```

**Terminal 3 - API Gateway:**
```bash
cd api-gateway
export $(cat ../.env | xargs)
mvn spring-boot:run
```

Wait for all services to start (look for "Started [ServiceName]Application")

---

## Step 6: Test It! (2 minutes)

### Test 1: Register User with OTP

```bash
curl -X POST http://localhost:8081/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "identifier": "test@example.com",
    "identifierType": "EMAIL"
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "OTP sent successfully"
}
```

**Check Identity Service Console for OTP:**
```
[OTP] Code for test@example.com: 123456
```

### Test 2: Verify OTP

```bash
curl -X POST http://localhost:8081/auth/otp/verify \
  -H "Content-Type: application/json" \
  -d '{
    "identifier": "test@example.com",
    "otp": "123456"
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Authentication successful",
  "data": {
    "accessToken": "eyJhbGci...",
    "refreshToken": "eyJhbGci...",
    "expiresIn": 3600,
    "user": {
      "id": "uuid",
      "email": "test@example.com"
    }
  }
}
```

**Save the accessToken for next steps!**

### Test 3: Set MPIN

```bash
# Replace YOUR_ACCESS_TOKEN with actual token from Test 2
curl -X POST http://localhost:8081/auth/mpin/set \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "mpin": "1234",
    "confirmMpin": "1234"
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "MPIN set successfully"
}
```

### Test 4: Login with MPIN (Quick Login!)

```bash
curl -X POST http://localhost:8081/auth/mpin/login \
  -H "Content-Type: application/json" \
  -d '{
    "identifier": "test@example.com",
    "mpin": "1234"
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "MPIN authentication successful",
  "data": {
    "accessToken": "eyJhbGci...",
    "refreshToken": "eyJhbGci...",
    "expiresIn": 3600
  }
}
```

✅ **SUCCESS!** You just logged in with MPIN (no OTP needed!)

### Test 5: Access Protected Resource

```bash
# Replace YOUR_ACCESS_TOKEN with token from Test 4
curl -X GET http://localhost:8081/api/data \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

**Expected Response:**
```json
{
  "success": true,
  "data": {
    "message": "This is protected business data",
    "userId": "uuid"
  }
}
```

---

## 🎉 You're Done!

Your microservices are now running with:
- ✅ OTP authentication
- ✅ MPIN quick login
- ✅ JWT validation
- ✅ Service-to-service communication
- ✅ Secure Keycloak integration

---

## Common Issues

### Issue: "Environment variable not set"
**Solution:**
```bash
# Make sure to export variables before running
export $(cat .env | xargs)
```

### Issue: Keycloak connection refused
**Solution:**
```bash
# Check Keycloak is running
docker-compose ps keycloak

# Check logs
docker-compose logs keycloak
```

### Issue: Port already in use
**Solution:**
```bash
# Find and kill process using port
# Linux/Mac:
lsof -ti:8081 | xargs kill -9

# Windows:
netstat -ano | findstr :8081
taskkill /PID <PID> /F
```

### Issue: Database connection error
**Solution:**
```bash
# Restart PostgreSQL
docker-compose restart postgres postgres-business

# Check logs
docker-compose logs postgres
```

---

## Next Steps

1. ✅ **Read Documentation:**
   - `README.md` - Full documentation
   - `ARCHITECTURE.md` - System design
   - `MIGRATION_GUIDE.md` - Migration details

2. ✅ **Test More Features:**
   - Try phone-based OTP
   - Test token refresh
   - Test MPIN status endpoint
   - Test removing MPIN

3. ✅ **Integrate with Frontend:**
   - Use provided API endpoints
   - Store JWT in SecureStore
   - Implement MPIN flow in UI

4. ✅ **Production Setup:**
   - Configure production URLs
   - Set up proper database
   - Use secrets management
   - Enable monitoring

---

## API Endpoints Reference

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/auth/register` | POST | Public | Register with OTP |
| `/auth/otp/send` | POST | Public | Send OTP |
| `/auth/otp/verify` | POST | Public | Verify OTP |
| `/auth/refresh` | POST | Public | Refresh token |
| `/auth/mpin/set` | POST | Protected | Set MPIN |
| `/auth/mpin/login` | POST | Public | Login with MPIN |
| `/auth/mpin/status` | GET | Protected | Check MPIN |
| `/auth/mpin` | DELETE | Protected | Remove MPIN |
| `/api/data` | GET | Protected | Get data |

---

## Stopping Services

```bash
# Stop Spring Boot services
# Press Ctrl+C in each terminal

# Stop Docker containers
docker-compose down

# Remove volumes (optional - cleans database)
docker-compose down -v
```

---

## Need Help?

1. Check logs for detailed errors
2. Review `KEYCLOAK_SERVICE_ACCOUNT_SETUP.md`
3. Check `MIGRATION_GUIDE.md` troubleshooting section
4. Verify all environment variables are set
5. Ensure Keycloak is fully started before running services

---

**Happy coding!** 🚀
