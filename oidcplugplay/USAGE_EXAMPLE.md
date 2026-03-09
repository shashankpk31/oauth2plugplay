# Usage Example - Complete Integration Guide

This guide shows how to integrate the OAuth2/OIDC starter in a complete Spring Boot application with React frontend.

## Spring Boot Application Example

### 1. Create New Spring Boot Project

```bash
spring init --dependencies=web my-app
cd my-app
```

### 2. Add OAuth2 Starter Dependency

Edit `pom.xml`:

```xml
<dependencies>
    <!-- Other dependencies -->

    <!-- OAuth2/OIDC Starter -->
    <dependency>
        <groupId>com.shashankpk</groupId>
        <artifactId>oauth2-oidc-spring-boot-starter</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

### 3. Configure Properties

`src/main/resources/application.properties`:

```properties
# Server Configuration
server.port=8080
spring.application.name=my-app

# OAuth2 Configuration
oauth2.enabled=true
oauth2.provider=KEYCLOAK
oauth2.client-id=my-app-client
oauth2.client-secret=your-client-secret
oauth2.issuer-uri=http://localhost:8180/realms/myrealm
oauth2.custom-login-enabled=true

# CORS for React frontend
oauth2.cors.enabled=true
oauth2.cors.allowed-origins=http://localhost:3000
oauth2.cors.allow-credentials=true

# Logging
logging.level.com.shashankpk.oauth2.starter=DEBUG
```

### 4. Create Protected REST Controller

`src/main/java/com/example/myapp/controller/UserController.java`:

```java
package com.example.myapp.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class UserController {

    @GetMapping("/profile")
    public Map<String, Object> getProfile(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("username", jwt.getClaim("preferred_username"));
        profile.put("email", jwt.getClaim("email"));
        profile.put("name", jwt.getClaim("name"));
        profile.put("subject", jwt.getSubject());
        return profile;
    }

    @GetMapping("/data")
    public Map<String, String> getProtectedData(@AuthenticationPrincipal Jwt jwt) {
        return Map.of(
            "message", "This is protected data",
            "user", jwt.getClaim("preferred_username")
        );
    }
}
```

### 5. Create Public REST Controller

`src/main/java/com/example/myapp/controller/PublicController.java`:

```java
package com.example.myapp.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }

    @GetMapping("/info")
    public Map<String, String> info() {
        return Map.of(
            "app", "My Application",
            "version", "1.0.0"
        );
    }
}
```

### 6. Custom Security Configuration (Optional)

If you need custom security rules, extend the default configuration:

`src/main/java/com/example/myapp/config/SecurityConfig.java`:

```java
package com.example.myapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    /**
     * Custom security configuration for your application endpoints
     * This has lower priority than OAuth2 security config
     */
    @Bean
    @Order(2)
    public SecurityFilterChain appSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**")
            .authorizeHttpRequests(authorize -> authorize
                // Public endpoints
                .requestMatchers("/api/public/**").permitAll()
                // Protected endpoints
                .requestMatchers("/api/**").authenticated()
            );

        return http.build();
    }
}
```

### 7. Run the Application

```bash
mvn spring-boot:run
```

The OAuth2 endpoints are automatically available:
- `GET http://localhost:8080/oauth2/authorize`
- `POST http://localhost:8080/oauth2/login`
- `POST http://localhost:8080/oauth2/token`
- `POST http://localhost:8080/oauth2/refresh`
- `GET http://localhost:8080/oauth2/userinfo`

---

## React Frontend Example

### 1. Create React App

```bash
npx create-react-app my-app-frontend
cd my-app-frontend
```

### 2. Install Dependencies

```bash
npm install axios react-router-dom
```

### 3. Create Auth Service

`src/services/authService.js`:

