# Architecture Documentation

## System Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          Frontend Applications                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                 │
│  │    React     │  │ React Native │  │  Web Browser │                 │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘                 │
└─────────┼──────────────────┼──────────────────┼──────────────────────────┘
          │                  │                  │
          │ HTTP/REST        │ HTTP/REST        │ OAuth2 Redirect
          │                  │                  │
┌─────────▼──────────────────▼──────────────────▼──────────────────────────┐
│               Spring Boot Application + OAuth2 Starter                    │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │                    OAuth2AuthenticationController                   │  │
│  │  /authorize | /login | /token | /refresh | /userinfo | /validate  │  │
│  └────────────────┬──────────────────────┬──────────────┬─────────────┘  │
│                   │                      │              │                 │
│  ┌────────────────▼──────────┐  ┌────────▼──────────┐  │                 │
│  │   OAuth2ClientService     │  │ TokenValidation   │  │                 │
│  │  - Token Exchange         │  │ Service           │  │                 │
│  │  - User Authentication    │  │  - JWT Decode     │  │                 │
│  │  - Token Refresh          │  │  - Validation     │  │                 │
│  └────────────────┬──────────┘  └───────────────────┘  │                 │
│                   │                                     │                 │
│  ┌────────────────▼─────────────────────────────────────▼─────────────┐  │
│  │                    OAuth2SecurityConfig                             │  │
│  │  - JWT Decoder         - CORS Configuration                         │  │
│  │  - Security Rules      - Session Management                         │  │
│  └──────────────────────────────────────────────────────────────────────┘ │
│  ┌──────────────────────────────────────────────────────────────────────┐ │
│  │                    OAuth2AutoConfiguration                           │  │
│  │  - Property Binding    - Bean Creation    - Provider Detection      │  │
│  └──────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────┬──────────────────────────────────────────────┘
                              │
                              │ OAuth2/OIDC Protocol
                              │ (HTTP/HTTPS)
                              │
┌─────────────────────────────▼──────────────────────────────────────────────┐
│                       OAuth2/OIDC Providers                                │
│  ┌──────────┐  ┌──────┐  ┌────────┐  ┌──────┐  ┌────────┐  ┌──────────┐ │
│  │ Keycloak │  │ Okta │  │ Google │  │ Meta │  │ GitHub │  │Microsoft │ │
│  └──────────┘  └──────┘  └────────┘  └──────┘  └────────┘  └──────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Component Architecture

### 1. Configuration Layer

```
OAuth2Properties
├── Provider Configuration (provider, client-id, client-secret)
├── Endpoint Configuration (issuer-uri, token-uri, etc.)
├── Feature Flags (custom-login-enabled)
├── JWT Settings (validation, expiry)
└── CORS Settings (origins, methods, headers)
```

### 2. Provider Layer

```
OAuth2Provider Enum
├── KEYCLOAK   → Pre-configured endpoints
├── OKTA       → Pre-configured endpoints
├── GOOGLE     → Pre-configured endpoints
├── META       → Pre-configured endpoints
├── GITHUB     → Pre-configured endpoints
├── MICROSOFT  → Pre-configured endpoints
└── CUSTOM     → User-defined endpoints
```

### 3. Service Layer

```
OAuth2ClientService
├── authenticateWithPassword()    → Custom login (password grant)
├── exchangeCodeForToken()        → Standard OAuth2 flow
├── refreshToken()                → Token refresh
├── getUserInfo()                 → Fetch user details
└── getAuthorizationUrl()         → Build auth URL

TokenValidationService
├── validateToken()               → JWT signature & claims
├── isTokenExpired()              → Expiry check
├── extractClaims()               → Claim extraction
└── validateAndGetSubject()       → Subject validation
```

### 4. Controller Layer

```
OAuth2AuthenticationController
├── GET  /oauth2/authorize        → Get authorization URL
├── POST /oauth2/login            → Custom username/password login
├── POST /oauth2/token            → Exchange code for token
├── POST /oauth2/refresh          → Refresh access token
├── GET  /oauth2/userinfo         → Get user information
├── GET  /oauth2/validate         → Validate token
├── POST /oauth2/logout           → Logout
└── GET  /oauth2/health           → Health check
```

### 5. Security Layer

```
OAuth2SecurityConfig
├── SecurityFilterChain
│   ├── Public endpoints (login, authorize, token, refresh)
│   ├── Protected endpoints (userinfo, validate)
│   └── Authenticated endpoints (all others)
├── JwtDecoder (JWK Set validation)
└── CORS Configuration
```

### 6. DTO Layer

```
Request DTOs
├── LoginRequest         → username, password, grantType
└── RefreshTokenRequest  → refreshToken

Response DTOs
├── TokenResponse        → access_token, refresh_token, expires_in, userInfo
├── UserInfo            → sub, username, email, name, picture
└── ApiResponse<T>      → success, message, data, timestamp
```

