# Project Structure

Complete file structure of the OTP-based Keycloak microservices project.

```
otp-keycloak-microservices/
│
├── docker-compose.yml                  # Docker compose for Keycloak + PostgreSQL
├── README.md                           # Main documentation and usage guide
├── SETUP_GUIDE.md                      # Step-by-step setup instructions
├── ARCHITECTURE.md                     # Architecture documentation
├── PROJECT_STRUCTURE.md                # This file
├── test-api.sh                         # Linux/Mac API testing script
└── test-api.bat                        # Windows API testing script
│
├── api-gateway/                        # API Gateway Service
│   ├── pom.xml                         # Maven configuration
│   └── src/main/
│       ├── java/com/auth/gateway/
│       │   ├── ApiGatewayApplication.java
│       │   ├── config/
│       │   │   └── SecurityConfig.java         # Spring Security configuration
│       │   └── filter/
│       │       └── JwtAuthenticationFilter.java # JWT extraction and header injection
│       └── resources/
│           └── application.yml         # Gateway configuration and routes
│
├── identity-auth-service/              # Identity and Authentication Service (ServiceA)
│   ├── pom.xml                         # Maven configuration
│   └── src/main/
│       ├── java/com/auth/identity/
│       │   ├── IdentityAuthServiceApplication.java
│       │   │
│       │   ├── model/                  # Entity models
│       │   │   ├── User.java           # User entity
│       │   │   └── OtpRecord.java      # OTP record entity
│       │   │
│       │   ├── repository/             # Data access layer
│       │   │   ├── UserRepository.java
│       │   │   └── OtpRepository.java
│       │   │
│       │   ├── dto/                    # Data transfer objects
│       │   │   ├── RegisterRequest.java
│       │   │   ├── SendOtpRequest.java
│       │   │   ├── VerifyOtpRequest.java
│       │   │   ├── RefreshTokenRequest.java
│       │   │   ├── TokenResponse.java
│       │   │   ├── UserDto.java
│       │   │   ├── OtpResponse.java
│       │   │   └── ApiResponse.java    # Generic API response wrapper
│       │   │
│       │   ├── service/                # Business logic layer
│       │   │   ├── AuthService.java    # Authentication orchestration
│       │   │   ├── OtpService.java     # OTP generation and validation
│       │   │   ├── KeycloakService.java # Keycloak integration
│       │   │   └── UserService.java    # User management
│       │   │
│       │   ├── controller/             # REST API endpoints
│       │   │   └── AuthController.java
│       │   │
│       │   ├── config/                 # Configuration classes
│       │   │   ├── SecurityConfig.java # Spring Security config
│       │   │   └── KeycloakConfig.java # Keycloak admin client config
│       │   │
│       │   └── exception/              # Exception handling
│       │       ├── AuthException.java
│       │       └── GlobalExceptionHandler.java
│       │
│       └── resources/
│           └── application.yml         # Service configuration
│
└── business-service/                   # Business Logic Service (ServiceB)
    ├── pom.xml                         # Maven configuration
    └── src/main/
        ├── java/com/auth/business/
        │   ├── BusinessServiceApplication.java
        │   │
        │   ├── config/                 # Configuration classes
        │   │   ├── SecurityConfig.java # Spring Security config
        │   │   └── FeignClientConfig.java # FeignClient with JWT propagation
        │   │
        │   ├── client/                 # FeignClient interfaces
        │   │   └── IdentityServiceClient.java # Call Identity Service
        │   │
        │   ├── service/                # Business logic layer
        │   │   └── BusinessService.java
        │   │
        │   └── controller/             # REST API endpoints
        │       └── BusinessController.java
        │
        └── resources/
            └── application.yml         # Service configuration
```

## File Descriptions

### Root Level Files

#### docker-compose.yml
- Defines Docker containers for:
  - Keycloak (port 8180)
  - PostgreSQL for Keycloak (port 5432)
  - PostgreSQL for Identity Service (port 5433)
  - PostgreSQL for Business Service (port 5434)

#### README.md
- Project overview
- Features list
- Quick start guide
- API usage examples
- Troubleshooting guide

#### SETUP_GUIDE.md
- Detailed step-by-step setup instructions
- Keycloak configuration guide
- Testing instructions