```javascript
import axios from 'axios';

const API_URL = 'http://localhost:8080';

class AuthService {
  // Standard OAuth2 flow - redirect to provider
  async initiateLogin() {
    try {
      const response = await axios.get(`${API_URL}/oauth2/authorize`, {
        params: {
          redirect_uri: `${window.location.origin}/callback`
        }
      });

      const { authorizationUrl, state } = response.data.data;

      // Save state for validation
      localStorage.setItem('oauth_state', state);

      // Redirect to OAuth2 provider
      window.location.href = authorizationUrl;
    } catch (error) {
      console.error('Failed to initiate login:', error);
      throw error;
    }
  }

  // Handle OAuth2 callback
  async handleCallback(code, state) {
    // Validate state
    const savedState = localStorage.getItem('oauth_state');
    if (state !== savedState) {
      throw new Error('Invalid state parameter');
    }

    try {
      const response = await axios.post(`${API_URL}/oauth2/token`, null, {
        params: {
          code,
          redirect_uri: `${window.location.origin}/callback`
        }
      });

      const tokenData = response.data.data;

      // Store tokens
      this.setTokens(tokenData);

      // Clean up
      localStorage.removeItem('oauth_state');

      return tokenData;
    } catch (error) {
      console.error('Failed to exchange code for token:', error);
      throw error;
    }
  }

  // Custom login with username/password
  async login(username, password) {
    try {
      const response = await axios.post(`${API_URL}/oauth2/login`, {
        username,
        password
      });

      const tokenData = response.data.data;
      this.setTokens(tokenData);

      return tokenData;
    } catch (error) {
      console.error('Login failed:', error);
      throw error;
    }
  }

  // Refresh access token
  async refreshToken() {
    const refreshToken = localStorage.getItem('refresh_token');

    if (!refreshToken) {
      throw new Error('No refresh token available');
    }

    try {
      const response = await axios.post(`${API_URL}/oauth2/refresh`, {
        refreshToken
      });

      const tokenData = response.data.data;
      this.setTokens(tokenData);

      return tokenData;
    } catch (error) {
      console.error('Failed to refresh token:', error);
      this.logout();
      throw error;
    }
  }

  // Get current user info
  async getUserInfo() {
    try {
      const response = await axios.get(`${API_URL}/oauth2/userinfo`, {
        headers: this.getAuthHeader()
      });

      return response.data.data;
    } catch (error) {
      console.error('Failed to get user info:', error);
      throw error;
    }
  }

  // Logout
  logout() {
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    localStorage.removeItem('user_info');
    window.location.href = '/login';
  }

  // Store tokens
  setTokens(tokenData) {
    localStorage.setItem('access_token', tokenData.access_token);

    if (tokenData.refresh_token) {
      localStorage.setItem('refresh_token', tokenData.refresh_token);
    }

    if (tokenData.userInfo) {
      localStorage.setItem('user_info', JSON.stringify(tokenData.userInfo));
    }
  }

  // Get access token
  getAccessToken() {
    return localStorage.getItem('access_token');
  }

  // Get auth header
  getAuthHeader() {
    const token = this.getAccessToken();
    return token ? { Authorization: `Bearer ${token}` } : {};
  }

  // Check if user is authenticated
  isAuthenticated() {
    return !!this.getAccessToken();
  }

  // Get stored user info
  getCurrentUser() {
    const userInfo = localStorage.getItem('user_info');
    return userInfo ? JSON.parse(userInfo) : null;
  }
}

export default new AuthService();
```

### 4. Create Axios Interceptor

`src/services/axiosConfig.js`:

```javascript
import axios from 'axios';
import authService from './authService';

const API_URL = 'http://localhost:8080';

// Create axios instance
const api = axios.create({
  baseURL: API_URL
});

// Request interceptor - add auth token
api.interceptors.request.use(
  (config) => {
    const token = authService.getAccessToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor - handle token refresh
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // If 401 and not already retried, try to refresh token
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        await authService.refreshToken();

        // Retry original request with new token
        const token = authService.getAccessToken();
        originalRequest.headers.Authorization = `Bearer ${token}`;

        return api(originalRequest);
      } catch (refreshError) {
        // Refresh failed, logout
        authService.logout();
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

export default api;
```

### 5. Create Login Component (Custom Login)

`src/components/Login.jsx`:

