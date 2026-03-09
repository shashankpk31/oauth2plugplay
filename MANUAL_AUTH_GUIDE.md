# Manual Authentication Implementation Guide

This guide shows how to implement authentication manually in your backend and frontend when you disable the OAuth2 starter library by setting `oauth2.enabled=false`.

## When to Use Manual Authentication

Set `oauth2.enabled=false` when you want to:
- Implement custom authentication logic
- Use a different authentication mechanism (Basic Auth, API Keys, etc.)
- Develop locally without OAuth2 provider
- Migrate from the OAuth2 library to custom implementation
- Have complete control over authentication flow

## Overview

When `oauth2.enabled=false`, the OAuth2 starter is completely bypassed. You need to:
1. **Backend**: Implement your own authentication and security configuration
2. **Frontend**: Implement your own authentication logic and API calls

---

## Backend Implementation (Spring Boot)

When the library is disabled, you must handle authentication yourself.

### Step 1: Create Custom Security Configuration

```java
package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Disable CSRF for REST APIs
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // No sessions
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/api/public/**").permitAll() // Public endpoints
                .requestMatchers("/api/admin/**").hasRole("ADMIN") // Admin only
                .anyRequest().authenticated() // All other endpoints require authentication
            )
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### Step 2: Create JWT Utility Class

```java
package com.example.demo.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    @Value("${jwt.secret:mySecretKey12345678901234567890}") // At least 32 characters
    private String secret;

    @Value("${jwt.expiration:3600000}") // 1 hour in milliseconds
    private long expiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    // Generate token
    public String generateToken(String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // Extract username from token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Extract role from token
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    // Extract expiration date
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // Extract specific claim
    public <T> T extractClaim(String token, java.util.function.Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // Check if token is expired
    public Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // Validate token
    public Boolean validateToken(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }
}
```

### Step 3: Create JWT Authentication Filter

```java
package com.example.demo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Extract token from Authorization header
        String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String token = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            token = authorizationHeader.substring(7);
            try {
                username = jwtUtil.extractUsername(token);
            } catch (Exception e) {
                // Invalid token
                logger.error("JWT token validation failed", e);
            }
        }

        // Validate token and set authentication
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            if (jwtUtil.validateToken(token, username)) {
                String role = jwtUtil.extractRole(token);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                username,
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                        );

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}
```

### Step 4: Create Authentication Controller

```java
package com.example.demo.controller;

import com.example.demo.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // In real application, fetch from database
    private final Map<String, User> users = new HashMap<>() {{
        put("user", new User("user", passwordEncoder.encode("password"), "USER"));
        put("admin", new User("admin", passwordEncoder.encode("admin123"), "ADMIN"));
    }};

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        User user = users.get(request.getUsername());

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());

        return ResponseEntity.ok(Map.of(
                "access_token", token,
                "token_type", "Bearer",
                "expires_in", 3600
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7); // Remove "Bearer "
            String username = jwtUtil.extractUsername(token);
            String role = jwtUtil.extractRole(token);

            return ResponseEntity.ok(Map.of(
                    "username", username,
                    "role", role
            ));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
        }
    }

    // DTOs
    public static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    private static class User {
        private String username;
        private String password;
        private String role;

        public User(String username, String password, String role) {
            this.username = username;
            this.password = password;
            this.role = role;
        }

        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public String getRole() { return role; }
    }
}
```

### Step 5: Add Required Dependencies

Add to `pom.xml` (if not already present):

```xml
<dependencies>
    <!-- JWT -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.12.3</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>0.12.3</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <version>0.12.3</version>
        <scope>runtime</scope>
    </dependency>

    <!-- Spring Security -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
</dependencies>
```

### Step 6: Configure Application Properties

```properties
# Disable OAuth2 starter
oauth2.enabled=false

# JWT Configuration
jwt.secret=myVeryLongSecretKeyThatIsAtLeast32CharactersLong123456
jwt.expiration=3600000

