# OAuth2/OIDC Spring Boot Starter

A flexible, plug-and-play Spring Boot starter for OAuth2/OIDC authentication that supports multiple providers including Keycloak, Okta, Google, Meta (Facebook), GitHub, Microsoft, and custom OAuth2 providers.

## Features

- **Multi-Provider Support**: Keycloak, Okta, Google, Meta, GitHub, Microsoft, and custom OAuth2 providers
- **Flexible Authentication**:
  - Standard OAuth2 Authorization Code Flow (redirect to provider's login page)
  - Custom Login Page Support (Resource Owner Password Credentials Grant)
- **Auto-Configuration**: Works like Spring Data JPA - just add dependency and configure properties
- **JWT Token Validation**: Built-in JWT validation and user info extraction
- **Token Refresh**: Automatic token refresh support
- **CORS Support**: Configurable CORS for React/React Native frontends
- **Comprehensive API**: REST endpoints for all authentication operations
- **Production Ready**: Exception handling, logging, validation

## Requirements

- Java 17 or higher
- Spring Boot 3.3.12
- Maven 3.6+

## Installation

### 1. Build and Install to Local Maven Repository

```bash
cd oidcplugplay
mvn clean install
```

This installs the starter to your local `.m2` repository:
```
~/.m2/repository/com/shashankpk/oauth2-oidc-spring-boot-starter/1.0.0/
```

### 2. Add Dependency to Your Spring Boot Project

Add this to your project's `pom.xml`:

```xml
<dependency>
    <groupId>com.shashankpk</groupId>
    <artifactId>oauth2-oidc-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 3. Configure Properties

Add OAuth2 configuration to your `application.properties` or `application.yml`:

## Configuration Examples

### Keycloak Configuration

```properties
# Basic Keycloak Configuration
oauth2.enabled=true
oauth2.provider=KEYCLOAK
oauth2.client-id=your-client-id
oauth2.client-secret=your-client-secret
oauth2.issuer-uri=http://localhost:8080/realms/your-realm

# Optional: Enable custom login page
oauth2.custom-login-enabled=true

# Optional: Custom scopes
oauth2.scopes=openid,profile,email,roles

# Optional: CORS configuration
oauth2.cors.enabled=true
oauth2.cors.allowed-origins=http://localhost:3000,http://localhost:19006
oauth2.cors.allow-credentials=true
```

### Okta Configuration

```properties
oauth2.enabled=true
oauth2.provider=OKTA
oauth2.client-id=your-okta-client-id
oauth2.client-secret=your-okta-client-secret
oauth2.issuer-uri=https://your-domain.okta.com/oauth2/default
oauth2.custom-login-enabled=true
```

### Google Configuration

```properties
oauth2.enabled=true
oauth2.provider=GOOGLE
oauth2.client-id=your-google-client-id.apps.googleusercontent.com
oauth2.client-secret=your-google-client-secret
oauth2.issuer-uri=https://accounts.google.com
oauth2.scopes=openid,profile,email
```

### Meta (Facebook) Configuration

```properties
oauth2.enabled=true
oauth2.provider=META
oauth2.client-id=your-facebook-app-id
oauth2.client-secret=your-facebook-app-secret
oauth2.issuer-uri=https://www.facebook.com
oauth2.scopes=public_profile,email
```

### Custom OAuth2 Provider

```properties
oauth2.enabled=true
oauth2.provider=CUSTOM
oauth2.client-id=your-client-id
oauth2.client-secret=your-client-secret
oauth2.issuer-uri=https://your-oauth-server.com
oauth2.authorization-uri=https://your-oauth-server.com/oauth/authorize
oauth2.token-uri=https://your-oauth-server.com/oauth/token
oauth2.user-info-uri=https://your-oauth-server.com/oauth/userinfo
oauth2.jwk-set-uri=https://your-oauth-server.com/oauth/jwks
```

## API Endpoints

The starter automatically exposes these REST endpoints:

### 1. Get Authorization URL (Standard OAuth2 Flow)

```http
GET /oauth2/authorize?redirect_uri=http://localhost:3000/callback&state=random-state

Response:
{
  "success": true,
  "message": "Success",
  "data": {
    "authorizationUrl": "http://keycloak:8080/realms/myrealm/protocol/openid-connect/auth?...",
    "state": "random-state"
  },
  "timestamp": "2024-02-05T10:30:00"
}
```

**Usage**: Redirect user to `authorizationUrl` in browser

### 2. Exchange Authorization Code for Token

```http
POST /oauth2/token?code=AUTH_CODE&redirect_uri=http://localhost:3000/callback

Response:
{
  "success": true,
  "message": "Authentication successful",
  "data": {
    "access_token": "eyJhbGc...",
    "token_type": "Bearer",
    "expires_in": 3600,
    "refresh_token": "eyJhbGc...",
    "scope": "openid profile email",
    "userInfo": {
      "sub": "user-id",
      "username": "john.doe",
      "email": "john@example.com",
      "name": "John Doe"
    }
  }
}
```

### 3. Custom Login (Username/Password)

```http
POST /oauth2/login
Content-Type: application/json

{
  "username": "john.doe",
  "password": "password123"
}

Response: Same as token exchange
```

**Note**: Requires `oauth2.custom-login-enabled=true` and provider must support Resource Owner Password Credentials Grant.

For Keycloak: Enable "Direct Access Grants" in client settings.

### 4. Refresh Token

```http
POST /oauth2/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGc..."
}

