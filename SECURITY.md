# Spring Security - Complete Guide

A comprehensive guide to understanding authentication, authorization, OAuth2, OIDC, JWT, and Spring Security concepts with practical examples and diagrams.

---

## Table of Contents

1. [Introduction to Spring Security](#1-introduction-to-spring-security)
2. [Authentication vs Authorization](#2-authentication-vs-authorization)
3. [Spring Security Architecture](#3-spring-security-architecture)
4. [Authentication Methods](#4-authentication-methods)
5. [OAuth2 and OpenID Connect (OIDC)](#5-oauth2-and-openid-connect-oidc)
6. [OAuth2 Grant Types (Flows)](#6-oauth2-grant-types-flows)
7. [Resource Server](#7-resource-server)
8. [Authorization Server](#8-authorization-server)
9. [JWT (JSON Web Tokens)](#9-jwt-json-web-tokens)
10. [Public Key Cryptography](#10-public-key-cryptography)
11. [Spring Security Components](#11-spring-security-components)
12. [Security Best Practices](#12-security-best-practices)
13. [Troubleshooting Guide](#13-troubleshooting-guide)

---

## 1. Introduction to Spring Security

Spring Security is a powerful and highly customizable authentication and access-control framework for Java applications. It's the de facto standard for securing Spring-based applications.

### Key Features
- **Comprehensive authentication support** - Various authentication mechanisms
- **Authorization** - Role-based and permission-based access control
- **Protection against common attacks** - CSRF, XSS, Session Fixation
- **Integration with OAuth2/OIDC** - Modern authentication protocols
- **Servlet API integration** - Works seamlessly with Spring MVC

---

## 2. Authentication vs Authorization

```mermaid
graph LR
    A[User Request] --> B{Authenticated?}
    B -->|No| C[Login Required]
    B -->|Yes| D{Authorized?}
    D -->|No| E[Access Denied 403]
    D -->|Yes| F[Access Granted 200]
    C --> G[Authentication Process]
    G --> B
```

### Authentication
**"Who are you?"** - Verifying the identity of a user.

- Process of confirming user identity
- Usually involves username/password, tokens, certificates
- Examples: Login with email/password, OAuth2 login, JWT validation

### Authorization
**"What can you do?"** - Determining what resources a user can access.

- Process of granting or denying access to resources
- Based on roles, permissions, or policies
- Examples: ROLE_ADMIN can delete users, ROLE_USER can only read

---

## 3. Spring Security Architecture

```mermaid
graph TB
    A[HTTP Request] --> B[Security Filter Chain]
    B --> C[Authentication Filter]
    C --> D[Authentication Manager]
    D --> E[Authentication Provider]
    E --> F[UserDetailsService]
    F --> G{User Exists?}
    G -->|Yes| H[Load User Details]
    G -->|No| I[Authentication Exception]
    H --> J[Authentication Object]
    J --> K[Security Context]
    K --> L[Access Decision Manager]
    L --> M{Authorized?}
    M -->|Yes| N[Controller]
    M -->|No| O[Access Denied Handler]
    N --> P[HTTP Response]
    O --> P
```

### Key Components

1. **Security Filter Chain** - Chain of filters that process security
2. **Authentication Manager** - Coordinates authentication
3. **Authentication Provider** - Performs actual authentication
4. **UserDetailsService** - Loads user-specific data
5. **Security Context** - Stores authentication information
6. **Access Decision Manager** - Handles authorization decisions

---

## 4. Authentication Methods

### 4.1 Form-Based Authentication

Traditional username/password form login.

```mermaid
sequenceDiagram
    participant User
    participant Browser
    participant App
    participant Database

    User->>Browser: Enter credentials
    Browser->>App: POST /login (username, password)
    App->>Database: Query user
    Database-->>App: User details
    App->>App: Verify password (BCrypt)
    App-->>Browser: Set Session Cookie
    Browser-->>User: Redirect to home

    Note over Browser,App: Subsequent requests include session cookie
```

**Configuration Example:**
```java
@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/home")
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/public/**").permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
```

### 4.2 HTTP Basic Authentication

Sends credentials in HTTP headers (Base64 encoded).

```mermaid
sequenceDiagram
    participant Client
    participant Server

    Client->>Server: GET /api/data
    Server-->>Client: 401 Unauthorized<br/>WWW-Authenticate: Basic
    Client->>Client: Encode credentials to Base64
    Client->>Server: GET /api/data<br/>Authorization: Basic dXNlcjpwYXNz
    Server->>Server: Decode & validate credentials
    Server-->>Client: 200 OK + Data
```

**Pros:** Simple to implement
**Cons:** Must send credentials with every request, not secure without HTTPS

### 4.3 JWT (Token-Based) Authentication

Modern stateless authentication using JSON Web Tokens.

```mermaid
sequenceDiagram
    participant Client
    participant Server
    participant Database

    Client->>Server: POST /login (username, password)
    Server->>Database: Validate credentials
    Database-->>Server: User valid
    Server->>Server: Generate JWT token
    Server-->>Client: 200 OK + JWT token

    Note over Client: Store JWT (localStorage/cookie)

    Client->>Server: GET /api/data<br/>Authorization: Bearer JWT
    Server->>Server: Validate JWT signature
    Server->>Server: Extract user info from JWT
    Server-->>Client: 200 OK + Data
```

**Advantages:**
- Stateless - No server-side session storage
- Scalable - Works across multiple servers
- Mobile-friendly - Easy to use in mobile apps
- Contains user information - Reduces database queries

---

## 5. OAuth2 and OpenID Connect (OIDC)

### What is OAuth2?

**OAuth2** is an authorization framework that enables applications to obtain limited access to user accounts on an HTTP service.

**Key Concept:** OAuth2 is about **authorization**, not authentication.

### What is OIDC?

**OpenID Connect (OIDC)** is an identity layer on top of OAuth2 that adds **authentication**.

```mermaid
graph TB
    A[OIDC - OpenID Connect]
    B[OAuth2 - Authorization Framework]
    C[HTTP/HTTPS]

    A -->|Built on top of| B
    B -->|Uses| C

    A1[Authentication<br/>Who you are]
    A2[ID Token<br/>User Identity]

    B1[Authorization<br/>What you can access]
    B2[Access Token<br/>API Access]

    A -.-> A1
    A -.-> A2
    B -.-> B1
    B -.-> B2
```

### OAuth2 vs OIDC

| Feature | OAuth2 | OIDC |
|---------|--------|------|
| Purpose | Authorization | Authentication + Authorization |
| Token Types | Access Token, Refresh Token | ID Token + OAuth2 tokens |
| User Info | No | Yes (via ID Token) |
| Use Case | API access | Single Sign-On (SSO) |

---

## 6. OAuth2 Grant Types (Flows)

### 6.1 Authorization Code Flow (Recommended)

**Most secure flow** for web and mobile applications.

```mermaid
sequenceDiagram
    participant User
    participant Client as Client App<br/>(Your Backend)
    participant Browser
    participant AuthServer as Authorization Server<br/>(Keycloak)
    participant ResourceServer as Resource Server<br/>(Protected API)

    User->>Client: 1. Click "Login with Keycloak"
    Client->>Client: 2. Generate state parameter
    Client-->>Browser: 3. Redirect to /oauth2/authorize

    Browser->>AuthServer: 4. GET /auth?<br/>response_type=code<br/>client_id=my-app<br/>redirect_uri=http://app.com/callback<br/>scope=openid profile email<br/>state=xyz123

    AuthServer-->>Browser: 5. Show login page
    Browser->>User: 6. Display login form
    User->>Browser: 7. Enter credentials
    Browser->>AuthServer: 8. POST /login (credentials)

    AuthServer->>AuthServer: 9. Validate credentials
    AuthServer-->>Browser: 10. Redirect to callback<br/>?code=AUTH_CODE&state=xyz123

    Browser->>Client: 11. GET /callback?code=AUTH_CODE&state=xyz123
    Client->>Client: 12. Validate state parameter

    Client->>AuthServer: 13. POST /token<br/>grant_type=authorization_code<br/>code=AUTH_CODE<br/>client_id=my-app<br/>client_secret=SECRET<br/>redirect_uri=http://app.com/callback

    AuthServer->>AuthServer: 14. Validate code & credentials
    AuthServer-->>Client: 15. Return tokens:<br/>{<br/>  access_token: "...",<br/>  refresh_token: "...",<br/>  id_token: "..."<br/>}

    Client->>Client: 16. Validate & decode ID token
    Client-->>Browser: 17. Set session/cookie
    Browser-->>User: 18. Logged in!

    Note over User,ResourceServer: Making API calls with access token

    User->>Client: 19. Request protected resource
    Client->>ResourceServer: 20. GET /api/data<br/>Authorization: Bearer ACCESS_TOKEN
    ResourceServer->>ResourceServer: 21. Validate token
    ResourceServer-->>Client: 22. Return data
    Client-->>User: 23. Display data
```

**Flow Breakdown:**

1. **Initiate Login** - User clicks login button
2. **Generate State** - Client generates random state for CSRF protection
3. **Redirect to Authorization** - Client redirects to authorization endpoint
4. **Authorization Request** - Browser requests authorization from auth server
5. **Login Page** - Auth server shows login page
6. **User Login** - User sees login form
7. **Submit Credentials** - User enters username/password
8. **Authenticate** - Browser submits credentials
9. **Validate** - Auth server validates credentials
10. **Authorization Code** - Auth server redirects with authorization code
11. **Callback** - Browser hits client callback URL with code
12. **Validate State** - Client validates CSRF protection
13. **Token Exchange** - Client exchanges code for tokens
14. **Validate Exchange** - Auth server validates the exchange
15. **Return Tokens** - Auth server returns access, refresh, and ID tokens
16. **Process Tokens** - Client validates and stores tokens
17. **Session Created** - Client creates user session
18. **Login Complete** - User is logged in
19-23. **API Access** - Using access token for API calls

**Why is this secure?**
- Authorization code is single-use and short-lived
- Client secret never exposed to browser
- State parameter prevents CSRF attacks
- Tokens obtained server-to-server

### 6.2 Resource Owner Password Credentials (Direct Auth)

**Less secure** - Only use when you control both client and server.

```mermaid
sequenceDiagram
    participant User
    participant Client as Client App<br/>(Your App)
    participant AuthServer as Authorization Server<br/>(Keycloak)

    User->>Client: 1. Enter username & password<br/>in your app's login form
    Client->>AuthServer: 2. POST /token<br/>grant_type=password<br/>username=user@example.com<br/>password=secret123<br/>client_id=my-app<br/>client_secret=SECRET

    AuthServer->>AuthServer: 3. Validate credentials

    alt Credentials Valid
        AuthServer-->>Client: 4. Return tokens:<br/>{<br/>  access_token: "...",<br/>  refresh_token: "...",<br/>  expires_in: 3600<br/>}
        Client->>Client: 5. Store tokens
        Client-->>User: 6. Logged in!
    else Credentials Invalid
        AuthServer-->>Client: 4. 401 Unauthorized<br/>{error: "invalid_grant"}
        Client-->>User: 5. Show error message
    end
```

**When to use:**
- You own both the client and server
- Building a custom login page
- Testing/development environments
- Migrating from legacy auth systems

**Security Concerns:**
- Client handles user credentials directly
- User credentials exposed to client application
- Breaks the OAuth2 delegation model

**In our project:** This is enabled when `oauth2.custom-login-enabled=true`

### 6.3 Client Credentials Flow

Used for **machine-to-machine** communication (no user involved).

```mermaid
sequenceDiagram
    participant Service1 as Service 1<br/>(Client)
    participant AuthServer as Authorization Server
    participant Service2 as Service 2<br/>(Resource Server)

    Service1->>AuthServer: 1. POST /token<br/>grant_type=client_credentials<br/>client_id=service1<br/>client_secret=SECRET<br/>scope=read:data

    AuthServer->>AuthServer: 2. Validate client credentials
    AuthServer-->>Service1: 3. Return access token:<br/>{access_token: "..."}

    Service1->>Service2: 4. GET /api/data<br/>Authorization: Bearer TOKEN
    Service2->>Service2: 5. Validate token
    Service2-->>Service1: 6. Return data
```

**Use Cases:**
- Microservices communication
- Background jobs accessing APIs
- Server-to-server integrations
- Automated scripts

### 6.4 Refresh Token Flow

Obtaining a new access token without re-authentication.

```mermaid
sequenceDiagram
    participant Client
    participant AuthServer as Authorization Server
    participant ResourceServer as Resource Server

    Note over Client: Access token expired

    Client->>ResourceServer: 1. GET /api/data<br/>Authorization: Bearer EXPIRED_TOKEN
    ResourceServer-->>Client: 2. 401 Unauthorized<br/>{error: "invalid_token"}

    Client->>Client: 3. Check for refresh token

    Client->>AuthServer: 4. POST /token<br/>grant_type=refresh_token<br/>refresh_token=REFRESH_TOKEN<br/>client_id=my-app<br/>client_secret=SECRET

    AuthServer->>AuthServer: 5. Validate refresh token

    alt Refresh Token Valid
        AuthServer-->>Client: 6. Return new tokens:<br/>{<br/>  access_token: "NEW_TOKEN",<br/>  refresh_token: "NEW_REFRESH",<br/>  expires_in: 3600<br/>}
        Client->>Client: 7. Store new tokens
        Client->>ResourceServer: 8. GET /api/data<br/>Authorization: Bearer NEW_TOKEN
        ResourceServer-->>Client: 9. 200 OK + Data
    else Refresh Token Invalid
        AuthServer-->>Client: 6. 401 Unauthorized
        Client-->>Client: 7. Redirect to login
    end
```

**Key Points:**
- Access tokens are short-lived (15min - 1hour)
- Refresh tokens are long-lived (days/weeks)
- Reduces need for frequent re-authentication
- Can be revoked for security

---

## 7. Resource Server

A **Resource Server** hosts protected resources (APIs) and validates access tokens.

```mermaid
graph TB
    subgraph "Resource Server (Your API)"
        A[Incoming Request]
        B[Extract Token from Header]
        C{Token Present?}
        D[JWT Validation]
        E{Valid Token?}
        F[Extract Claims]
        G[Check Permissions]
        H{Authorized?}
        I[Process Request]
        J[Return Response]
        K[Return 401]
        L[Return 403]

        A --> B
        B --> C
        C -->|No| K
        C -->|Yes| D
        D --> E
        E -->|No| K
        E -->|Yes| F
        F --> G
        G --> H
        H -->|No| L
        H -->|Yes| I
        I --> J
    end

    M[Authorization Server<br/>Keycloak]

    D -.->|Fetch JWK Set| M
```

### Resource Server Configuration

```java
@Configuration
@EnableWebSecurity
public class ResourceServerConfig {

    @Bean
    public SecurityFilterChain resourceServerFilterChain(HttpSecurity http) throws Exception {
        http
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/public/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/user/**").hasAnyRole("USER", "ADMIN")
                .anyRequest().authenticated()
            );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter =
            new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter jwtAuthenticationConverter =
            new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(
            grantedAuthoritiesConverter
        );

        return jwtAuthenticationConverter;
    }
}
```

### Token Validation Process

```mermaid
sequenceDiagram
    participant Client
    participant ResourceServer as Resource Server<br/>(Your API)
    participant AuthServer as Authorization Server<br/>(Keycloak)

    Note over ResourceServer: First request - no cached JWK Set

    Client->>ResourceServer: GET /api/data<br/>Authorization: Bearer JWT_TOKEN
    ResourceServer->>ResourceServer: Extract JWT from header
    ResourceServer->>ResourceServer: Decode JWT header
    ResourceServer->>ResourceServer: Extract 'kid' (Key ID)

    ResourceServer->>AuthServer: GET /.well-known/jwks.json
    AuthServer-->>ResourceServer: Return JWK Set (public keys)
    ResourceServer->>ResourceServer: Cache JWK Set

    ResourceServer->>ResourceServer: Find matching public key using 'kid'
    ResourceServer->>ResourceServer: Verify JWT signature
    ResourceServer->>ResourceServer: Validate claims:<br/>- exp (expiration)<br/>- iat (issued at)<br/>- iss (issuer)<br/>- aud (audience)

    alt Token Valid
        ResourceServer->>ResourceServer: Extract user info & roles
        ResourceServer->>ResourceServer: Check authorization
        ResourceServer-->>Client: 200 OK + Data
    else Token Invalid
        ResourceServer-->>Client: 401 Unauthorized
    end

    Note over ResourceServer: Subsequent requests use cached JWK Set
```

---

## 8. Authorization Server

The **Authorization Server** issues tokens after authenticating users.

```mermaid
graph TB
    subgraph "Authorization Server (Keycloak)"
        A[User Authentication]
        B[Token Generation]
        C[Token Signing]
        D[User Management]
        E[Client Management]
        F[Scope Management]
        G[JWK Set Endpoint]
    end

    H[Resource Owner<br/>User]
    I[Client Application<br/>Your App]
    J[Resource Server<br/>Your API]

    H -->|Authenticate| A
    A --> B
    B --> C
    C -->|Issue Tokens| I
    I -->|Use Tokens| J
    J -->|Validate| G

    D -.-> A
    E -.-> A
    F -.-> B
```

### Key Responsibilities

1. **Authenticate Users** - Verify user credentials
2. **Issue Tokens** - Generate access, refresh, and ID tokens
3. **Token Management** - Sign, validate, and revoke tokens
4. **User Management** - Store and manage user accounts
5. **Client Registration** - Register and manage OAuth2 clients
6. **Scope Management** - Define and enforce access scopes

### Keycloak Endpoints

```
# OpenID Configuration (Discovery)
GET http://localhost:8180/realms/myrealm/.well-known/openid-configuration

# Authorization Endpoint
GET http://localhost:8180/realms/myrealm/protocol/openid-connect/auth

# Token Endpoint
POST http://localhost:8180/realms/myrealm/protocol/openid-connect/token

# User Info Endpoint
GET http://localhost:8180/realms/myrealm/protocol/openid-connect/userinfo

# JWK Set (Public Keys)
GET http://localhost:8180/realms/myrealm/protocol/openid-connect/certs

# Logout Endpoint
POST http://localhost:8180/realms/myrealm/protocol/openid-connect/logout
```

---

## 9. JWT (JSON Web Tokens)

### JWT Structure

A JWT consists of three parts separated by dots (`.`):

```
eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
[        HEADER        ].[             PAYLOAD              ].[           SIGNATURE          ]
```

```mermaid
graph LR
    A[JWT Token] --> B[Header]
    A --> C[Payload]
    A --> D[Signature]

    B --> B1[Algorithm: RS256<br/>Type: JWT<br/>Key ID: kid]
    C --> C1[Subject: user123<br/>Issuer: Keycloak<br/>Expiration: 1h<br/>Roles: USER, ADMIN]
    D --> D1[HMAC or RSA Signature<br/>Verifies integrity]
```

### JWT Components

#### 1. Header
```json
{
  "alg": "RS256",      // Algorithm: RSA with SHA-256
  "typ": "JWT",        // Type: JSON Web Token
  "kid": "abc123"      // Key ID: Which key to use for verification
}
```

#### 2. Payload (Claims)
```json
{
  // Standard claims
  "sub": "user123",                    // Subject: User identifier
  "iss": "http://localhost:8180/...",  // Issuer: Who issued this token
  "aud": "my-app",                     // Audience: Who this token is for
  "exp": 1735689600,                   // Expiration: Unix timestamp
  "iat": 1735686000,                   // Issued At: Unix timestamp
  "nbf": 1735686000,                   // Not Before: Unix timestamp

  // Custom claims
  "email": "user@example.com",
  "name": "John Doe",
  "roles": ["USER", "ADMIN"],
  "preferred_username": "johndoe"
}
```

#### 3. Signature
```
RSASHA256(
  base64UrlEncode(header) + "." + base64UrlEncode(payload),
  privateKey
)
```

### JWT Validation Flow

```mermaid
flowchart TD
    A[Receive JWT Token] --> B[Split token into parts]
    B --> C{3 parts?}
    C -->|No| Z[Invalid Token]
    C -->|Yes| D[Decode header & payload]

    D --> E[Extract 'kid' from header]
    E --> F[Fetch public key using kid]
    F --> G{Key found?}
    G -->|No| Z
    G -->|Yes| H[Verify signature]

    H --> I{Signature valid?}
    I -->|No| Z
    I -->|Yes| J[Check 'exp' claim]

    J --> K{Expired?}
    K -->|Yes| Z
    K -->|No| L[Check 'iss' claim]

    L --> M{Issuer valid?}
    M -->|No| Z
    M -->|Yes| N[Check 'aud' claim]

    N --> O{Audience valid?}
    O -->|No| Z
    O -->|Yes| P[Check 'nbf' claim]

    P --> Q{Before nbf?}
    Q -->|Yes| Z
    Q -->|No| R[Extract user info & roles]

    R --> S[Token Valid ✓]

    style S fill:#90EE90
    style Z fill:#FFB6C6
```

### JWT vs Opaque Tokens

| Feature | JWT | Opaque Token |
|---------|-----|--------------|
| Format | JSON, self-contained | Random string |
| Validation | Signature verification (local) | Database/API lookup (remote) |
| Size | Large (1-2 KB) | Small (20-40 bytes) |
| Stateless | Yes | No |
| Revocation | Difficult | Easy |
| Use Case | Microservices, APIs | Session management |

---

## 10. Public Key Cryptography

### Symmetric vs Asymmetric Encryption

```mermaid
graph TB
    subgraph "Symmetric Encryption (Same Key)"
        A1[Plaintext] -->|Encrypt| B1[Secret Key]
        B1 --> C1[Ciphertext]
        C1 -->|Decrypt| D1[Secret Key]
        D1 --> E1[Plaintext]
    end

    subgraph "Asymmetric Encryption (Key Pair)"
        A2[Plaintext] -->|Encrypt| B2[Public Key 🔓]
        B2 --> C2[Ciphertext]
        C2 -->|Decrypt| D2[Private Key 🔒]
        D2 --> E2[Plaintext]
    end
```

### How Public/Private Keys Work

```mermaid
sequenceDiagram
    participant AuthServer as Authorization Server<br/>(Has Private Key 🔒)
    participant ResourceServer as Resource Server<br/>(Has Public Key 🔓)
    participant Attacker as Attacker 👿

    Note over AuthServer: Generate Key Pair
    AuthServer->>AuthServer: Private Key 🔒 (Keep Secret!)
    AuthServer->>ResourceServer: Public Key 🔓 (Share Freely)

    Note over AuthServer: Sign JWT Token
    AuthServer->>AuthServer: Create JWT payload
    AuthServer->>AuthServer: Sign with Private Key 🔒
    AuthServer->>ResourceServer: Send JWT (signed)

    Note over ResourceServer: Verify JWT Token
    ResourceServer->>ResourceServer: Decode JWT
    ResourceServer->>ResourceServer: Verify signature with Public Key 🔓
    ResourceServer->>ResourceServer: ✓ Signature Valid - Token trusted!

    Note over Attacker: Try to forge JWT
    Attacker->>Attacker: Create fake JWT payload
    Attacker->>Attacker: Sign with own key ❌
    Attacker->>ResourceServer: Send fake JWT
    ResourceServer->>ResourceServer: Verify with Public Key 🔓
    ResourceServer->>ResourceServer: ✗ Signature Invalid - Rejected!
```

### Why Do We Need Public/Private Keys?

#### Problem: Token Tampering
Without signatures, anyone could modify JWT tokens:

```mermaid
graph LR
    A[Original JWT:<br/>role: USER] -->|Attacker modifies| B[Modified JWT:<br/>role: ADMIN]
    B --> C[Resource Server<br/>accepts fake token ❌]

    style C fill:#FFB6C6
```

#### Solution: Digital Signatures

```mermaid
graph TB
    subgraph "Authorization Server"
        A[Create JWT<br/>role: USER]
        B[Sign with Private Key 🔒]
        C[Add signature to JWT]
    end

    subgraph "Resource Server"
        D[Receive JWT]
        E[Verify signature with Public Key 🔓]
        F{Signature Valid?}
        G[Accept Token ✓]
        H[Reject Token ✗]
    end

    A --> B --> C
    C --> D --> E --> F
    F -->|Yes| G
    F -->|No| H

    style G fill:#90EE90
    style H fill:#FFB6C6
```

### Key Properties

1. **Private Key** 🔒
   - Kept secret by Authorization Server (Keycloak)
   - Used to **sign** JWT tokens
   - If compromised, attacker can issue valid tokens
   - Must be stored securely

2. **Public Key** 🔓
   - Shared publicly (via JWK Set endpoint)
   - Used to **verify** JWT signatures
   - Safe to distribute - can't be used to sign tokens
   - Downloaded by Resource Servers

### JWK Set (JSON Web Key Set)

```json
{
  "keys": [
    {
      "kid": "abc123",                    // Key ID
      "kty": "RSA",                       // Key Type: RSA
      "alg": "RS256",                     // Algorithm: RSA-SHA256
      "use": "sig",                       // Use: Signature
      "n": "0vx7agoebGcQSuu...",         // RSA Modulus (public key)
      "e": "AQAB"                         // RSA Exponent (public key)
    }
  ]
}
```

### Complete Token Flow with Keys

```mermaid
sequenceDiagram
    participant User
    participant Client as Client App
    participant AuthServer as Auth Server<br/>(Keycloak 🔒)
    participant ResourceServer as Resource Server<br/>(Your API 🔓)

    Note over AuthServer: Setup: Generate RSA key pair

    User->>Client: Login request
    Client->>AuthServer: POST /token (credentials)

    Note over AuthServer: Create & Sign JWT
    AuthServer->>AuthServer: 1. Create JWT payload:<br/>{sub: "user123", role: "USER"}
    AuthServer->>AuthServer: 2. Sign payload with Private Key 🔒
    AuthServer->>AuthServer: 3. Combine: header.payload.signature

    AuthServer-->>Client: Return JWT
    Client->>Client: Store JWT

    Note over ResourceServer: First request: Fetch public key

    Client->>ResourceServer: GET /api/data<br/>Authorization: Bearer JWT
    ResourceServer->>AuthServer: GET /certs (JWK Set)
    AuthServer-->>ResourceServer: Return Public Key 🔓
    ResourceServer->>ResourceServer: Cache Public Key 🔓

    Note over ResourceServer: Verify JWT signature
    ResourceServer->>ResourceServer: 1. Decode JWT
    ResourceServer->>ResourceServer: 2. Extract signature
    ResourceServer->>ResourceServer: 3. Verify signature using Public Key 🔓
    ResourceServer->>ResourceServer: 4. Signature Valid ✓
    ResourceServer->>ResourceServer: 5. Extract user info & roles

    ResourceServer-->>Client: 200 OK + Data
```

---

## 11. Spring Security Components

### 11.1 SecurityFilterChain

The chain of filters that process every HTTP request.

```mermaid
graph TB
    A[HTTP Request] --> B[SecurityContextPersistenceFilter]
    B --> C[UsernamePasswordAuthenticationFilter]
    C --> D[OAuth2AuthorizationRequestRedirectFilter]
    D --> E[OAuth2LoginAuthenticationFilter]
    E --> F[BearerTokenAuthenticationFilter]
    F --> G[ExceptionTranslationFilter]
    G --> H[FilterSecurityInterceptor]
    H --> I[Your Controller]
    I --> J[HTTP Response]

    G -.->|401/403| K[AuthenticationEntryPoint]
    K -.-> J
```

### 11.2 Authentication Flow in Spring Security

```mermaid
sequenceDiagram
    participant Request
    participant Filter as Authentication Filter
    participant Manager as Authentication Manager
    participant Provider as Authentication Provider
    participant UserDetails as UserDetailsService
    participant Context as Security Context

    Request->>Filter: HTTP Request with credentials
    Filter->>Filter: Extract credentials
    Filter->>Filter: Create Authentication object (unauthenticated)

    Filter->>Manager: authenticate(authObject)
    Manager->>Provider: authenticate(authObject)

    alt Username/Password Auth
        Provider->>UserDetails: loadUserByUsername(username)
        UserDetails->>UserDetails: Query database
        UserDetails-->>Provider: UserDetails object
        Provider->>Provider: Compare passwords (BCrypt)
    else JWT Auth
        Provider->>Provider: Decode JWT
        Provider->>Provider: Verify signature
        Provider->>Provider: Validate claims
    end

    alt Authentication Successful
        Provider-->>Manager: Authenticated Authentication object
        Manager-->>Filter: Authenticated Authentication object
        Filter->>Context: Store in SecurityContext
        Filter-->>Request: Continue to controller
    else Authentication Failed
        Provider-->>Manager: AuthenticationException
        Manager-->>Filter: AuthenticationException
        Filter-->>Request: 401 Unauthorized
    end
```

### 11.3 Authorization Flow

```mermaid
sequenceDiagram
    participant Request
    participant Filter as FilterSecurityInterceptor
    participant Manager as AccessDecisionManager
    participant Voter as AccessDecisionVoter
    participant Context as Security Context

    Request->>Filter: Authenticated request
    Filter->>Context: Get Authentication object
    Context-->>Filter: Authentication (user + roles)

    Filter->>Filter: Get required authorities for endpoint
    Note over Filter: e.g., @PreAuthorize("hasRole('ADMIN')")

    Filter->>Manager: decide(authentication, request, authorities)
    Manager->>Voter: vote(authentication, request, authorities)

    alt User has required role
        Voter-->>Manager: ACCESS_GRANTED
        Manager-->>Filter: Access approved
        Filter-->>Request: Continue to controller
    else User lacks required role
        Voter-->>Manager: ACCESS_DENIED
        Manager-->>Filter: AccessDeniedException
        Filter-->>Request: 403 Forbidden
    end
```

### 11.4 Key Annotations

#### @EnableWebSecurity
Enables Spring Security configuration.

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    // Security beans
}
```

#### @PreAuthorize / @PostAuthorize
Method-level security with SpEL expressions.

```java
@RestController
public class AdminController {

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/users/{id}")
    public void deleteUser(@PathVariable Long id) {
        userService.delete(id);
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/profile")
    public UserProfile getProfile() {
        return userService.getCurrentUserProfile();
    }

    @PreAuthorize("#username == authentication.principal.username")
    @GetMapping("/users/{username}/private")
    public PrivateData getPrivateData(@PathVariable String username) {
        return userService.getPrivateData(username);
    }
}
```

#### @Secured
Simple role-based security.

```java
@Secured("ROLE_ADMIN")
@DeleteMapping("/data")
public void deleteData() {
    // Only ADMIN can access
}
```

#### @RolesAllowed (JSR-250)
Standard Java security annotation.

```java
@RolesAllowed({"ADMIN", "MANAGER"})
@GetMapping("/reports")
public List<Report> getReports() {
    return reportService.getAllReports();
}
```

### 11.5 SecurityContext

Thread-local storage for authentication information.

```java
// Get current authentication
Authentication auth = SecurityContextHolder.getContext().getAuthentication();

// Get username
String username = auth.getName();

// Get authorities (roles)
Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();

// Check if user has role
boolean isAdmin = authorities.stream()
    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

// In controllers, use @AuthenticationPrincipal
@GetMapping("/me")
public UserInfo getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
    String email = jwt.getClaim("email");
    String name = jwt.getClaim("name");
    return new UserInfo(email, name);
}
```

---

## 12. Security Best Practices

### 12.1 Token Security

```mermaid
flowchart TD
    A[Token Security Checklist] --> B[Short expiration time]
    A --> C[Use HTTPS only]
    A --> D[Store securely]
    A --> E[Implement refresh tokens]
    A --> F[Validate all claims]

    B --> B1[Access: 15min - 1hr<br/>Refresh: days/weeks]
    C --> C1[Never send tokens over HTTP]
    D --> D1[httpOnly cookies OR<br/>localStorage with care]
    E --> E1[Don't re-authenticate often]
    F --> F1[exp, iss, aud, nbf]

    style A fill:#FFE5B4
```

### 12.2 Password Security

```java
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt with strength 12 (default is 10)
        return new BCryptPasswordEncoder(12);
    }
}

// Usage
String rawPassword = "user123";
String hashedPassword = passwordEncoder.encode(rawPassword);
// Result: $2a$12$xyz...abc (different each time!)

// Verification
boolean matches = passwordEncoder.matches(rawPassword, hashedPassword);
```

### 12.3 CORS Configuration

```java
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Specify allowed origins (NEVER use "*" in production with credentials)
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",
            "http://localhost:5173",
            "https://myapp.com"
        ));

        // Allowed methods
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS"
        ));

        // Allowed headers
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With"
        ));

        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);

        // Cache preflight response for 1 hour
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
```

### 12.4 CSRF Protection

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        // For traditional web apps (session-based)
        .csrf(csrf -> csrf
            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
        )

        // For REST APIs with JWT (stateless)
        .csrf(csrf -> csrf.disable()) // Safe because no session cookies

        .authorizeHttpRequests(auth -> auth
            .anyRequest().authenticated()
        );

    return http.build();
}
```

**When to disable CSRF:**
- REST APIs using JWT (no session cookies)
- Stateless authentication
- Mobile applications

**When to enable CSRF:**
- Traditional web applications with sessions
- Cookie-based authentication
- Server-side rendered pages

### 12.5 Common Security Headers

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .headers(headers -> headers
            // Prevent clickjacking
            .frameOptions(frame -> frame.deny())

            // Prevent MIME type sniffing
            .contentTypeOptions(contentType -> contentType.disable())

            // XSS protection
            .xssProtection(xss -> xss.disable()) // Modern browsers handle this

            // HTTPS enforcement
            .httpStrictTransportSecurity(hsts -> hsts
                .includeSubDomains(true)
                .maxAgeInSeconds(31536000) // 1 year
            )

            // Content Security Policy
            .contentSecurityPolicy(csp -> csp
                .policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline'")
            )
        );

    return http.build();
}
```

### 12.6 Rate Limiting

```java
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, RateLimiter> limiters = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String clientId = getClientIdentifier(request);
        RateLimiter limiter = limiters.computeIfAbsent(
            clientId,
            k -> RateLimiter.create(100.0) // 100 requests per second
        );

        if (limiter.tryAcquire()) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(429); // Too Many Requests
            response.getWriter().write("Rate limit exceeded");
        }
    }

    private String getClientIdentifier(HttpServletRequest request) {
        // Use IP address or authenticated user ID
        String username = SecurityContextHolder.getContext()
            .getAuthentication()
            .getName();

        return username != null ? username : request.getRemoteAddr();
    }
}
```

---

## 13. Troubleshooting Guide

### Common Issues and Solutions

#### Issue 1: "Invalid token" error

```mermaid
flowchart TD
    A[401 Invalid Token] --> B{Token format correct?}
    B -->|No| C[Check Bearer prefix:<br/>Authorization: Bearer TOKEN]
    B -->|Yes| D{Token expired?}
    D -->|Yes| E[Use refresh token<br/>to get new access token]
    D -->|No| F{Signature valid?}
    F -->|No| G[Check issuer-uri matches<br/>Check JWK Set accessible]
    F -->|Yes| H{Claims valid?}
    H -->|No| I[Check iss, aud, exp claims<br/>Enable validation in config]

    style A fill:#FFB6C6
