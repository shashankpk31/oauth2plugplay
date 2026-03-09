# OAuth2 OIDC Spring Boot Starter - Architecture Documentation

This document provides a comprehensive overview of the technical architecture, design patterns, and implementation details of the OAuth2 OIDC Spring Boot Starter library.

## Table of Contents

1. [Overview](#overview)
2. [Architecture Diagram](#architecture-diagram)
3. [Component Structure](#component-structure)
4. [Auto-Configuration Mechanism](#auto-configuration-mechanism)
5. [Security Flow](#security-flow)
6. [Token Validation Flow](#token-validation-flow)
7. [Toggle Mechanism](#toggle-mechanism)
8. [Design Patterns](#design-patterns)
9. [Dependencies](#dependencies)

---

## Overview

The OAuth2 OIDC Spring Boot Starter is a plug-and-play library that provides enterprise-level authentication and authorization for Spring Boot applications. It follows Spring Boot's auto-configuration pattern to require zero code from developers - just add the dependency and configure properties.

### Key Characteristics

- **Zero Code Required**: Auto-configures all security components
- **Conditional Configuration**: Only activates when `oauth2.enabled=true`
- **Provider-Agnostic**: Works with multiple OAuth2 providers
- **Stateless**: JWT-based authentication, no server-side sessions
- **Production-Ready**: Built with Spring Security best practices

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                      Spring Boot Application                     │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │        OAuth2 OIDC Spring Boot Starter (Library)         │   │
│  │                                                           │   │
│  │  ┌─────────────────────────────────────────────────┐    │   │
│  │  │  OAuth2AutoConfiguration                        │    │   │
│  │  │  - Conditional on oauth2.enabled=true          │    │   │
│  │  │  - Loads OAuth2Properties                      │    │   │
│  │  │  - Validates configuration                      │    │   │
│  │  └─────────────────────────────────────────────────┘    │   │
│  │                         │                                 │   │
│  │                         ▼                                 │   │
│  │  ┌─────────────────────────────────────────────────┐    │   │
│  │  │  OAuth2SecurityConfig                           │    │   │
│  │  │  - SecurityFilterChain bean                    │    │   │
│  │  │  - JwtDecoder bean                             │    │   │
│  │  │  - CORS configuration                           │    │   │
│  │  │  - Endpoint permissions                         │    │   │
│  │  └─────────────────────────────────────────────────┘    │   │
│  │                         │                                 │   │
│  │                         ▼                                 │   │
│  │  ┌─────────────────────────────────────────────────┐    │   │
│  │  │  OAuth2AuthenticationController                 │    │   │
│  │  │  - /oauth2/authorize                           │    │   │
│  │  │  - /oauth2/token                               │    │   │
│  │  │  - /oauth2/login                               │    │   │
│  │  │  - /oauth2/refresh                             │    │   │
│  │  │  - /oauth2/userinfo                            │    │   │
│  │  │  - /oauth2/validate                            │    │   │
│  │  └─────────────────────────────────────────────────┘    │   │
│  │                         │                                 │   │
│  │                         ▼                                 │   │
│  │  ┌─────────────────────────────────────────────────┐    │   │
│  │  │  OAuth2ClientService                            │    │   │
│  │  │  - exchangeCodeForToken()                      │    │   │
│  │  │  - authenticateWithPassword()                  │    │   │
│  │  │  - refreshToken()                              │    │   │
│  │  │  - getUserInfo()                               │    │   │
│  │  │  - getAuthorizationUrl()                       │    │   │
│  │  └─────────────────────────────────────────────────┘    │   │
│  │                         │                                 │   │
│  │                         ▼                                 │   │
│  │  ┌─────────────────────────────────────────────────┐    │   │
│  │  │  TokenValidationService                         │    │   │
│  │  │  - validateToken()                             │    │   │
│  │  │  - extractClaims()                             │    │   │
│  │  └─────────────────────────────────────────────────┘    │   │
│  │                                                           │   │
│  └───────────────────────────────────────────────────────────┘   │
│                                                                   │
│  ┌───────────────────────────────────────────────────────────┐   │
│  │  User's Application Code (Controllers, Services, etc.)   │   │
│  │  - @PreAuthorize("hasRole('ADMIN')")                    │   │
│  │  - @AuthenticationPrincipal Jwt jwt                     │   │
│  └───────────────────────────────────────────────────────────┘   │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
                            │
                            ▼
        ┌──────────────────────────────────────┐
        │  OAuth2 Provider (Keycloak/Okta/etc) │
        │  - Authorization endpoint             │
        │  - Token endpoint                     │
        │  - UserInfo endpoint                  │
        │  - JWK Set endpoint                   │
        └──────────────────────────────────────┘
```

---

## Component Structure

### Package Organization

```
com.shashankpk.oauth2.starter/
├── config/
│   └── OAuth2AutoConfiguration.java       # Auto-configuration entry point
├── controller/
│   └── OAuth2AuthenticationController.java # REST API endpoints
├── dto/
│   ├── LoginRequest.java                  # Username/password login request
│   ├── TokenResponse.java                 # OAuth2 token response
│   ├── UserInfo.java                      # User information
│   ├── RefreshTokenRequest.java           # Token refresh request
│   └── ApiResponse.java                   # Standardized API response
├── exception/
│   ├── OAuth2AuthenticationException.java # Custom exception
│   └── GlobalExceptionHandler.java        # Global error handling
├── properties/
│   └── OAuth2Properties.java              # Configuration properties
├── provider/
│   └── OAuth2Provider.java                # Provider-specific configurations
├── security/
│   └── OAuth2SecurityConfig.java          # Spring Security configuration
└── service/
    ├── OAuth2ClientService.java           # OAuth2 client operations
    └── TokenValidationService.java        # Token validation logic
```

### Component Descriptions

#### 1. OAuth2AutoConfiguration

**Purpose**: Entry point for Spring Boot auto-configuration

**Key Features**:
- Conditional activation based on `oauth2.enabled` property
- Validates OAuth2 configuration on startup
- Logs configuration details for debugging
- Creates WebClient bean for HTTP communication

**Annotations**:
```java
@AutoConfiguration                                    // Spring Boot auto-configuration
@ConditionalOnClass(...)                             // Only if OAuth2 classes present
@ConditionalOnProperty(prefix = "oauth2",            // Only if oauth2.enabled=true
                       name = "enabled",
                       havingValue = "true",
                       matchIfMissing = true)
@EnableConfigurationProperties(OAuth2Properties.class) // Bind properties
@ComponentScan(basePackages = "com.shashankpk.oauth2.starter") // Scan components
```

**Configuration Validation**:
- Checks client-id, client-secret, and issuer-uri are configured
- Validates provider-specific requirements
- Warns about missing JWK Set URI for custom providers

#### 2. OAuth2Properties

**Purpose**: Configuration properties binding

**Structure**:
```java
@ConfigurationProperties(prefix = "oauth2")
public class OAuth2Properties {
    private boolean enabled = true;
    private String provider = "KEYCLOAK";
    private String clientId;
    private String clientSecret;
    private String issuerUri;
    private String[] scopes = {"openid", "profile", "email"};
    private boolean customLoginEnabled = false;
    private JwtProperties jwt = new JwtProperties();
    private CorsProperties cors = new CorsProperties();
    // ... more properties
}
```

**Nested Configuration**:
- `JwtProperties`: JWT validation settings
- `CorsProperties`: CORS configuration

#### 3. OAuth2SecurityConfig

**Purpose**: Configure Spring Security for OAuth2

**Key Responsibilities**:
1. **SecurityFilterChain**: Define endpoint permissions
2. **JwtDecoder**: Configure JWT token validation
3. **CORS**: Enable cross-origin requests

**Security Configuration**:
```java
http
    .csrf(AbstractHttpConfigurer::disable)                    // Disable CSRF for REST APIs
    .cors(cors -> cors.configurationSource(...))              // Configure CORS
    .sessionManagement(session ->
        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))  // No sessions
    .authorizeHttpRequests(authorize -> authorize
        .requestMatchers("/oauth2/**").permitAll()            // Public OAuth2 endpoints
        .anyRequest().authenticated()                         // All others require auth
    )
    .oauth2ResourceServer(oauth2 -> oauth2
        .jwt(jwt -> jwt.decoder(jwtDecoder()))               // JWT validation
    );
```

**JWT Decoder**:
- Uses `NimbusJwtDecoder` with JWK Set URI
- Automatically fetches public keys from OAuth2 provider
- Caches keys for performance
- Validates token signature and claims

#### 4. OAuth2AuthenticationController

**Purpose**: REST API endpoints for authentication operations

**Endpoints**:

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/oauth2/authorize` | GET | Get authorization URL for OAuth2 flow |
| `/oauth2/token` | POST | Exchange authorization code for tokens |
| `/oauth2/login` | POST | Custom login with username/password |
| `/oauth2/refresh` | POST | Refresh access token |
| `/oauth2/userinfo` | GET | Get authenticated user information |
| `/oauth2/validate` | GET | Validate JWT token |
| `/oauth2/logout` | POST | Logout (client-side token removal) |
| `/oauth2/health` | GET | Health check |
| `/oauth2/config` | GET | Get authentication configuration |

**Example Flow**:
```java
@PostMapping("/token")
public ResponseEntity<ApiResponse<TokenResponse>> exchangeToken(
        @RequestParam String code,
        @RequestParam String redirectUri) {

    TokenResponse tokenResponse = oauth2ClientService.exchangeCodeForToken(code, redirectUri);

    // Optionally fetch user info
    if (tokenResponse.getAccessToken() != null) {
        UserInfo userInfo = oauth2ClientService.getUserInfo(tokenResponse.getAccessToken());
        tokenResponse.setUserInfo(userInfo);
    }

    return ResponseEntity.ok(ApiResponse.success("Authentication successful", tokenResponse));
}
```

#### 5. OAuth2ClientService

**Purpose**: Handle OAuth2 protocol operations

**Key Methods**:

**exchangeCodeForToken()**:
- Exchanges authorization code for access token
- Used in authorization code flow
- Makes POST request to token endpoint

**authenticateWithPassword()**:
- Direct authentication with username/password
- Uses Resource Owner Password Credentials Grant
- Only works when `customLoginEnabled=true`

**refreshToken()**:
- Refreshes expired access token
- Uses refresh token grant
- Returns new access and refresh tokens

**getUserInfo()**:
- Retrieves user information from OAuth2 provider
- Uses access token for authentication
- Returns UserInfo object

**Implementation Details**:
```java
private TokenResponse executeTokenRequest(String tokenUri, MultiValueMap<String, String> formData) {
    WebClient webClient = webClientBuilder.build();

    return webClient.post()
            .uri(tokenUri)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData(formData))
            .retrieve()
            .bodyToMono(TokenResponse.class)
            .block();
}
```

#### 6. TokenValidationService

**Purpose**: JWT token validation and claim extraction

**Key Methods**:
- `validateToken()`: Validates JWT signature and expiration
- `extractClaims()`: Extracts claims from JWT
- Uses Spring Security's JwtDecoder internally

#### 7. OAuth2Provider

**Purpose**: Provider-specific endpoint configuration

**Supported Providers**:
- KEYCLOAK
- OKTA
- GOOGLE
- META (Facebook)
- GITHUB
- MICROSOFT
- CUSTOM

**Provider Configuration**:
Each provider has methods to build:
- Authorization URI
- Token URI
- UserInfo URI
- JWK Set URI

**Example**:
```java
public String buildTokenUri(String issuerUri) {
    switch (this) {
        case KEYCLOAK:
            return issuerUri + "/protocol/openid-connect/token";
        case OKTA:
            return issuerUri + "/v1/token";
        case GOOGLE:
            return "https://oauth2.googleapis.com/token";
        // ... more providers
    }
}
```

---

## Auto-Configuration Mechanism

### How Spring Boot Auto-Configuration Works

1. **spring.factories File**: Located in `src/main/resources/META-INF/spring.factories`

```properties
org.springframework.boot.autoconfigure.AutoConfiguration=\
com.shashankpk.oauth2.starter.config.OAuth2AutoConfiguration
```

2. **Conditional Activation**: Library only activates when conditions are met

```java
@ConditionalOnProperty(prefix = "oauth2", name = "enabled", havingValue = "true", matchIfMissing = true)
```

3. **Component Scanning**: All components are automatically discovered

```java
@ComponentScan(basePackages = "com.shashankpk.oauth2.starter")
```

### Configuration Loading Order

```
1. Application starts
          ↓
2. Spring Boot reads spring.factories
          ↓
3. Checks @ConditionalOnProperty (oauth2.enabled)
          ↓
4. If true, loads OAuth2AutoConfiguration
          ↓
5. OAuth2AutoConfiguration validates properties
          ↓
6. Creates beans (SecurityFilterChain, JwtDecoder, etc.)
          ↓
7. Component scan discovers services and controllers
          ↓
8. OAuth2SecurityConfig configures Spring Security
          ↓
9. Application ready with OAuth2 enabled
```

---

## Security Flow

### 1. Authorization Code Flow

```
┌──────┐                                           ┌─────────────┐
│      │                                           │             │
│ User │                                           │  Frontend   │
│      │                                           │             │
└──┬───┘                                           └──────┬──────┘
   │                                                      │
   │ 1. Click "Login"                                    │
   │─────────────────────────────────────────────────────▶
   │                                                      │
   │                                    ┌─────────────────┴────────────────┐
   │                                    │ 2. GET /oauth2/authorize         │
   │                                    │    ?redirectUri=http://app.com   │
   │                                    └─────────────────┬────────────────┘
   │                                                      │
   │                                           ┌──────────▼──────────┐
   │                                           │   Controller calls  │
   │                                           │   OAuth2ClientSvc   │
   │                                           └──────────┬──────────┘
   │                                                      │
   │                                    ┌─────────────────┴────────────────┐
   │                                    │ 3. Return authorization URL      │
   │◀────────────────────────────────────────────────────────────────────────
   │                                                      │
   │ 4. Redirect to Keycloak login page                  │
   │──────────────────────────────────────────────────────▶
   │                                                      │
   │                          ┌────────────────────────────┴────────────┐
   │                          │  OAuth2 Provider (Keycloak)             │
   │                          │  - Shows login page                     │
   │                          │  - User enters credentials              │
   │                          │  - Validates credentials                │
   │                          │  - Generates authorization code         │
   │                          └────────────────────────────┬────────────┘
   │                                                      │
   │ 5. Redirect with authorization code                  │
   │    ?code=AUTH_CODE&state=xyz                        │
   │◀──────────────────────────────────────────────────────
   │                                                      │
   │ 6. POST /oauth2/token                               │
   │    {code: "AUTH_CODE", redirectUri: "..."}          │
   │─────────────────────────────────────────────────────▶
   │                                                      │
   │                                    ┌─────────────────┴────────────────┐
   │                                    │ 7. OAuth2ClientService           │
   │                                    │    - Exchange code for token     │
   │                                    │    - POST to provider token endpoint│
   │                                    └──────────┬───────────────────────┘
   │                                              │
   │                          ┌────────────────────────────┴────────────┐
   │                          │  OAuth2 Provider                        │
   │                          │  - Validates code                       │
   │                          │  - Returns access_token, refresh_token  │
   │                          └────────────────────────────┬────────────┘
   │                                                      │
   │ 8. Return tokens                                     │
   │◀──────────────────────────────────────────────────────
   │                                                      │
   │ {access_token: "...", refresh_token: "...", ...}    │
```

### 2. Token Validation on Protected Endpoints

```
┌──────────┐                 ┌──────────────────┐                 ┌─────────────┐
│ Frontend │                 │  Spring Security │                 │   Provider  │
│          │                 │  Filter Chain    │                 │  (Keycloak) │
└────┬─────┘                 └────────┬─────────┘                 └──────┬──────┘
     │                                │                                   │
     │ 1. GET /api/profile            │                                   │
     │    Authorization: Bearer TOKEN │                                   │
     │────────────────────────────────▶                                   │
     │                                │                                   │
     │                      ┌─────────┴─────────┐                         │
     │                      │ BearerTokenAuth   │                         │
     │                      │ Filter            │                         │
     │                      │ - Extract token   │                         │
     │                      └─────────┬─────────┘                         │
     │                                │                                   │
     │                      ┌─────────┴─────────┐                         │
     │                      │ JwtDecoder        │                         │
     │                      │ - Decode JWT      │                         │
     │                      │ - Extract kid     │                         │
     │                      └─────────┬─────────┘                         │
     │                                │                                   │
     │                                │ 2. Fetch JWK Set (first time)     │
     │                                │───────────────────────────────────▶
     │                                │                                   │
     │                                │ 3. Return public keys             │
     │                                │◀───────────────────────────────────
     │                                │                                   │
     │                      ┌─────────┴─────────┐                         │
     │                      │ JwtDecoder        │                         │
     │                      │ - Verify signature│                         │
     │                      │ - Validate exp    │                         │
     │                      │ - Validate iss    │                         │
     │                      │ - Extract claims  │                         │
     │                      └─────────┬─────────┘                         │
     │                                │                                   │
     │                      ┌─────────┴─────────┐                         │
     │                      │ SecurityContext   │                         │
     │                      │ - Store auth      │                         │
     │                      └─────────┬─────────┘                         │
     │                                │                                   │
     │                      ┌─────────┴─────────┐                         │
     │                      │ Access Decision   │                         │
     │                      │ Manager           │                         │
     │                      │ - Check roles     │                         │
     │                      └─────────┬─────────┘                         │
     │                                │                                   │
     │                      ┌─────────┴─────────┐                         │
     │                      │ Your Controller   │                         │
     │                      │ @GetMapping       │                         │
     │                      └─────────┬─────────┘                         │
     │                                │                                   │
     │ 4. Return response             │                                   │
     │◀────────────────────────────────                                   │
     │                                                                    │
```

---

## Token Validation Flow

### JWT Validation Steps

1. **Extract Token**: From Authorization header (`Bearer <token>`)
2. **Decode Header**: Parse JWT header to get algorithm and key ID
3. **Fetch Public Key**: Get matching public key from JWK Set endpoint (cached)
4. **Verify Signature**: Validate token signature using public key
5. **Validate Claims**:
   - `exp` (expiration): Token not expired
   - `iss` (issuer): Matches configured issuer
   - `aud` (audience): Matches expected audience (if configured)
   - `nbf` (not before): Current time is after nbf
6. **Extract User Info**: Get subject, roles, and other claims
7. **Set Authentication**: Store in SecurityContext
8. **Authorization**: Check if user has required roles

### JWT Structure

```
eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6ImFiYzEyMyJ9.
eyJzdWIiOiJ1c2VyMTIzIiwiaXNzIjoiaHR0cDovL2tleWNsb2FrOjgxODAvcmVhbG1zL215cmVhbG0iLCJleHAiOjE3MDU4ODk2MDAsImlhdCI6MTcwNTg4NjAwMCwicm9sZXMiOlsiVVNFUiJdfQ.
SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c

[        HEADER        ].[                  PAYLOAD                  ].[   SIGNATURE   ]
```

**Header**:
```json
{
  "alg": "RS256",
  "typ": "JWT",
  "kid": "abc123"
}
```

**Payload**:
```json
{
  "sub": "user123",
  "iss": "http://keycloak:8180/realms/myrealm",
  "exp": 1705889600,
  "iat": 1705886000,
  "roles": ["USER"]
}
```

**Signature**: RSA signature using provider's private key

---

## Toggle Mechanism

### How `oauth2.enabled` Works

The toggle mechanism uses Spring Boot's conditional configuration:

```java
@ConditionalOnProperty(
    prefix = "oauth2",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true  // Default to enabled if property not set
)
```

### When `oauth2.enabled=true` (Default)

1. OAuth2AutoConfiguration activates
2. All security beans are created
3. JWT validation enabled
4. OAuth2 endpoints available
5. Protected endpoints require authentication

### When `oauth2.enabled=false`

1. OAuth2AutoConfiguration is skipped
2. No security beans created
3. No JWT validation
4. No OAuth2 endpoints
5. Application runs without OAuth2 authentication
6. User must implement custom security

### Environment-Based Configuration

**Development**:
```properties
# application-dev.properties
oauth2.enabled=false
```

**Production**:
```properties
# application-prod.properties
oauth2.enabled=true
oauth2.provider=KEYCLOAK
oauth2.client-id=${OAUTH2_CLIENT_ID}
oauth2.client-secret=${OAUTH2_CLIENT_SECRET}
oauth2.issuer-uri=${OAUTH2_ISSUER_URI}
```

---

## Design Patterns

### 1. Auto-Configuration Pattern

**Purpose**: Automatic configuration based on classpath and properties

**Implementation**:
- `@AutoConfiguration` annotation
- `spring.factories` file
- Conditional beans with `@ConditionalOnProperty`

### 2. Strategy Pattern

**Purpose**: Support multiple OAuth2 providers

**Implementation**: `OAuth2Provider` enum with provider-specific strategies

```java
public enum OAuth2Provider {
    KEYCLOAK {
        public String buildTokenUri(String issuerUri) {
            return issuerUri + "/protocol/openid-connect/token";
        }
    },
    OKTA {
        public String buildTokenUri(String issuerUri) {
            return issuerUri + "/v1/token";
        }
    }
    // ... more providers
}
```

### 3. Template Method Pattern

**Purpose**: Common token request flow with provider-specific details

**Implementation**: `OAuth2ClientService.executeTokenRequest()`

### 4. Builder Pattern

**Purpose**: Flexible configuration of OAuth2 properties

**Implementation**: Nested configuration classes in `OAuth2Properties`

### 5. Facade Pattern

**Purpose**: Simplify OAuth2 operations for controllers

**Implementation**: `OAuth2ClientService` provides simple interface to complex OAuth2 operations

---

## Dependencies

### Core Dependencies

```xml
<!-- Spring Boot Starter Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- Spring Security OAuth2 Resource Server -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>

<!-- Spring Boot Starter Web -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- Spring WebFlux (for WebClient) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>

<!-- Lombok -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>

<!-- Jakarta Validation -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

### Dependency Purpose

| Dependency | Purpose |
|------------|---------|
| spring-boot-starter-security | Core Spring Security framework |
| spring-boot-starter-oauth2-resource-server | JWT validation and OAuth2 resource server support |
| spring-boot-starter-web | REST API controllers |
| spring-boot-starter-webflux | WebClient for HTTP requests to OAuth2 provider |
| lombok | Reduce boilerplate code |
| spring-boot-starter-validation | Request validation |

---

## Summary

The OAuth2 OIDC Spring Boot Starter provides a complete, production-ready authentication solution through:

1. **Auto-Configuration**: Zero code required, just properties
2. **Conditional Activation**: Toggle with `oauth2.enabled` property
3. **Provider Agnostic**: Works with multiple OAuth2 providers
4. **Security Best Practices**: JWT validation, CORS, stateless architecture
5. **Complete REST API**: All OAuth2 operations exposed as endpoints
6. **Flexible**: Supports both standard OAuth2 flow and custom login pages

The architecture follows Spring Boot conventions and best practices, making it familiar to Spring developers and easy to integrate into existing applications.

---

**Version**: 1.0.0
**Last Updated**: 2026-02-05