Response: New access token
```

### 5. Get User Info

```http
GET /oauth2/userinfo
Authorization: Bearer eyJhbGc...

Response:
{
  "success": true,
  "data": {
    "sub": "user-id",
    "username": "john.doe",
    "email": "john@example.com",
    "name": "John Doe",
    "emailVerified": true
  }
}
```

### 6. Validate Token

```http
GET /oauth2/validate
Authorization: Bearer eyJhbGc...

Response:
{
  "success": true,
  "message": "Token is valid",
  "data": {
    "sub": "user-id",
    "iss": "http://keycloak:8080/realms/myrealm",
    "exp": 1707136200,
    "iat": 1707132600,
    ...
  }
}
```

### 7. Logout

```http
POST /oauth2/logout
Authorization: Bearer eyJhbGc...

Response:
{
  "success": true,
  "message": "Logged out successfully"
}
```

### 8. Health Check

```http
GET /oauth2/health

Response:
{
  "success": true,
  "data": {
    "status": "UP",
    "service": "OAuth2/OIDC Starter"
  }
}
```

## Integration Examples

### React Frontend Example

```javascript
// 1. Redirect to OAuth2 provider
const initiateLogin = async () => {
  const response = await fetch(
    'http://localhost:8080/oauth2/authorize?redirect_uri=http://localhost:3000/callback'
  );
  const data = await response.json();

  // Save state for validation
  localStorage.setItem('oauth_state', data.data.state);

  // Redirect to provider
  window.location.href = data.data.authorizationUrl;
};

// 2. Handle callback (in your callback component)
const handleCallback = async () => {
  const urlParams = new URLSearchParams(window.location.search);
  const code = urlParams.get('code');
  const state = urlParams.get('state');

  // Validate state
  if (state !== localStorage.getItem('oauth_state')) {
    console.error('Invalid state');
    return;
  }

  // Exchange code for token
  const response = await fetch(
    `http://localhost:8080/oauth2/token?code=${code}&redirect_uri=http://localhost:3000/callback`,
    { method: 'POST' }
  );

  const data = await response.json();

  // Store tokens
  localStorage.setItem('access_token', data.data.access_token);
  localStorage.setItem('refresh_token', data.data.refresh_token);

  // Store user info
  localStorage.setItem('user', JSON.stringify(data.data.userInfo));

  // Redirect to home
  window.location.href = '/';
};

// 3. Custom login (if enabled)
const customLogin = async (username, password) => {
  const response = await fetch('http://localhost:8080/oauth2/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password })
  });

  const data = await response.json();

  if (data.success) {
    localStorage.setItem('access_token', data.data.access_token);
    localStorage.setItem('refresh_token', data.data.refresh_token);
    localStorage.setItem('user', JSON.stringify(data.data.userInfo));
  }
};

// 4. Make authenticated requests
const makeAuthenticatedRequest = async (url) => {
  const token = localStorage.getItem('access_token');

  const response = await fetch(url, {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });

  return response.json();
};

// 5. Refresh token
const refreshAccessToken = async () => {
  const refreshToken = localStorage.getItem('refresh_token');

  const response = await fetch('http://localhost:8080/oauth2/refresh', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken })
  });

  const data = await response.json();
  localStorage.setItem('access_token', data.data.access_token);
};
```

### React Native Example

```javascript
import * as WebBrowser from 'expo-web-browser';
import * as Linking from 'expo-linking';

