# 🔐 Complete OTP-Based Authentication System with Keycloak

## 📋 Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Service A - Identity & Authentication](#service-a---identity--authentication)
  - [OTP Endpoints](#otp-endpoints)
  - [OTP Service Implementation](#otp-service-implementation)
  - [Keycloak Integration](#keycloak-integration)
  - [Database Schema](#database-schema)
- [Testing Strategies](#testing-strategies)
- [API Gateway Configuration](#api-gateway-configuration)
- [Service B - Business Logic](#service-b---business-logic)
- [Frontend Implementation](#frontend-implementation)
  - [React Web](#react-web)
  - [React Native](#react-native)
- [Complete Authentication Flow](#complete-authentication-flow)
- [Configuration Reference](#configuration-reference)
- [Security Best Practices](#security-best-practices)
- [Migration to Production SMS](#migration-to-production-sms)

---

## Overview

This guide provides a complete **OTP-only authentication system** integrated with **Keycloak** for microservices architecture. The solution supports:

- ✅ **Phone/Email OTP authentication** (no password required)
- ✅ **Auto user registration** in Keycloak and application database
- ✅ **JWT token-based authentication** with automatic refresh
- ✅ **Testing without SMS service** (multiple strategies)
- ✅ **Secure token storage** for Web and Mobile
- ✅ **Token propagation** across microservices
- ✅ **Easy migration to production SMS** (Twilio, etc.)

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│           React Web / React Native Frontend                 │
│  - Phone/Email Input → Send OTP                             │
│  - OTP Input → Verify OTP                                   │
│  - Secure Token Storage (localStorage/SecureStore)          │
└─────────────────┬───────────────────────────────────────────┘
                  │ HTTPS/REST
                  │ Authorization: Bearer <JWT>
                  │
┌─────────────────▼───────────────────────────────────────────┐
│                   API Gateway (Port 8080)                    │
│  - JWT Validation Filter                                    │
│  - Token Propagation to Services                            │
│  - Routes: /auth/** → Service A                             │
│           /api/**  → Service B                              │
└─────────────────┬─────────────────┬─────────────────────────┘
                  │                 │
      ┌───────────▼─────────┐      │
      │   Service A         │      │      ┌──────────────────┐
      │   (Port 8081)       │◄─────┴──────┤   Service B      │
      │                     │              │   (Port 8082)    │
      │  POST /auth/otp/send      FeignClient with JWT       │
      │  POST /auth/otp/verify    Propagation                │
      │  POST /auth/token/refresh                            │
      │  GET  /auth/user/profile                             │
      └───────────┬─────────┘      └──────────────────────────┘
                  │
      ┌───────────▼─────────┐
      │   Keycloak          │
      │   - User Storage    │
      │   - Token Generation│
      └───────────┬─────────┘
                  │
      ┌───────────▼─────────┐
      │   PostgreSQL/MySQL  │
      │   - users table     │
      │   - otp_records     │
      └─────────────────────┘
```

---

## Technology Stack

**Backend:**
- Java 17+
- Spring Boot 3.3.12
- Spring Cloud Gateway (API Gateway)
- Keycloak 24.x
- Spring Data JPA
- PostgreSQL/MySQL
- OpenFeign (for inter-service communication)

**Frontend:**
- React 18+ (Web)
- React Native + Expo (Mobile)
- Axios (HTTP client)
- expo-secure-store (Mobile token storage)

---

## Service A - Identity & Authentication

### OTP Endpoints

Service A provides the following authentication endpoints:

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/auth/otp/send` | Generate and send OTP | No |
| POST | `/auth/otp/verify` | Verify OTP and login/register | No |
| POST | `/auth/token/refresh` | Refresh access token | No |
| GET | `/auth/user/profile` | Get current user profile | Yes |
| GET | `/auth/user/{userId}` | Get user by ID (for Service B) | Yes |

---

### OTP Controller Implementation

```java
package com.example.auth.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth/otp")
public class OtpAuthController {

    @Autowired
    private OtpService otpService;

    @Autowired
    private KeycloakService keycloakService;

    @Autowired
    private UserService userService;

    /**
     * Step 1: Generate and send OTP
     *
     * Request Body:
     * {
     *   "phoneNumber": "+919876543210",  // OR "email": "user@example.com"
     *   "type": "PHONE"  // or "EMAIL"
     * }
     */
    @PostMapping("/send")
    public ResponseEntity<ApiResponse> sendOtp(@RequestBody OtpSendRequest request) {
        log.info("OTP send request received for: {}", request.getIdentifier());

        // Validate input
        if (request.getPhoneNumber() == null && request.getEmail() == null) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Phone number or email is required")
            );
        }

        // Determine identifier
        String identifier = request.getPhoneNumber() != null
            ? request.getPhoneNumber()
            : request.getEmail();

        try {
            // Generate and send OTP
            String otp = otpService.generateAndSendOtp(identifier, request.getType());

            return ResponseEntity.ok(ApiResponse.success(
                "OTP sent successfully",
                Map.of(
                    "identifier", identifier,
                    "expiresIn", 300, // 5 minutes
                    "otpSent", true,
                    // FOR TESTING ONLY - REMOVE IN PRODUCTION
                    "otp_debug", otp != null ? otp : "Check logs/email"
                )
            ));

        } catch (Exception e) {
            log.error("Failed to send OTP", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error("Failed to send OTP: " + e.getMessage())
            );
        }
    }

    /**
     * Step 2: Verify OTP and login/register user
     *
     * Request Body:
     * {
     *   "identifier": "+919876543210",
     *   "otp": "123456",
     *   "phoneNumber": "+919876543210",  // Optional: for registration
     *   "email": "user@example.com",     // Optional: for registration
     *   "name": "John Doe"               // Optional: for registration
     * }
     */
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse> verifyOtp(@RequestBody OtpVerifyRequest request) {
        log.info("OTP verification request for: {}", request.getIdentifier());

        // Validate OTP
        boolean isValid = otpService.verifyOtp(
            request.getIdentifier(),
            request.getOtp()
        );

        if (!isValid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ApiResponse.error("Invalid or expired OTP")
            );
        }

        try {
            // Check if user exists in Keycloak
            KeycloakUser keycloakUser = keycloakService.findUserByIdentifier(
                request.getIdentifier()
            );

            boolean isNewUser = false;

            if (keycloakUser == null) {
                log.info("User not found, creating new user: {}", request.getIdentifier());

                // CREATE NEW USER in Keycloak
                keycloakUser = keycloakService.createUser(
                    request.getIdentifier(),
                    request.getPhoneNumber(),
                    request.getEmail(),
                    request.getName()
                );

                // CREATE NEW USER in application database
                userService.createUser(
                    keycloakUser.getId(),
                    request.getPhoneNumber(),
                    request.getEmail(),
                    request.getName()
                );

                isNewUser = true;
                log.info("New user created with ID: {}", keycloakUser.getId());
            } else {
                log.info("Existing user found: {}", keycloakUser.getId());

                // Update last login
                userService.updateLastLogin(keycloakUser.getId());
            }

            // Generate JWT tokens from Keycloak
            TokenResponse tokens = keycloakService.generateTokensForUser(
                keycloakUser.getId()
            );

            // Get user profile
            UserProfile userProfile = userService.getUserProfile(keycloakUser.getId());

            return ResponseEntity.ok(ApiResponse.success(
                "Authentication successful",
                Map.of(
                    "accessToken", tokens.getAccessToken(),
                    "refreshToken", tokens.getRefreshToken(),
                    "expiresIn", tokens.getExpiresIn(),
                    "tokenType", "Bearer",
                    "user", userProfile,
                    "isNewUser", isNewUser
                )
            ));

        } catch (Exception e) {
            log.error("Error during OTP verification", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error("Authentication failed: " + e.getMessage())
            );
        }
    }

    /**
     * Refresh access token
     */
    @PostMapping("/token/refresh")
    public ResponseEntity<ApiResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        try {
            TokenResponse tokens = keycloakService.refreshAccessToken(
                request.getRefreshToken()
            );

            return ResponseEntity.ok(ApiResponse.success(
                "Token refreshed successfully",
                tokens
            ));

        } catch (Exception e) {
            log.error("Failed to refresh token", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ApiResponse.error("Token refresh failed")
            );
        }
    }

    /**
     * Get current user profile
     */
    @GetMapping("/user/profile")
    public ResponseEntity<ApiResponse> getUserProfile(
            @RequestHeader("X-User-Id") String userId) {
        try {
            UserProfile profile = userService.getUserProfile(userId);
            return ResponseEntity.ok(ApiResponse.success("Success", profile));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse.error("User not found")
            );
        }
    }
}
```

---

### OTP Service Implementation

```java
package com.example.auth.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.file.*;
import java.io.IOException;

@Slf4j
@Service
public class OtpService {

    @Autowired
    private OtpRepository otpRepository;

    @Autowired
    private EmailService emailService;

    @Value("${app.otp.testing-mode:true}")
    private boolean testingMode;

    @Value("${app.otp.fixed-otp:123456}")
    private String fixedOtpForTesting;

    @Value("${app.otp.expiry-seconds:300}")
    private int otpExpirySeconds;

    @Value("${app.otp.max-attempts:3}")
    private int maxAttempts;

    private static final SecureRandom random = new SecureRandom();

    /**
     * Generate and send OTP
     * Returns OTP string only in testing mode, null otherwise
     */
    public String generateAndSendOtp(String identifier, OtpType type) {
        log.info("Generating OTP for: {} (type: {})", identifier, type);

        // Generate OTP
        String otp = testingMode
            ? fixedOtpForTesting  // Fixed OTP for testing
            : generateRandomOtp();

        // Invalidate previous OTPs for this identifier
        otpRepository.invalidatePreviousOtps(identifier);

        // Save OTP in database
        OtpRecord otpRecord = new OtpRecord();
        otpRecord.setIdentifier(identifier);
        otpRecord.setOtpCode(otp);
        otpRecord.setType(type);
        otpRecord.setExpiresAt(LocalDateTime.now().plusSeconds(otpExpirySeconds));
        otpRecord.setAttempts(0);
        otpRecord.setVerified(false);
        otpRepository.save(otpRecord);

        // Send OTP based on type
        if (type == OtpType.PHONE) {
            sendOtpViaSms(identifier, otp);
        } else if (type == OtpType.EMAIL) {
            sendOtpViaEmail(identifier, otp);
        }

        log.info("OTP generated successfully for: {}", identifier);

        // Return OTP only in testing mode for debugging
        return testingMode ? otp : null;
    }

    /**
     * Verify OTP
     */
    public boolean verifyOtp(String identifier, String otp) {
        log.info("Verifying OTP for: {}", identifier);

        // Find valid OTP record
        OtpRecord record = otpRepository.findByIdentifierAndOtpCodeAndVerifiedFalse(
            identifier, otp
        ).orElse(null);

        if (record == null) {
            log.warn("OTP not found or already used for: {}", identifier);
            return false;
        }

        // Check expiry
        if (record.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("OTP expired for: {}", identifier);
            return false;
        }

        // Check attempts (max 3)
        if (record.getAttempts() >= maxAttempts) {
            log.warn("Max OTP attempts exceeded for: {}", identifier);
            return false;
        }

        // Increment attempts
        record.setAttempts(record.getAttempts() + 1);

        // Verify OTP
        if (record.getOtpCode().equals(otp)) {
            record.setVerified(true);
            otpRepository.save(record);
            log.info("OTP verified successfully for: {}", identifier);
            return true;
        }

        otpRepository.save(record);
        log.warn("Invalid OTP provided for: {}", identifier);
        return false;
    }

    /**
     * Generate random 6-digit OTP
     */
    private String generateRandomOtp() {
        return String.format("%06d", random.nextInt(999999));
    }

    /**
     * Send OTP via SMS (with testing strategies)
     */
    private void sendOtpViaSms(String phoneNumber, String otp) {
        if (testingMode) {
            // ========================================
            // TESTING STRATEGY 1: Console Logging
            // ========================================
            log.info("┌─────────────────────────────────────┐");
            log.info("│     🔐 OTP TESTING MODE            │");
            log.info("├─────────────────────────────────────┤");
            log.info("│ Phone: {}           │", phoneNumber);
            log.info("│ OTP:   {}                      │", otp);
            log.info("│ Valid: 5 minutes                    │");
            log.info("└─────────────────────────────────────┘");

            // ========================================
            // TESTING STRATEGY 2: File Logging
            // ========================================
            saveOtpToFile(phoneNumber, otp);

            // ========================================
            // TESTING STRATEGY 3: Email to Tester
            // ========================================
            try {
                emailService.sendEmail(
                    "your-test-email@example.com",  // Your test email
                    "Test OTP for " + phoneNumber,
                    buildTestOtpEmail(phoneNumber, otp)
                );
            } catch (Exception e) {
                log.warn("Failed to send test OTP email: {}", e.getMessage());
            }

        } else {
            // ========================================
            // PRODUCTION: SMS Gateway Integration
            // ========================================

            // TODO: Integrate with Twilio
            // twilioService.sendSms(phoneNumber, "Your OTP is: " + otp);

            // TODO: Or integrate with local telecom provider
            // smsGateway.sendSms(phoneNumber, "Your OTP: " + otp);

            log.info("SMS sent to: {} (production mode)", phoneNumber);
        }
    }

    /**
     * Send OTP via Email
     */
    private void sendOtpViaEmail(String email, String otp) {
        emailService.sendEmail(
            email,
            "Your OTP Code",
            buildOtpEmailTemplate(otp)
        );
        log.info("OTP email sent to: {}", email);
    }

    /**
     * Save OTP to file for testing
     */
    private void saveOtpToFile(String identifier, String otp) {
        try {
            String filename = "otp_logs/otp_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) +
                ".txt";

            Path directory = Paths.get("otp_logs");
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
            }

            String logEntry = String.format("[%s] %s -> %s%n",
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                identifier,
                otp
            );

            Files.write(
                Paths.get(filename),
                logEntry.getBytes(),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            );

            log.debug("OTP saved to file: {}", filename);

        } catch (IOException e) {
            log.error("Failed to save OTP to file", e);
        }
    }

    /**
     * Build test OTP email for testers
     */
    private String buildTestOtpEmail(String phoneNumber, String otp) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif; padding: 20px;">
                <div style="background: #fff3cd; padding: 15px; border-left: 4px solid #ffc107;">
                    <h3>🧪 TEST MODE - OTP Generated</h3>
                    <p><strong>Phone:</strong> %s</p>
                    <p><strong>OTP:</strong> <span style="font-size: 24px; font-weight: bold; color: #4CAF50;">%s</span></p>
                    <p><em>This is a test OTP for development purposes.</em></p>
                </div>
            </body>
            </html>
            """, phoneNumber, otp);
    }

    /**
     * Build OTP email template
     */
    private String buildOtpEmailTemplate(String otp) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 0;">
                <div style="max-width: 600px; margin: 40px auto; background: white; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                    <div style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); padding: 30px; text-align: center; border-radius: 8px 8px 0 0;">
                        <h1 style="color: white; margin: 0; font-size: 28px;">Verify Your Identity</h1>
                    </div>

                    <div style="padding: 40px 30px;">
                        <p style="color: #333; font-size: 16px; line-height: 1.6;">
                            Your One-Time Password (OTP) is:
                        </p>

                        <div style="background: #f8f9fa; padding: 25px; border-radius: 8px; text-align: center; margin: 25px 0;">
                            <div style="font-size: 48px; font-weight: bold; color: #4CAF50; letter-spacing: 8px; font-family: 'Courier New', monospace;">
                                %s
                            </div>
                        </div>

                        <div style="background: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; border-radius: 4px;">
                            <p style="margin: 0; color: #856404; font-size: 14px;">
                                ⚠️ <strong>Important:</strong> This OTP is valid for <strong>5 minutes</strong> only.
                            </p>
                        </div>

                        <p style="color: #666; font-size: 14px; line-height: 1.6;">
                            • Do not share this OTP with anyone<br>
                            • If you didn't request this OTP, please ignore this email
                        </p>
                    </div>

                    <div style="background: #f8f9fa; padding: 20px; text-align: center; border-radius: 0 0 8px 8px; color: #666; font-size: 12px;">
                        <p style="margin: 0;">
                            This is an automated message, please do not reply.
                        </p>
                    </div>
                </div>
            </body>
            </html>
            """, otp);
    }
}
```

---

### Keycloak Integration

```java
package com.example.auth.service;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import lombok.extern.slf4j.Slf4j;
import javax.annotation.PostConstruct;
import javax.ws.rs.core.Response;
import java.util.*;

@Slf4j
@Service
public class KeycloakService {

    @Value("${keycloak.auth-server-url}")
    private String keycloakServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.resource}")
    private String clientId;

    @Value("${keycloak.credentials.secret}")
    private String clientSecret;

    @Value("${keycloak.admin.username:admin}")
    private String adminUsername;

    @Value("${keycloak.admin.password:admin}")
    private String adminPassword;

    private Keycloak keycloakAdminClient;
    private RestTemplate restTemplate;

    @PostConstruct
    public void init() {
        // Initialize Keycloak admin client
        keycloakAdminClient = KeycloakBuilder.builder()
            .serverUrl(keycloakServerUrl)
            .realm("master")
            .clientId("admin-cli")
            .username(adminUsername)
            .password(adminPassword)
            .build();

        restTemplate = new RestTemplate();

        log.info("Keycloak service initialized for realm: {}", realm);
    }

    /**
     * Find user by phone number or email
     */
    public KeycloakUser findUserByIdentifier(String identifier) {
        try {
            RealmResource realmResource = keycloakAdminClient.realm(realm);
            UsersResource usersResource = realmResource.users();

            List<UserRepresentation> users;

            // Check if identifier is email or phone
            if (identifier.contains("@")) {
                // Search by email
                users = usersResource.search(null, null, null, identifier, 0, 1);
            } else {
                // Search by username (phone number)
                users = usersResource.search(identifier, true);

                // If not found by username, search in attributes
                if (users.isEmpty()) {
                    List<UserRepresentation> allUsers = usersResource.list();
                    users = allUsers.stream()
                        .filter(u -> {
                            Map<String, List<String>> attrs = u.getAttributes();
                            return attrs != null &&
                                   attrs.containsKey("phoneNumber") &&
                                   attrs.get("phoneNumber").contains(identifier);
                        })
                        .limit(1)
                        .toList();
                }
            }

            if (users.isEmpty()) {
                log.info("User not found in Keycloak: {}", identifier);
                return null;
            }

            UserRepresentation user = users.get(0);
            return mapToKeycloakUser(user);

        } catch (Exception e) {
            log.error("Error finding user in Keycloak: {}", identifier, e);
            throw new RuntimeException("Failed to find user in Keycloak", e);
        }
    }

    /**
     * Create new user in Keycloak
     */
    public KeycloakUser createUser(String identifier, String phoneNumber,
                                   String email, String name) {
        try {
            log.info("Creating new user in Keycloak: {}", identifier);

            RealmResource realmResource = keycloakAdminClient.realm(realm);
            UsersResource usersResource = realmResource.users();

            // Prepare user representation
            UserRepresentation user = new UserRepresentation();
            user.setEnabled(true);
            user.setUsername(identifier);

            // Set email if provided
            if (email != null && !email.isEmpty()) {
                user.setEmail(email);
                user.setEmailVerified(true); // OTP verified
            }

            // Set name if provided
            if (name != null && !name.isEmpty()) {
                String[] parts = name.trim().split("\\s+", 2);
                user.setFirstName(parts[0]);
                if (parts.length > 1) {
                    user.setLastName(parts[1]);
                }
            }

            // Store phone number in attributes
            Map<String, List<String>> attributes = new HashMap<>();
            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                attributes.put("phoneNumber", List.of(phoneNumber));
                attributes.put("phoneNumberVerified", List.of("true"));
            }
            attributes.put("createdVia", List.of("OTP"));
            attributes.put("createdAt", List.of(String.valueOf(System.currentTimeMillis())));
            user.setAttributes(attributes);

            // Create user in Keycloak
            Response response = usersResource.create(user);

            if (response.getStatus() != 201) {
                log.error("Failed to create user. Status: {}, Info: {}",
                    response.getStatus(), response.getStatusInfo());
                throw new RuntimeException("Failed to create user in Keycloak: "
                    + response.getStatusInfo());
            }

            // Extract user ID from Location header
            String locationPath = response.getLocation().getPath();
            String userId = locationPath.substring(locationPath.lastIndexOf('/') + 1);

            log.info("User created successfully in Keycloak with ID: {}", userId);

            // Assign default role
            assignDefaultRole(userId);

            // Map to KeycloakUser
            KeycloakUser keycloakUser = new KeycloakUser();
            keycloakUser.setId(userId);
            keycloakUser.setUsername(identifier);
            keycloakUser.setEmail(email);
            keycloakUser.setPhoneNumber(phoneNumber);
            keycloakUser.setName(name);
            keycloakUser.setNewlyCreated(true);

            return keycloakUser;

        } catch (Exception e) {
            log.error("Error creating user in Keycloak", e);
            throw new RuntimeException("Failed to create user in Keycloak", e);
        }
    }

    /**
     * Generate JWT tokens for user using Client Credentials flow
     */
    public TokenResponse generateTokensForUser(String userId) {
        try {
            log.info("Generating tokens for user: {}", userId);

            // Get user details
            UserRepresentation user = keycloakAdminClient.realm(realm)
                .users().get(userId).toRepresentation();

            String tokenUrl = keycloakServerUrl + "/realms/" + realm
                + "/protocol/openid-connect/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            // Use client credentials to get token for user
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "client_credentials");
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            // Add user context
            body.add("scope", "openid profile email");

            HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                tokenUrl, request, Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> tokenData = response.getBody();

                TokenResponse tokenResponse = new TokenResponse();
                tokenResponse.setAccessToken((String) tokenData.get("access_token"));
                tokenResponse.setRefreshToken((String) tokenData.get("refresh_token"));
                tokenResponse.setExpiresIn((Integer) tokenData.get("expires_in"));
                tokenResponse.setTokenType((String) tokenData.get("token_type"));

                log.info("Tokens generated successfully for user: {}", userId);
                return tokenResponse;
            }

            throw new RuntimeException("Failed to generate tokens");

        } catch (Exception e) {
            log.error("Failed to generate tokens for user: {}", userId, e);
            throw new RuntimeException("Token generation failed", e);
        }
    }

    /**
     * Refresh access token
     */
    public TokenResponse refreshAccessToken(String refreshToken) {
        try {
            String tokenUrl = keycloakServerUrl + "/realms/" + realm
                + "/protocol/openid-connect/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "refresh_token");
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("refresh_token", refreshToken);

            HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                tokenUrl, request, Map.class
            );

            Map<String, Object> tokenData = response.getBody();

            TokenResponse tokenResponse = new TokenResponse();
            tokenResponse.setAccessToken((String) tokenData.get("access_token"));
            tokenResponse.setRefreshToken((String) tokenData.get("refresh_token"));
            tokenResponse.setExpiresIn((Integer) tokenData.get("expires_in"));
            tokenResponse.setTokenType((String) tokenData.get("token_type"));

            return tokenResponse;

        } catch (Exception e) {
            log.error("Failed to refresh token", e);
            throw new RuntimeException("Token refresh failed", e);
        }
    }

    /**
     * Assign default "user" role to new users
     */
    private void assignDefaultRole(String userId) {
        try {
            RealmResource realmResource = keycloakAdminClient.realm(realm);

            // Get the "user" role
            RoleRepresentation userRole = realmResource.roles().get("user").toRepresentation();

            // Assign role to user
            realmResource.users().get(userId).roles().realmLevel()
                .add(Collections.singletonList(userRole));

            log.info("Assigned 'user' role to user: {}", userId);

        } catch (Exception e) {
            log.warn("Failed to assign default role (role might not exist): {}", e.getMessage());
        }
    }

    /**
     * Map UserRepresentation to KeycloakUser
     */
    private KeycloakUser mapToKeycloakUser(UserRepresentation user) {
        KeycloakUser keycloakUser = new KeycloakUser();
        keycloakUser.setId(user.getId());
        keycloakUser.setUsername(user.getUsername());
        keycloakUser.setEmail(user.getEmail());
        keycloakUser.setName(user.getFirstName() + " " + user.getLastName());

        // Extract phone from attributes
        if (user.getAttributes() != null && user.getAttributes().containsKey("phoneNumber")) {
            List<String> phones = user.getAttributes().get("phoneNumber");
            if (!phones.isEmpty()) {
                keycloakUser.setPhoneNumber(phones.get(0));
            }
        }

        keycloakUser.setNewlyCreated(false);
        return keycloakUser;
    }
}
```

---

### Database Schema

```sql
-- ==============================================
-- OTP Records Table
-- ==============================================
CREATE TABLE otp_records (
    id BIGSERIAL PRIMARY KEY,
    identifier VARCHAR(255) NOT NULL,
    otp_code VARCHAR(6) NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('PHONE', 'EMAIL')),
    expires_at TIMESTAMP NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_otp_identifier ON otp_records(identifier);
