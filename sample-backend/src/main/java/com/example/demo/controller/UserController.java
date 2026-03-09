package com.example.demo.controller;

import com.example.demo.dto.ApiResponseDto;
import com.example.demo.dto.UserProfileResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Protected REST controller demonstrating JWT-based authentication
 * All endpoints require valid JWT token in Authorization header
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
public class UserController {

    /**
     * Get current user profile from JWT token
     *
     * Example: GET /api/user/profile
     * Header: Authorization: Bearer <jwt-token>
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponseDto<UserProfileResponse>> getUserProfile(
            @AuthenticationPrincipal Jwt jwt) {

    	log.info("Fetching profile for user: {}", (String) jwt.getClaim("preferred_username"));


        UserProfileResponse profile = UserProfileResponse.builder()
                .username(jwt.getClaim("preferred_username"))
                .email(jwt.getClaim("email"))
                .firstName(jwt.getClaim("given_name"))
                .lastName(jwt.getClaim("family_name"))
                .fullName(jwt.getClaim("name"))
                .emailVerified(jwt.getClaim("email_verified"))
                .roles(extractRoles(jwt))
                .build();

        return ResponseEntity.ok(ApiResponseDto.success("User profile retrieved successfully", profile));
    }

    /**
     * Get all JWT claims for debugging
     */
    @GetMapping("/claims")
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> getAllClaims(
            @AuthenticationPrincipal Jwt jwt) {

        log.info("Fetching all claims for user: {}",(String)  jwt.getClaim("preferred_username"));

        return ResponseEntity.ok(ApiResponseDto.success("JWT claims retrieved successfully", jwt.getClaims()));
    }

    /**
     * Protected endpoint - only authenticated users can access
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> getDashboard(
            @AuthenticationPrincipal Jwt jwt) {

        String username = jwt.getClaim("preferred_username");
        log.info("Dashboard accessed by user: {}", username);

        Map<String, Object> dashboardData = Map.of(
                "welcomeMessage", "Welcome to your dashboard, " + username + "!",
                "lastLogin", jwt.getIssuedAt(),
                "tokenExpiry", jwt.getExpiresAt(),
                "recentActivities", List.of(
                        "Logged in successfully",
                        "Viewed profile",
                        "Accessed dashboard"
                )
        );

        return ResponseEntity.ok(ApiResponseDto.success(dashboardData));
    }

    /**
     * Extract roles from JWT token
     * Keycloak stores roles in realm_access.roles or resource_access
     */
    @SuppressWarnings("unchecked")
    private List<String> extractRoles(Jwt jwt) {
        try {
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess != null && realmAccess.containsKey("roles")) {
                return (List<String>) realmAccess.get("roles");
            }
        } catch (Exception e) {
            log.warn("Failed to extract roles from JWT: {}", e.getMessage());
        }
        return List.of();
    }
}
