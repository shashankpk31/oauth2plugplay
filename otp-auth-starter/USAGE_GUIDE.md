# 📖 Complete Usage Guide - OTP Auth Starter

## Table of Contents

1. [Backend Integration](#backend-integration)
2. [Frontend Integration - React Web](#frontend-integration---react-web)
3. [Frontend Integration - React Native](#frontend-integration---react-native)
4. [API Reference](#api-reference)
5. [Configuration Examples](#configuration-examples)

---

## Backend Integration

### Step 1: Create a New Spring Boot Project

```bash
spring init --dependencies=web,data-jpa,postgresql my-app
cd my-app
```

### Step 2: Add the OTP Auth Starter Dependency

Edit `pom.xml`:

```xml
<dependencies>
    <!-- Your existing dependencies -->

    <!-- OTP Auth Starter -->
    <dependency>
        <groupId>com.shashankpk</groupId>
        <artifactId>otp-auth-spring-boot-starter</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

### Step 3: Configure application.properties

```properties
# Server Configuration
server.port=8081

# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/myappdb
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# OTP Auth Configuration
otp.auth.enabled=true

# Keycloak Configuration
otp.auth.keycloak.server-url=http://localhost:8180
otp.auth.keycloak.realm=myapp-realm
otp.auth.keycloak.client-id=myapp-client
otp.auth.keycloak.client-secret=YOUR_CLIENT_SECRET
otp.auth.keycloak.admin-username=admin
otp.auth.keycloak.admin-password=admin
otp.auth.keycloak.default-role=user

# OTP Configuration
otp.auth.otp.testing-mode=true
otp.auth.otp.fixed-otp=123456
otp.auth.otp.expiry-seconds=300
otp.auth.otp.max-attempts=3
otp.auth.otp.enable-file-logging=true
otp.auth.otp.test-email-recipient=your-test@example.com

# Email Configuration (Optional)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# CORS Configuration
otp.auth.cors.enabled=true
otp.auth.cors.allowed-origins=http://localhost:3000,http://localhost:5173,http://localhost:19006
otp.auth.cors.allow-credentials=true

# Logging
logging.level.com.shashankpk.otpauth=DEBUG
logging.level.org.springframework.security=INFO
```

### Step 4: Create Your Main Application Class

```java
package com.example.myapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MyAppApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyAppApplication.class, args);
    }
}
```

### Step 5: (Optional) Create Your Own Controllers

```java
package com.example.myapp.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class MyController {

    @GetMapping("/hello")
    public String hello(@RequestHeader("X-User-Id") String userId) {
        return "Hello from authenticated user: " + userId;
    }

    @GetMapping("/data")
    public Map<String, Object> getData(@RequestHeader("X-User-Id") String userId) {
        // Your business logic here
        return Map.of(
            "userId", userId,
            "data", "Your protected data"
        );
    }
}
```

### Step 6: Run Your Application

```bash
mvn spring-boot:run
```

**That's it!** Your application now has:
- ✅ OTP authentication endpoints at `/auth/otp/*`
- ✅ Auto user creation in Keycloak
- ✅ JWT token management
- ✅ Database tables auto-created

---

## Frontend Integration - React Web

### Project Setup

```bash
npm create vite@latest my-app -- --template react
cd my-app
npm install axios
```

### 1. Create Token Storage Utility

Create `src/utils/tokenStorage.js`:

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

### 2. Create API Client

Create `src/utils/apiClient.js`:

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

      const user = TokenStorage.getUser();
      if (user && user.id) {
        config.headers['X-User-Id'] = user.id;
      }
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Auto refresh token on 401
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

### 3. Create Auth Service

Create `src/services/authService.js`:

```javascript
import apiClient from '../utils/apiClient';
import TokenStorage from '../utils/tokenStorage';

class AuthService {
  async sendOtp(identifier, type = 'PHONE') {
    const payload = type === 'PHONE'
      ? { phoneNumber: identifier, type }
      : { email: identifier, type: 'EMAIL' };

    const response = await apiClient.post('/auth/otp/send', payload);
    return response.data;
  }

  async verifyOtp(identifier, otp, additionalData = {}) {
    const payload = {
      identifier,
      otp,
      phoneNumber: additionalData.phoneNumber || identifier,
      email: additionalData.email,
      name: additionalData.name
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

  logout() {
    TokenStorage.clearTokens();
    window.location.href = '/login';
  }

  isAuthenticated() {
    return !!TokenStorage.getAccessToken() && !TokenStorage.isTokenExpired();
  }

  getUser() {
    return TokenStorage.getUser();
  }
}

export default new AuthService();
```

### 4. Create Login Component

Create `src/components/Login.jsx`:

```jsx
import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AuthService from '../services/authService';
import './Login.css';

function Login() {
  const navigate = useNavigate();
  const [step, setStep] = useState(1);
  const [phoneNumber, setPhoneNumber] = useState('');
  const [otp, setOtp] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSendOtp = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      const response = await AuthService.sendOtp(phoneNumber, 'PHONE');

      if (response.data.otp_debug) {
        alert(`Test Mode - OTP: ${response.data.otp_debug}`);
      }

      setStep(2);
    } catch (err) {
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
      await AuthService.verifyOtp(phoneNumber, otp, {
        phoneNumber,
        name: 'Web User'
      });

      navigate('/dashboard');
    } catch (err) {
      setError(err.response?.data?.message || 'Invalid OTP');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-container">
      <div className="login-card">
        <h2>🔐 Login with OTP</h2>

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
              />
            </div>

            {error && <div className="error">{error}</div>}

            <button type="submit" disabled={loading}>
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
                onChange={(e) => setOtp(e.target.value)}
                required
              />
            </div>

            {error && <div className="error">{error}</div>}

            <button type="submit" disabled={loading}>
              {loading ? 'Verifying...' : 'Verify OTP'}
            </button>

            <button type="button" onClick={() => setStep(1)}>
              Change Number
            </button>
          </form>
        )}
      </div>
    </div>
  );
}

export default Login;
```

### 5. Create Protected Route

Create `src/components/ProtectedRoute.jsx`:

```jsx
import React from 'react';
import { Navigate } from 'react-router-dom';
import AuthService from '../services/authService';

function ProtectedRoute({ children }) {
  if (!AuthService.isAuthenticated()) {
    return <Navigate to="/login" replace />;
  }
  return children;
}

export default ProtectedRoute;
```

### 6. Setup Routing

Update `src/App.jsx`:

```jsx
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Login from './components/Login';
import Dashboard from './components/Dashboard';
import ProtectedRoute from './components/ProtectedRoute';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route
          path="/dashboard"
          element={
            <ProtectedRoute>
              <Dashboard />
            </ProtectedRoute>
          }
        />
        <Route path="/" element={<Login />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
```

### 7. Create Environment File

Create `.env`:

```env
VITE_API_URL=http://localhost:8081
```

---

## Frontend Integration - React Native

### Project Setup

```bash
npx create-expo-app my-app
cd my-app
npx expo install expo-secure-store
npm install axios
```

### 1. Create Secure Storage

Create `utils/secureStorage.js`:

```javascript
import * as SecureStore from 'expo-secure-store';

class SecureStorage {
  static async saveTokens(accessToken, refreshToken, expiresIn) {
    await SecureStore.setItemAsync('accessToken', accessToken);
    await SecureStore.setItemAsync('refreshToken', refreshToken);
    await SecureStore.setItemAsync('tokenExpiry',
      (Date.now() + (expiresIn * 1000)).toString()
    );
  }

  static async getAccessToken() {
    return await SecureStore.getItemAsync('accessToken');
  }

  static async getRefreshToken() {
    return await SecureStore.getItemAsync('refreshToken');
  }

  static async clearTokens() {
    await SecureStore.deleteItemAsync('accessToken');
    await SecureStore.deleteItemAsync('refreshToken');
    await SecureStore.deleteItemAsync('tokenExpiry');
    await SecureStore.deleteItemAsync('user');
  }

  static async saveUser(user) {
    await SecureStore.setItemAsync('user', JSON.stringify(user));
  }

  static async getUser() {
    const user = await SecureStore.getItemAsync('user');
    return user ? JSON.parse(user) : null;
  }
}

export default SecureStorage;
```

### 2. Create API Client

Create `utils/apiClient.js`:

```javascript
import axios from 'axios';
import SecureStorage from './secureStorage';

const API_BASE_URL = 'http://YOUR_COMPUTER_IP:8081';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 10000
});

apiClient.interceptors.request.use(
  async (config) => {
    const token = await SecureStorage.getAccessToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;

      const user = await SecureStorage.getUser();
      if (user && user.id) {
        config.headers['X-User-Id'] = user.id;
      }
    }
    return config;
  },
  (error) => Promise.reject(error)
);

export default apiClient;
```

### 3. Create Login Screen

Create `screens/LoginScreen.js`:

```javascript
import React, { useState } from 'react';
import { View, Text, TextInput, TouchableOpacity, Alert, StyleSheet } from 'react-native';
import apiClient from '../utils/apiClient';
import SecureStorage from '../utils/secureStorage';

export default function LoginScreen({ navigation }) {
  const [step, setStep] = useState(1);
  const [phoneNumber, setPhoneNumber] = useState('');
  const [otp, setOtp] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSendOtp = async () => {
    setLoading(true);

    try {
      const response = await apiClient.post('/auth/otp/send', {
        phoneNumber,
        type: 'PHONE'
      });

      if (response.data.data.otp_debug) {
        Alert.alert('Test OTP', response.data.data.otp_debug);
      }

      setStep(2);
    } catch (error) {
      Alert.alert('Error', 'Failed to send OTP');
    } finally {
      setLoading(false);
    }
  };

  const handleVerifyOtp = async () => {
    setLoading(true);

    try {
      const response = await apiClient.post('/auth/otp/verify', {
        identifier: phoneNumber,
        otp,
        phoneNumber,
        name: 'Mobile User'
      });

      const { accessToken, refreshToken, expiresIn, user } = response.data.data;
      await SecureStorage.saveTokens(accessToken, refreshToken, expiresIn);
      await SecureStorage.saveUser(user);

      navigation.replace('Home');
    } catch (error) {
      Alert.alert('Error', 'Invalid OTP');
    } finally {
      setLoading(false);
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>🔐 Login with OTP</Text>

      {step === 1 ? (
        <>
          <TextInput
            style={styles.input}
            placeholder="+919876543210"
            value={phoneNumber}
            onChangeText={setPhoneNumber}
            keyboardType="phone-pad"
          />
          <TouchableOpacity style={styles.button} onPress={handleSendOtp}>
            <Text style={styles.buttonText}>Send OTP</Text>
          </TouchableOpacity>
        </>
      ) : (
        <>
          <TextInput
            style={styles.input}
            placeholder="Enter OTP"
            value={otp}
            onChangeText={setOtp}
            keyboardType="number-pad"
            maxLength={6}
          />
          <TouchableOpacity style={styles.button} onPress={handleVerifyOtp}>
            <Text style={styles.buttonText}>Verify OTP</Text>
          </TouchableOpacity>
        </>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 20, justifyContent: 'center' },
  title: { fontSize: 24, fontWeight: 'bold', marginBottom: 30, textAlign: 'center' },
  input: { borderWidth: 1, borderColor: '#ddd', padding: 15, borderRadius: 8, marginBottom: 15 },
  button: { backgroundColor: '#4CAF50', padding: 15, borderRadius: 8, alignItems: 'center' },
  buttonText: { color: '#fff', fontSize: 16, fontWeight: 'bold' }
});
```

---

## API Reference

### POST /auth/otp/send

Send OTP to phone or email.

**Request:**
```json
{
  "phoneNumber": "+919876543210",
  "type": "PHONE"
}
```

**Response:**
```json
{
  "success": true,
  "message": "OTP sent successfully",
  "data": {
    "identifier": "+919876543210",
    "expiresIn": 300,
    "otpSent": true,
    "otp_debug": "123456"
  }
}
```

### POST /auth/otp/verify

Verify OTP and login/register user.

**Request:**
```json
{
  "identifier": "+919876543210",
  "otp": "123456",
  "phoneNumber": "+919876543210",
  "name": "John Doe"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Authentication successful",
  "data": {
    "accessToken": "eyJhbGc...",
    "refreshToken": "eyJhbGc...",
    "expiresIn": 3600,
    "tokenType": "Bearer",
    "user": {
      "id": "uuid",
      "phoneNumber": "+919876543210",
      "name": "John Doe"
    },
    "isNewUser": true
  }
}
```

### POST /auth/otp/refresh

Refresh access token.

**Request:**
```json
{
  "refreshToken": "eyJhbGc..."
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "access_token": "eyJhbGc...",
    "refresh_token": "eyJhbGc...",
    "expires_in": 3600
  }
}
```

---

## Configuration Examples

### Development Configuration

```properties
# Use testing mode with console logging
otp.auth.otp.testing-mode=true
otp.auth.otp.fixed-otp=123456
otp.auth.otp.enable-file-logging=true

# Allow all origins
otp.auth.cors.allowed-origins=*

# Show SQL for debugging
spring.jpa.show-sql=true
logging.level.com.shashankpk.otpauth=DEBUG
```

### Production Configuration

```properties
# Disable testing mode
otp.auth.otp.testing-mode=false

# Restrict CORS
otp.auth.cors.allowed-origins=https://myapp.com

# Use environment variables
otp.auth.keycloak.server-url=${KEYCLOAK_URL}
otp.auth.keycloak.client-secret=${KEYCLOAK_SECRET}
spring.datasource.url=${DATABASE_URL}
spring.mail.password=${MAIL_PASSWORD}

# Production logging
logging.level.com.shashankpk.otpauth=INFO
spring.jpa.show-sql=false
```

---

**That's everything you need to integrate the OTP Auth Starter!** 🚀