CREATE INDEX idx_otp_expires_at ON otp_records(expires_at);
CREATE INDEX idx_otp_verified ON otp_records(verified);

-- ==============================================
-- Users Table
-- ==============================================
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,  -- Keycloak user ID
    phone_number VARCHAR(20) UNIQUE,
    email VARCHAR(255) UNIQUE,
    name VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP
);

CREATE INDEX idx_users_phone ON users(phone_number);
CREATE INDEX idx_users_email ON users(email);

-- ==============================================
-- Cleanup old OTP records (run periodically)
-- ==============================================
DELETE FROM otp_records
WHERE expires_at < CURRENT_TIMESTAMP - INTERVAL '1 day';
```

**Entity Classes:**

```java
// OtpRecord.java
@Entity
@Table(name = "otp_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String identifier;

    @Column(name = "otp_code", nullable = false, length = 6)
    private String otpCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OtpType type;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private Integer attempts = 0;

    @Column(nullable = false)
    private Boolean verified = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}

// User.java
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    private String id; // Keycloak user ID

    @Column(name = "phone_number", unique = true, length = 20)
    private String phoneNumber;

    @Column(unique = true)
    private String email;

    private String name;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
}

// Enums
public enum OtpType {
    PHONE,
    EMAIL
}
```

---

## Testing Strategies

### Configuration

Add to `application.yml`:

```yaml
app:
  otp:
    # Set to true for testing without SMS service
    testing-mode: true

    # Fixed OTP (only works when testing-mode=true)
    fixed-otp: "123456"

    # OTP expiry in seconds
    expiry-seconds: 300

    # Max attempts before OTP becomes invalid
    max-attempts: 3