```javascript
import React, { useState } from 'react';
import authService from '../services/authService';
import './Login.css';

function Login() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      await authService.login(username, password);
      window.location.href = '/dashboard';
    } catch (err) {
      setError(err.response?.data?.message || 'Login failed');
    } finally {
      setLoading(false);
    }
  };

  const handleOAuthLogin = () => {
    authService.initiateLogin();
  };

  return (
    <div className="login-container">
      <div className="login-box">
        <h2>Login</h2>

        {error && <div className="error-message">{error}</div>}

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Username</label>
            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
              disabled={loading}
            />
          </div>

          <div className="form-group">
            <label>Password</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              disabled={loading}
            />
          </div>

          <button type="submit" disabled={loading}>
            {loading ? 'Logging in...' : 'Login'}
          </button>
        </form>

        <div className="divider">OR</div>

        <button
          className="oauth-button"
          onClick={handleOAuthLogin}
          disabled={loading}
        >
          Login with OAuth2 Provider
        </button>
      </div>
    </div>
  );
}

export default Login;
```

### 6. Create OAuth Callback Component

`src/components/Callback.jsx`:

```javascript
import React, { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import authService from '../services/authService';

function Callback() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [error, setError] = useState('');

  useEffect(() => {
    const handleCallback = async () => {
      const code = searchParams.get('code');
      const state = searchParams.get('state');
      const errorParam = searchParams.get('error');

      if (errorParam) {
        setError(`Authentication failed: ${errorParam}`);
        setTimeout(() => navigate('/login'), 3000);
        return;
      }

      if (!code) {
        setError('No authorization code received');
        setTimeout(() => navigate('/login'), 3000);
        return;
      }

      try {
        await authService.handleCallback(code, state);
        navigate('/dashboard');
      } catch (err) {
        setError(err.message || 'Failed to complete authentication');
        setTimeout(() => navigate('/login'), 3000);
      }
    };

    handleCallback();
  }, [searchParams, navigate]);

  return (
    <div className="callback-container">
      {error ? (
        <div>
          <h2>Error</h2>
          <p>{error}</p>
          <p>Redirecting to login...</p>
        </div>
      ) : (
        <div>
          <h2>Completing authentication...</h2>
          <p>Please wait...</p>
        </div>
      )}
    </div>
  );
}

export default Callback;
```

### 7. Create Dashboard Component

`src/components/Dashboard.jsx`:

```javascript
import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import authService from '../services/authService';
import api from '../services/axiosConfig';
import './Dashboard.css';

function Dashboard() {
  const [user, setUser] = useState(null);
  const [data, setData] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    if (!authService.isAuthenticated()) {
      navigate('/login');
      return;
    }

    loadUserData();
  }, [navigate]);

  const loadUserData = async () => {
    try {
      // Get user info from stored data
      const currentUser = authService.getCurrentUser();
      setUser(currentUser);

      // Fetch protected data
      const response = await api.get('/api/data');
      setData(response.data);
    } catch (error) {
      console.error('Failed to load user data:', error);
    }
  };

  const handleLogout = async () => {
    try {
      await api.post('/oauth2/logout');
    } finally {
      authService.logout();
    }
  };

  if (!user) {
    return <div>Loading...</div>;
  }

  return (
    <div className="dashboard-container">
      <header className="dashboard-header">
        <h1>Dashboard</h1>
        <button onClick={handleLogout}>Logout</button>
      </header>

      <div className="dashboard-content">
        <div className="user-info-card">
          <h2>User Information</h2>
          <p><strong>Username:</strong> {user.username}</p>
          <p><strong>Email:</strong> {user.email}</p>
          <p><strong>Name:</strong> {user.name}</p>
        </div>

        {data && (
          <div className="data-card">
            <h2>Protected Data</h2>
            <pre>{JSON.stringify(data, null, 2)}</pre>
          </div>
        )}
      </div>
    </div>
  );
}

export default Dashboard;
```

### 8. Create App Router

`src/App.js`:

```javascript
import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Login from './components/Login';
import Callback from './components/Callback';
import Dashboard from './components/Dashboard';
import authService from './services/authService';

function PrivateRoute({ children }) {
  return authService.isAuthenticated() ? children : <Navigate to="/login" />;
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/callback" element={<Callback />} />
        <Route
          path="/dashboard"
          element={
            <PrivateRoute>
              <Dashboard />
            </PrivateRoute>
          }
        />
        <Route path="/" element={<Navigate to="/dashboard" />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
```

