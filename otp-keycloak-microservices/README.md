# OTP-Based Keycloak Microservices Architecture

Complete microservices setup with OTP authentication, MPIN quick login, JWT propagation, and Keycloak integration.

## 🎯 Features

### Authentication
- **OTP Authentication**: Email or Phone based OTP (console logging for testing)
- **MPIN Quick Login**: 4-6 digit PIN for fast login (like banking apps)
- **Session Management**: MPIN sessions expire after 30 days
- **JWT Tokens**: Secure token-based authentication via Keycloak

### Security
- **Multi-Layer JWT Validation**: At Gateway, ServiceA, and ServiceB
- **Encrypted MPIN Storage**: AES-256-GCM encryption
- **Service Account**: Secure Keycloak integration (no admin credentials in code)
- **Failed Attempt Limiting**: Protection against brute force attacks

### Architecture
- **Spring Cloud Gateway MVC**: Blocking/servlet-based gateway
- **User Context Propagation**: X-User-* headers throughout the system
- **FeignClient Integration**: JWT propagation between services
- **Stateless Design**: No session state, fully JWT-based

## 🏗️ Architecture

```
React Native App
       ↓
API Gateway (8081) - JWT Validation & Header Injection
       ↓
    ┌──┴──┐
    ↓     ↓
ServiceA  ServiceB
(8082)    (8083)
Identity  Business
  ↓          ↓
  ↓    (FeignClient)
  ↓          ↓
Keycloak (8180)
```

## 📊 Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Java | OpenJDK | 17 |
| Spring Boot | Spring Boot | 3.5.11 |
| Spring Cloud | Gateway MVC | 2025.0.1 |
| Spring Security | OAuth2 Resource Server | Latest |
| Keycloak | Keycloak | 23.0.0 |
| Database | PostgreSQL | 15 |
| Encryption | AES-256-GCM | - |

## Services

### 1. API Gateway (Port 8081)
- Routes requests to services
- Validates JWT tokens
- Extracts user claims and adds headers
- Routes:
  - `/auth/**` → Identity Auth Service
  - `/api/**` → Business Service

### 2. Identity Auth Service (Port 8082)
- OTP generation and verification
- User registration
- JWT token issuance via Keycloak
- Endpoints:
  - `POST /auth/register` - Register with email/phone
  - `POST /auth/otp/send` - Send OTP
  - `POST /auth/otp/verify` - Verify OTP and get JWT
  - `POST /auth/refresh` - Refresh access token

### 3. Business Service (Port 8083)
- Business logic APIs
- JWT validation
- FeignClient to Identity Service
- Endpoints:
  - `GET /api/data` - Sample protected endpoint
  - `GET /api/user/profile` - Get user profile

## 📋 Prerequisites

- Java 17+
- Maven 3.6+
- Docker & Docker Compose
- PostgreSQL (via Docker)
- OpenSSL (for generating encryption keys)

## Quick Start

### 1. Start Infrastructure

```bash
# Start Keycloak and PostgreSQL
docker-compose up -d

# Wait for Keycloak to be ready (check logs)
docker-compose logs -f keycloak
```

### 2. Configure Keycloak

Access Keycloak Admin Console: http://localhost:8180/admin
- Username: admin
- Password: admin

#### Create Realm
1. Click "Create Realm"
2. Name: `myrealm`
3. Click "Create"

#### Create Client
1. Go to Clients → Create Client
2. Client ID: `backend-service`
3. Client authentication: ON
4. Valid redirect URIs: `*`
5. Web origins: `*`
6. Save

#### Enable Direct Access Grants
1. Clients → backend-service → Settings
2. Enable "Direct access grants"
3. Save

#### Get Client Secret
1. Clients → backend-service → Credentials
2. Copy the "Client secret"
3. Save it for environment variables setup

#### Setup Service Account (IMPORTANT for Security!)
Instead of using admin credentials, create a dedicated service account:

1. Follow instructions in `KEYCLOAK_SERVICE_ACCOUNT_SETUP.md`
2. Create service user: `service-user-manager`
3. Assign roles: `view-users`, `manage-users`, `query-users`
4. Set a strong password

**Why?** Using admin credentials in code is a critical security risk!

### 2.5. Setup Environment Variables

Create a `.env` file in project root (add to `.gitignore`):

```bash
# Keycloak Configuration
KEYCLOAK_SERVER_URL=http://localhost:8180
KEYCLOAK_REALM=myrealm
KEYCLOAK_CLIENT_ID=backend-service
KEYCLOAK_CLIENT_SECRET=your-client-secret-from-keycloak
KEYCLOAK_SERVICE_USERNAME=service-user-manager
KEYCLOAK_SERVICE_PASSWORD=your-strong-service-password

# MPIN Encryption Key (generate with: openssl rand -base64 32)
MPIN_ENCRYPTION_KEY=your-base64-encoded-encryption-key
```

**Generate MPIN encryption key:**
```bash
openssl rand -base64 32
```

Copy the output to `MPIN_ENCRYPTION_KEY` in your `.env` file.

### 3. Build Services

```bash
# Build API Gateway
cd api-gateway
mvn clean package
cd ..

# Build Identity Auth Service
cd identity-auth-service
mvn clean package
cd ..

# Build Business Service
cd business-service
mvn clean package
cd ..
```

### 4. Run Services

```bash
# Terminal 1 - Identity Auth Service
cd identity-auth-service
mvn spring-boot:run

# Terminal 2 - Business Service
cd business-service
mvn spring-boot:run

# Terminal 3 - API Gateway
cd api-gateway
mvn spring-boot:run
```

## API Usage Examples

### 1. Register User

```bash
# Register with email
curl -X POST http://localhost:8081/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "identifier": "user@example.com",
    "identifierType": "EMAIL"
  }'

# OR Register with phone
curl -X POST http://localhost:8081/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "identifier": "+919876543210",
    "identifierType": "PHONE"
  }'
```

**Response:**
```json
{
  "success": true,
  "message": "OTP sent successfully",
  "data": {
    "identifier": "user@example.com",
    "expiresIn": 300
  }
}
```

**Check console for OTP** - it will be logged like:
```
[OTP] Code for user@example.com: 123456
```

### 2. Verify OTP

```bash
curl -X POST http://localhost:8081/auth/otp/verify \
  -H "Content-Type: application/json" \
  -d '{
    "identifier": "user@example.com",
    "otp": "123456"
  }'
```

**Response:**
```json
{
  "success": true,
  "message": "Authentication successful",
  "data": {
    "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 3600,
    "tokenType": "Bearer",
    "user": {
      "id": "uuid",
      "email": "user@example.com",
      "phone": null
    }
  }
}
```

### 3. Login (Existing User)

```bash
# Send OTP
curl -X POST http://localhost:8081/auth/otp/send \
  -H "Content-Type: application/json" \
  -d '{
    "identifier": "user@example.com"
  }'

# Verify OTP (same as step 2)
```

### 4. Access Protected Endpoint

```bash
# Get data from Business Service
curl -X GET http://localhost:8081/api/data \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

**Response:**
```json
{
  "success": true,
  "data": {
    "message": "This is protected business data",
    "timestamp": "2024-03-11T10:30:00",
    "userId": "uuid-from-jwt"
  }
}
```

### 5. Get User Profile

```bash
curl -X GET http://localhost:8081/api/user/profile \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "email": "user@example.com",
    "phone": "+919876543210",
    "keycloakId": "keycloak-uuid"
  }
}
```

### 6. Refresh Token

```bash
curl -X POST http://localhost:8081/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "YOUR_REFRESH_TOKEN"
  }'
```

### 7. Set MPIN (Protected - After OTP Login)

```bash
# User must be authenticated with OTP first to set MPIN
curl -X POST http://localhost:8081/auth/mpin/set \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "mpin": "1234",
    "confirmMpin": "1234"
  }'