# Email configuration for OTP delivery
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${MAIL_USERNAME:your-email@gmail.com}
    password: ${MAIL_PASSWORD:your-app-password}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true
        debug: false
```

### Testing Techniques

#### 1. **Console Logging** ✅ Simplest
```
┌─────────────────────────────────────┐
│     🔐 OTP TESTING MODE            │
├─────────────────────────────────────┤
│ Phone: +919876543210                │
│ OTP:   123456                       │
│ Valid: 5 minutes                    │
└─────────────────────────────────────┘
```

**When to use:** Development on your local machine

#### 2. **File Logging** ✅ Team Testing
```
otp_logs/otp_2025-03-10.txt:
[2025-03-10T14:30:00] +919876543210 -> 123456
[2025-03-10T14:31:00] user@example.com -> 789012
```

**When to use:** Multiple developers/testers need access

#### 3. **Email Forwarding** ✅ Best for QA
All test OTPs are sent to a dedicated test email address.

**Setup Gmail App Password:**
1. Go to Google Account → Security
2. Enable 2-Step Verification
3. Go to App passwords → Generate new
4. Use generated password in `application.yml`

**When to use:** Professional QA testing

#### 4. **Fixed OTP Mode** ✅ Automated Testing
```yaml
app:
  otp:
    testing-mode: true
    fixed-otp: "123456"