```

**Debug Steps:**
```bash
# 1. Check token expiration
echo "YOUR_JWT_TOKEN" | cut -d'.' -f2 | base64 -d | jq

# 2. Verify issuer URI is accessible
curl http://localhost:8180/realms/sample-realm/.well-known/openid-configuration

# 3. Check JWK Set endpoint
curl http://localhost:8180/realms/sample-realm/protocol/openid-connect/certs

# 4. Enable debug logging
logging.level.org.springframework.security=DEBUG
logging.level.org.springframework.security.oauth2=TRACE
```

#### Issue 2: CORS errors

```
Access to fetch at 'http://localhost:8081/api/data' from origin
'http://localhost:3000' has been blocked by CORS policy
```

**Solution:**
```properties
# application.properties
oauth2.cors.enabled=true
oauth2.cors.allowed-origins=http://localhost:3000,http://localhost:5173
oauth2.cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS
oauth2.cors.allowed-headers=*
oauth2.cors.allow-credentials=true
```

#### Issue 3: "Access Denied" (403)

```mermaid
flowchart TD
    A[403 Forbidden] --> B{Authenticated?}
    B -->|No| C[Authentication failed first<br/>Fix authentication issue]
    B -->|Yes| D{Has required role?}
    D -->|No| E[Check user roles in token<br/>Check role mapping in Keycloak]
    D -->|Yes| F{Role prefix correct?}
    F -->|No| G[Spring Security expects ROLE_<br/>prefix by default<br/>Configure converter]
    F -->|Yes| H{Endpoint permissions correct?}
    H -->|No| I[Review @PreAuthorize<br/>Review SecurityFilterChain config]

    style A fill:#FFB6C6
