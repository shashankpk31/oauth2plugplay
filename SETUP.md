# OAuth2 OIDC Spring Boot Starter - Setup Guide

Complete setup guide for backend and frontend with the OAuth2 OIDC Spring Boot Starter.

## Prerequisites

- Java 17+
- Maven 3.6+
- Docker (for Keycloak)
- Node.js 16+ (for frontend)

---

## Keycloak Setup

### Run Keycloak

```bash
docker run -d --name keycloak -p 8180:8080 \
  -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:latest start-dev
```

Access: http://localhost:8180 (admin/admin)

### Configure Keycloak

1. **Create Realm**: sample-realm
2. **Create Client**: sample-app (Client authentication: ON, Direct access grants: ON)
3. **Get Client Secret**: Credentials tab
4. **Create User**: testuser / password

---

## Backend Setup

### Add Dependency

```xml
<dependency>
    <groupId>com.shashankpk</groupId>
    <artifactId>oauth2-oidc-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Configure Properties

```properties
oauth2.enabled=true
oauth2.provider=KEYCLOAK
oauth2.client-id=sample-app
oauth2.client-secret=YOUR_SECRET
oauth2.issuer-uri=http://localhost:8180/realms/sample-realm
oauth2.custom-login-enabled=true
oauth2.cors.allowed-origins=http://localhost:5173
```

### Create Controller

```java
@RestController
@RequestMapping("/api")
public class UserController {
    @GetMapping("/profile")
    public Map<String, Object> getProfile(@AuthenticationPrincipal Jwt jwt) {
        return Map.of(
            "username", jwt.getClaim("preferred_username"),
            "email", jwt.getClaim("email")
        );
    }
}
```

### Run

```bash
mvn spring-boot:run
```

---

## Frontend Setup

### Create React App

```bash
npm create vite@latest my-frontend -- --template react
cd my-frontend
npm install react-router-dom
```

### Auth Service

Create `src/services/authService.js`:

```javascript
const API = 'http://localhost:8081';

class AuthService {
  async login(username, password) {
    const res = await fetch(`${API}/oauth2/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    });
    const { data } = await res.json();
    localStorage.setItem('token', data.access_token);
  }

  logout() {
    localStorage.removeItem('token');
  }

  getToken() {
    return localStorage.getItem('token');
  }
}

export default new AuthService();
```

### Run

```bash
npm run dev
```

---

## Provider Configuration

### Keycloak
```properties
oauth2.provider=KEYCLOAK
oauth2.issuer-uri=http://localhost:8180/realms/your-realm
```

### Okta
```properties
oauth2.provider=OKTA
oauth2.issuer-uri=https://your-domain.okta.com/oauth2/default
```

### Google
```properties
oauth2.provider=GOOGLE
oauth2.custom-login-enabled=false
```

---

## Testing

```bash
# Login
curl -X POST http://localhost:8081/oauth2/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password"}'

# Get profile
curl http://localhost:8081/api/profile \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

## Troubleshooting

- **Keycloak not starting**: `docker restart keycloak`
- **Invalid credentials**: Check client secret
- **CORS errors**: Check allowed origins
- **Token invalid**: Enable debug logging

---

See also:
- [ARCHITECTURE.md](ARCHITECTURE.md) - Technical documentation
- [SECURITY.md](SECURITY.md) - Security guide
- [MANUAL_AUTH_GUIDE.md](MANUAL_AUTH_GUIDE.md) - Custom auth implementation
