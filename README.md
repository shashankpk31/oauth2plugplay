# OAuth2 OIDC Spring Boot Starter

A plug-and-play Spring Boot starter dependency that provides **enterprise-level authentication and authorization** for any Spring Boot application with zero code required.

## Overview

This repository contains a production-ready OAuth2/OIDC Spring Boot Starter library that can be added as a dependency to any Spring Boot application to instantly enable secure authentication and authorization using industry-standard OAuth2 and OpenID Connect protocols.

## Key Features

- **Plug & Play** - Add one dependency, configure properties, and you're done
- **Multi-Provider Support** - Works with Keycloak, Okta, Google, Meta, GitHub, Microsoft, and custom OAuth2 providers
- **Environment Variable Toggle** - Enable/disable authentication with a single property
- **Zero Code Required** - Auto-configures all security components
- **Production Ready** - Built with Spring Boot 3.3.12 and Java 17
- **Enterprise Level** - JWT validation, token refresh, role-based access control
- **REST API** - Complete authentication API endpoints
- **Frontend Ready** - Works seamlessly with React, Angular, Vue, and mobile apps

## Quick Start

### 1. Add Dependency

Add to your Spring Boot project's `pom.xml`:

```xml
<dependency>
    <groupId>com.shashankpk</groupId>
    <artifactId>oauth2-oidc-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. Configure Properties

Add to your `application.properties`:

```properties
# Enable/Disable Authentication (Environment Variable)
oauth2.enabled=true

# Provider Configuration
oauth2.provider=KEYCLOAK
oauth2.client-id=your-client-id
oauth2.client-secret=your-client-secret
oauth2.issuer-uri=http://localhost:8180/realms/your-realm

# Optional: Enable custom login page
oauth2.custom-login-enabled=true
```

### 3. Run Your Application

```bash
mvn spring-boot:run
```

**That's it!** Your Spring Boot application now has:
- JWT-based authentication
- Role-based authorization
- Token refresh mechanism
- Complete authentication REST API
- CORS support
- Security best practices

## Environment Variable Toggle

The library can be **enabled or disabled** using a single environment variable:

### Enable Authentication (Default)
```properties
oauth2.enabled=true
```
When enabled, the library auto-configures all OAuth2/OIDC security components.

### Disable Authentication
```properties
oauth2.enabled=false
```
When disabled, the library is completely bypassed and your application runs without authentication.

**Use Cases for Disabling:**
- Local development without authentication
- Testing environments
- Migrating from legacy authentication
- Implementing custom authentication (see [Manual Authentication Guide](MANUAL_AUTH_GUIDE.md))

## Supported OAuth2 Providers

| Provider | Status | Custom Login | Documentation |
|----------|--------|--------------|---------------|
| Keycloak | ✅ Fully Supported | ✅ Yes | [See Setup](SETUP.md#keycloak) |
| Okta | ✅ Fully Supported | ✅ Yes | [See Setup](SETUP.md#okta) |
| Google | ✅ Fully Supported | ❌ No | [See Setup](SETUP.md#google) |
| Meta (Facebook) | ✅ Fully Supported | ❌ No | [See Setup](SETUP.md#meta) |
| GitHub | ✅ Fully Supported | ❌ No | [See Setup](SETUP.md#github) |
| Microsoft | ✅ Fully Supported | ⚠️ Limited | [See Setup](SETUP.md#microsoft) |
| Custom | ✅ Fully Supported | ⚠️ Depends | [See Setup](SETUP.md#custom) |

## Available REST API Endpoints

Once the library is added, these endpoints are automatically available:

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/oauth2/authorize` | GET | Get authorization URL for OAuth2 flow |
| `/oauth2/login` | POST | Login with username/password (if custom login enabled) |
| `/oauth2/token` | POST | Exchange authorization code for tokens |
| `/oauth2/refresh` | POST | Refresh access token |
| `/oauth2/userinfo` | GET | Get authenticated user information |
| `/oauth2/validate` | GET | Validate JWT token |
| `/oauth2/logout` | POST | Logout user |
| `/oauth2/health` | GET | Health check |

## Project Structure