# CORS
spring.web.cors.allowed-origins=http://localhost:3000,http://localhost:5173
spring.web.cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS
spring.web.cors.allowed-headers=*
spring.web.cors.allow-credentials=true
```

---

## Frontend Implementation (React)

When the backend uses custom authentication, update your frontend accordingly.

### Step 1: Create Authentication Service

Create `src/services/authService.js`:

```javascript
const API_BASE_URL = 'http://localhost:8081';

class AuthService {
  // Login with username and password
  async login(username, password) {
    try {
      const response = await fetch(`${API_BASE_URL}/api/auth/login`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ username, password }),
      });

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || 'Login failed');
      }

      const data = await response.json();

      // Store token in localStorage
      localStorage.setItem('access_token', data.access_token);

      return data;
    } catch (error) {
      console.error('Login error:', error);
      throw error;
    }
  }

  // Logout
  logout() {
    localStorage.removeItem('access_token');
    window.location.href = '/';
  }

  // Get current user
  async getCurrentUser() {
    const token = this.getToken();

    if (!token) {
      throw new Error('No token found');
    }

    try {
      const response = await fetch(`${API_BASE_URL}/api/auth/me`, {
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        throw new Error('Failed to fetch user');
      }

      return await response.json();
    } catch (error) {
      console.error('Get current user error:', error);
      this.logout();
      throw error;
    }
  }

  // Make authenticated API call
  async fetchWithAuth(url, options = {}) {
    const token = this.getToken();

    if (!token) {
      throw new Error('No authentication token');
    }

    const headers = {
      ...options.headers,
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    };

    try {
      const response = await fetch(url, {
        ...options,
        headers,
      });

      if (response.status === 401) {
        // Token expired or invalid
        this.logout();
        throw new Error('Session expired');
      }

      return response;
    } catch (error) {
      console.error('API call error:', error);
      throw error;
    }
  }

  // Get token from storage
  getToken() {
    return localStorage.getItem('access_token');
  }

  // Check if user is authenticated
  isAuthenticated() {
    return !!this.getToken();
  }
}

export default new AuthService();
```

### Step 2: Create Login Page

Create `src/pages/LoginPage.jsx`:

```javascript
import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import authService from '../services/authService';

function LoginPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      await authService.login(username, password);
      navigate('/dashboard');
    } catch (err) {
      setError(err.message || 'Login failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ maxWidth: '400px', margin: '100px auto', padding: '20px' }}>
      <h2>Login</h2>

      {error && (
        <div style={{ color: 'red', marginBottom: '10px' }}>
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit}>
        <div style={{ marginBottom: '15px' }}>
          <label>Username:</label>
          <input
            type="text"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            required
            style={{ width: '100%', padding: '8px', marginTop: '5px' }}
          />
        </div>

        <div style={{ marginBottom: '15px' }}>
          <label>Password:</label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            style={{ width: '100%', padding: '8px', marginTop: '5px' }}
          />
        </div>

        <button
          type="submit"
          disabled={loading}
          style={{
            width: '100%',
            padding: '10px',
            backgroundColor: '#007bff',
            color: 'white',
            border: 'none',
            cursor: loading ? 'not-allowed' : 'pointer',
          }}
        >
          {loading ? 'Logging in...' : 'Login'}
        </button>
      </form>

      <div style={{ marginTop: '20px', fontSize: '14px', color: '#666' }}>
        <p>Test credentials:</p>
        <p>User: user / password</p>
        <p>Admin: admin / admin123</p>
      </div>
    </div>
  );
}

export default LoginPage;
```

### Step 3: Create Protected Route Component

Create `src/components/PrivateRoute.jsx`:

```javascript
import React from 'react';
import { Navigate } from 'react-router-dom';
import authService from '../services/authService';

function PrivateRoute({ children }) {
  if (!authService.isAuthenticated()) {
    return <Navigate to="/" replace />;
  }

  return children;
}

export default PrivateRoute;
```

### Step 4: Create API Service

Create `src/services/apiService.js`:

```javascript
import authService from './authService';

const API_BASE_URL = 'http://localhost:8081';

class ApiService {
  // Get user profile
  async getProfile() {
    const response = await authService.fetchWithAuth(
      `${API_BASE_URL}/api/profile`
    );

    if (!response.ok) {
      throw new Error('Failed to fetch profile');
    }

    return response.json();
  }

  // Get protected data
  async getProtectedData() {
    const response = await authService.fetchWithAuth(
      `${API_BASE_URL}/api/data`
    );

    if (!response.ok) {
      throw new Error('Failed to fetch data');
    }

    return response.json();
  }

  // Admin-only operation
  async deleteUser(userId) {
    const response = await authService.fetchWithAuth(
      `${API_BASE_URL}/api/admin/users/${userId}`,
      {
        method: 'DELETE',
      }
    );

    if (!response.ok) {
      throw new Error('Failed to delete user');
    }

    return true;
  }
}

export default new ApiService();
```

### Step 5: Update App Router

Update `src/main.jsx` or `src/App.jsx`:

```javascript
import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import ProfilePage from './pages/ProfilePage';
import PrivateRoute from './components/PrivateRoute';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<LoginPage />} />

        <Route
          path="/dashboard"
          element={
            <PrivateRoute>
              <DashboardPage />
            </PrivateRoute>
          }
        />

        <Route
          path="/profile"
          element={
            <PrivateRoute>
              <ProfilePage />
            </PrivateRoute>
          }
        />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
```

---

## Testing Manual Authentication

### Backend Testing

```bash
# Login
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"password"}'

# Response:
# {
#   "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
#   "token_type": "Bearer",
#   "expires_in": 3600
# }

# Get current user
curl http://localhost:8081/api/auth/me \
  -H "Authorization: Bearer YOUR_TOKEN"

# Access protected endpoint
curl http://localhost:8081/api/profile \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Frontend Testing

1. Start frontend: `npm run dev`
2. Navigate to `http://localhost:5173`
3. Login with test credentials:
   - User: `user` / `password`
   - Admin: `admin` / `admin123`
4. Test protected routes

---

## Comparison: OAuth2 Library vs Manual Implementation

| Feature | OAuth2 Library (`oauth2.enabled=true`) | Manual Implementation (`oauth2.enabled=false`) |
|---------|---------------------------------------|-----------------------------------------------|
| Setup Complexity | Simple - just properties | Complex - write all code |
| OAuth2 Providers | Keycloak, Okta, Google, etc. | Not supported (custom only) |
| JWT Validation | Automatic with JWK Set | Manual implementation |
| Token Refresh | Built-in | Must implement manually |
| CORS | Auto-configured | Must configure manually |
| Security | Enterprise-grade | Depends on implementation |
| Maintenance | Library handles updates | You maintain all code |
| Use Case | Production OAuth2 apps | Custom auth, simple apps |

---

## Recommendation

- **Use `oauth2.enabled=true`**: For production applications with OAuth2 providers
- **Use `oauth2.enabled=false`**: Only when you need complete custom control or don't use OAuth2

For most applications, using the OAuth2 starter library is recommended as it provides enterprise-level security with zero code required.

---

## Additional Resources

- [Spring Security Documentation](https://docs.spring.io/spring-security/reference/)
- [JWT.io](https://jwt.io) - JWT decoder and validator
- [JJWT Library](https://github.com/jwtk/jjwt) - Java JWT library used in examples

---

**Note**: The manual implementation shown here is a simplified example. Production applications should include additional features like:
- User database integration
- Password reset functionality
- Account locking after failed attempts
- Token refresh mechanism
- Proper error handling
- Logging and monitoring
- Rate limiting