#### ARCHITECTURE.md
- System architecture diagrams
- Component descriptions
- Authentication flows
- Security architecture
- Deployment considerations

#### test-api.sh / test-api.bat
- Automated API testing scripts
- Demonstrates all endpoints
- Linux/Mac and Windows versions

---

## API Gateway

### Core Files

**ApiGatewayApplication.java**
- Spring Boot main class
- Entry point for Gateway service

**SecurityConfig.java**
- Configures Spring Security
- JWT validation setup
- Public endpoint definitions

**JwtAuthenticationFilter.java**
- Custom Gateway filter
- Extracts JWT claims
- Adds X-User-* headers to requests
- Routes requests with user context

**application.yml**
- Gateway routes configuration
- Service URLs (hardcoded IPs)
- CORS configuration
- Security settings
- Keycloak JWK Set URI

---

## Identity Auth Service

### Model Layer

**User.java**
- User entity (JPA)
- Fields: id, keycloakId, email, phone
- Timestamps: createdAt, updatedAt

**OtpRecord.java**
- OTP record entity (JPA)
- Fields: identifier, otp, attempts, expiresAt
- Methods: isExpired(), incrementAttempts()

### Repository Layer

**UserRepository.java**
- JPA repository for User
- Methods: findByEmail, findByPhone, findByKeycloakId
- Existence checks

**OtpRepository.java**
- JPA repository for OtpRecord
- Custom queries for OTP retrieval
- Cleanup methods for expired OTPs

### DTO Layer

**RegisterRequest.java**
- Registration request body
- Fields: identifier, identifierType (EMAIL/PHONE)

**SendOtpRequest.java**
- OTP send request body
- Field: identifier

**VerifyOtpRequest.java**
- OTP verification request
- Fields: identifier, otp
- Validation: otp must be 6 digits

**TokenResponse.java**
- JWT token response
- Fields: accessToken, refreshToken, expiresIn, user

**UserDto.java**
- User data transfer object
- Fields: id, keycloakId, email, phone

**OtpResponse.java**
- OTP generation response
- Fields: identifier, expiresIn

**ApiResponse<T>.java**
- Generic API response wrapper
- Fields: success, message, data, timestamp
- Static factory methods

### Service Layer

**AuthService.java**
- Main authentication orchestration
- Methods:
  - register(): Handle registration with OTP
  - sendOtp(): Send OTP for login
  - verifyOtp(): Verify OTP and issue JWT
  - refreshToken(): Refresh access token

**OtpService.java**
- OTP generation and validation
- Methods:
  - generateAndSaveOtp(): Create and store OTP
  - verifyOtp(): Validate OTP with expiry and attempts
  - cleanupExpiredOtps(): Scheduled cleanup job
- Logs OTP to console for testing

**KeycloakService.java**
- Keycloak integration
- Methods:
  - createUser(): Create user in Keycloak
  - getTokenForUser(): Get JWT from Keycloak
  - refreshToken(): Refresh JWT
  - getUserByUsername(): Fetch user from Keycloak

**UserService.java**
- User management
- Methods:
  - createUser(): Create user in database
  - findByIdentifier(): Find by email or phone
  - getUserById(): Get user by ID
  - toDto(): Convert entity to DTO

### Controller Layer

**AuthController.java**
- REST API endpoints
- Endpoints:
  - POST /auth/register
  - POST /auth/otp/send
  - POST /auth/otp/verify
  - POST /auth/refresh
  - GET /auth/user/me (protected)
  - GET /auth/user/{id} (protected)

### Config Layer

**SecurityConfig.java**
- Spring Security configuration
- JWT resource server setup
- Public endpoint definitions
- Stateless session management

**KeycloakConfig.java**
- Keycloak Admin Client bean
- Connection configuration

### Exception Layer

**AuthException.java**
- Custom authentication exception
- Thrown for auth-related errors

**GlobalExceptionHandler.java**
- Global exception handling
- Returns standardized error responses
- Handles validation errors

### Configuration

**application.yml**
- Database connection (port 5433)
- Keycloak configuration
- JWT settings
- OTP settings (expiry, attempts, length)
- Logging configuration

---