```
Keycloak_Poc/
├── oidcplugplay/              # The plug-and-play library (Spring Boot Starter)
│   ├── src/main/java/
│   │   └── com/shashankpk/oauth2/starter/
│   │       ├── config/        # Auto-configuration classes
│   │       ├── controller/    # REST API endpoints
│   │       ├── dto/          # Data transfer objects
│   │       ├── exception/    # Exception handling
│   │       ├── properties/   # Configuration properties
│   │       ├── provider/     # OAuth2 provider definitions
│   │       ├── security/     # Security configuration
│   │       └── service/      # Business logic
│   └── pom.xml
├── sample-backend/           # Example Spring Boot application using the library
├── sample-frontend/          # Example React application
├── README.md                 # This file
├── SETUP.md                  # Complete setup guide
├── ARCHITECTURE.md           # Technical architecture documentation
├── SECURITY.md              # Security best practices
└── MANUAL_AUTH_GUIDE.md     # Manual authentication when oauth2.enabled=false
```

## Example Usage

### Backend (Spring Boot)

```java
@RestController
@RequestMapping("/api")
public class UserController {

    // Authenticated endpoint - requires valid JWT
    @GetMapping("/profile")
    public Map<String, Object> getProfile(@AuthenticationPrincipal Jwt jwt) {
        return Map.of(
            "id", jwt.getSubject(),
            "username", jwt.getClaim("preferred_username"),
            "email", jwt.getClaim("email"),
            "roles", jwt.getClaim("roles")
        );
    }

    // Admin-only endpoint
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/users/{id}")
    public void deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
    }
}
```

### Frontend (React)

```javascript
// Login
const response = await fetch('http://localhost:8081/oauth2/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username: 'user', password: 'pass' })
});

const { data } = await response.json();
localStorage.setItem('access_token', data.access_token);

// Make authenticated request
const profileResponse = await fetch('http://localhost:8081/api/profile', {
  headers: {
    'Authorization': `Bearer ${localStorage.getItem('access_token')}`
  }
});
```

## Documentation

| Document | Description |
|----------|-------------|
| [SETUP.md](SETUP.md) | Complete setup guide for backend and frontend |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Technical architecture and design |
| [SECURITY.md](SECURITY.md) | Security considerations and best practices |
| [MANUAL_AUTH_GUIDE.md](MANUAL_AUTH_GUIDE.md) | How to implement authentication manually when disabled |

## Building from Source

```bash
cd oidcplugplay
mvn clean install
```

This will:
1. Compile the library
2. Run tests
3. Install to local Maven repository (`~/.m2/repository`)

## Requirements

- Java 17 or higher
- Spring Boot 3.3.12
- Maven 3.6+

## Use Cases

1. **Microservices** - Add consistent authentication across all services
2. **REST APIs** - Secure your API endpoints with JWT
3. **Web Applications** - Full-stack authentication (React/Angular/Vue + Spring Boot)
4. **Mobile Apps** - Mobile-friendly token-based authentication
5. **Enterprise Applications** - Multi-tenant support with provider switching

## Configuration Properties

### Core Properties
```properties
# Enable/disable OAuth2 (Environment Variable)
oauth2.enabled=true

# Provider selection
oauth2.provider=KEYCLOAK

# OAuth2 credentials
oauth2.client-id=your-client-id
oauth2.client-secret=your-client-secret
oauth2.issuer-uri=http://localhost:8180/realms/your-realm

# Scopes
oauth2.scopes=openid,profile,email
```

### Advanced Properties
```properties
# Custom login page support
oauth2.custom-login-enabled=true

# JWT validation
oauth2.jwt.validate-issuer=true
oauth2.jwt.validate-audience=false
oauth2.jwt.access-token-validity=3600
oauth2.jwt.refresh-token-validity=86400

# CORS configuration
oauth2.cors.enabled=true
oauth2.cors.allowed-origins=http://localhost:3000,http://localhost:5173
oauth2.cors.allow-credentials=true
```

See [SETUP.md](SETUP.md) for complete configuration options.

## License

MIT License

## Author

Shashank PK

## Contributing

Contributions are welcome! Please follow standard Spring Boot conventions.

## Support

For issues and questions:
1. Check the [SETUP.md](SETUP.md) guide
2. Review [SECURITY.md](SECURITY.md) for security-related issues
3. See [MANUAL_AUTH_GUIDE.md](MANUAL_AUTH_GUIDE.md) for custom implementations
4. Enable DEBUG logging: `logging.level.com.shashankpk.oauth2.starter=DEBUG`

---

**Built with Java 17, Spring Boot 3.3.12, and Spring Security OAuth2**