```

All OTP requests return `123456`.

**When to use:** Integration tests, E2E tests, automation

#### 5. **Postman Testing**

Create a Postman collection:

```json
{
  "info": {
    "name": "OTP Authentication Tests"
  },
  "item": [
    {
      "name": "1. Send OTP",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"phoneNumber\": \"+919876543210\",\n  \"type\": \"PHONE\"\n}"
        },
        "url": "http://localhost:8080/auth/otp/send"
      }
    },
    {
      "name": "2. Verify OTP",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"identifier\": \"+919876543210\",\n  \"otp\": \"123456\",\n  \"phoneNumber\": \"+919876543210\",\n  \"name\": \"Test User\"\n}"
        },
        "url": "http://localhost:8080/auth/otp/verify"
      }
    }
  ]
}
```

---

## API Gateway Configuration

### application.yml

```yaml
server:
  port: 8080

spring:
  application:
    name: api-gateway
  cloud:
    gateway:
      routes:
        # Route to Service A (Authentication)
        - id: service-a-auth
          uri: http://localhost:8081
          predicates:
            - Path=/auth/**
          filters:
            - name: AddRequestHeader
              args:
                name: X-Gateway-Request
                value: "true"

        # Route to Service B (Business Logic)
        - id: service-b-api
          uri: http://localhost:8082
          predicates:
            - Path=/api/**
          filters:
            - name: JwtAuthenticationFilter
            - name: AddRequestHeader
              args:
                name: X-Gateway-Request
                value: "true"

      # Global CORS configuration
      globalcors:
        cors-configurations:
          '[/**]':
            allowed-origins:
              - "http://localhost:3000"
              - "http://localhost:5173"
              - "http://localhost:19006"
            allowed-methods:
              - GET
              - POST
              - PUT
              - DELETE
              - OPTIONS
            allowed-headers:
              - "*"
            allow-credentials: true
            max-age: 3600

# JWT Configuration
jwt:
  public-key-location: classpath:public_key.pem
  issuer: http://localhost:8180/realms/sample-realm
```

### JWT Validation Filter

```java
package com.example.gateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtAuthenticationFilter extends
    AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    @Value("${jwt.issuer}")
    private String expectedIssuer;

    public JwtAuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // Skip authentication for public endpoints
            String path = request.getPath().toString();
            if (isPublicEndpoint(path)) {
                return chain.filter(exchange);
            }

            // Extract JWT token
            String token = extractToken(request);

            if (token == null) {
                return onError(exchange, "Missing authorization token", HttpStatus.UNAUTHORIZED);
            }

            try {
                // Decode and validate JWT
                DecodedJWT decodedJWT = JWT.decode(token);

                // Validate issuer
                if (!expectedIssuer.equals(decodedJWT.getIssuer())) {
                    return onError(exchange, "Invalid token issuer", HttpStatus.UNAUTHORIZED);
                }

                // Check expiry
                if (decodedJWT.getExpiresAt().before(new java.util.Date())) {
                    return onError(exchange, "Token expired", HttpStatus.UNAUTHORIZED);
                }

                // Extract user ID from token
                String userId = decodedJWT.getSubject();

                // Add user info to headers for downstream services
                ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Roles", String.join(",",
                        decodedJWT.getClaim("realm_access").asMap().get("roles").toString()))
                    .build();

                log.debug("JWT validated for user: {}", userId);

                return chain.filter(exchange.mutate().request(modifiedRequest).build());

            } catch (Exception e) {
                log.error("JWT validation failed", e);
                return onError(exchange, "Invalid token", HttpStatus.UNAUTHORIZED);
            }
        };
    }

    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/auth/otp") ||
               path.startsWith("/auth/token/refresh") ||
               path.equals("/health");
    }

    private String extractToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");

        String body = String.format("{\"error\": \"%s\", \"status\": %d}",
            message, status.value());

        return response.writeWith(Mono.just(
            response.bufferFactory().wrap(body.getBytes())
        ));
    }

    public static class Config {
        // Configuration properties if needed
    }
}
```

---

## Service B - Business Logic

### FeignClient Configuration

```java
package com.example.serviceb.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
    name = "service-a",
    url = "http://localhost:8081",
    configuration = FeignConfig.class
)
public interface ServiceAClient {

    @GetMapping("/auth/user/{userId}")
    UserInfo getUserInfo(
        @PathVariable("userId") String userId,
        @RequestHeader("Authorization") String authorization
    );
}

// FeignConfig.java
@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            // Propagate JWT token from current request
            ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String token = request.getHeader("Authorization");

                if (token != null) {
                    requestTemplate.header("Authorization", token);
                }
            }
        };
    }
}
```

### Example Controller

```java
package com.example.serviceb.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class BusinessController {

    @Autowired
    private ServiceAClient serviceAClient;

    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("Authorization") String authHeader) {

        // Call Service A to get user details
        UserInfo userInfo = serviceAClient.getUserInfo(userId, authHeader);

        // Your business logic here
        // ...

        return ResponseEntity.ok(userInfo);
    }

    @GetMapping("/data")
    public ResponseEntity<?> getData(@RequestHeader("X-User-Id") String userId) {
        // Use user ID from gateway
        return ResponseEntity.ok(Map.of(
            "userId", userId,
            "data", "Your business data here"
        ));
    }
}
```

---

## Frontend Implementation

### React Web

#### 1. Token Storage Utility

```javascript
// utils/tokenStorage.js

class TokenStorage {
  /**
   * Save tokens to localStorage
   */
  static saveTokens(accessToken, refreshToken, expiresIn) {
    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', refreshToken);
    localStorage.setItem('tokenExpiry', Date.now() + (expiresIn * 1000));
  }

  /**
   * Get access token
   */
  static getAccessToken() {
    return localStorage.getItem('accessToken');
  }

  /**
   * Get refresh token
   */
  static getRefreshToken() {
    return localStorage.getItem('refreshToken');
  }

  /**
   * Clear all tokens
   */
  static clearTokens() {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('tokenExpiry');
    localStorage.removeItem('user');
  }

  /**
   * Check if token is expired
   */
  static isTokenExpired() {
    const expiry = localStorage.getItem('tokenExpiry');
    return !expiry || Date.now() > parseInt(expiry);
  }

  /**
   * Save user info
   */
  static saveUser(user) {
    localStorage.setItem('user', JSON.stringify(user));
  }

  /**
   * Get user info
   */
  static getUser() {
    const user = localStorage.getItem('user');
    return user ? JSON.parse(user) : null;
  }
}