## Business Service

### Config Layer

**SecurityConfig.java**
- Spring Security configuration
- JWT resource server setup
- All endpoints protected

**FeignClientConfig.java**
- FeignClient configuration
- JWT token propagation interceptor
- User context header propagation

### Client Layer

**IdentityServiceClient.java**
- FeignClient interface
- Calls Identity Service endpoints
- Automatic JWT propagation
- Methods:
  - getUserById(): Get user by ID
  - getCurrentUser(): Get current user profile

### Service Layer

**BusinessService.java**
- Business logic
- Methods:
  - getBusinessData(): Sample business data
  - getUserProfile(): Fetch profile via FeignClient
  - getUserProfileById(): Fetch user by ID via FeignClient

### Controller Layer

**BusinessController.java**
- REST API endpoints
- Endpoints:
  - GET /api/data (demonstrates JWT + headers)
  - GET /api/user/profile (FeignClient test)
  - GET /api/user/{id} (FeignClient with param)
  - GET /api/health

### Configuration

**application.yml**
- Database connection (port 5434)
- Identity Service URL (hardcoded)
- FeignClient settings
- JWT settings
- Logging configuration

---

## Key Technologies by Component

### API Gateway
- Spring Cloud Gateway
- Spring Security OAuth2 Resource Server
- Reactive programming (WebFlux)

### Identity Auth Service
- Spring Boot Web
- Spring Data JPA
- Spring Security OAuth2 Resource Server
- Keycloak Admin Client
- PostgreSQL
- WebClient (for REST calls)

### Business Service
- Spring Boot Web
- Spring Data JPA
- Spring Security OAuth2 Resource Server
- Spring Cloud OpenFeign
- PostgreSQL

---

## Configuration Files Summary

### API Gateway (application.yml)
```yaml
Key configurations:
- Routes to services (hardcoded URLs)
- JWT issuer-uri and jwk-set-uri
- CORS settings
- Port: 8081
```

### Identity Auth Service (application.yml)
```yaml
Key configurations:
- Database URL (port 5433)
- Keycloak server URL and realm
- Client ID and secret
- OTP settings (expiry, attempts)
- JWT settings
- Port: 8082
```

### Business Service (application.yml)
```yaml
Key configurations:
- Database URL (port 5434)
- Identity Service URL (hardcoded)
- FeignClient timeouts
- JWT settings
- Port: 8083
```

---

## Dependencies Summary

### Common Dependencies (All Services)
- spring-boot-starter-web
- spring-boot-starter-security
- spring-boot-starter-oauth2-resource-server
- lombok
- spring-boot-starter-actuator

### API Gateway Specific
- spring-cloud-starter-gateway

### Identity Auth Service Specific
- spring-boot-starter-data-jpa
- postgresql
- keycloak-admin-client
- spring-boot-starter-webflux
- spring-boot-starter-validation

### Business Service Specific
- spring-boot-starter-data-jpa
- postgresql
- spring-cloud-starter-openfeign

---

## Build and Run Commands

### Build All Services
```bash
# API Gateway
cd api-gateway && mvn clean package

# Identity Auth Service
cd identity-auth-service && mvn clean package

# Business Service
cd business-service && mvn clean package
```

### Run All Services
```bash
# Terminal 1
cd identity-auth-service && mvn spring-boot:run

# Terminal 2
cd business-service && mvn spring-boot:run

# Terminal 3
cd api-gateway && mvn spring-boot:run
```

---

## Port Mapping

| Service | Port | Purpose |
|---------|------|---------|
| API Gateway | 8081 | Entry point for all requests |
| Identity Service | 8082 | Authentication and user management |
| Business Service | 8083 | Business logic APIs |
| Keycloak | 8180 | Identity provider |
| PostgreSQL (Keycloak) | 5432 | Keycloak database |
| PostgreSQL (Identity) | 5433 | Identity service database |
| PostgreSQL (Business) | 5434 | Business service database |

---

## Next Steps

1. **Review SETUP_GUIDE.md** for setup instructions
2. **Read ARCHITECTURE.md** for architectural details
3. **Follow README.md** for API usage examples
4. **Run test-api.sh** to test all endpoints

---

**Happy Coding!**
