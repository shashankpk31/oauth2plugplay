# 🔐 OTP Authentication Spring Boot Starter

A **plug-and-play** Spring Boot starter for OTP-based authentication with Keycloak. Just add the dependency, configure properties, and you're ready to go!

## ✨ Features

- ✅ **Zero Code Required** - Auto-configuration handles everything
- ✅ **OTP Authentication** - Phone or Email based OTP
- ✅ **Keycloak Integration** - Auto user creation and JWT token management
- ✅ **Testing Mode** - Fixed OTP for development (no SMS needed)
- ✅ **Email Support** - Built-in email OTP delivery
- ✅ **CORS Support** - Ready for React/React Native frontends
- ✅ **REST API** - Complete authentication endpoints
- ✅ **Production Ready** - Exception handling, validation, logging

## 📋 Requirements

- Java 17+
- Spring Boot 3.3.12
- Keycloak 24.x
- PostgreSQL/MySQL (for OTP and user storage)

## 🚀 Quick Start

### 1. Build and Install the Starter

```bash
cd otp-auth-starter
mvn clean install
```

This installs the starter to your local Maven repository (`~/.m2/repository`).

### 2. Add Dependency to Your Project

Add this to your Spring Boot application's `pom.xml`:

```xml
<dependency>
    <groupId>com.shashankpk</groupId>
    <artifactId>otp-auth-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 3. Configure Properties

Add to your `application.properties` or `application.yml`:

```properties
# Database Configuration (Required)
spring.datasource.url=jdbc:postgresql://localhost:5432/yourdb
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.jpa.hibernate.ddl-auto=update

# Keycloak Configuration (Required)
otp.auth.keycloak.server-url=http://localhost:8180
otp.auth.keycloak.realm=sample-realm
otp.auth.keycloak.client-id=sample-app
otp.auth.keycloak.client-secret=your-client-secret
otp.auth.keycloak.admin-username=admin
otp.auth.keycloak.admin-password=admin

