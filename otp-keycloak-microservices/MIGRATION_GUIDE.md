# Migration Guide: Reactive Gateway → MVC Gateway + MPIN Feature

This guide covers migrating from the old version to the new version with:
- Spring Boot 3.5.11 + Spring Cloud 2025.0.1
- Spring Cloud Gateway MVC (blocking/servlet-based)
- Keycloak Service Account (secure credentials)
- MPIN Quick Login Feature

---

## What Changed?

### 1. **Spring Boot & Spring Cloud Versions**

**Before:**
- Spring Boot: 3.3.0
- Spring Cloud: 2023.0.0

**After:**
- Spring Boot: 3.5.11
- Spring Cloud: 2025.0.1

**Why?** Latest stable versions with bug fixes and performance improvements.

---

### 2. **API Gateway: Reactive → MVC-Based**

#### **Before (Reactive - WebFlux)**

```xml
<!-- Old dependency -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>
```

```java
// Reactive filter using Mono, ServerHttpRequest
@EnableWebFluxSecurity
public class SecurityConfig {
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        // Reactive configuration
    }
}
```

#### **After (MVC-Based - Servlet)**

```xml
<!-- New dependency -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway-mvc</artifactId>
</dependency>
```

```java
// Blocking filter using HttpServletRequest
@EnableWebSecurity
public class SecurityConfig {
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        // Traditional servlet configuration
    }
}
```

**Key Changes:**
- `ServerHttpRequest` → `HttpServletRequest`
- `Mono`, `Flux`, `Reactor` → Standard blocking Java
- `AbstractGatewayFilterFactory` → `HandlerFilterFunction`
- `@EnableWebFluxSecurity` → `@EnableWebSecurity`
- Routes configured programmatically in `GatewayRoutesConfig`

**Why?** Simpler, more familiar blocking model. Better for traditional microservices that don't need reactive streams.

---

### 3. **Keycloak Security: Admin → Service Account**

#### **Before (INSECURE)**

```yaml
keycloak:
  admin-username: admin  # ❌ Hardcoded admin credentials
  admin-password: admin  # ❌ CRITICAL SECURITY RISK!
```

#### **After (SECURE)**

```yaml
keycloak:
  admin-username: ${KEYCLOAK_SERVICE_USERNAME}  # ✅ Service account from env
  admin-password: ${KEYCLOAK_SERVICE_PASSWORD}  # ✅ Secure!
```

**Setup Required:**
1. Create service account in Keycloak (see `KEYCLOAK_SERVICE_ACCOUNT_SETUP.md`)
2. Assign limited roles: `view-users`, `manage-users`, `query-users`
3. Store credentials in environment variables (never in code!)

**Why?** Using admin credentials in code is a critical security vulnerability. Service accounts follow least privilege principle.

---

### 4. **New Feature: MPIN Quick Login**

MPIN (Mobile PIN) allows users to login quickly after initial OTP authentication, similar to banking apps.

**Key Characteristics:**
- 4-6 digit PIN
- Session-based (expires after 30 days by default)
- Encrypted storage (AES-256-GCM)
- Limited failed attempts (3 by default)
- Must renew with OTP after expiry

**New Endpoints:**
- `POST /auth/mpin/set` - Set MPIN (protected)
- `POST /auth/mpin/login` - Login with MPIN (public)
- `DELETE /auth/mpin` - Remove MPIN (protected)
- `GET /auth/mpin/status` - Check MPIN status (protected)

**New Database Table:**
```sql
CREATE TABLE mpin_records (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    identifier VARCHAR(255) NOT NULL,
    encrypted_mpin VARCHAR(255) NOT NULL,
    failed_attempts INT DEFAULT 0,
    session_expires_at TIMESTAMP NOT NULL,
    last_used_at TIMESTAMP,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## Migration Steps

### Step 1: Update Dependencies

Update all three `pom.xml` files (api-gateway, identity-auth-service, business-service):

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.11</version>
</parent>

<properties>
    <spring-cloud.version>2025.0.1</spring-cloud.version>
</properties>
```

### Step 2: Update API Gateway

**2.1. Update Dependency in `api-gateway/pom.xml`:**
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway-mvc</artifactId>
</dependency>
```

**2.2. Replace Files:**
- `SecurityConfig.java` - Use MVC-based security
- `JwtAuthenticationFilter.java` - Use blocking filter
- Create `GatewayRoutesConfig.java` - Configure routes programmatically

**2.3. Update `application.yml`:**
```yaml
spring:
  cloud:
    gateway:
      mvc:  # Add 'mvc' level
        routes:
          # ... routes