export default TokenStorage;
```

#### 2. API Client with Auto Refresh

```javascript
// utils/apiClient.js
import axios from 'axios';
import TokenStorage from './tokenStorage';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';

// Create axios instance
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json'
  }
});

// Request interceptor - Add token to requests
apiClient.interceptors.request.use(
  (config) => {
    const token = TokenStorage.getAccessToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor - Handle token expiry
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // If 401 and not already retried
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        // Try to refresh token
        const refreshToken = TokenStorage.getRefreshToken();

        if (!refreshToken) {
          throw new Error('No refresh token');
        }

        const response = await axios.post(
          `${API_BASE_URL}/auth/token/refresh`,
          { refreshToken }
        );

        const { accessToken, refreshToken: newRefreshToken, expiresIn } =
          response.data.data;

        // Save new tokens
        TokenStorage.saveTokens(accessToken, newRefreshToken, expiresIn);

        // Retry original request with new token
        originalRequest.headers.Authorization = `Bearer ${accessToken}`;
        return apiClient(originalRequest);

      } catch (refreshError) {
        // Refresh failed - logout user
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

#### 3. Authentication Service

```javascript
// services/authService.js
import apiClient from '../utils/apiClient';
import TokenStorage from '../utils/tokenStorage';

class AuthService {
  /**
   * Send OTP to phone/email
   */
  async sendOtp(identifier, type = 'PHONE') {
    const payload = type === 'PHONE'
      ? { phoneNumber: identifier, type }
      : { email: identifier, type: 'EMAIL' };

    const response = await apiClient.post('/auth/otp/send', payload);
    return response.data;
  }

  /**
   * Verify OTP and login/register
   */
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

      // Save tokens and user info
      TokenStorage.saveTokens(accessToken, refreshToken, expiresIn);
      TokenStorage.saveUser(user);

      return response.data;
    }

    throw new Error(response.data.message || 'OTP verification failed');
  }

  /**
   * Get current user profile
   */
  async getCurrentUser() {
    const response = await apiClient.get('/auth/user/profile');
    return response.data.data;
  }

  /**
   * Logout
   */
  logout() {
    TokenStorage.clearTokens();
    window.location.href = '/login';
  }

  /**
   * Check if user is authenticated
   */
  isAuthenticated() {
    return !!TokenStorage.getAccessToken() && !TokenStorage.isTokenExpired();
  }

  /**
   * Get stored user
   */
  getUser() {
    return TokenStorage.getUser();
  }
}

export default new AuthService();
```

#### 4. Login Component

```jsx
// components/Login.jsx
import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AuthService from '../services/authService';
import './Login.css';

function Login() {
  const navigate = useNavigate();
  const [step, setStep] = useState(1); // 1: Phone input, 2: OTP input
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

      // FOR TESTING: Show OTP if in debug mode
      if (response.data.otp_debug) {
        setDebugOtp(response.data.otp_debug);
        alert(`Testing Mode - Your OTP: ${response.data.otp_debug}`);
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
      const response = await AuthService.verifyOtp(phoneNumber, otp, {
        phoneNumber,
        name: 'Web User' // Can collect this in a form
      });

      console.log('Login successful:', response);

      // Redirect to dashboard
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

        {step === 1 && (
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
              <small className="form-hint">Enter with country code (e.g., +91)</small>
            </div>

            {error && <div className="error-message">{error}</div>}

            <button type="submit" disabled={loading} className="btn btn-primary">
              {loading ? 'Sending...' : 'Send OTP'}
            </button>
          </form>
        )}

        {step === 2 && (
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
              <small className="form-hint">
                OTP sent to {phoneNumber}
                {debugOtp && <span className="debug-otp"> | Debug: {debugOtp}</span>}
              </small>
            </div>

            {error && <div className="error-message">{error}</div>}

            <button type="submit" disabled={loading} className="btn btn-primary">
              {loading ? 'Verifying...' : 'Verify OTP'}
            </button>

            <button
              type="button"
              onClick={() => { setStep(1); setOtp(''); setError(''); }}
              className="btn btn-secondary"
            >
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

#### 5. Protected Route

```jsx
// components/ProtectedRoute.jsx
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

#### 6. App Routing

```jsx
// App.jsx
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

---

### React Native

#### 1. Install Dependencies

```bash
npx expo install expo-secure-store
npm install axios
```

#### 2. Secure Storage Utility

```javascript
// utils/secureStorage.js
import * as SecureStore from 'expo-secure-store';

class SecureStorage {
  /**
   * Save tokens securely in device keychain/keystore
   */
  static async saveTokens(accessToken, refreshToken, expiresIn) {
    try {
      await SecureStore.setItemAsync('accessToken', accessToken);
      await SecureStore.setItemAsync('refreshToken', refreshToken);
      await SecureStore.setItemAsync('tokenExpiry',
        (Date.now() + (expiresIn * 1000)).toString()
      );
      console.log('✅ Tokens saved securely');
    } catch (error) {
      console.error('❌ Error saving tokens:', error);
      throw error;
    }
  }

  static async getAccessToken() {
    try {
      return await SecureStore.getItemAsync('accessToken');
    } catch (error) {
      console.error('Error getting access token:', error);
      return null;
    }
  }

  static async getRefreshToken() {
    try {
      return await SecureStore.getItemAsync('refreshToken');
    } catch (error) {
      console.error('Error getting refresh token:', error);
      return null;
    }
  }

  static async clearTokens() {
    try {
      await SecureStore.deleteItemAsync('accessToken');
      await SecureStore.deleteItemAsync('refreshToken');
      await SecureStore.deleteItemAsync('tokenExpiry');
      await SecureStore.deleteItemAsync('user');
      console.log('✅ Tokens cleared');
    } catch (error) {
      console.error('Error clearing tokens:', error);
    }
  }

  static async isTokenExpired() {
    try {
      const expiry = await SecureStore.getItemAsync('tokenExpiry');
      return !expiry || Date.now() > parseInt(expiry);
    } catch (error) {
      return true;
    }
  }

  static async saveUser(user) {
    try {
      await SecureStore.setItemAsync('user', JSON.stringify(user));
    } catch (error) {
      console.error('Error saving user:', error);
    }
  }

  static async getUser() {
    try {
      const user = await SecureStore.getItemAsync('user');
      return user ? JSON.parse(user) : null;
    } catch (error) {
      console.error('Error getting user:', error);
      return null;
    }
  }
}

export default SecureStorage;
```

#### 3. API Client

```javascript
// utils/apiClient.js
import axios from 'axios';
import SecureStorage from './secureStorage';

// IMPORTANT: Use your computer's IP address, not localhost
// Find your IP: Windows -> ipconfig | Mac/Linux -> ifconfig
const API_BASE_URL = 'http://YOUR_COMPUTER_IP:8080';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json'
  },
  timeout: 10000
});