```

**Response:**
```json
{
  "success": true,
  "message": "MPIN set successfully",
  "data": "MPIN has been configured for quick login"
}
```

### 8. Login with MPIN (Public - Quick Login)

```bash
# Fast login using MPIN (no OTP needed)
curl -X POST http://localhost:8081/auth/mpin/login \
  -H "Content-Type: application/json" \
  -d '{
    "identifier": "user@example.com",
    "mpin": "1234"
  }'
```

**Response:**
```json
{
  "success": true,
  "message": "MPIN authentication successful",
  "data": {
    "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 3600,
    "tokenType": "Bearer",
    "user": {
      "id": "uuid",
      "email": "user@example.com"
    }
  }
}
```

### 9. Check MPIN Status

```bash
# Check if MPIN is set and valid
curl -X GET http://localhost:8081/auth/mpin/status \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

**Response:**
```json
{
  "success": true,
  "data": {
    "isSet": true,
    "isValid": true,
    "sessionExpiresAt": "2024-04-10T10:30:00",
    "failedAttempts": 0,
    "lastUsedAt": "2024-03-11T10:30:00"
  }
}
```

### 10. Remove MPIN

```bash
# Delete MPIN (user must login with OTP again)
curl -X DELETE http://localhost:8081/auth/mpin \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

**Response:**
```json
{
  "success": true,
  "message": "MPIN deleted successfully",
  "data": "You will need to login with OTP"
}
```

## 🔐 MPIN Quick Login Workflow

MPIN (Mobile PIN) provides a quick login experience similar to banking apps:

### When to Use MPIN?

**First Time User:**
1. Register with OTP
2. Login with OTP (get JWT tokens)
3. Optionally set MPIN for future quick logins
4. Next time: Login directly with MPIN (no OTP needed!)

**Returning User with MPIN:**
1. Enter identifier + MPIN
2. Get JWT tokens instantly (no OTP wait)
3. MPIN valid for 30 days (configurable)
4. After expiry: Must login with OTP again to renew

### MPIN Security Features

- **Encrypted Storage**: AES-256-GCM encryption (never stored in plaintext)
- **Session Expiry**: Default 30 days (like banking apps)
- **Failed Attempt Limit**: 3 attempts, then blocked
- **Auto Cleanup**: Expired sessions automatically cleaned
- **Per-User**: Each user has their own MPIN

### MPIN vs OTP

| Feature | OTP | MPIN |
|---------|-----|------|
| Initial Setup | Always available | Requires OTP login first |
| Speed | Slower (wait for OTP) | Instant |
| Validity | One-time use | 30 days session |
| Security | High (external delivery) | High (encrypted storage) |
| Use Case | New users, expired MPIN | Quick daily logins |

### Complete User Journey Example

```bash
# Day 1: New User Registration
curl -X POST http://localhost:8081/auth/register \
  -d '{"identifier": "user@example.com", "identifierType": "EMAIL"}'
# → Receive OTP: 123456

curl -X POST http://localhost:8081/auth/otp/verify \
  -d '{"identifier": "user@example.com", "otp": "123456"}'
# → Get JWT tokens

# Set MPIN for quick login
curl -X POST http://localhost:8081/auth/mpin/set \
  -H "Authorization: Bearer JWT_TOKEN" \
  -d '{"mpin": "1234", "confirmMpin": "1234"}'
# → MPIN set successfully

# Day 2-30: Quick Login with MPIN
curl -X POST http://localhost:8081/auth/mpin/login \
  -d '{"identifier": "user@example.com", "mpin": "1234"}'
# → Get JWT tokens instantly (no OTP!)

# Day 31: MPIN Session Expired
curl -X POST http://localhost:8081/auth/mpin/login \
  -d '{"identifier": "user@example.com", "mpin": "1234"}'
# → Error: "MPIN session expired. Please login with OTP."

