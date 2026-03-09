# Quick Start Guide

Get started with the OAuth2/OIDC Spring Boot Starter in 5 minutes!

## Step 1: Install the Starter (Already Done)

The starter is already installed in your local Maven repository:
```
C:\Users\Dell\.m2\repository\com\shashankpk\oauth2-oidc-spring-boot-starter\1.0.0\
```

## Step 2: Create a New Spring Boot Application

```bash
# Create a new Spring Boot project
spring init --dependencies=web my-secure-app
cd my-secure-app
```

Or use Spring Initializr: https://start.spring.io/

## Step 3: Add the OAuth2 Starter Dependency

Edit your `pom.xml` and add:

```xml
<dependency>
    <groupId>com.shashankpk</groupId>
    <artifactId>oauth2-oidc-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Step 4: Configure OAuth2 Provider

Create `src/main/resources/application.properties`:

### For Keycloak

```properties
# Server
server.port=8080

# OAuth2 Configuration
oauth2.enabled=true
oauth2.provider=KEYCLOAK
oauth2.client-id=my-app
oauth2.client-secret=your-client-secret-from-keycloak
oauth2.issuer-uri=http://localhost:8180/realms/myrealm

# Enable custom login page (optional)
oauth2.custom-login-enabled=true

# CORS for frontend
oauth2.cors.enabled=true
oauth2.cors.allowed-origins=http://localhost:3000
oauth2.cors.allow-credentials=true
```

### For Okta

```properties
oauth2.enabled=true
oauth2.provider=OKTA
oauth2.client-id=0oa2abc123xyz
oauth2.client-secret=your-okta-secret
oauth2.issuer-uri=https://dev-12345.okta.com/oauth2/default
oauth2.custom-login-enabled=true
```

### For Google

```properties
oauth2.enabled=true
oauth2.provider=GOOGLE
oauth2.client-id=123456.apps.googleusercontent.com
oauth2.client-secret=your-google-secret
oauth2.issuer-uri=https://accounts.google.com
```

## Step 5: Create a Protected REST Controller (Optional)

`src/main/java/com/example/demo/controller/ApiController.java`:

```java
package com.example.demo.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    @GetMapping("/hello")
    public Map<String, String> hello(@AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaim("preferred_username");
        return Map.of(
            "message", "Hello, " + username + "!",
            "user", username
        );
    }

    @GetMapping("/profile")
    public Map<String, Object> profile(@AuthenticationPrincipal Jwt jwt) {
        return jwt.getClaims();
    }
}
```

## Step 6: Run Your Application

```bash
mvn spring-boot:run
```

You'll see the OAuth2 starter initialization:
```
================================================================================
OAuth2/OIDC Spring Boot Starter - Auto-Configuration
================================================================================
Provider: Keycloak
Client ID: my-app
Issuer URI: http://localhost:8180/realms/myrealm
Custom Login Enabled: true
CORS Enabled: true
================================================================================
```

## Step 7: Test the Authentication

The starter automatically provides these endpoints:

### 1. Health Check
```bash
curl http://localhost:8080/oauth2/health
```

Response:
```json
{
  "success": true,
  "data": {
    "status": "UP",
    "service": "OAuth2/OIDC Starter"
  }
}
```

### 2. Custom Login (Username/Password)
```bash
curl -X POST http://localhost:8080/oauth2/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password"
  }'
```

Response:
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
    "token_type": "Bearer",
    "expires_in": 300,
    "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "scope": "openid profile email",
    "userInfo": {
      "sub": "123e4567-e89b-12d3-a456-426614174000",
      "username": "testuser",
      "email": "test@example.com",
      "name": "Test User"
    }
  }
}
```

### 3. Get Authorization URL (for Standard OAuth2 Flow)
```bash
curl "http://localhost:8080/oauth2/authorize?redirect_uri=http://localhost:3000/callback"
```

Response:
```json
{
  "success": true,
  "data": {
    "authorizationUrl": "http://localhost:8180/realms/myrealm/protocol/openid-connect/auth?response_type=code&client_id=my-app&redirect_uri=http://localhost:3000/callback&scope=openid%20profile%20email&state=abc123",
    "state": "abc123"
  }
}
```

