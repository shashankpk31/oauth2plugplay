# Sample Backend - Spring Boot with oidcplugplay

This is a sample Spring Boot application demonstrating enterprise-level authentication using the **oidcplugplay** OAuth2/OIDC starter library with Keycloak.

## Features

- ✅ JWT-based authentication using oidcplugplay
- ✅ Protected REST APIs with role-based access
- ✅ Custom login support (username/password)
- ✅ Token validation and user info extraction
- ✅ CORS configured for React frontend
- ✅ Comprehensive error handling
- ✅ Health check endpoints

## Technology Stack

- Java 17
- Spring Boot 3.3.12
- oidcplugplay 1.0.0 (OAuth2/OIDC Starter)
- Keycloak (via Docker)
- Maven

## Project Structure

```
sample-backend/
├── src/main/java/com/example/demo/
│   ├── SampleBackendApplication.java     # Main application
│   ├── config/
│   │   └── WebConfig.java                # CORS configuration
│   ├── controller/
│   │   ├── PublicController.java         # Public endpoints (no auth)
│   │   └── UserController.java           # Protected endpoints (requires JWT)
│   └── dto/
│       ├── ApiResponseDto.java           # Standard API response wrapper
│       └── UserProfileResponse.java      # User profile DTO
├── src/main/resources/
│   ├── application.properties            # Main configuration
│   └── application-dev.properties        # Development profile
└── pom.xml
```

## API Endpoints

### OAuth2/OIDC Endpoints (Provided by oidcplugplay)

These endpoints are automatically available once you add the oidcplugplay dependency:

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/oauth2/login` | Login with username/password | No |
| POST | `/oauth2/token` | Exchange auth code for token | No |
| POST | `/oauth2/refresh` | Refresh access token | No |
| GET | `/oauth2/userinfo` | Get user info from token | Yes |
| GET | `/oauth2/validate` | Validate JWT token | Yes |
| POST | `/oauth2/logout` | Logout user | No |
| GET | `/oauth2/health` | Health check | No |

### Application Endpoints

#### Public Endpoints (No Authentication Required)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/public/hello` | Public hello endpoint |
| GET | `/api/public/health` | Application health check |

#### Protected Endpoints (JWT Token Required)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/user/profile` | Get current user profile |
| GET | `/api/user/claims` | Get all JWT claims |
| GET | `/api/user/dashboard` | User dashboard data |

## Setup Instructions

### 1. Start Keycloak with Docker

From the root directory:

```bash
docker-compose up -d
```

Wait for Keycloak to start (check logs):

```bash
docker logs -f keycloak-auth-server
```

Keycloak will be available at: http://localhost:8180

### 2. Configure Keycloak

Access Keycloak Admin Console:
- URL: http://localhost:8180
- Username: `admin`
- Password: `admin`

#### Create Realm

1. Click on "Create realm" (top left dropdown)
2. Name: `sample-realm`
3. Click "Create"

#### Create Client

1. Go to "Clients" → "Create client"
2. **Client ID**: `sample-app`
3. Click "Next"
4. **Client authentication**: ON (to get client secret)
5. **Authorization**: OFF
6. **Authentication flow**:
   - ✅ Standard flow
   - ✅ Direct access grants (for username/password login)
7. Click "Next"
8. **Valid redirect URIs**:
   - `http://localhost:5173/*`
   - `http://localhost:8081/*`
9. **Valid post logout redirect URIs**: `http://localhost:5173/*`
10. **Web origins**: `http://localhost:5173`
11. Click "Save"

#### Get Client Secret

1. Go to "Clients" → "sample-app" → "Credentials" tab
2. Copy the "Client secret"
3. Update `application.properties`:
   ```properties
   oauth2.client-secret=<paste-client-secret-here>
   ```

#### Create Test User

1. Go to "Users" → "Create new user"
2. **Username**: `testuser`
3. **Email**: `testuser@example.com`
4. **Email verified**: ON
5. **First name**: `Test`
6. **Last name**: `User`
7. Click "Create"
8. Go to "Credentials" tab
9. Click "Set password"
10. **Password**: `password123`
11. **Temporary**: OFF
12. Click "Save"

### 3. Build and Run the Application

```bash
cd sample-backend
mvn clean install
mvn spring-boot:run
```

Or run with dev profile:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The application will start on: http://localhost:8081

## Testing the API

### 1. Test Public Endpoint (No Auth)

```bash
curl http://localhost:8081/api/public/hello
```

### 2. Login and Get Token

```bash
curl -X POST http://localhost:8081/oauth2/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'
```