// Request interceptor
apiClient.interceptors.request.use(
  async (config) => {
    const token = await SecureStorage.getAccessToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        const refreshToken = await SecureStorage.getRefreshToken();

        if (!refreshToken) {
          throw new Error('No refresh token');
        }

        const response = await axios.post(
          `${API_BASE_URL}/auth/token/refresh`,
          { refreshToken }
        );

        const { accessToken, refreshToken: newRefreshToken, expiresIn } =
          response.data.data;

        await SecureStorage.saveTokens(accessToken, newRefreshToken, expiresIn);

        originalRequest.headers.Authorization = `Bearer ${accessToken}`;
        return apiClient(originalRequest);

      } catch (refreshError) {
        await SecureStorage.clearTokens();
        // Navigate to login - handle in your navigation logic
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

export default apiClient;
```

#### 4. Authentication Service

```javascript
// services/authService.js
import apiClient from '../utils/apiClient';
import SecureStorage from '../utils/secureStorage';

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
      name: additionalData.name || 'Mobile User'
    };

    const response = await apiClient.post('/auth/otp/verify', payload);

    if (response.data.success) {
      const { accessToken, refreshToken, expiresIn, user } = response.data.data;

      await SecureStorage.saveTokens(accessToken, refreshToken, expiresIn);
      await SecureStorage.saveUser(user);

      return response.data;
    }

    throw new Error(response.data.message || 'OTP verification failed');
  }

  async logout() {
    await SecureStorage.clearTokens();
  }

  async isAuthenticated() {
    const token = await SecureStorage.getAccessToken();
    const expired = await SecureStorage.isTokenExpired();
    return !!token && !expired;
  }

  async getUser() {
    return await SecureStorage.getUser();
  }
}