# Email Configuration (Optional - for email OTP)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# OTP Testing Mode (Disable in production)
otp.auth.otp.testing-mode=true
otp.auth.otp.fixed-otp=123456
```

### 4. Run Your Application

```bash
mvn spring-boot:run
```

**That's it!** The starter automatically:
- Creates REST endpoints at `/auth/otp/*`
- Sets up database tables
- Configures Keycloak integration
- Enables CORS for your frontend

## 📡 API Endpoints

The starter automatically exposes these endpoints:

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/auth/otp/send` | Send OTP to phone/email |
| POST | `/auth/otp/verify` | Verify OTP and login/register |
| POST | `/auth/otp/refresh` | Refresh access token |
| GET | `/auth/otp/profile` | Get user profile |
| GET | `/auth/otp/health` | Health check |

## 📱 Frontend Integration

### React Web Example

```javascript
// 1. Send OTP
const response = await fetch('http://localhost:8080/auth/otp/send', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    phoneNumber: '+919876543210',
    type: 'PHONE'
  })
});

const data = await response.json();
console.log('OTP:', data.data.otp_debug); // Testing mode only

// 2. Verify OTP
const verifyResponse = await fetch('http://localhost:8080/auth/otp/verify', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    identifier: '+919876543210',
    otp: '123456',
    phoneNumber: '+919876543210',
    name: 'John Doe'
  })
});

const verifyData = await verifyResponse.json();
localStorage.setItem('accessToken', verifyData.data.accessToken);
localStorage.setItem('refreshToken', verifyData.data.refreshToken);

// 3. Make authenticated requests
const profileResponse = await fetch('http://localhost:8080/auth/otp/profile', {
  headers: {
    'Authorization': `Bearer ${localStorage.getItem('accessToken')}`,
    'X-User-Id': verifyData.data.user.id
  }
});
```

### React Native Example

```javascript
import * as SecureStore from 'expo-secure-store';

// 1. Send OTP
const response = await fetch('http://YOUR_IP:8080/auth/otp/send', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    phoneNumber: '+919876543210',
    type: 'PHONE'
  })
});

// 2. Verify and store tokens
const verifyResponse = await fetch('http://YOUR_IP:8080/auth/otp/verify', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    identifier: '+919876543210',
    otp: '123456',
    phoneNumber: '+919876543210',
    name: 'John Doe'
  })
});

const data = await verifyResponse.json();
await SecureStore.setItemAsync('accessToken', data.data.accessToken);
await SecureStore.setItemAsync('refreshToken', data.data.refreshToken);
```

## ⚙️ Configuration Reference

### Required Configuration

```properties
# Keycloak
otp.auth.keycloak.server-url=http://localhost:8180
otp.auth.keycloak.realm=sample-realm
otp.auth.keycloak.client-id=sample-app
otp.auth.keycloak.client-secret=your-secret
```

### Optional Configuration

```properties
# OTP Settings
otp.auth.otp.testing-mode=true              # Enable testing mode
otp.auth.otp.fixed-otp=123456               # Fixed OTP for testing
otp.auth.otp.length=6                       # OTP length
otp.auth.otp.expiry-seconds=300             # 5 minutes
otp.auth.otp.max-attempts=3                 # Max verification attempts
otp.auth.otp.enable-file-logging=true       # Log OTPs to file
otp.auth.otp.test-email-recipient=test@example.com

# Keycloak Admin
otp.auth.keycloak.admin-username=admin
otp.auth.keycloak.admin-password=admin
otp.auth.keycloak.default-role=user

# Email
otp.auth.email.enabled=true

# CORS
otp.auth.cors.enabled=true
otp.auth.cors.allowed-origins=http://localhost:3000,http://localhost:5173
otp.auth.cors.allow-credentials=true
```

## 🧪 Testing Without SMS

The starter includes multiple testing strategies:

### 1. Console Logging (Default)

```
┌─────────────────────────────────────┐
│     🔐 OTP TESTING MODE            │
├─────────────────────────────────────┤
│ Phone: +919876543210                │
│ OTP:   123456                       │
│ Valid: 5 minutes                    │
└─────────────────────────────────────┘
```

### 2. File Logging

Check `otp_logs/otp_YYYY-MM-DD.txt`:

```
[2025-03-10T14:30:00] +919876543210 -> 123456
```

### 3. Email Forwarding

Set `otp.auth.otp.test-email-recipient` to receive all test OTPs via email.

### 4. Fixed OTP Mode

All OTP requests return the configured `fixed-otp` (default: 123456).

### 5. API Response (Testing Mode)

OTP is included in the response:

```json
{
  "success": true,
  "data": {
    "otpSent": true,
    "otp_debug": "123456"
  }
}
```

## 🔧 Keycloak Setup

### 1. Start Keycloak

```bash
docker run -p 8180:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:latest start-dev
```

### 2. Create Realm

- Login to http://localhost:8180
- Create realm: `sample-realm`

### 3. Create Client

- Client ID: `sample-app`
- Client authentication: ON
- Standard flow: Enabled
- Direct access grants: Enabled (for token generation)
- Get client secret from Credentials tab

### 4. Create Role

- Go to Realm roles
- Create role: `user` (default role for new users)

## 📦 Database Tables

The starter automatically creates these tables:

```sql
-- OTP Records
CREATE TABLE otp_records (
    id BIGSERIAL PRIMARY KEY,
    identifier VARCHAR(255),
    otp_code VARCHAR(10),
    type VARCHAR(20),
    expires_at TIMESTAMP,
    attempts INTEGER,
    verified BOOLEAN,
    created_at TIMESTAMP
);

-- Users
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    phone_number VARCHAR(20),
    email VARCHAR(255),
    name VARCHAR(255),
    active BOOLEAN,
    created_at TIMESTAMP,
    last_login_at TIMESTAMP
);
```

## 🌐 Production Deployment

### 1. Disable Testing Mode

```properties
otp.auth.otp.testing-mode=false
```

### 2. Configure SMS Provider (Future)

```properties
otp.auth.sms.enabled=true
otp.auth.sms.provider=twilio
otp.auth.sms.twilio.account-sid=YOUR_SID
otp.auth.sms.twilio.auth-token=YOUR_TOKEN
otp.auth.sms.twilio.from-number=+1234567890
```

### 3. Use Environment Variables

```properties
otp.auth.keycloak.server-url=${KEYCLOAK_URL}
otp.auth.keycloak.client-secret=${KEYCLOAK_SECRET}
spring.datasource.url=${DATABASE_URL}
spring.mail.password=${MAIL_PASSWORD}
```

## 🔒 Security Considerations

1. **Never commit secrets** - Use environment variables
2. **Use HTTPS in production**
3. **Disable testing mode in production**
4. **Set appropriate CORS origins**
5. **Use strong database passwords**
6. **Rotate Keycloak client secrets regularly**

## 🐛 Troubleshooting

### Issue: "OTP not received"

Check console logs or `otp_logs/` directory in testing mode.

### Issue: "Keycloak connection failed"

- Verify Keycloak is running
- Check `server-url`, `realm`, and credentials
- Check network connectivity

### Issue: "User creation failed"

- Verify admin credentials
- Check if realm exists
- Enable DEBUG logging: `logging.level.com.shashankpk.otpauth=DEBUG`

### Issue: "Database error"

- Verify datasource configuration
- Check if database is running
- Ensure Hibernate can create tables (`ddl-auto=update`)

## 📚 Examples

See the `/examples` directory for complete working examples:

- `example-backend/` - Spring Boot app using the starter
- `example-react/` - React web frontend
- `example-react-native/` - React Native mobile app

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📄 License

MIT License

## 👨‍💻 Author

Shashank PK

---

**Ready to use!** Just add the dependency and configure properties. Your OTP authentication is ready! 🚀