## Authentication Flows

### Flow 1: Standard OAuth2 Authorization Code Flow

```
┌────────┐                ┌──────────┐                ┌─────────┐
│Frontend│                │ Backend  │                │Provider │
└────┬───┘                └────┬─────┘                └────┬────┘
     │                         │                           │
     │ 1. GET /authorize       │                           │
     ├────────────────────────>│                           │
     │                         │                           │
     │ 2. Authorization URL    │                           │
     │<────────────────────────┤                           │
     │                         │                           │
     │ 3. Redirect to Provider │                           │
     ├───────────────────────────────────────────────────>│
     │                         │                           │
     │              4. User Login & Consent                │
     │                         │                           │
     │ 5. Redirect with code   │                           │
     │<───────────────────────────────────────────────────┤
     │                         │                           │
     │ 6. POST /token?code=xxx │                           │
     ├────────────────────────>│                           │
     │                         │ 7. Exchange code          │
     │                         ├──────────────────────────>│
     │                         │                           │
     │                         │ 8. Access Token           │
     │                         │<──────────────────────────┤
     │                         │ 9. GET /userinfo          │
     │                         ├──────────────────────────>│
     │                         │                           │
     │                         │ 10. User Info             │
     │                         │<──────────────────────────┤
     │ 11. Token + User Info   │                           │
     │<────────────────────────┤                           │
     │                         │                           │
```

### Flow 2: Custom Login (Password Grant)

```
┌────────┐                ┌──────────┐                ┌─────────┐
│Frontend│                │ Backend  │                │Provider │
└────┬───┘                └────┬─────┘                └────┬────┘
     │                         │                           │
     │ 1. POST /login          │                           │
     │    {username, password} │                           │
     ├────────────────────────>│                           │
     │                         │ 2. Password Grant Request │
     │                         ├──────────────────────────>│
     │                         │                           │
     │                         │ 3. Access Token           │
     │                         │<──────────────────────────┤
     │                         │ 4. GET /userinfo          │
     │                         ├──────────────────────────>│
     │                         │                           │
     │                         │ 5. User Info              │
     │                         │<──────────────────────────┤
     │ 6. Token + User Info    │                           │
     │<────────────────────────┤                           │
     │                         │                           │
```

### Flow 3: Token Refresh

```
┌────────┐                ┌──────────┐                ┌─────────┐
│Frontend│                │ Backend  │                │Provider │
└────┬───┘                └────┬─────┘                └────┬────┘
     │                         │                           │
     │ 1. API Request          │                           │
     │    (expired token)      │                           │
     ├────────────────────────>│                           │
     │                         │                           │
     │ 2. 401 Unauthorized     │                           │
     │<────────────────────────┤                           │
     │                         │                           │
     │ 3. POST /refresh        │                           │
     │    {refreshToken}       │                           │
     ├────────────────────────>│                           │
     │                         │ 4. Refresh Token Request  │
     │                         ├──────────────────────────>│
     │                         │                           │
     │                         │ 5. New Access Token       │
     │                         │<──────────────────────────┤
     │ 6. New Access Token     │                           │
     │<────────────────────────┤                           │
     │                         │                           │
     │ 7. Retry Original Request (with new token)         │
     ├────────────────────────>│                           │
     │                         │                           │
     │ 8. Success Response     │                           │
     │<────────────────────────┤                           │
     │                         │                           │
```

## Security Architecture

### JWT Token Validation

```
Incoming Request with Bearer Token
        │
        ▼
Extract Token from Authorization Header
        │
        ▼
OAuth2SecurityConfig → JwtDecoder
        │
        ├─> Fetch JWK Set from Provider
        │   (Cached for performance)
        │
        ├─> Verify Token Signature
        │   (RSA public key from JWK Set)
        │
        ├─> Validate Claims
        │   ├─> Check expiration (exp)
        │   ├─> Check issuer (iss)
        │   ├─> Check audience (aud) [optional]
        │   └─> Check not before (nbf)
        │
        ▼
Valid JWT → Spring Security Context
        │
        ▼
@AuthenticationPrincipal Jwt available in controllers
```

### CORS Configuration

```
Preflight Request (OPTIONS)
        │
        ▼
CorsConfiguration
├─> Check Origin against allowed-origins
├─> Check Method against allowed-methods
├─> Check Headers against allowed-headers
├─> Set max-age for cache
└─> Set allow-credentials
        │
        ▼
CORS Headers Added to Response
├─> Access-Control-Allow-Origin
├─> Access-Control-Allow-Methods
├─> Access-Control-Allow-Headers
├─> Access-Control-Expose-Headers
├─> Access-Control-Allow-Credentials
└─> Access-Control-Max-Age
```

## Data Flow