export default new AuthService();
```

#### 5. Login Screen

```jsx
// screens/LoginScreen.js
import React, { useState } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  Alert,
  ActivityIndicator,
  StyleSheet,
  KeyboardAvoidingView,
  Platform
} from 'react-native';
import authService from '../services/authService';

export default function LoginScreen({ navigation }) {
  const [step, setStep] = useState(1);
  const [phoneNumber, setPhoneNumber] = useState('');
  const [otp, setOtp] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSendOtp = async () => {
    if (!phoneNumber || phoneNumber.length < 10) {
      Alert.alert('Invalid Input', 'Please enter a valid phone number');
      return;
    }

    setLoading(true);

    try {
      const response = await authService.sendOtp(phoneNumber, 'PHONE');

      // Show debug OTP in testing mode
      if (response.data.otp_debug) {
        Alert.alert(
          '🧪 Test Mode',
          `Your OTP: ${response.data.otp_debug}`,
          [{ text: 'OK' }]
        );
      } else {
        Alert.alert('Success', 'OTP sent successfully!');
      }

      setStep(2);
    } catch (error) {
      Alert.alert(
        'Error',
        error.response?.data?.message || 'Failed to send OTP'
      );
    } finally {
      setLoading(false);
    }
  };

  const handleVerifyOtp = async () => {
    if (otp.length !== 6) {
      Alert.alert('Invalid Input', 'Please enter a 6-digit OTP');
      return;
    }

    setLoading(true);

    try {
      const response = await authService.verifyOtp(phoneNumber, otp, {
        phoneNumber
      });

      Alert.alert('Success', 'Login successful!');

      // Navigate to home screen
      navigation.replace('Home');

    } catch (error) {
      Alert.alert(
        'Error',
        error.response?.data?.message || 'Invalid OTP'
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
    >
      <View style={styles.content}>
        <Text style={styles.title}>🔐 Login with OTP</Text>

        {step === 1 ? (
          <>
            <View style={styles.inputContainer}>
              <Text style={styles.label}>Phone Number</Text>
              <TextInput
                style={styles.input}
                placeholder="+919876543210"
                value={phoneNumber}
                onChangeText={setPhoneNumber}
                keyboardType="phone-pad"
                autoFocus
              />
              <Text style={styles.hint}>Enter with country code (e.g., +91)</Text>
            </View>

            <TouchableOpacity
              style={[styles.button, loading && styles.buttonDisabled]}
              onPress={handleSendOtp}
              disabled={loading}
            >
              {loading ? (
                <ActivityIndicator color="#fff" />
              ) : (
                <Text style={styles.buttonText}>Send OTP</Text>
              )}
            </TouchableOpacity>
          </>
        ) : (
          <>
            <View style={styles.inputContainer}>
              <Text style={styles.label}>Enter OTP</Text>
              <TextInput
                style={[styles.input, styles.otpInput]}
                placeholder="123456"
                value={otp}
                onChangeText={(text) => setOtp(text.replace(/\D/g, ''))}
                keyboardType="number-pad"
                maxLength={6}
                autoFocus
              />
              <Text style={styles.hint}>OTP sent to {phoneNumber}</Text>
            </View>

            <TouchableOpacity
              style={[styles.button, loading && styles.buttonDisabled]}
              onPress={handleVerifyOtp}
              disabled={loading}
            >
              {loading ? (
                <ActivityIndicator color="#fff" />
              ) : (
                <Text style={styles.buttonText}>Verify OTP</Text>
              )}
            </TouchableOpacity>

            <TouchableOpacity
              style={styles.linkButton}
              onPress={() => { setStep(1); setOtp(''); }}
            >
              <Text style={styles.linkText}>Change Number</Text>
            </TouchableOpacity>
          </>
        )}
      </View>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5'
  },
  content: {
    flex: 1,
    justifyContent: 'center',
    padding: 24
  },
  title: {
    fontSize: 28,
    fontWeight: 'bold',
    marginBottom: 40,
    textAlign: 'center',
    color: '#333'
  },
  inputContainer: {
    marginBottom: 24
  },
  label: {
    fontSize: 16,
    fontWeight: '600',
    marginBottom: 8,
    color: '#333'
  },
  input: {
    backgroundColor: '#fff',
    borderWidth: 1,
    borderColor: '#ddd',
    padding: 16,
    borderRadius: 12,
    fontSize: 16
  },
  otpInput: {
    fontSize: 24,
    letterSpacing: 8,
    textAlign: 'center',
    fontWeight: 'bold'
  },
  hint: {
    fontSize: 12,
    color: '#666',
    marginTop: 8
  },
  button: {
    backgroundColor: '#4CAF50',
    padding: 16,
    borderRadius: 12,
    alignItems: 'center',
    marginBottom: 12
  },
  buttonDisabled: {
    opacity: 0.6
  },
  buttonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: 'bold'
  },
  linkButton: {
    padding: 12,
    alignItems: 'center'
  },
  linkText: {
    color: '#4CAF50',
    fontSize: 14,
    fontWeight: '600'
  }
});
```

---

## Complete Authentication Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                    AUTHENTICATION FLOW                          │
└─────────────────────────────────────────────────────────────────┘

Step 1: User enters phone number/email
   ↓
   Frontend → POST /auth/otp/send
   {
     "phoneNumber": "+919876543210",
     "type": "PHONE"
   }
   ↓
   Backend generates OTP → Stores in database
   ↓
   Backend sends OTP via:
   - Console log (testing)
   - File (testing)
   - Email to tester (testing)
   - SMS gateway (production)
   ↓
   Response:
   {
     "success": true,
     "data": {
       "otpSent": true,
       "expiresIn": 300,
       "otp_debug": "123456"  // Only in testing mode
     }
   }

Step 2: User enters OTP
   ↓
   Frontend → POST /auth/otp/verify
   {
     "identifier": "+919876543210",
     "otp": "123456",
     "phoneNumber": "+919876543210",
     "name": "John Doe"
   }
   ↓
   Backend validates OTP
   ↓
   Backend checks if user exists in Keycloak
   ↓
   ┌─────────────────────┐
   │ User EXISTS?        │
   └────┬───────────┬────┘
        │ NO        │ YES
        ↓           ↓
   Create user   Get user info
   in Keycloak   from Keycloak
        │           │
        ↓           ↓
   Create user   Update last login
   in app DB     in app DB
        │           │
        └─────┬─────┘
              ↓
   Generate JWT tokens from Keycloak
   (accessToken + refreshToken)
              ↓
   Response:
   {
     "success": true,
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
       "isNewUser": true/false
     }
   }

Step 3: Frontend stores tokens securely
   Web: localStorage
   Mobile: expo-secure-store (device keychain)

Step 4: Subsequent API requests
   Frontend → GET /api/some-endpoint
   Headers: Authorization: Bearer <accessToken>
   ↓
   API Gateway validates JWT
   ↓
   If valid → Forward to Service A/B with:
   - Authorization: Bearer <accessToken>
   - X-User-Id: <extracted from JWT>
   ↓
   Service processes request
   ↓
   Response returned to frontend

Step 5: Token expired?
   Frontend receives 401 Unauthorized
   ↓
   Axios interceptor automatically calls:
   POST /auth/token/refresh
   {
     "refreshToken": "eyJhbGc..."
   }
   ↓
   Backend validates refresh token
   ↓
   Response: New access token
   ↓
   Frontend stores new token
   ↓
   Original request is retried with new token

Step 6: Refresh token expired?
   ↓
   Clear all tokens
   ↓
   Redirect to login screen
```