### 9. Run React App

```bash
npm start
```

---

## React Native Example

### Auth Service for React Native

`services/authService.js`:

```javascript
import axios from 'axios';
import * as SecureStore from 'expo-secure-store';
import * as WebBrowser from 'expo-web-browser';
import * as Linking from 'expo-linking';

const API_URL = 'http://your-backend-ip:8080';

class AuthService {
  // Custom login (recommended for mobile)
  async login(username, password) {
    try {
      const response = await axios.post(`${API_URL}/oauth2/login`, {
        username,
        password
      });

      const tokenData = response.data.data;
      await this.setTokens(tokenData);

      return tokenData;
    } catch (error) {
      throw error;
    }
  }

  // OAuth2 browser flow (alternative)
  async loginWithBrowser() {
    try {
      const redirectUri = Linking.createURL('callback');

      const response = await axios.get(`${API_URL}/oauth2/authorize`, {
        params: { redirect_uri: redirectUri }
      });

      const { authorizationUrl, state } = response.data.data;
      await SecureStore.setItemAsync('oauth_state', state);

      const result = await WebBrowser.openAuthSessionAsync(
        authorizationUrl,
        redirectUri
      );

      if (result.type === 'success') {
        const url = new URL(result.url);
        const code = url.searchParams.get('code');
        const returnedState = url.searchParams.get('state');

        const savedState = await SecureStore.getItemAsync('oauth_state');
        if (returnedState !== savedState) {
          throw new Error('Invalid state');
        }

        const tokenResponse = await axios.post(`${API_URL}/oauth2/token`, null, {
          params: { code, redirect_uri: redirectUri }
        });

        const tokenData = tokenResponse.data.data;
        await this.setTokens(tokenData);

        return tokenData;
      }

      throw new Error('Authentication cancelled');
    } catch (error) {
      throw error;
    }
  }

  async setTokens(tokenData) {
    await SecureStore.setItemAsync('access_token', tokenData.access_token);

    if (tokenData.refresh_token) {
      await SecureStore.setItemAsync('refresh_token', tokenData.refresh_token);
    }

    if (tokenData.userInfo) {
      await SecureStore.setItemAsync('user_info', JSON.stringify(tokenData.userInfo));
    }
  }

  async getAccessToken() {
    return await SecureStore.getItemAsync('access_token');
  }

  async logout() {
    await SecureStore.deleteItemAsync('access_token');
    await SecureStore.deleteItemAsync('refresh_token');
    await SecureStore.deleteItemAsync('user_info');
  }
}

export default new AuthService();
```

---

## Testing

### Test OAuth2 Endpoints

```bash
# Health check
curl http://localhost:8080/oauth2/health

# Custom login
curl -X POST http://localhost:8080/oauth2/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password"}'

# Validate token
curl http://localhost:8080/oauth2/validate \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"

# Get user info
curl http://localhost:8080/oauth2/userinfo \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"

# Refresh token
curl -X POST http://localhost:8080/oauth2/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"YOUR_REFRESH_TOKEN"}'
```

---

## Complete Flow Diagram

```
1. User opens React app → /login page

2a. Custom Login Flow:
   User enters username/password
   → POST /oauth2/login
   → Backend calls Keycloak token endpoint
   → Returns access_token + user info
   → Store tokens in localStorage
   → Redirect to /dashboard

2b. OAuth2 Standard Flow:
   User clicks "Login with Provider"
   → GET /oauth2/authorize
   → Redirect to Keycloak login page
   → User authenticates on Keycloak
   → Redirect back to /callback?code=xxx
   → POST /oauth2/token with code
   → Returns access_token + user info
   → Store tokens in localStorage
   → Redirect to /dashboard

3. Authenticated Requests:
   Frontend makes API calls with Authorization: Bearer token
   → Backend validates JWT
   → Returns protected data

4. Token Refresh:
   On 401 error → POST /oauth2/refresh
   → Get new access_token
   → Retry original request

5. Logout:
   POST /oauth2/logout
   → Clear localStorage
   → Redirect to /login
```

This complete example demonstrates a production-ready integration of the OAuth2/OIDC starter!