### Configuration Bootstrap

```
Application Startup
        │
        ▼
@AutoConfiguration Detected
        │
        ▼
OAuth2AutoConfiguration
├─> Load OAuth2Properties from application.properties
├─> Validate required properties
├─> Detect provider and build endpoints
├─> Log configuration summary
└─> Create beans (WebClient, Services, Controllers)
        │
        ▼
OAuth2SecurityConfig
├─> Create JwtDecoder with JWK Set URI
├─> Configure SecurityFilterChain
│   ├─> Public endpoints: /oauth2/authorize, /login, /token
│   ├─> Protected endpoints: /oauth2/userinfo, /validate
│   └─> All others: authenticated
└─> Configure CORS
        │
        ▼
Application Ready
```

### Request Processing

```
HTTP Request
        │
        ▼
Spring MVC DispatcherServlet
        │
        ▼
Spring Security FilterChain
        │
        ├─> Public endpoint? → Pass through
        │
        └─> Protected endpoint?
            │
            ▼
        Extract JWT from Authorization header
            │
            ▼
        JwtDecoder validates token
            │
            ├─> Invalid → 401 Unauthorized
            │
            └─> Valid
                │
                ▼
            Set SecurityContext with Jwt
                │
                ▼
        Controller Method Execution
                │
                ▼
        @AuthenticationPrincipal Jwt injected
                │
                ▼
        Business Logic
                │
                ▼
        Response
```

## Extension Points

### 1. Custom Security Rules

```java
@Bean
@Order(2)
public SecurityFilterChain customSecurity(HttpSecurity http) {
    http.securityMatcher("/api/**")
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/admin/**").hasRole("ADMIN")
            .requestMatchers("/api/**").authenticated()
        );
    return http.build();
}
```

### 2. Custom Token Processing

```java
@Component
public class CustomTokenEnhancer {

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        Jwt jwt = (Jwt) event.getAuthentication().getPrincipal();
        // Custom processing
    }
}
```

### 3. Custom User Info Mapping

Extend `OAuth2ClientService` to customize user info mapping for specific providers.

## Performance Considerations

### Caching Strategy

```
JWK Set Caching
├─> Cached in JwtDecoder (default: indefinite with refresh)
├─> Reduces latency for token validation
└─> Automatically refreshes on key rotation

Token Validation
├─> Signature validation: ~10ms (with cached JWK)
├─> Claims validation: <1ms
└─> Total: ~15ms per request
```

### Scalability

```
Stateless Design
├─> No server-side sessions
├─> Tokens contain all necessary information
├─> Horizontal scaling without session replication
└─> Suitable for microservices architecture
```

## Monitoring & Observability

### Logging Points

```
INFO  - Configuration summary at startup
INFO  - Authentication success/failure
DEBUG - Token requests and responses
DEBUG - User info retrieval
ERROR - Authentication errors with details
```

### Metrics (Future Enhancement)

```
Proposed Metrics:
├─> oauth2.authentication.success (counter)
├─> oauth2.authentication.failure (counter)
├─> oauth2.token.validation.time (histogram)
├─> oauth2.token.refresh.count (counter)
└─> oauth2.provider.request.time (histogram)
```

## Deployment Architecture

### Single Application

```
┌─────────────────────────────────┐
│   Spring Boot Application       │
│  ┌───────────────────────────┐  │
│  │ OAuth2 Starter (Library)  │  │
│  └───────────────────────────┘  │
│  ┌───────────────────────────┐  │
│  │ Your Application Code     │  │
│  └───────────────────────────┘  │
└─────────────────────────────────┘
```

### Microservices

```
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│  Service A   │  │  Service B   │  │  Service C   │
│  + OAuth2    │  │  + OAuth2    │  │  + OAuth2    │
│    Starter   │  │    Starter   │  │    Starter   │
└──────┬───────┘  └──────┬───────┘  └──────┬───────┘
       │                 │                 │
       └─────────────────┼─────────────────┘
                         │
                    ┌────▼────┐
                    │ OAuth2  │
                    │Provider │
                    └─────────┘
```

All services validate tokens independently using JWK Set from provider.

## Technology Stack

```
Core Framework
├─> Java 17
├─> Spring Boot 3.3.12
├─> Spring Security 6.3.9
└─> Maven

OAuth2 Libraries
├─> Spring Security OAuth2 Client
├─> Spring Security OAuth2 Resource Server
├─> Spring Security OAuth2 JOSE
└─> Nimbus JOSE + JWT 9.37.3

HTTP Client
└─> Spring WebFlux (WebClient)

Utilities
├─> Lombok (code generation)
├─> Jackson (JSON processing)
└─> Jakarta Validation (input validation)
```

This architecture provides a solid foundation for enterprise OAuth2/OIDC authentication with flexibility, security, and performance.