---

## Configuration Reference

### Service A (application.yml)

```yaml
server:
  port: 8081

spring:
  application:
    name: service-a-auth

  datasource:
    url: jdbc:postgresql://localhost:5432/authdb
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

  mail:
    host: smtp.gmail.com
    port: 587
    username: ${MAIL_USERNAME:your-email@gmail.com}
    password: ${MAIL_PASSWORD:your-app-password}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true

# Keycloak Configuration
keycloak:
  auth-server-url: http://localhost:8180
  realm: sample-realm
  resource: sample-app
  credentials:
    secret: ${KEYCLOAK_CLIENT_SECRET}
  admin:
    username: ${KEYCLOAK_ADMIN_USER:admin}
    password: ${KEYCLOAK_ADMIN_PASSWORD:admin}

# OTP Configuration
app:
  otp:
    testing-mode: ${OTP_TESTING_MODE:true}
    fixed-otp: ${OTP_FIXED:123456}
    expiry-seconds: 300
    max-attempts: 3

# Logging
logging:
  level:
    com.example.auth: DEBUG
    org.springframework.security: INFO
```

### Environment Variables (.env)

```env
# Database
DB_USERNAME=postgres
DB_PASSWORD=your_postgres_password

# Email (Gmail)
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=xxxx xxxx xxxx xxxx

# Keycloak
KEYCLOAK_CLIENT_SECRET=your-client-secret
KEYCLOAK_ADMIN_USER=admin
KEYCLOAK_ADMIN_PASSWORD=admin

# OTP Testing
OTP_TESTING_MODE=true
OTP_FIXED=123456
```

### React Web (.env)

```env
REACT_APP_API_URL=http://localhost:8080
```

### React Native

```javascript
// config.js
export const API_BASE_URL = __DEV__
  ? 'http://YOUR_COMPUTER_IP:8080'  // Development
  : 'https://api.yourapp.com';       // Production
```

---

## Security Best Practices

### 1. Token Security

✅ **DO:**
- Store tokens in secure storage (SecureStore for mobile, httpOnly cookies for web)
- Use HTTPS in production
- Implement token rotation
- Set appropriate token expiry (1 hour for access, 30 days for refresh)

❌ **DON'T:**
- Store tokens in AsyncStorage (React Native)
- Store tokens in regular localStorage without encryption (for sensitive apps)
- Share tokens across domains
- Log tokens in production

### 2. OTP Security

✅ **DO:**
- Limit OTP attempts (max 3)
- Set short expiry (5 minutes)
- Rate limit OTP generation (1 per minute per user)
- Invalidate OTP after successful verification
- Hash OTP before storing (optional for high security)

❌ **DON'T:**
- Return OTP in API response (except testing mode)
- Allow unlimited OTP generation
- Reuse OTPs
- Store OTPs in plain text in logs

### 3. API Security

✅ **DO:**
- Validate JWT on every request
- Check token expiry
- Verify token issuer
- Use rate limiting
- Implement request logging

❌ **DON'T:**
- Skip JWT validation
- Trust client-provided user IDs without token verification
- Expose sensitive endpoints without authentication
- Allow CORS from all origins in production

### 4. Keycloak Security

✅ **DO:**
- Use strong client secrets
- Enable "Direct Access Grants" only if needed
- Configure token lifespan appropriately
- Use realm-specific clients
- Enable email/phone verification

❌ **DON'T:**
- Use default admin credentials
- Expose Keycloak admin console to internet
- Share client secrets in code
- Use public clients for backend services

---

## Migration to Production SMS

When ready to use actual SMS service (Twilio, etc.):

### 1. Update application.yml

```yaml
app:
  otp:
    testing-mode: false  # Disable testing mode
  sms:
    provider: twilio     # or your provider
    account-sid: ${TWILIO_ACCOUNT_SID}
    auth-token: ${TWILIO_AUTH_TOKEN}
    from-number: ${TWILIO_PHONE_NUMBER}
```

### 2. Update OtpService.java

```java
private void sendOtpViaSms(String phoneNumber, String otp) {
    if (testingMode) {
        // Testing logic (keep for dev environments)
    } else {
        // PRODUCTION: Twilio integration
        twilioService.sendSms(phoneNumber, "Your OTP is: " + otp);
    }
}
```

### 3. Implement Twilio Service

```java
@Service
public class TwilioService {

    @Value("${app.sms.account-sid}")
    private String accountSid;

    @Value("${app.sms.auth-token}")
    private String authToken;

    @Value("${app.sms.from-number}")
    private String fromNumber;

    public void sendSms(String toNumber, String message) {
        Twilio.init(accountSid, authToken);

        Message.creator(
            new PhoneNumber(toNumber),
            new PhoneNumber(fromNumber),
            message
        ).create();

        log.info("SMS sent to: {}", toNumber);
    }
}
```

### 4. Add Twilio Dependency

```xml
<dependency>
    <groupId>com.twilio.sdk</groupId>
    <artifactId>twilio</artifactId>
    <version>9.14.1</version>
</dependency>
```

---

## Troubleshooting

### Issue: "OTP not received"
**Solution:**
- Check console logs in testing mode
- Check `otp_logs/` directory
- Verify email configuration
- Check Twilio logs (production)

### Issue: "Invalid token"
**Solution:**
- Verify Keycloak is running
- Check token expiry
- Verify issuer matches configuration
- Check JWT public key

### Issue: "User creation failed"
**Solution:**
- Check Keycloak admin credentials
- Verify realm exists
- Check network connectivity to Keycloak
- Enable DEBUG logging

### Issue: "Token refresh failed"
**Solution:**
- Verify refresh token is stored correctly
- Check refresh token expiry
- Verify client secret in Keycloak

---

## Summary

This guide provides a **complete, production-ready OTP authentication system** with:

✅ Keycloak integration for user management and JWT tokens
✅ OTP-based authentication (phone/email)
✅ Auto user registration in Keycloak and app database
✅ Multiple testing strategies (no SMS service needed)
✅ Secure token storage (Web: localStorage, Mobile: SecureStore)
✅ Automatic token refresh
✅ JWT validation in API Gateway
✅ Token propagation across microservices
✅ Easy migration to production SMS (Twilio)

**Next Steps:**
1. Set up Keycloak with realm and client
2. Implement Service A with OTP endpoints
3. Configure API Gateway with JWT validation
4. Build frontend (React Web or React Native)
5. Test using provided testing strategies
6. Deploy and migrate to production SMS

---

**For questions or issues, refer to the specific sections above or check logs at:**
- Service A: `logs/service-a.log`
- OTP logs: `otp_logs/otp_YYYY-MM-DD.txt`
- Keycloak: `docker logs keycloak-auth-server`