### 4. Get User Info (with access token)
```bash
curl http://localhost:8080/oauth2/userinfo \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### 5. Validate Token
```bash
curl http://localhost:8080/oauth2/validate \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### 6. Refresh Token
```bash
curl -X POST http://localhost:8080/oauth2/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "YOUR_REFRESH_TOKEN"
  }'
```

### 7. Access Protected Endpoints
```bash
curl http://localhost:8080/api/hello \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

## Frontend Integration

### React Example

```javascript
// Login
const login = async (username, password) => {
  const response = await fetch('http://localhost:8080/oauth2/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password })
  });

  const data = await response.json();

  if (data.success) {
    localStorage.setItem('access_token', data.data.access_token);
    localStorage.setItem('refresh_token', data.data.refresh_token);
    return data.data.userInfo;
  }

  throw new Error(data.message);
};

// Make authenticated request
const fetchProtectedData = async () => {
  const token = localStorage.getItem('access_token');

  const response = await fetch('http://localhost:8080/api/hello', {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });

  return response.json();
};
```

### React Native Example

```javascript
import * as SecureStore from 'expo-secure-store';

// Login
const login = async (username, password) => {
  const response = await fetch('http://your-ip:8080/oauth2/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password })
  });

  const data = await response.json();

  if (data.success) {
    await SecureStore.setItemAsync('access_token', data.data.access_token);
    await SecureStore.setItemAsync('refresh_token', data.data.refresh_token);
    return data.data.userInfo;
  }

  throw new Error(data.message);
};
```

## Keycloak Setup (5 minutes)

If you need to set up Keycloak for testing:

### 1. Run Keycloak with Docker

```bash
docker run -d \
  --name keycloak \
  -p 8180:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:latest \
  start-dev
```

### 2. Access Keycloak Admin Console

Open: http://localhost:8180
Login: admin / admin

### 3. Create Realm

1. Click "Create Realm"
2. Name: `myrealm`
3. Click "Create"

### 4. Create Client

1. Clients → Create Client
2. Client ID: `my-app`
3. Next
4. Enable "Client authentication"
5. Enable "Direct access grants" (for custom login)
6. Next
7. Valid redirect URIs: `http://localhost:3000/*`
8. Save

### 5. Get Client Secret

1. Go to Clients → my-app → Credentials
2. Copy the "Client secret"
3. Use it in your `application.properties`

### 6. Create Test User

1. Users → Add User
2. Username: `testuser`
3. Create
4. Go to Credentials tab
5. Set Password: `password`
6. Disable "Temporary"
7. Save

### 7. Test

```bash
curl -X POST http://localhost:8080/oauth2/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password"}'
```

## What's Next?

1. **Customize Security**: Add custom security rules in your application
2. **Add Protected Endpoints**: Create REST APIs that require authentication
3. **Frontend Integration**: Connect React or React Native frontend
4. **Production Setup**: Configure production OAuth2 provider (Okta, Auth0, etc.)

## Common Issues

### Issue: "JWK Set URI not configured"
**Solution**: Verify your `oauth2.issuer-uri` is correct and accessible.

### Issue: "Custom login is not enabled"
**Solution**: Set `oauth2.custom-login-enabled=true` in properties.

### Issue: CORS errors
**Solution**: Add your frontend URL to `oauth2.cors.allowed-origins`.

### Issue: "Direct Access Grants must be enabled"
**Solution**: In Keycloak, go to Clients → your-client → Settings → Enable "Direct Access Grants Enabled".

## Documentation

- **README.md**: Complete documentation
- **CONFIGURATION_EXAMPLES.md**: Detailed provider configurations
- **USAGE_EXAMPLE.md**: Full integration examples with code

## Support

For issues or questions, create an issue in the project repository.

---

**You're all set!** Your Spring Boot application now has enterprise-grade OAuth2/OIDC authentication in less than 5 minutes.
