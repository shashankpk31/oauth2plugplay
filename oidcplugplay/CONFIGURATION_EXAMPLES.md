# OAuth2/OIDC Configuration Examples

This document provides detailed configuration examples for various OAuth2 providers.

## Table of Contents
1. [Keycloak](#keycloak)
2. [Okta](#okta)
3. [Google](#google)
4. [Meta (Facebook)](#meta-facebook)
5. [GitHub](#github)
6. [Microsoft Azure AD](#microsoft-azure-ad)
7. [Custom OAuth2 Provider](#custom-oauth2-provider)

---

## Keycloak

### Standard Configuration

```properties
# Basic Configuration
oauth2.enabled=true
oauth2.provider=KEYCLOAK
oauth2.client-id=my-app-client
oauth2.client-secret=your-keycloak-client-secret
oauth2.issuer-uri=http://localhost:8080/realms/myrealm

# Scopes
oauth2.scopes=openid,profile,email,roles

# CORS (for React/React Native)
oauth2.cors.enabled=true
oauth2.cors.allowed-origins=http://localhost:3000,http://localhost:19006
oauth2.cors.allow-credentials=true
```

### With Custom Login Page

```properties
oauth2.enabled=true
oauth2.provider=KEYCLOAK
oauth2.client-id=my-app-client
oauth2.client-secret=your-keycloak-client-secret
oauth2.issuer-uri=http://localhost:8080/realms/myrealm

# Enable custom login (requires Direct Access Grants in Keycloak client)
oauth2.custom-login-enabled=true

# Optional: Specify direct auth endpoint explicitly
oauth2.direct-auth-token-uri=http://localhost:8080/realms/myrealm/protocol/openid-connect/token
```

### Keycloak Client Setup

```yaml
Client Configuration in Keycloak:
  - Client ID: my-app-client
  - Client Protocol: openid-connect
  - Access Type: confidential
  - Standard Flow Enabled: ON (for authorization code flow)
  - Direct Access Grants Enabled: ON (for custom login)
  - Valid Redirect URIs:
      - http://localhost:3000/*
      - myapp://*  (for mobile)
  - Web Origins: http://localhost:3000
  - Backchannel Logout Session Required: ON (optional)
```

---

## Okta

### Standard Configuration

```properties
oauth2.enabled=true
oauth2.provider=OKTA
oauth2.client-id=0oa2abc123xyz456
oauth2.client-secret=your-okta-client-secret
oauth2.issuer-uri=https://dev-12345678.okta.com/oauth2/default

# Scopes
oauth2.scopes=openid,profile,email

# CORS
oauth2.cors.enabled=true
oauth2.cors.allowed-origins=http://localhost:3000
```

### With Custom Login Page

```properties
oauth2.enabled=true
oauth2.provider=OKTA
oauth2.client-id=0oa2abc123xyz456
oauth2.client-secret=your-okta-client-secret
oauth2.issuer-uri=https://dev-12345678.okta.com/oauth2/default

# Enable custom login (requires Resource Owner Password grant in Okta)
oauth2.custom-login-enabled=true

# Note: Okta requires additional configuration for password grant
oauth2.direct-auth-token-uri=https://dev-12345678.okta.com/oauth2/default/v1/token
```

### Okta Application Setup

```yaml
Application Configuration in Okta:
  - Application type: Web Application
  - Grant type allowed:
      - Authorization Code
      - Resource Owner Password (for custom login)
  - Sign-in redirect URIs: http://localhost:3000/callback
  - Sign-out redirect URIs: http://localhost:3000
  - Trusted Origins:
      - Origin URL: http://localhost:3000
      - Type: CORS
```

---

## Google

### Standard Configuration

```properties
oauth2.enabled=true
oauth2.provider=GOOGLE
oauth2.client-id=123456789-abc.apps.googleusercontent.com
oauth2.client-secret=GOCSPX-your-client-secret
oauth2.issuer-uri=https://accounts.google.com

# Google scopes
oauth2.scopes=openid,profile,email

# CORS
oauth2.cors.enabled=true
oauth2.cors.allowed-origins=http://localhost:3000
```

### Google OAuth2 Setup

```yaml
Google Cloud Console:
  1. Create Project
  2. Enable Google+ API
  3. OAuth consent screen:
      - User Type: External
      - Scopes: email, profile, openid
  4. Credentials → Create OAuth client ID:
      - Application type: Web application
      - Authorized redirect URIs: http://localhost:3000/callback
      - Authorized JavaScript origins: http://localhost:3000
```

**Note**: Google doesn't support Resource Owner Password Credentials Grant (custom login with username/password). Use Authorization Code Flow only.

---

## Meta (Facebook)

### Standard Configuration

```properties
oauth2.enabled=true
oauth2.provider=META
oauth2.client-id=your-facebook-app-id
oauth2.client-secret=your-facebook-app-secret
oauth2.issuer-uri=https://www.facebook.com

# Facebook scopes (permissions)
oauth2.scopes=public_profile,email

# CORS
oauth2.cors.enabled=true
oauth2.cors.allowed-origins=http://localhost:3000

# Facebook-specific endpoints (optional override)
oauth2.authorization-uri=https://www.facebook.com/v18.0/dialog/oauth
oauth2.token-uri=https://graph.facebook.com/v18.0/oauth/access_token
oauth2.user-info-uri=https://graph.facebook.com/me?fields=id,name,email,picture
```

### Facebook App Setup

```yaml
Facebook Developers Console:
  1. Create App → Consumer
  2. Add Facebook Login product
  3. Settings → Basic:
      - App ID: your-facebook-app-id
      - App Secret: your-facebook-app-secret
  4. Facebook Login → Settings:
      - Valid OAuth Redirect URIs: http://localhost:3000/callback
      - Client OAuth Login: Yes
      - Web OAuth Login: Yes
```

**Note**: Facebook also doesn't support password grant. Custom login not available.

---

## GitHub

### Standard Configuration

```properties
oauth2.enabled=true
oauth2.provider=GITHUB
oauth2.client-id=your-github-client-id
oauth2.client-secret=your-github-client-secret
oauth2.issuer-uri=https://github.com

# GitHub scopes
oauth2.scopes=read:user,user:email

# CORS
oauth2.cors.enabled=true
oauth2.cors.allowed-origins=http://localhost:3000

# GitHub-specific endpoints (optional override)
oauth2.authorization-uri=https://github.com/login/oauth/authorize
oauth2.token-uri=https://github.com/login/oauth/access_token
oauth2.user-info-uri=https://api.github.com/user
```

### GitHub OAuth App Setup

```yaml
GitHub Settings:
  1. Developer settings → OAuth Apps → New OAuth App
  2. Configuration:
      - Application name: Your App Name
      - Homepage URL: http://localhost:3000
      - Authorization callback URL: http://localhost:3000/callback
  3. Generate client secret
```

**Note**: GitHub doesn't support JWT tokens or custom login with password.

---

## Microsoft Azure AD

### Standard Configuration

```properties
oauth2.enabled=true
oauth2.provider=MICROSOFT
oauth2.client-id=your-azure-application-id
oauth2.client-secret=your-azure-client-secret
oauth2.issuer-uri=https://login.microsoftonline.com/your-tenant-id/v2.0

# Microsoft scopes
oauth2.scopes=openid,profile,email,User.Read

# CORS
oauth2.cors.enabled=true
oauth2.cors.allowed-origins=http://localhost:3000

# Microsoft-specific endpoints (optional)
oauth2.authorization-uri=https://login.microsoftonline.com/your-tenant-id/oauth2/v2.0/authorize
oauth2.token-uri=https://login.microsoftonline.com/your-tenant-id/oauth2/v2.0/token
oauth2.jwk-set-uri=https://login.microsoftonline.com/your-tenant-id/discovery/v2.0/keys
```

### Azure AD App Registration

```yaml
Azure Portal → Azure Active Directory → App registrations:
  1. New registration
  2. Configuration:
      - Name: Your App
      - Supported account types: Choose based on requirement
      - Redirect URI: Web → http://localhost:3000/callback
  3. Certificates & secrets → New client secret
  4. API permissions → Add Microsoft Graph → Delegated:
      - openid, profile, email, User.Read
  5. Authentication:
      - Access tokens: Yes
      - ID tokens: Yes
```

### With Resource Owner Password Credentials (ROPC)

```properties
oauth2.enabled=true
oauth2.provider=MICROSOFT
oauth2.client-id=your-azure-application-id
oauth2.client-secret=your-azure-client-secret
oauth2.issuer-uri=https://login.microsoftonline.com/your-tenant-id/v2.0

# Enable custom login
oauth2.custom-login-enabled=true
oauth2.direct-auth-token-uri=https://login.microsoftonline.com/your-tenant-id/oauth2/v2.0/token

# ROPC specific scopes
oauth2.scopes=https://graph.microsoft.com/.default
```

**Note**: ROPC is not recommended by Microsoft and doesn't work with MFA-enabled accounts.

---

## Custom OAuth2 Provider

### Full Configuration

```properties
oauth2.enabled=true
oauth2.provider=CUSTOM
oauth2.client-id=your-custom-client-id
oauth2.client-secret=your-custom-client-secret
oauth2.issuer-uri=https://oauth.your-domain.com

# All endpoints must be specified for custom provider
oauth2.authorization-uri=https://oauth.your-domain.com/authorize
oauth2.token-uri=https://oauth.your-domain.com/token
oauth2.user-info-uri=https://oauth.your-domain.com/userinfo
oauth2.jwk-set-uri=https://oauth.your-domain.com/jwks
oauth2.logout-uri=https://oauth.your-domain.com/logout

# Custom scopes
oauth2.scopes=openid,profile,email,custom_scope

# Enable custom login if supported
oauth2.custom-login-enabled=true
oauth2.direct-auth-token-uri=https://oauth.your-domain.com/token

# CORS
oauth2.cors.enabled=true
oauth2.cors.allowed-origins=http://localhost:3000

# JWT validation
oauth2.jwt.validate-issuer=true
oauth2.jwt.validate-audience=false
oauth2.jwt.access-token-validity=3600
oauth2.jwt.refresh-token-validity=86400
```

---

## Environment-Specific Configuration

### application-dev.properties (Development)

```properties
# Development with local Keycloak
oauth2.enabled=true
oauth2.provider=KEYCLOAK
oauth2.client-id=dev-client
oauth2.client-secret=dev-secret
oauth2.issuer-uri=http://localhost:8080/realms/dev-realm
oauth2.custom-login-enabled=true

# Permissive CORS for development
oauth2.cors.enabled=true
oauth2.cors.allowed-origins=*
oauth2.cors.allow-credentials=true

# Verbose logging
logging.level.com.shashankpk.oauth2.starter=DEBUG
logging.level.org.springframework.security=DEBUG
```

### application-prod.properties (Production)

```properties
# Production with secure settings
oauth2.enabled=true
oauth2.provider=KEYCLOAK
oauth2.client-id=${OAUTH2_CLIENT_ID}
oauth2.client-secret=${OAUTH2_CLIENT_SECRET}
oauth2.issuer-uri=${OAUTH2_ISSUER_URI}
oauth2.custom-login-enabled=false

# Strict CORS
oauth2.cors.enabled=true
oauth2.cors.allowed-origins=https://app.yourdomain.com
oauth2.cors.allow-credentials=true

# Production logging
logging.level.com.shashankpk.oauth2.starter=INFO
logging.level.org.springframework.security=WARN

# JWT validation
oauth2.jwt.validate-issuer=true
oauth2.jwt.validate-audience=true
oauth2.jwt.audience=your-app-audience
```

---

## Testing Configuration

### application-test.properties

```properties
# Test with mock OAuth2 server or test realm
oauth2.enabled=true
oauth2.provider=KEYCLOAK
oauth2.client-id=test-client
oauth2.client-secret=test-secret
oauth2.issuer-uri=http://localhost:8081/realms/test-realm
oauth2.custom-login-enabled=true

# Test CORS
oauth2.cors.enabled=false

# Test logging
logging.level.com.shashankpk.oauth2.starter=TRACE
```

---

## Multi-Tenant Configuration

If you need to support multiple providers in the same application:

```properties
# Default provider
oauth2.provider=KEYCLOAK
oauth2.client-id=default-client
oauth2.client-secret=default-secret
oauth2.issuer-uri=http://localhost:8080/realms/default

# Custom properties for additional providers
oauth2.custom-properties.okta-client-id=okta-specific-id
oauth2.custom-properties.okta-secret=okta-specific-secret
oauth2.custom-properties.google-client-id=google-specific-id
```

**Note**: Multi-tenant support would require custom implementation using the custom-properties map.

---

## Troubleshooting

### Common Issues and Solutions

1. **Invalid Redirect URI**
   - Ensure redirect URI in provider matches exactly (including trailing slash)
   - Check allowed redirect URIs in provider console

2. **CORS Errors**
   - Add frontend URL to `oauth2.cors.allowed-origins`
   - Ensure `oauth2.cors.allow-credentials=true` if using cookies

3. **Token Validation Fails**
   - Verify `oauth2.issuer-uri` matches JWT issuer claim
   - Check `oauth2.jwk-set-uri` is accessible

4. **Custom Login Not Working**
   - Ensure `oauth2.custom-login-enabled=true`
   - Verify provider supports password grant
   - Check "Direct Access Grants" enabled in Keycloak

5. **401 Unauthorized**
   - Token might be expired - implement token refresh
   - Check token is sent in Authorization header correctly
   - Validate token signature and claims