# Must login with OTP again
curl -X POST http://localhost:8081/auth/otp/send \
  -d '{"identifier": "user@example.com"}'
# → MPIN session automatically renewed for another 30 days
```

## 🔄 Testing JWT Propagation

The system demonstrates JWT propagation in multiple ways:

### 1. Gateway → ServiceA/B
- Gateway validates JWT
- Extracts claims (userId, email, phone)
- Adds X-User-* headers
- Forwards request with both JWT and headers

### 2. ServiceA → ServiceB (FeignClient)
- ServiceA receives request with JWT
- FeignClient automatically propagates JWT
- ServiceB validates JWT independently
- ServiceB can also read user context from headers

### Example Flow:

```bash
# Call Business Service endpoint that internally calls Identity Service
curl -X GET http://localhost:8081/api/user/profile \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**What happens:**
1. Gateway validates JWT
2. Adds headers: X-User-Id, X-User-Email
3. Routes to Business Service
4. Business Service validates JWT again
5. Business Service calls Identity Service via FeignClient (JWT propagated)
6. Identity Service validates JWT and returns user data
7. Business Service returns response

## 🗄️ Database Schema

### Identity Auth Service (PostgreSQL - Port 5433)

**users table:**
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    keycloak_id VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE,
    phone VARCHAR(20) UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**otp_records table:**
```sql
CREATE TABLE otp_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(255) NOT NULL,
    otp VARCHAR(6) NOT NULL,
    attempts INT DEFAULT 0,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    verified BOOLEAN DEFAULT FALSE
);
```

**mpin_records table (NEW):**
```sql
CREATE TABLE mpin_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    identifier VARCHAR(255) NOT NULL,
    encrypted_mpin VARCHAR(255) NOT NULL,  -- AES-256 encrypted
    failed_attempts INT DEFAULT 0,
    session_expires_at TIMESTAMP NOT NULL,  -- Default: 30 days from creation
    last_used_at TIMESTAMP,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_mpin_user_id ON mpin_records(user_id);
CREATE INDEX idx_mpin_identifier ON mpin_records(identifier);
```

### Business Service (PostgreSQL - Port 5434)

**business_data table:**
```sql
-- Your business logic tables here
```

## ⚙️ Configuration

### Key Configuration Files

#### identity-auth-service/application.yml

```yaml
keycloak:
  server-url: ${KEYCLOAK_SERVER_URL:http://localhost:8180}
  realm: ${KEYCLOAK_REALM:myrealm}
  client-id: ${KEYCLOAK_CLIENT_ID:backend-service}
  client-secret: ${KEYCLOAK_CLIENT_SECRET}
  admin-username: ${KEYCLOAK_SERVICE_USERNAME}
  admin-password: ${KEYCLOAK_SERVICE_PASSWORD}

otp:
  expiry-minutes: 5
  max-attempts: 3
  length: 6

mpin:
  length: 4
  min-length: 4
  max-length: 6
  session-expiry-days: 30  # How long MPIN stays valid
  max-attempts: 3  # Failed attempts before blocking
  encryption:
    key: ${MPIN_ENCRYPTION_KEY}
```

### Customizing MPIN Behavior

**Change MPIN Session Duration:**
```yaml
mpin:
  session-expiry-days: 60  # 60 days instead of 30
```

**Change MPIN Length Requirements:**
```yaml
mpin:
  min-length: 6  # Require 6-digit MPIN
  max-length: 6
```

**Change Failed Attempt Limit:**
```yaml
mpin:
  max-attempts: 5  # Allow 5 failed attempts
```

## Troubleshooting

### Keycloak not starting
```bash
# Check logs
docker-compose logs keycloak

# Restart
docker-compose restart keycloak
```

### Port already in use
```bash
# Check what's using the port
netstat -ano | findstr :8081

# Change ports in application.yml files
```

### OTP not showing in console
- Check the Identity Auth Service console/logs
- OTPs are logged with prefix `[OTP]`

