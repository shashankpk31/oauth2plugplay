# 🔗 How to Use OTP Auth Starter in Your Projects

This guide shows how to integrate the **OTP Auth Starter** into your existing `sample-backend` and `sample-frontend` projects.

---

## 📦 Part 1: Build the Starter

### Step 1: Build and Install

```bash
cd otp-auth-starter
mvn clean install
```

**Expected Output:**
```
[INFO] Installing .../otp-auth-spring-boot-starter-1.0.0.jar
[INFO] BUILD SUCCESS
```

This installs the starter to: `C:\Users\Dell\.m2\repository\com\shashankpk\otp-auth-spring-boot-starter\1.0.0\`

---

## 🔧 Part 2: Backend Integration (sample-backend)

### Step 1: Add Dependency

Edit `sample-backend/pom.xml`:

```xml
<dependencies>
    <!-- Your existing dependencies -->

    <!-- ADD THIS: OTP Auth Starter -->
    <dependency>
        <groupId>com.shashankpk</groupId>
        <artifactId>otp-auth-spring-boot-starter</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

### Step 2: Update application.properties

Edit `sample-backend/src/main/resources/application.properties`:

```properties
# Server Configuration
server.port=8081

# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/sampledb
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# ========================================
# OTP AUTH STARTER CONFIGURATION
# ========================================

# Enable OTP Authentication
otp.auth.enabled=true

# Keycloak Configuration
otp.auth.keycloak.server-url=http://localhost:8180
otp.auth.keycloak.realm=sample-realm
otp.auth.keycloak.client-id=sample-app
otp.auth.keycloak.client-secret=YOUR_CLIENT_SECRET_HERE
otp.auth.keycloak.admin-username=admin
otp.auth.keycloak.admin-password=admin
otp.auth.keycloak.default-role=user

# OTP Configuration (Testing Mode)
otp.auth.otp.testing-mode=true
otp.auth.otp.fixed-otp=123456
otp.auth.otp.length=6
otp.auth.otp.expiry-seconds=300
otp.auth.otp.max-attempts=3
otp.auth.otp.enable-file-logging=true
otp.auth.otp.test-email-recipient=your-test@example.com

# Email Configuration (Optional - for email OTP)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# CORS Configuration
otp.auth.cors.enabled=true
otp.auth.cors.allowed-origins=http://localhost:5173,http://localhost:3000,http://localhost:19006
otp.auth.cors.allow-credentials=true

# Logging
logging.level.com.shashankpk.otpauth=DEBUG
logging.level.com.example.demo=DEBUG
```

### Step 3: Remove Old OAuth2 Dependencies (Optional)

If you're replacing your existing `oidcplugplay`, you can remove it:

```xml
<!-- REMOVE OR COMMENT OUT -->
<!--
<dependency>
    <groupId>com.shashankpk</groupId>
    <artifactId>oauth2-oidc-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
-->
```

### Step 4: Update Your Controllers (Optional)

The starter automatically exposes `/auth/otp/*` endpoints. If you have custom controllers, you can use them alongside:

**Example: Keep your existing controllers**

```java
// sample-backend/src/main/java/com/example/demo/controller/UserController.java
package com.example.demo.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    // This endpoint will work with OTP Auth
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("Authorization") String authHeader) {

        // User is already authenticated via OTP Auth Starter
        // userId is passed from API Gateway/Frontend

        return ResponseEntity.ok(Map.of(
            "userId", userId,
            "message", "Profile data",
            "authenticated", true
        ));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(Map.of(
            "userId", userId,
            "dashboard", "Your dashboard data"
        ));
    }
}
```

### Step 5: Run the Backend

```bash
cd sample-backend
mvn clean install
mvn spring-boot:run
```

**Expected Output:**
```
╔════════════════════════════════════════════════════════════════╗
║        OTP Authentication Starter Initialized                  ║
╠════════════════════════════════════════════════════════════════╣
║ Keycloak Server: http://localhost:8180                         ║
║ Realm:           sample-realm                                  ║
║ Client ID:       sample-app                                    ║
║ Testing Mode:    ENABLED ⚠️                                    ║
║ OTP Expiry:      300 seconds                                   ║
╚════════════════════════════════════════════════════════════════╝

Tomcat started on port(s): 8081 (http)
```

### Step 6: Test the Endpoints

```bash
# Send OTP
curl -X POST http://localhost:8081/auth/otp/send \
  -H "Content-Type: application/json" \
  -d '{"phoneNumber":"+919876543210","type":"PHONE"}'

# Expected Response:
# {
#   "success": true,
#   "data": {
#     "otpSent": true,
#     "otp_debug": "123456"
#   }
# }

# Verify OTP
curl -X POST http://localhost:8081/auth/otp/verify \
  -H "Content-Type: application/json" \
  -d '{
    "identifier": "+919876543210",
    "otp": "123456",
    "phoneNumber": "+919876543210",
    "name": "Test User"
  }'

# Expected Response:
# {
#   "success": true,
#   "data": {
#     "accessToken": "eyJhbGc...",
#     "refreshToken": "eyJhbGc...",
#     "user": {...}
#   }
# }
```