// Custom login for mobile
const mobileLogin = async (username, password) => {
  try {
    const response = await fetch('http://your-backend:8080/oauth2/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    });

    const data = await response.json();

    if (data.success) {
      // Store tokens securely (use SecureStore)
      await SecureStore.setItemAsync('access_token', data.data.access_token);
      await SecureStore.setItemAsync('refresh_token', data.data.refresh_token);
    }
  } catch (error) {
    console.error('Login failed:', error);
  }
};

// OAuth2 flow (opens browser)
const browserLogin = async () => {
  const response = await fetch(
    'http://your-backend:8080/oauth2/authorize?redirect_uri=myapp://callback'
  );
  const data = await response.json();

  const result = await WebBrowser.openAuthSessionAsync(
    data.data.authorizationUrl,
    'myapp://callback'
  );

  if (result.type === 'success') {
    const code = new URL(result.url).searchParams.get('code');
    // Exchange code for token
  }
};
```

## Configuration Reference

| Property | Description | Default | Required |
|----------|-------------|---------|----------|
| `oauth2.enabled` | Enable/disable OAuth2 | `true` | No |
| `oauth2.provider` | Provider type (KEYCLOAK, OKTA, GOOGLE, META, GITHUB, MICROSOFT, CUSTOM) | `KEYCLOAK` | Yes |
| `oauth2.client-id` | OAuth2 client ID | - | Yes |
| `oauth2.client-secret` | OAuth2 client secret | - | Yes |
| `oauth2.issuer-uri` | OAuth2 issuer/authorization server URI | - | Yes |
| `oauth2.authorization-uri` | Authorization endpoint (auto-detected) | - | No |
| `oauth2.token-uri` | Token endpoint (auto-detected) | - | No |
| `oauth2.user-info-uri` | User info endpoint (auto-detected) | - | No |
| `oauth2.jwk-set-uri` | JWK Set URI for token validation (auto-detected) | - | No |
| `oauth2.scopes` | OAuth2 scopes | `openid,profile,email` | No |
| `oauth2.custom-login-enabled` | Enable custom login page support | `false` | No |
| `oauth2.cors.enabled` | Enable CORS | `true` | No |
| `oauth2.cors.allowed-origins` | Allowed origins | `*` | No |
| `oauth2.cors.allow-credentials` | Allow credentials | `true` | No |

## Keycloak Setup

### 1. Create Realm
- Login to Keycloak Admin Console
- Create a new realm (e.g., "myrealm")

### 2. Create Client
- Clients → Create Client
- Client ID: `your-client-id`
- Client Protocol: `openid-connect`
- Access Type: `confidential`
- Valid Redirect URIs: `http://localhost:3000/*` (for React), `myapp://*` (for React Native)
- Web Origins: `http://localhost:3000`

### 3. Enable Direct Access Grants (for custom login)
- Clients → your-client → Settings
- Enable "Direct Access Grants Enabled"
- Save

### 4. Get Client Secret
- Clients → your-client → Credentials
- Copy the Secret

### 5. Create Users
- Users → Add User
- Set username, email, etc.
- Credentials → Set Password → Disable "Temporary"

## Security Considerations

1. **Never expose client secret in frontend code**
2. **Use HTTPS in production**
3. **Validate state parameter** to prevent CSRF
4. **Store tokens securely**:
   - Browser: `httpOnly` cookies or secure localStorage
   - Mobile: SecureStore/Keychain
5. **Implement token refresh** before expiration
6. **Resource Owner Password Credentials Grant** (custom login) is less secure than Authorization Code Flow - use only when necessary

## Troubleshooting

### Issue: "Custom login is not enabled"
**Solution**: Set `oauth2.custom-login-enabled=true` and ensure your provider supports password grant.

### Issue: "JWK Set URI not configured"
**Solution**: For CUSTOM provider, set `oauth2.jwk-set-uri` property explicitly.

### Issue: CORS errors
**Solution**: Configure CORS properly:
```properties
oauth2.cors.enabled=true
oauth2.cors.allowed-origins=http://localhost:3000
oauth2.cors.allow-credentials=true
```

### Issue: Token validation fails
**Solution**: Ensure `oauth2.issuer-uri` matches the issuer claim in JWT token.

## License

MIT License

## Author

Shashank PK

## Contributing

Contributions are welcome! Please feel free to submit pull requests.
