package com.shashankpk.otpauth.controller;

import com.shashankpk.otpauth.dto.*;
import com.shashankpk.otpauth.service.KeycloakService;
import com.shashankpk.otpauth.service.OtpService;
import com.shashankpk.otpauth.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
     * Send OTP
     * POST /auth/otp/send
     */
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<?>> sendOtp(@Valid @RequestBody OtpSendRequest request) {
        log.info("OTP send request for: {}", request.getIdentifier());

        if (request.getPhoneNumber() == null && request.getEmail() == null) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Phone number or email is required")
            );
        }

        try {
            String identifier = request.getIdentifier();
            String otp = otpService.generateAndSendOtp(identifier, request.getType());

            Map<String, Object> responseData = Map.of(
                "identifier", identifier,
                "expiresIn", 300,
                "otpSent", true
            );

            // Add debug OTP in testing mode
            if (otp != null) {
                responseData = Map.of(
                    "identifier", identifier,
                    "expiresIn", 300,
                    "otpSent", true,
                    "otp_debug", otp
                );
            }

            return ResponseEntity.ok(ApiResponse.success("OTP sent successfully", responseData));

        } catch (Exception e) {
            log.error("Failed to send OTP", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error("Failed to send OTP: " + e.getMessage())
            );
        }
    }

    /**
     * Verify OTP
     * POST /auth/otp/verify
     */
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<?>> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        log.info("OTP verification request for: {}", request.getIdentifier());

        boolean isValid = otpService.verifyOtp(request.getIdentifier(), request.getOtp());

        if (!isValid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ApiResponse.error("Invalid or expired OTP")
            );
        }

        try {
            KeycloakUser keycloakUser = keycloakService.findUserByIdentifier(request.getIdentifier());

            boolean isNewUser = false;

            if (keycloakUser == null) {
                log.info("Creating new user: {}", request.getIdentifier());

                keycloakUser = keycloakService.createUser(
                    request.getIdentifier(),
                    request.getPhoneNumber(),
                    request.getEmail(),
                    request.getName()
                );

                userService.createUser(
                    keycloakUser.getId(),
                    request.getPhoneNumber(),
                    request.getEmail(),
                    request.getName()
                );

                isNewUser = true;
            } else {
                userService.updateLastLogin(keycloakUser.getId());
            }

            TokenResponse tokens = keycloakService.generateTokensForUser(keycloakUser.getId());
            UserProfile userProfile = userService.getUserProfile(keycloakUser.getId());

            Map<String, Object> responseData = Map.of(
                "accessToken", tokens.getAccessToken(),
                "refreshToken", tokens.getRefreshToken(),
                "expiresIn", tokens.getExpiresIn(),
                "tokenType", "Bearer",
                "user", userProfile,
                "isNewUser", isNewUser
            );

            return ResponseEntity.ok(ApiResponse.success("Authentication successful", responseData));

        } catch (Exception e) {
            log.error("Error during OTP verification", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error("Authentication failed: " + e.getMessage())
            );
        }
    }

    /**
     * Refresh token
     * POST /auth/otp/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<?>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("Token refresh request");

        try {
            TokenResponse tokens = keycloakService.refreshAccessToken(request.getRefreshToken());
            return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", tokens));

        } catch (Exception e) {
            log.error("Failed to refresh token", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ApiResponse.error("Token refresh failed")
            );
        }
    }

    /**
     * Get user profile
     * GET /auth/otp/profile
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<?>> getUserProfile(@RequestHeader("X-User-Id") String userId) {
        log.info("Get profile request for user: {}", userId);

        try {
            UserProfile profile = userService.getUserProfile(userId);
            return ResponseEntity.ok(ApiResponse.success("Success", profile));

        } catch (Exception e) {
            log.error("Error getting user profile", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse.error("User not found")
            );
        }
    }

    /**
     * Health check
     * GET /auth/otp/health
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<?>> healthCheck() {
        Map<String, String> healthData = Map.of(
            "status", "UP",
            "service", "OTP Authentication Starter"
        );
        return ResponseEntity.ok(ApiResponse.success("Healthy", healthData));
    }
}