---

## 🎨 Part 3: Frontend Integration (sample-frontend - React)

### Step 1: Install Dependencies

```bash
cd sample-frontend
npm install axios
```

### Step 2: Create Token Storage Utility

Create `sample-frontend/src/utils/tokenStorage.js`:

```javascript
class TokenStorage {
  static saveTokens(accessToken, refreshToken, expiresIn) {
    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', refreshToken);
    localStorage.setItem('tokenExpiry', Date.now() + (expiresIn * 1000));
  }

  static getAccessToken() {
    return localStorage.getItem('accessToken');
  }

  static getRefreshToken() {
    return localStorage.getItem('refreshToken');
  }

  static clearTokens() {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('tokenExpiry');
    localStorage.removeItem('user');
  }

  static isTokenExpired() {
    const expiry = localStorage.getItem('tokenExpiry');
    return !expiry || Date.now() > parseInt(expiry);
  }

  static saveUser(user) {
    localStorage.setItem('user', JSON.stringify(user));
  }

  static getUser() {
    const user = localStorage.getItem('user');
    return user ? JSON.parse(user) : null;
  }
}

export default TokenStorage;
```

### Step 3: Create API Client with Auto-Refresh

Create `sample-frontend/src/utils/apiClient.js`:

```javascript
import axios from 'axios';
import TokenStorage from './tokenStorage';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8081';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json'
  }
});

// Add token to requests
apiClient.interceptors.request.use(
  (config) => {
    const token = TokenStorage.getAccessToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;

      // Add user ID header
      const user = TokenStorage.getUser();
      if (user && user.id) {
        config.headers['X-User-Id'] = user.id;
      }
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Auto refresh on 401
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        const refreshToken = TokenStorage.getRefreshToken();
        const response = await axios.post(
          `${API_BASE_URL}/auth/otp/refresh`,
          { refreshToken }
        );

        const { accessToken, refreshToken: newRefreshToken, expiresIn } =
          response.data.data;

        TokenStorage.saveTokens(accessToken, newRefreshToken, expiresIn);

        originalRequest.headers.Authorization = `Bearer ${accessToken}`;
        return apiClient(originalRequest);

      } catch (refreshError) {
        TokenStorage.clearTokens();
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

export default apiClient;
```

### Step 4: Create Auth Service

Create `sample-frontend/src/services/authService.js`:

```javascript
import apiClient from '../utils/apiClient';
import TokenStorage from '../utils/tokenStorage';

class AuthService {
  /**
   * Send OTP
   */
  async sendOtp(identifier, type = 'PHONE') {
    const payload = type === 'PHONE'
      ? { phoneNumber: identifier, type }
      : { email: identifier, type: 'EMAIL' };

    const response = await apiClient.post('/auth/otp/send', payload);
    return response.data;
  }

  /**
   * Verify OTP and login
   */
  async verifyOtp(identifier, otp, additionalData = {}) {
    const payload = {
      identifier,
      otp,
      phoneNumber: additionalData.phoneNumber || identifier,
      email: additionalData.email,
      name: additionalData.name || 'Web User'
    };

    const response = await apiClient.post('/auth/otp/verify', payload);

    if (response.data.success) {
      const { accessToken, refreshToken, expiresIn, user } = response.data.data;

      TokenStorage.saveTokens(accessToken, refreshToken, expiresIn);
      TokenStorage.saveUser(user);

      return response.data;
    }

    throw new Error(response.data.message || 'Verification failed');
  }

  /**
   * Logout
   */
  logout() {
    TokenStorage.clearTokens();
    window.location.href = '/login';
  }

  /**
   * Check if authenticated
   */
  isAuthenticated() {
    return !!TokenStorage.getAccessToken() && !TokenStorage.isTokenExpired();
  }

  /**
   * Get current user
   */
  getUser() {
    return TokenStorage.getUser();
  }

  /**
   * Make authenticated API call
   */
  async callApi(endpoint, method = 'GET', data = null) {
    const config = { method, url: endpoint };
    if (data) {
      config.data = data;
    }
    const response = await apiClient.request(config);
    return response.data;
  }
}

export default new AuthService();
```

### Step 5: Create Login Component

Create `sample-frontend/src/components/OtpLogin.jsx`:

```jsx
import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AuthService from '../services/authService';
import './OtpLogin.css';

function OtpLogin() {
  const navigate = useNavigate();
  const [step, setStep] = useState(1);
  const [phoneNumber, setPhoneNumber] = useState('');
  const [otp, setOtp] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [debugOtp, setDebugOtp] = useState('');

  const handleSendOtp = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setDebugOtp('');

    try {
      const response = await AuthService.sendOtp(phoneNumber, 'PHONE');
      console.log('OTP sent:', response);

      // Show debug OTP if in testing mode
      if (response.data.otp_debug) {
        setDebugOtp(response.data.otp_debug);
        alert(`🧪 Test Mode\nYour OTP: ${response.data.otp_debug}`);
      }

      setStep(2);
    } catch (err) {
      console.error('Send OTP error:', err);
      setError(err.response?.data?.message || 'Failed to send OTP');
    } finally {
      setLoading(false);
    }
  };

  const handleVerifyOtp = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      const response = await AuthService.verifyOtp(phoneNumber, otp, {
        phoneNumber,
        name: 'Web User'
      });

      console.log('Login successful:', response);

      // Redirect to dashboard
      navigate('/dashboard');

    } catch (err) {
      console.error('Verify OTP error:', err);
      setError(err.response?.data?.message || 'Invalid OTP');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="otp-login-container">
      <div className="otp-login-card">
        <h2>🔐 Login with OTP</h2>
        <p className="subtitle">Powered by OTP Auth Starter</p>

        {step === 1 ? (
          <form onSubmit={handleSendOtp}>
            <div className="form-group">
              <label>Phone Number</label>
              <input
                type="tel"
                placeholder="+919876543210"
                value={phoneNumber}
                onChange={(e) => setPhoneNumber(e.target.value)}
                required
                className="form-input"
              />
              <small>Enter with country code (e.g., +91)</small>
            </div>

            {error && <div className="error-message">{error}</div>}

            <button type="submit" disabled={loading} className="btn-primary">
              {loading ? 'Sending...' : 'Send OTP'}
            </button>
          </form>
        ) : (
          <form onSubmit={handleVerifyOtp}>
            <div className="form-group">
              <label>Enter OTP</label>
              <input
                type="text"
                placeholder="123456"
                maxLength="6"
                value={otp}
                onChange={(e) => setOtp(e.target.value.replace(/\D/g, ''))}
                required
                className="form-input otp-input"
                autoFocus
              />
              <small>
                OTP sent to {phoneNumber}
                {debugOtp && <span className="debug-otp"> | Debug: {debugOtp}</span>}
              </small>
            </div>

            {error && <div className="error-message">{error}</div>}

            <button type="submit" disabled={loading} className="btn-primary">
              {loading ? 'Verifying...' : 'Verify OTP'}
            </button>

            <button
              type="button"
              onClick={() => { setStep(1); setOtp(''); setError(''); }}
              className="btn-secondary"
            >
              Change Number
            </button>
          </form>
        )}
      </div>
    </div>
  );
}

export default OtpLogin;
```

### Step 6: Create Styles

Create `sample-frontend/src/components/OtpLogin.css`:

```css
.otp-login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.otp-login-card {
  background: white;
  padding: 40px;
  border-radius: 16px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
  width: 100%;
  max-width: 400px;
}

.otp-login-card h2 {
  margin: 0 0 8px 0;
  text-align: center;
  color: #333;
}

.subtitle {
  text-align: center;
  color: #666;
  font-size: 14px;
  margin-bottom: 24px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 600;
  color: #333;
}

.form-input {
  width: 100%;
  padding: 12px;
  border: 2px solid #ddd;
  border-radius: 8px;
  font-size: 16px;
  transition: border-color 0.3s;
}

.form-input:focus {
  outline: none;
  border-color: #667eea;
}

.otp-input {
  font-size: 24px;
  letter-spacing: 8px;
  text-align: center;
  font-weight: bold;
}

.form-group small {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: #666;
}

.debug-otp {
  color: #4CAF50;
  font-weight: bold;
}

.error-message {
  background: #fee;
  color: #c33;
  padding: 12px;
  border-radius: 8px;
  margin-bottom: 16px;
  font-size: 14px;
}

.btn-primary,
.btn-secondary {
  width: 100%;
  padding: 14px;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  margin-bottom: 12px;
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-secondary {
  background: transparent;
  color: #667eea;
  border: 2px solid #667eea;
}

.btn-secondary:hover {
  background: #667eea;
  color: white;
}
```

### Step 7: Update App Routing

Update `sample-frontend/src/App.jsx`:

```jsx
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import OtpLogin from './components/OtpLogin';
import Dashboard from './components/Dashboard'; // Your existing dashboard
import AuthService from './services/authService';

// Protected Route Component
function ProtectedRoute({ children }) {
  if (!AuthService.isAuthenticated()) {
    return <Navigate to="/login" replace />;
  }
  return children;
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<OtpLogin />} />
        <Route
          path="/dashboard"
          element={
            <ProtectedRoute>
              <Dashboard />
            </ProtectedRoute>
          }
        />
        <Route path="/" element={<Navigate to="/login" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
```

