# Architecture Documentation

## System Overview

This document describes the architecture of the OTP-based Keycloak microservices system.

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     React Native / Web App                       │
│                    (Frontend Application)                        │
└────────────────────────┬────────────────────────────────────────┘
                         │ HTTP/REST
                         │ Authorization: Bearer <JWT>
                         │
┌────────────────────────▼────────────────────────────────────────┐
│                      API Gateway (8081)                          │
│  ┌────────────────────────────────────────────────────────┐    │
│  │  Security Filter                                       │    │
│  │  - Validates JWT (using Keycloak JWK Set)            │    │
│  │  - Extracts claims (sub, email, phone, roles)        │    │
│  │  - Adds X-User-* headers                             │    │
│  └────────────────────────────────────────────────────────┘    │
│                                                                  │
│  Routes:                                                         │
│  - /auth/**  → Identity Auth Service                           │
│  - /api/**   → Business Service                                │
└─────────────────┬─────────────────────┬─────────────────────────┘
                  │                     │
                  │                     │
        ┌─────────▼──────────┐   ┌─────▼──────────────┐
        │  Identity Auth     │   │  Business Service  │
        │  Service (8082)    │   │     (8083)         │
        │                    │   │                    │
        │  - OTP Generation  │   │  - Business Logic  │
        │  - OTP Validation  │   │  - FeignClient     │
        │  - User Management │   │  - JWT Validation  │
        │  - JWT Issuance    │   │                    │
        │                    │◄──┤  Calls via Feign   │
        │  Database:         │   │  (JWT propagated)  │
        │  - users           │   │                    │
        │  - otp_records     │   │  Database:         │
        └──────────┬─────────┘   │  - business_data   │
                   │              └────────────────────┘
                   │
                   │ Admin API
                   │ Token Generation
                   │
        ┌──────────▼─────────┐
        │   Keycloak (8180)  │
        │   Realm: myrealm   │
        │                    │
        │  - User Store      │
        │  - JWT Issuer      │
        │  - JWK Set         │
        └────────────────────┘
```

## Component Details

### 1. API Gateway

**Technology:** Spring Cloud Gateway

**Port:** 8081

**Responsibilities:**
- Route requests to appropriate microservices
- JWT validation (first line of defense)
- Extract user information from JWT
- Add user context headers (X-User-Id, X-User-Email, X-User-Phone)
- CORS handling

**Filters:**
- `JwtAuthenticationFilter` - Extracts JWT claims and adds headers
- Public endpoints bypass authentication

**Configuration:**
```yaml
Routes:
  - /auth/register → ServiceA (Public)
  - /auth/otp/send → ServiceA (Public)
  - /auth/otp/verify → ServiceA (Public)
  - /auth/** → ServiceA (Protected)
  - /api/** → ServiceB (Protected)
```

### 2. Identity Auth Service (ServiceA)

**Technology:** Spring Boot 3.3.0

**Port:** 8082

**Database:** PostgreSQL (port 5433)

**Responsibilities:**
- User registration with OTP
- OTP generation and validation
- User management
- Keycloak integration
- JWT token issuance

**Key Features:**
- OTP expiry (5 minutes default)
- Max OTP attempts (3 default)
- OTP logging to console (for testing)
- Automatic OTP cleanup

**Endpoints:**
- `POST /auth/register` - Register with email/phone + OTP
- `POST /auth/otp/send` - Send OTP for login
- `POST /auth/otp/verify` - Verify OTP and get JWT
- `POST /auth/refresh` - Refresh access token
- `GET /auth/user/me` - Get current user (protected)
- `GET /auth/user/{id}` - Get user by ID (protected)

**Database Schema:**

**users table:**
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    keycloak_id VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE,
    phone VARCHAR(20) UNIQUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

**otp_records table:**
```sql
CREATE TABLE otp_records (
    id UUID PRIMARY KEY,
    identifier VARCHAR(255) NOT NULL,
    otp VARCHAR(6) NOT NULL,
    attempts INT DEFAULT 0,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP,
    verified BOOLEAN DEFAULT FALSE
);
```

### 3. Business Service (ServiceB)

**Technology:** Spring Boot 3.3.0

**Port:** 8083

**Database:** PostgreSQL (port 5434)

**Responsibilities:**
- Business logic operations
- JWT validation (resource server)
- Call Identity Service via FeignClient
- Process user context from headers

**Key Features:**
- Independent JWT validation
- FeignClient with JWT propagation
- User context from headers

**Endpoints:**
- `GET /api/data` - Get business data
- `GET /api/user/profile` - Get user profile (calls ServiceA via Feign)
- `GET /api/user/{id}` - Get user by ID (calls ServiceA via Feign)
- `GET /api/health` - Health check

**FeignClient Configuration:**
- Automatically propagates JWT from request
- Propagates user context headers
- 5-second connection timeout
- 5-second read timeout

### 4. Keycloak

**Version:** 23.0.0

**Port:** 8180

**Realm:** myrealm

**Client:** backend-service (confidential)

**Responsibilities:**
- User storage
- JWT token generation
- Token validation (via JWK Set)
- User authentication

**Configuration:**
- Direct access grants enabled
- Client credentials grant enabled
- JWK Set endpoint exposed

## Authentication Flows

### Flow 1: Registration with OTP

```
1. User → Gateway → ServiceA: POST /auth/register
   Body: { identifier: "user@example.com", identifierType: "EMAIL" }

2. ServiceA:
   - Check if user exists → reject if exists
   - Generate 6-digit OTP
   - Store OTP in database (expires in 5 min)
   - Log OTP to console
   - Return success

3. User receives OTP from console

4. User → Gateway → ServiceA: POST /auth/otp/verify
   Body: { identifier: "user@example.com", otp: "123456" }

5. ServiceA:
   - Validate OTP (check expiry, attempts)
   - Create user in Keycloak
   - Create user in local database
   - Get JWT from Keycloak
   - Return: { accessToken, refreshToken, user }

6. User stores JWT in SecureStore
```

### Flow 2: Login with OTP (Existing User)

```
1. User → Gateway → ServiceA: POST /auth/otp/send
   Body: { identifier: "user@example.com" }

2. ServiceA:
   - Check if user exists → reject if not
   - Generate OTP
   - Store OTP in database
   - Log OTP to console
   - Return success

3. User receives OTP

4. User → Gateway → ServiceA: POST /auth/otp/verify
   (Same as registration flow step 4-6)
```

### Flow 3: Access Protected Resource

```
1. User → Gateway: GET /api/data
   Header: Authorization: Bearer <JWT>

2. Gateway:
   - Validate JWT with Keycloak JWK Set
   - If invalid → 401 Unauthorized
   - If valid:
     - Extract claims (sub, email, phone)
     - Add headers: X-User-Id, X-User-Email, X-User-Phone
     - Route to ServiceB

3. ServiceB:
   - Validate JWT again (independent validation)
   - Read user context from headers (optional)
   - Execute business logic
   - Return data

4. Gateway → User: Response with data
```

### Flow 4: Service-to-Service Communication

```
1. User → Gateway → ServiceB: GET /api/user/profile
   Header: Authorization: Bearer <JWT>

2. ServiceB receives request:
   - JWT in Authorization header
   - User context in X-User-* headers

3. ServiceB → ServiceA via FeignClient:
   - FeignClient interceptor extracts JWT from context
   - Adds JWT to outgoing request
   - Also propagates X-User-* headers
   - Calls: GET /auth/user/me

4. ServiceA:
   - Validates JWT
   - Returns user profile

5. ServiceB → User: Returns profile data
```

## Security Architecture

### JWT Validation (Multi-Layer)

1. **API Gateway:**
   - First validation layer
   - Uses Keycloak JWK Set
   - Validates signature and expiry
   - Rejects invalid requests early

2. **Individual Services:**
   - Independent validation
   - Each service validates JWT
   - Stateless security
   - No trust between services

3. **JWT Structure:**
```json
{
  "sub": "keycloak-user-id",
  "email": "user@example.com",
  "phone_number": "+919876543210",
  "preferred_username": "user@example.com",
  "realm_access": {
    "roles": ["user", "admin"]
  },
  "iss": "http://localhost:8180/realms/myrealm",
  "exp": 1710157200,
  "iat": 1710153600
}
```

### User Context Propagation

**Headers Added by Gateway:**
- `X-User-Id`: Subject (sub) from JWT
- `X-User-Email`: Email claim
- `X-User-Phone`: Phone number claim
- `X-User-Username`: Preferred username
- `X-User-Roles`: Comma-separated roles

**Benefits:**
- Services can read user context without parsing JWT
- Convenience for business logic
- Still validated via JWT
- No security compromise

### OTP Security

**Features:**
- Secure random number generation
- Expiry after 5 minutes
- Maximum 3 attempts
- One-time use
- Automatic cleanup of expired OTPs

**Validation:**
```java
1. Find latest unverified OTP for identifier
2. Check expiry → reject if expired
3. Check attempts → reject if >= max
4. Compare OTP → increment attempts if wrong
5. Mark as verified if correct
6. Return success/failure
```

## Data Flow

### Request Flow (Detailed)

```
User Request
    ↓
API Gateway (port 8081)
    ↓
1. Spring Security Filter Chain
    ↓
2. JWT Authentication Filter
    - Extract JWT from Authorization header
    - Validate with Keycloak JWK Set
    - Create Authentication object
    ↓
3. JwtAuthenticationFilter (Custom)
    - Extract claims from JWT
    - Add X-User-* headers
    ↓
4. Route to appropriate service
    ↓
Service (8082 or 8083)
    ↓
5. Spring Security Filter Chain
    ↓
6. JWT Resource Server
    - Validate JWT independently
    - Create Authentication object
    ↓
7. Controller
    - Access JWT via @AuthenticationPrincipal
    - Read headers via HttpServletRequest
    - Execute business logic
    ↓
8. Response
    ↓
User
```

## Scalability Considerations

### Stateless Design
- No session state
- JWT contains all necessary information
- Services can be scaled horizontally
- Load balancer can distribute randomly

### Database Scaling
- Each service has its own database
- Can scale databases independently
- Database per service pattern

### Caching Strategy
- JWK Set cached by each service
- Reduces calls to Keycloak
- Automatic refresh on key rotation

## Monitoring Points

### Metrics to Monitor
1. **OTP Generation Rate**
   - Track OTP requests per minute
   - Detect potential abuse

2. **OTP Success Rate**
   - Monitor verification success/failure
   - Track expired OTPs

3. **JWT Validation Failures**
   - Track invalid/expired tokens
   - Security incident detection

4. **Service Response Times**
   - API Gateway latency
   - Service latency
   - Database query times

5. **FeignClient Calls**
   - Success/failure rate
   - Latency

### Logging Strategy

**Levels:**
- `INFO`: Authentication success/failure
- `DEBUG`: Request routing, JWT claims
- `ERROR`: Authentication errors, validation failures

**Key Logs:**
```
[OTP] Code for user@example.com: 123456
[AUTH] User authenticated: user-id
[GATEWAY] Routing request to: identity-service
[FEIGN] Propagating JWT to service call
[ERROR] JWT validation failed: Token expired
```

## Technology Stack Summary

| Component | Technology | Version |
|-----------|-----------|---------|
| Java | OpenJDK | 17 |
| Spring Boot | Spring Boot | 3.3.0 |
| Spring Cloud | Spring Cloud Gateway | 2023.0.0 |
| Spring Security | OAuth2 Resource Server | - |
| Keycloak | Keycloak | 23.0.0 |
| Database | PostgreSQL | 15 |
| Build Tool | Maven | 3.6+ |
| Container | Docker | Latest |

## Deployment Architecture

### Development (Current)
```
All services on localhost:
- API Gateway: 8081
- Identity Service: 8082
- Business Service: 8083
- Keycloak: 8180
- PostgreSQL: 5432, 5433, 5434
```

### Production (Recommended)
```
Load Balancer
    ↓
API Gateway (multiple instances)
    ↓
    ├─→ Identity Service (multiple instances)
    └─→ Business Service (multiple instances)
        ↓
    Database Cluster
        ↓
    Keycloak Cluster
```

## Benefits of This Architecture

1. **Stateless**: No session management, fully JWT-based
2. **Secure**: JWT validated at every layer
3. **Scalable**: Services can scale independently
4. **Decoupled**: Standard REST/HTTP communication
5. **Flexible**: Easy to add new services
6. **Observable**: Clear logging and monitoring points
7. **Testable**: Each service can be tested independently
8. **Maintainable**: Clean separation of concerns

## Future Enhancements

1. **MPIN Support**: Quick login for trusted devices
2. **Rate Limiting**: Prevent OTP abuse
3. **Email/SMS Integration**: Real OTP delivery
4. **API Documentation**: Swagger/OpenAPI
5. **Distributed Tracing**: Zipkin/Jaeger
6. **Circuit Breakers**: Resilience4j
7. **Service Discovery**: Eureka/Consul
8. **Config Server**: Centralized configuration
9. **Message Queue**: Async operations
10. **Caching Layer**: Redis for performance