```

**Debug:**
```java
@GetMapping("/debug/token")
public Map<String, Object> debugToken(@AuthenticationPrincipal Jwt jwt) {
    Map<String, Object> debug = new HashMap<>();
    debug.put("claims", jwt.getClaims());
    debug.put("subject", jwt.getSubject());
    debug.put("authorities", SecurityContextHolder.getContext()
        .getAuthentication()
        .getAuthorities());
    return debug;
}
```

#### Issue 4: Keycloak connection refused

```
Connection refused: connect to http://localhost:8180
```

**Checklist:**
1. Keycloak is running: `docker ps | grep keycloak`
2. Port is correct: `8180` not `8080`
3. Realm name matches configuration
4. Network connectivity: `curl http://localhost:8180`

#### Issue 5: Custom login not working

```
Custom login is not enabled. Set oauth2.custom-login-enabled=true
```

**Solution:**
```properties
# Enable custom login
oauth2.custom-login-enabled=true

# Make sure Direct Access Grants is enabled in Keycloak:
# Clients -> your-client -> Settings -> Direct access grants: ON
```

---

## Practical Examples

### Example 1: Protect Endpoints by Role

```java
@RestController
@RequestMapping("/api")
public class SecureController {

    // Public endpoint - no authentication required
    @GetMapping("/public/health")
    public String health() {
        return "OK";
    }

    // Authenticated endpoint - any logged-in user
    @GetMapping("/user/profile")
    public UserProfile getProfile(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        return userService.getProfile(userId);
    }

    // Admin only endpoint
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/users/{id}")
    public void deleteUser(@PathVariable Long id) {
        userService.delete(id);
    }

    // Multiple roles allowed
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping("/reports")
    public List<Report> getReports() {
        return reportService.getAll();
    }

    // Custom permission check
    @PreAuthorize("@securityService.canAccessResource(#resourceId, authentication)")
    @GetMapping("/resources/{resourceId}")
    public Resource getResource(@PathVariable Long resourceId) {
        return resourceService.get(resourceId);
    }
}

@Service
public class SecurityService {
    public boolean canAccessResource(Long resourceId, Authentication auth) {
        // Custom business logic
        Resource resource = resourceService.get(resourceId);
        return resource.getOwnerId().equals(auth.getName()) ||
               auth.getAuthorities().stream()
                   .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
```