### Step 8: Create .env File

Create `sample-frontend/.env`:

```env
VITE_API_URL=http://localhost:8081
```

### Step 9: Run the Frontend

```bash
cd sample-frontend
npm install
npm run dev
```

Open http://localhost:5173/login

---

## 🧪 Testing the Complete Flow

### 1. Start All Services

```bash
# Terminal 1: Start Keycloak
docker run -p 8180:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:latest start-dev

# Terminal 2: Start Backend
cd sample-backend
mvn spring-boot:run

# Terminal 3: Start Frontend
cd sample-frontend
npm run dev
```

### 2. Setup Keycloak

1. Go to http://localhost:8180
2. Login with `admin` / `admin`
3. Create realm: `sample-realm`
4. Create client: `sample-app`
5. Enable "Client authentication"
6. Enable "Direct access grants"
7. Copy client secret to `application.properties`

### 3. Test Login Flow

1. Open http://localhost:5173/login
2. Enter phone: `+919876543210`
3. Click "Send OTP"
4. Alert shows: "Your OTP: 123456"
5. Enter OTP: `123456`
6. Click "Verify OTP"
7. ✅ Redirected to dashboard!

### 4. Check Logs

**Backend Console:**
```
┌─────────────────────────────────────┐
│     🔐 OTP TESTING MODE            │
├─────────────────────────────────────┤
│ Phone: +919876543210                │
│ OTP:   123456                       │
│ Valid: 5 minutes                    │
└─────────────────────────────────────┘

Creating new user: +919876543210
User created with ID: abc-123-def
```

**OTP File:**
Check `sample-backend/otp_logs/otp_2025-03-10.txt`

---

## 🔄 Switching from oidcplugplay to OTP Auth Starter

### Comparison

| Feature | oidcplugplay | OTP Auth Starter |
|---------|--------------|------------------|
| Authentication | OAuth2 redirect | OTP-based |
| User Creation | Manual | Automatic |
| Frontend | Username/Password form | Phone/OTP form |
| Testing | OAuth2 credentials | Fixed OTP |
| Keycloak | Optional | Integrated |

### Migration Steps

1. **Keep both starters** (they don't conflict)
2. **Use different endpoints:**
   - OAuth2: `/oauth2/*`
   - OTP: `/auth/otp/*`
3. **Choose per endpoint:** Some users use OAuth2, others use OTP
4. **Or switch completely:** Remove `oidcplugplay` dependency

---

## 🚀 Production Deployment

### 1. Disable Testing Mode

```properties
otp.auth.otp.testing-mode=false
```

### 2. Use Environment Variables

```properties
otp.auth.keycloak.server-url=${KEYCLOAK_URL:http://localhost:8180}
otp.auth.keycloak.client-secret=${KEYCLOAK_SECRET}
spring.datasource.url=${DATABASE_URL}
spring.datasource.password=${DB_PASSWORD}
spring.mail.password=${MAIL_PASSWORD}
```

### 3. Set Environment Variables

```bash
export KEYCLOAK_URL=https://keycloak.yourapp.com
export KEYCLOAK_SECRET=prod-secret
export DATABASE_URL=jdbc:postgresql://prod-db:5432/appdb
export DB_PASSWORD=prod-db-password
export MAIL_PASSWORD=smtp-password
```

---

## 📊 Summary

**What you get:**

✅ **Backend** - Just add dependency + properties
✅ **Frontend** - Copy 3 files (tokenStorage, apiClient, authService)
✅ **OTP Endpoints** - Automatically available at `/auth/otp/*`
✅ **Testing Mode** - Fixed OTP 123456, no SMS needed
✅ **Keycloak Integration** - Auto user creation and JWT tokens
✅ **Production Ready** - Just disable testing mode

**Total effort:** ~30 minutes to integrate! 🎉

---

## 🆘 Troubleshooting

### Issue: "Cannot resolve dependency"

```bash
cd otp-auth-starter
mvn clean install -U

cd sample-backend
mvn clean install
```

### Issue: "Keycloak connection failed"

- Check Keycloak is running: http://localhost:8180
- Verify `server-url`, `realm`, `client-id`, `client-secret`

### Issue: "OTP not found"

- Check backend console logs
- Check `otp_logs/` directory
- Verify testing mode is enabled

### Issue: "Frontend can't connect"

- Check backend is running on port 8081
- Verify CORS origins in `application.properties`
- Check browser console for errors

---

**You're all set! Your sample projects now have OTP authentication! 🚀**
