package com.shashankpk.oauth2.starter.controller;

import com.shashankpk.oauth2.starter.dto.*;
import com.shashankpk.oauth2.starter.properties.OAuth2Properties;
import com.shashankpk.oauth2.starter.service.OAuth2ClientService;
import com.shashankpk.oauth2.starter.service.TokenValidationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for OAuth2 authentication operations
 * Provides endpoints for both standard OAuth2 flow and custom login
 */
@Slf4j
@RestController
@RequestMapping("/oauth2")
@RequiredArgsConstructor
public class OAuth2AuthenticationController {

    private final OAuth2ClientService oauth2ClientService;
    private final TokenValidationService tokenValidationService;
    private final OAuth2Properties oauth2Properties;

    /**
     * Get authorization URL for OAuth2 flow
     * Frontend redirects user to this URL to start authentication
     *
     * GET /oauth2/authorize?redirect_uri=http://localhost:3000/callback
     */
    @GetMapping("/authorize")
    public ResponseEntity<ApiResponse<Map<String, String>>> getAuthorizationUrl(
            @RequestParam String redirectUri,
            @RequestParam(required = false) String state) {

        if (state == null) {
            state = UUID.randomUUID().toString();
        }

        String authUrl = oauth2ClientService.getAuthorizationUrl(state, redirectUri);

        log.info("Generated authorization URL for state: {}", state);

        return ResponseEntity.ok(ApiResponse.success(
                Map.of(
                        "authorizationUrl", authUrl,
                        "state", state
                )
        ));
    }

    /**
     * Exchange authorization code for tokens
     * Called by frontend after user is redirected back from OAuth2 provider
     *
     * POST /oauth2/token
     * Body: { "code": "auth-code", "redirectUri": "http://localhost:3000/callback" }
     */
    @PostMapping("/token")
    public ResponseEntity<ApiResponse<TokenResponse>> exchangeToken(
            @RequestParam String code,
            @RequestParam String redirectUri) {

        log.info("Exchanging authorization code for token");

        TokenResponse tokenResponse = oauth2ClientService.exchangeCodeForToken(code, redirectUri);

        // Optionally fetch user info
        if (tokenResponse.getAccessToken() != null) {
            try {
                UserInfo userInfo = oauth2ClientService.getUserInfo(tokenResponse.getAccessToken());
                tokenResponse.setUserInfo(userInfo);
            } catch (Exception e) {
                log.warn("Failed to fetch user info: {}", e.getMessage());
            }
        }

        return ResponseEntity.ok(ApiResponse.success("Authentication successful", tokenResponse));
    }

    /**
     * Custom login with username and password
     * For applications with custom login pages
     *
     * POST /oauth2/login
     * Body: { "username": "user", "password": "password" }
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {

        log.info("Custom login attempt for user: {}", loginRequest.getUsername());

        TokenResponse tokenResponse = oauth2ClientService.authenticateWithPassword(loginRequest);

        // Fetch user info
        if (tokenResponse.getAccessToken() != null) {
            try {
                UserInfo userInfo = oauth2ClientService.getUserInfo(tokenResponse.getAccessToken());
                tokenResponse.setUserInfo(userInfo);
            } catch (Exception e) {
                log.warn("Failed to fetch user info: {}", e.getMessage());
            }
        }

        return ResponseEntity.ok(ApiResponse.success("Login successful", tokenResponse));
    }

    /**
     * Refresh access token using refresh token
     *
     * POST /oauth2/refresh
     * Body: { "refreshToken": "refresh-token" }
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {

        log.info("Refreshing access token");

        TokenResponse tokenResponse = oauth2ClientService.refreshToken(refreshTokenRequest.getRefreshToken());

        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", tokenResponse));
    }

    /**
     * Get current user information from access token
     *
     * GET /oauth2/userinfo
     * Header: Authorization: Bearer <access-token>
     */
    @GetMapping("/userinfo")
    public ResponseEntity<ApiResponse<UserInfo>> getUserInfo(
            @RequestHeader("Authorization") String authorization) {

        String token = extractToken(authorization);
        log.info("Fetching user info from token");

        UserInfo userInfo = oauth2ClientService.getUserInfo(token);

        return ResponseEntity.ok(ApiResponse.success(userInfo));
    }

    /**
     * Validate token and get claims
     *
     * GET /oauth2/validate
     * Header: Authorization: Bearer <access-token>
     */
    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateToken(
            @RequestHeader("Authorization") String authorization) {

        String token = extractToken(authorization);
        log.info("Validating token");

        Map<String, Object> claims = tokenValidationService.extractClaims(token);

        return ResponseEntity.ok(ApiResponse.success("Token is valid", claims));
    }

    /**
     * Logout endpoint
     * Note: Actual logout depends on the OAuth2 provider implementation
     *
     * POST /oauth2/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        log.info("Logout request received");

        // In a real implementation, you might want to:
        // 1. Invalidate the token on the server side (if using opaque tokens)
        // 2. Call the provider's logout endpoint
        // 3. Clear session data

        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    /**
     * Health check endpoint for the OAuth2 starter
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, String>>> health() {
        return ResponseEntity.ok(ApiResponse.success(
                Map.of(
                        "status", "UP",
                        "service", "OAuth2/OIDC Starter"
                )
        ));
    }

    /**
     * Get authentication configuration
     * Returns which authentication mode is enabled (custom login vs OAuth2 redirect)
     * Frontend uses this to dynamically adapt its UI
     *
     * GET /oauth2/config
     */
    @GetMapping("/config")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAuthConfig() {
        log.debug("Fetching authentication configuration");

        return ResponseEntity.ok(ApiResponse.success(
                Map.of(
                        "customLoginEnabled", oauth2Properties.isCustomLoginEnabled(),
                        "provider", oauth2Properties.getProvider(),
                        "clientId", oauth2Properties.getClientId()
                )
        ));
    }

    /**
     * Extract token from Authorization header
     */
    private String extractToken(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        throw new IllegalArgumentException("Invalid authorization header");
    }
}