Response:
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "token_type": "Bearer",
    "expires_in": 3600,
    "userInfo": {
      "sub": "...",
      "email": "testuser@example.com",
      "name": "Test User"
    }
  }
}
```

### 3. Access Protected Endpoint

```bash
# Save the token
TOKEN="<paste-access-token-here>"

# Get user profile
curl http://localhost:8081/api/user/profile \
  -H "Authorization: Bearer $TOKEN"

# Get dashboard
curl http://localhost:8081/api/user/dashboard \
  -H "Authorization: Bearer $TOKEN"

# Get all JWT claims
curl http://localhost:8081/api/user/claims \
  -H "Authorization: Bearer $TOKEN"
```

### 4. Refresh Token

```bash
curl -X POST http://localhost:8081/oauth2/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "<paste-refresh-token-here>"
  }'
```

### 5. Validate Token

```bash
curl http://localhost:8081/oauth2/validate \
  -H "Authorization: Bearer $TOKEN"
```

### 6. Get User Info

```bash
curl http://localhost:8081/oauth2/userinfo \
  -H "Authorization: Bearer $TOKEN"
```

### 7. Logout

```bash
curl -X POST http://localhost:8081/oauth2/logout \
  -H "Authorization: Bearer $TOKEN"
```

## Configuration Properties

The application uses the oidcplugplay starter, which provides comprehensive OAuth2/OIDC configuration:

```properties
# Enable OAuth2
oauth2.enabled=true

# Provider type
oauth2.provider=KEYCLOAK

# Client credentials
oauth2.client-id=sample-app
oauth2.client-secret=your-client-secret

# Keycloak endpoints
oauth2.issuer-uri=http://localhost:8180/realms/sample-realm

# OAuth2 scopes
oauth2.scopes=openid,profile,email

# Enable custom login (username/password)
oauth2.custom-login-enabled=true

# JWT validation
oauth2.jwt.validate-issuer=true
oauth2.jwt.validate-audience=false
```

## How It Works

### 1. Authentication Flow

```
Frontend → POST /oauth2/login
         → oidcplugplay validates credentials with Keycloak
         → Keycloak returns JWT tokens
         → Frontend stores access_token
```

### 2. Accessing Protected Endpoints

```
Frontend → GET /api/user/profile
         → Header: Authorization: Bearer <jwt-token>
         → Spring Security validates JWT
         → JWT decoded and injected as @AuthenticationPrincipal
         → Controller extracts user info from JWT
         → Returns user profile
```

### 3. Token Refresh

```
Frontend → POST /oauth2/refresh
         → Body: { "refreshToken": "..." }
         → oidcplugplay requests new token from Keycloak
         → Returns new access_token
```

## Integration with React Frontend

The React Vite frontend (sample-frontend) is configured to work seamlessly with this backend:

1. User enters credentials in React login form
2. React calls `POST /oauth2/login`
3. Backend validates with Keycloak and returns JWT
4. React stores JWT in localStorage
5. React includes JWT in Authorization header for all API calls
6. Backend validates JWT and processes requests

## Troubleshooting

### 401 Unauthorized Error

- Check if JWT token is included in Authorization header
- Verify token hasn't expired
- Check Keycloak issuer URI matches configuration

### CORS Errors

- Verify CORS configuration in `WebConfig.java`
- Check if frontend origin is allowed
- Ensure credentials are allowed if using cookies

### Connection Refused (Keycloak)

- Check if Keycloak Docker container is running: `docker ps`
- Verify port 8180 is not in use: `netstat -ano | findstr :8180`
- Check Keycloak logs: `docker logs keycloak-auth-server`

### Invalid Client Secret

- Regenerate client secret in Keycloak
- Update `oauth2.client-secret` in application.properties
- Restart the application

## Production Considerations

For production deployment, consider:

1. **Use environment variables** for sensitive data:
   ```properties
   oauth2.client-secret=${OAUTH2_CLIENT_SECRET}
   oauth2.issuer-uri=${OAUTH2_ISSUER_URI}
   ```

2. **Enable HTTPS** for secure communication

3. **Configure proper CORS** origins (not wildcard *)

4. **Use external Keycloak** (not Docker dev mode)

5. **Enable audience validation**:
   ```properties
   oauth2.jwt.validate-audience=true
   oauth2.jwt.audience=sample-app
   ```

6. **Set appropriate token validity**:
   ```properties
   oauth2.jwt.access-token-validity=1800  # 30 minutes
   oauth2.jwt.refresh-token-validity=604800  # 7 days
   ```

## References

- [oidcplugplay Documentation](../oidcplugplay/README.md)
- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [Spring Security OAuth2](https://docs.spring.io/spring-security/reference/servlet/oauth2/index.html)

## License

MIT License
