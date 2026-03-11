# OTP-Based Keycloak Microservices Architecture

Complete microservices setup with OTP authentication, JWT propagation, and Keycloak integration.

## Architecture

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
  ↓
Keycloak (8180)
```

## Features

- **OTP Authentication**: Email or Phone based OTP (console logging for testing)
- **JWT Validation**: At every layer (Gateway, ServiceA, ServiceB)
- **User Context Propagation**: X-User-* headers throughout the system
- **FeignClient Integration**: JWT propagation between services
- **Stateless Architecture**: No session management

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

## Prerequisites

- Java 17+
- Maven 3.6+
- Docker & Docker Compose
- PostgreSQL (via Docker)

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
3. Update `identity-auth-service/src/main/resources/application.yml` with this secret

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

## Testing JWT Propagation

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

## Database Schema

### Identity Auth Service

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

## Configuration

### Update Keycloak Client Secret

After creating the Keycloak client, update the secret in:
- `identity-auth-service/src/main/resources/application.yml`

```yaml
keycloak:
  client-secret: YOUR_CLIENT_SECRET_HERE
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

## Architecture Benefits

1. **Stateless**: No session management, fully JWT-based
2. **Secure**: JWT validated at every layer
3. **Scalable**: Services can be scaled independently
4. **Flexible**: OTP via email or phone
5. **Testable**: OTP logged to console for easy testing
6. **Decoupled**: Services communicate via standard protocols

## Next Steps

1. Add MPIN support (optional per-user quick login)
2. Implement rate limiting for OTP requests
3. Add email service integration (replace console logging)
4. Add SMS service integration for phone OTP
5. Implement API documentation with Swagger/OpenAPI
6. Add monitoring and logging (ELK stack)
7. Add distributed tracing (Zipkin/Jaeger)
8. Implement circuit breakers (Resilience4j)

## License

MIT License