```

### Step 3: Setup Keycloak Service Account

**3.1. Follow `KEYCLOAK_SERVICE_ACCOUNT_SETUP.md`:**
- Create service user: `service-user-manager`
- Assign roles: `view-users`, `manage-users`, `query-users`
- Set strong password

**3.2. Update `identity-auth-service/application.yml`:**
```yaml
keycloak:
  server-url: ${KEYCLOAK_SERVER_URL:http://localhost:8180}
  realm: ${KEYCLOAK_REALM:myrealm}
  client-id: ${KEYCLOAK_CLIENT_ID:backend-service}
  client-secret: ${KEYCLOAK_CLIENT_SECRET}
  admin-username: ${KEYCLOAK_SERVICE_USERNAME}
  admin-password: ${KEYCLOAK_SERVICE_PASSWORD}
```

**3.3. Create `.env` file (add to `.gitignore`):**
```bash
KEYCLOAK_SERVER_URL=http://localhost:8180
KEYCLOAK_REALM=myrealm
KEYCLOAK_CLIENT_ID=backend-service
KEYCLOAK_CLIENT_SECRET=your-client-secret
KEYCLOAK_SERVICE_USERNAME=service-user-manager
KEYCLOAK_SERVICE_PASSWORD=your-service-password
MPIN_ENCRYPTION_KEY=your-base64-encryption-key
```

**3.4. Generate MPIN encryption key:**
```bash
# Run this command to generate a key
cd identity-auth-service
mvn compile exec:java -Dexec.mainClass="com.auth.identity.util.EncryptionUtil"
```

### Step 4: Database Migration (MPIN Table)

Run on your PostgreSQL database (port 5433):

```sql
CREATE TABLE IF NOT EXISTS mpin_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    identifier VARCHAR(255) NOT NULL,
    encrypted_mpin VARCHAR(255) NOT NULL,
    failed_attempts INT DEFAULT 0,
    session_expires_at TIMESTAMP NOT NULL,
    last_used_at TIMESTAMP,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_mpin_user_id ON mpin_records(user_id);
CREATE INDEX idx_mpin_identifier ON mpin_records(identifier);
```

Or simply let Hibernate auto-create it (if `ddl-auto: update` is enabled).

### Step 5: Rebuild & Restart Services

```bash
# Clean and build all services
mvn clean package -DskipTests

# Start services in order
# Terminal 1
cd identity-auth-service
mvn spring-boot:run

# Terminal 2
cd business-service
mvn spring-boot:run

# Terminal 3
cd api-gateway
mvn spring-boot:run
```

### Step 6: Verify Migration

**Test Gateway MVC:**
```bash
curl http://localhost:8081/actuator/health
```

**Test OTP Flow (should still work):**
```bash
# Register
curl -X POST http://localhost:8081/auth/register \
  -H "Content-Type: application/json" \
  -d '{"identifier": "test@example.com", "identifierType": "EMAIL"}'

# Verify OTP (check console for OTP)
curl -X POST http://localhost:8081/auth/otp/verify \
  -H "Content-Type: application/json" \
  -d '{"identifier": "test@example.com", "otp": "123456"}'
```

**Test MPIN Flow (new):**
```bash
# Set MPIN (requires JWT from OTP login)
curl -X POST http://localhost:8081/auth/mpin/set \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"mpin": "1234", "confirmMpin": "1234"}'

# Login with MPIN
curl -X POST http://localhost:8081/auth/mpin/login \
  -H "Content-Type: application/json" \
  -d '{"identifier": "test@example.com", "mpin": "1234"}'
```

---

## Breaking Changes

### 1. **Gateway Filter API Changed**

**Before:**
```java
public class CustomFilter extends AbstractGatewayFilterFactory<Config> {
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            // Reactive code with Mono
        };
    }
}
```

**After:**
```java
public class CustomFilter {
    public static HandlerFilterFunction<?, ?> apply() {
        return (request, next) -> {
            // Blocking code
        };
    }
}
```

### 2. **Route Configuration Changed**

**Before (application.yml only):**
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: my-route
          filters:
            - name: MyCustomFilter
```

**After (Java Config):**
```java
@Bean
public RouterFunction<ServerResponse> gatewayRoutes() {
    return route("my-route")
        .route(path("/path/**"), HandlerFunctions.http("http://service"))
        .filter(MyCustomFilter.apply())
        .build();
}
```

### 3. **Environment Variables Required**