### Example 2: Extract User Info from JWT

```java
@RestController
public class UserController {

    @GetMapping("/me")
    public UserInfo getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        return UserInfo.builder()
            .id(jwt.getSubject())
            .username(jwt.getClaim("preferred_username"))
            .email(jwt.getClaim("email"))
            .name(jwt.getClaim("name"))
            .roles(jwt.getClaim("roles"))
            .build();
    }

    @GetMapping("/me/roles")
    public Set<String> getRoles() {
        return SecurityContextHolder.getContext()
            .getAuthentication()
            .getAuthorities()
            .stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toSet());
    }
}
```

### Example 3: Frontend Integration (React)

```javascript
// AuthService.js
class AuthService {
  // Standard OAuth2 flow
  async loginWithOAuth2() {
    // Get authorization URL
    const response = await fetch('http://localhost:8081/oauth2/authorize');
    const data = await response.json();

    // Redirect to Keycloak login page
    window.location.href = data.data.authorizationUrl;
  }

  // Handle callback
  async handleCallback(code, state) {
    const response = await fetch('http://localhost:8081/oauth2/token', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ code, redirectUri: window.location.origin })
    });

    const data = await response.json();

    // Store tokens
    localStorage.setItem('access_token', data.data.access_token);
    localStorage.setItem('refresh_token', data.data.refresh_token);

    return data.data;
  }

  // Custom login (if enabled)
  async loginWithPassword(username, password) {
    const response = await fetch('http://localhost:8081/oauth2/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    });

    if (!response.ok) {
      throw new Error('Login failed');
    }

    const data = await response.json();
    localStorage.setItem('access_token', data.data.access_token);
    localStorage.setItem('refresh_token', data.data.refresh_token);

    return data.data;
  }

  // Make authenticated request
  async fetchProtectedData() {
    const token = localStorage.getItem('access_token');

    const response = await fetch('http://localhost:8081/api/data', {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });

    if (response.status === 401) {
      // Token expired, try refresh
      await this.refreshToken();
      return this.fetchProtectedData(); // Retry
    }

    return response.json();
  }

  // Refresh token
  async refreshToken() {
    const refreshToken = localStorage.getItem('refresh_token');

    const response = await fetch('http://localhost:8081/oauth2/refresh', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken })
    });

    if (!response.ok) {
      // Refresh failed, need to login again
      this.logout();
      throw new Error('Session expired');
    }

    const data = await response.json();
    localStorage.setItem('access_token', data.data.access_token);
    localStorage.setItem('refresh_token', data.data.refresh_token);
  }

  // Logout
  logout() {
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    window.location.href = '/login';
  }
}

export default new AuthService();
```