### JWT validation failing
- Ensure Keycloak is running: http://localhost:8180
- Verify issuer-uri in application.yml matches Keycloak realm
- Check JWK Set URI is accessible

### Service-to-service communication failing
- Verify both services are running
- Check FeignClient configuration
- Ensure JWT is being propagated (check logs)

### MPIN encryption failing
**Error:** "Failed to initialize encryption"

**Solution:**
```bash
# Generate a valid key
openssl rand -base64 32

# Set in environment
export MPIN_ENCRYPTION_KEY="generated-key-here"
```

### MPIN login failing
**Error:** "MPIN not set for this user"

**Solution:**
- User must set MPIN first via `/auth/mpin/set` after OTP login
- Check MPIN status via `/auth/mpin/status`

**Error:** "MPIN session expired"

**Solution:**
- User must login with OTP again
- MPIN session will be automatically renewed

**Error:** "MPIN blocked due to too many failed attempts"

**Solution:**
- User must login with OTP to reset failed attempts
- MPIN will be unblocked after successful OTP login

## Architecture Benefits

1. **Stateless**: No session management, fully JWT-based
2. **Secure**: JWT validated at every layer
3. **Scalable**: Services can be scaled independently
4. **Flexible**: OTP via email or phone
5. **Testable**: OTP logged to console for easy testing
6. **Decoupled**: Services communicate via standard protocols

## 🚀 Next Steps / Future Enhancements

### Security & Authentication
- [ ] Add biometric authentication support
- [ ] Implement device fingerprinting
- [ ] Add 2FA (TOTP) option
- [ ] Implement rate limiting for OTP/MPIN requests
- [ ] Add account lockout policies

### Integration
- [ ] Add email service integration (replace console OTP logging)
- [ ] Add SMS service integration for phone OTP (Twilio, AWS SNS)
- [ ] Integrate with push notification services
- [ ] Add social login (Google, Facebook, Apple)

### Monitoring & Observability
- [ ] Implement API documentation with Swagger/OpenAPI
- [ ] Add monitoring and logging (ELK stack)
- [ ] Add distributed tracing (Zipkin/Jaeger)
- [ ] Set up Prometheus + Grafana dashboards
- [ ] Add security event logging and alerts

### Performance & Scalability
- [ ] Implement circuit breakers (Resilience4j)
- [ ] Add Redis caching layer
- [ ] Set up load balancing
- [ ] Implement API rate limiting
- [ ] Add database connection pooling optimization

### DevOps
- [ ] Create Kubernetes deployment manifests
- [ ] Set up CI/CD pipelines (GitHub Actions, Jenkins)
- [ ] Add automated integration tests
- [ ] Set up secrets management (HashiCorp Vault, AWS Secrets Manager)
- [ ] Create Docker Compose for production

## 📚 Documentation

- `README.md` - This file (getting started, API examples)
- `ARCHITECTURE.md` - Detailed architecture and flows
- `KEYCLOAK_SERVICE_ACCOUNT_SETUP.md` - Service account setup guide
- `MIGRATION_GUIDE.md` - Migration from old version
- `PROJECT_STRUCTURE.md` - Code organization
- `SETUP_GUIDE.md` - Detailed setup instructions

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📝 API Summary

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/auth/register` | POST | Public | Register new user with OTP |
| `/auth/otp/send` | POST | Public | Send OTP for login |
| `/auth/otp/verify` | POST | Public | Verify OTP and get JWT |
| `/auth/refresh` | POST | Public | Refresh access token |
| `/auth/mpin/set` | POST | Protected | Set MPIN for quick login |
| `/auth/mpin/login` | POST | Public | Login with MPIN |
| `/auth/mpin/status` | GET | Protected | Check MPIN status |
| `/auth/mpin` | DELETE | Protected | Remove MPIN |
| `/auth/user/me` | GET | Protected | Get current user |
| `/api/data` | GET | Protected | Business data |
| `/api/user/profile` | GET | Protected | User profile (via FeignClient) |

## License

MIT License