**Before:** Could run with default values (insecure)

**After:** Must set environment variables:
- `KEYCLOAK_CLIENT_SECRET`
- `KEYCLOAK_SERVICE_USERNAME`
- `KEYCLOAK_SERVICE_PASSWORD`
- `MPIN_ENCRYPTION_KEY`

---

## Rollback Plan

If you need to rollback:

1. **Restore old pom.xml files** (Spring Boot 3.3.0, Spring Cloud 2023.0.0)
2. **Restore old gateway dependency:**
   ```xml
   <artifactId>spring-cloud-starter-gateway</artifactId>
   ```
3. **Restore old filter implementations** (reactive)
4. **Remove MPIN table** (if needed):
   ```sql
   DROP TABLE IF EXISTS mpin_records;
   ```
5. **Restore old application.yml** (without mvc level)

---

## Troubleshooting

### Issue: Gateway fails to start

**Error:** `NoSuchMethodError` or `ClassNotFoundException`

**Solution:** Clean Maven cache and rebuild:
```bash
mvn clean install -U
rm -rf ~/.m2/repository/org/springframework/cloud
mvn clean package
```

### Issue: Keycloak authentication fails

**Error:** "Insufficient permissions" or "Invalid credentials"

**Solution:**
1. Verify service account created correctly in Keycloak
2. Check environment variables are set:
   ```bash
   echo $KEYCLOAK_SERVICE_USERNAME
   echo $KEYCLOAK_SERVICE_PASSWORD
   ```
3. Test service account credentials manually (see KEYCLOAK_SERVICE_ACCOUNT_SETUP.md)

### Issue: MPIN encryption fails

**Error:** "Failed to initialize encryption"

**Solution:**
1. Generate valid encryption key:
   ```bash
   openssl rand -base64 32
   ```
2. Set in environment:
   ```bash
   export MPIN_ENCRYPTION_KEY="your-generated-key"
   ```
3. Ensure key is exactly 32 bytes (256 bits) when decoded

### Issue: Database migration fails

**Error:** "Table mpin_records does not exist"

**Solution:**
1. Check Hibernate ddl-auto setting:
   ```yaml
   spring:
     jpa:
       hibernate:
         ddl-auto: update
   ```
2. Or manually create table using SQL in Step 4 above

---

## Performance Impact

- **Gateway:** MVC-based gateway may have slightly lower throughput than reactive for high-concurrency scenarios, but better latency for typical request patterns
- **MPIN:** Adds ~10ms latency for encryption/decryption operations
- **Database:** New MPIN table with indexes - minimal impact

---

## Security Improvements

✅ **Before:** Admin credentials in code (CRITICAL RISK)
✅ **After:** Service account with limited permissions (SECURE)

✅ **Before:** No quick login option (always OTP)
✅ **After:** MPIN quick login with AES-256 encryption

✅ **Before:** No credential rotation strategy
✅ **After:** Environment-based credentials, easy to rotate

---

## Support

If you encounter issues during migration:

1. Check this migration guide
2. Review `KEYCLOAK_SERVICE_ACCOUNT_SETUP.md`
3. Check logs for detailed error messages
4. Verify all environment variables are set correctly
5. Test each component individually before integration testing

---

## Changelog Summary

### Version 2.0 (Current)

**Added:**
- MPIN quick login feature
- MPIN encryption with AES-256-GCM
- MPIN session management with auto-expiry
- Service account support for Keycloak
- Environment variable configuration
- Scheduled cleanup for expired MPIN sessions

**Changed:**
- Upgraded to Spring Boot 3.5.11
- Upgraded to Spring Cloud 2025.0.1
- Migrated API Gateway from reactive to MVC-based
- Refactored security configuration for servlet model
- Updated filter implementations for blocking model

**Security:**
- Fixed: Admin credentials in code (critical vulnerability)
- Added: Service account with limited privileges
- Added: Environment-based credential management
- Added: AES-256 encryption for MPIN storage

**Deprecated:**
- Reactive gateway implementation
- Hardcoded Keycloak admin credentials

---

## Next Steps After Migration

1. ✅ Test all authentication flows (OTP + MPIN)
2. ✅ Verify JWT propagation still works
3. ✅ Test service-to-service communication (FeignClient)
4. ✅ Set up production environment variables
5. ✅ Configure MPIN session expiry for your use case
6. ✅ Set up monitoring for MPIN failed attempts
7. ✅ Plan credential rotation schedule
8. ✅ Update API documentation for frontend team