---

## Summary

### Key Takeaways

1. **Authentication** = Who you are (Login)
2. **Authorization** = What you can do (Permissions)
3. **OAuth2** = Authorization framework for API access
4. **OIDC** = OAuth2 + Authentication (adds ID Token)
5. **JWT** = Self-contained tokens with user info
6. **Resource Server** = API that validates tokens
7. **Authorization Server** = Issues tokens (Keycloak)
8. **Public/Private Keys** = Secure token signing & verification

### When to Use What

| Scenario | Recommended Flow |
|----------|------------------|
| Web application with backend | Authorization Code Flow |
| Mobile application | Authorization Code Flow + PKCE |
| Custom login page (own UI) | Resource Owner Password Credentials |
| Microservices (no user) | Client Credentials Flow |
| Refresh expired token | Refresh Token Flow |

### Security Checklist

- [ ] Use HTTPS in production
- [ ] Short access token expiration (15min - 1hr)
- [ ] Implement refresh tokens
- [ ] Validate all JWT claims (exp, iss, aud)
- [ ] Use strong password hashing (BCrypt)
- [ ] Enable CORS properly (no wildcard with credentials)
- [ ] Implement rate limiting
- [ ] Add security headers
- [ ] Never log sensitive data (tokens, passwords)
- [ ] Keep dependencies updated
- [ ] Use environment variables for secrets
- [ ] Enable security logging

---

## Further Reading

### Official Documentation
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/index.html)
- [OAuth 2.0 RFC 6749](https://datatracker.ietf.org/doc/html/rfc6749)
- [OpenID Connect Specification](https://openid.net/specs/openid-connect-core-1_0.html)
- [JWT RFC 7519](https://datatracker.ietf.org/doc/html/rfc7519)
- [Keycloak Documentation](https://www.keycloak.org/documentation)

### Video Tutorials
- OAuth 2.0 and OpenID Connect (by Okta)
- Spring Security - Complete Course (by Amigoscode)
- JWT Tutorial (by Tech With Tim)

### Tools
- [jwt.io](https://jwt.io) - Decode and verify JWT tokens
- [OAuth 2.0 Playground](https://www.oauth.com/playground/) - Test OAuth flows
- Postman - API testing with OAuth2 support

---

**Built with ❤️ for developers learning Spring Security**

*Last Updated: 2026-02-05*
