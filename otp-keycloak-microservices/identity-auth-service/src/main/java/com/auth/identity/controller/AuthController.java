package com.auth.identity.controller;

import com.auth.identity.dto.*;
import com.auth.identity.model.User;
import com.auth.identity.service.AuthService;
import com.auth.identity.service.MpinService;
import com.auth.identity.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final MpinService mpinService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<OtpResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        log.info("Registration request received for: {}", request.getIdentifier());

        OtpResponse response = authService.register(request);

        return ResponseEntity.ok(
                ApiResponse.success("OTP sent successfully", response)
        );
    }

    @PostMapping("/otp/send")
    public ResponseEntity<ApiResponse<OtpResponse>> sendOtp(
            @Valid @RequestBody SendOtpRequest request) {
        log.info("Send OTP request received for: {}", request.getIdentifier());

        OtpResponse response = authService.sendOtp(request.getIdentifier());

        return ResponseEntity.ok(
                ApiResponse.success("OTP sent successfully", response)
        );
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<ApiResponse<TokenResponse>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {
        log.info("Verify OTP request received for: {}", request.getIdentifier());

        TokenResponse response = authService.verifyOtp(
                request.getIdentifier(),
                request.getOtp()
        );

        return ResponseEntity.ok(
                ApiResponse.success("Authentication successful", response)
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        log.info("Refresh token request received");

        TokenResponse response = authService.refreshToken(request.getRefreshToken());

        return ResponseEntity.ok(
                ApiResponse.success("Token refreshed successfully", response)
        );
    }

    @GetMapping("/user/me")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser(
            @AuthenticationPrincipal Jwt jwt) {
        log.info("Get current user request received");

        String userId = jwt.getSubject();
        User user = userService.findByKeycloakId(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(
                ApiResponse.success(userService.toDto(user))
        );
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(
            @PathVariable UUID userId,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("Get user by ID request received: {}", userId);

        User user = userService.getUserById(userId);

        return ResponseEntity.ok(
                ApiResponse.success(userService.toDto(user))
        );
    }

    // ============== MPIN Endpoints ==============

    /**
     * Set or update MPIN (protected endpoint - requires valid JWT)
     * User must be authenticated via OTP before setting MPIN
     */
    @PostMapping("/mpin/set")
    public ResponseEntity<ApiResponse<String>> setMpin(
            @Valid @RequestBody SetMpinRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("Set MPIN request received");

        // Validate MPIN matches confirmation
        if (!request.getMpin().equals(request.getConfirmMpin())) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("MPIN and Confirm MPIN do not match")
            );
        }

        // Get user ID from JWT
        String keycloakId = jwt.getSubject();
        User user = userService.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Set MPIN
        mpinService.setMpin(user.getId(), request.getMpin());

        return ResponseEntity.ok(
                ApiResponse.success("MPIN set successfully", "MPIN has been configured for quick login")
        );
    }

    /**
     * Login with MPIN (public endpoint)
     * Quick login using MPIN - returns JWT tokens if valid
     */
    @PostMapping("/mpin/login")
    public ResponseEntity<ApiResponse<TokenResponse>> loginWithMpin(
            @Valid @RequestBody MpinLoginRequest request) {
        log.info("MPIN login request received for: {}", request.getIdentifier());

        MpinService.MpinValidationResult result = mpinService.validateMpin(
                request.getIdentifier(),
                request.getMpin()
        );

        // Convert to TokenResponse
        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken((String) result.getTokens().get("access_token"))
                .refreshToken((String) result.getTokens().get("refresh_token"))
                .expiresIn((Integer) result.getTokens().get("expires_in"))
                .tokenType((String) result.getTokens().get("token_type"))
                .user(userService.toDto(result.getUser()))
                .build();

        return ResponseEntity.ok(
                ApiResponse.success("MPIN authentication successful", tokenResponse)
        );
    }

    /**
     * Delete MPIN (protected endpoint)
     * Remove MPIN for current user
     */
    @DeleteMapping("/mpin")
    public ResponseEntity<ApiResponse<String>> deleteMpin(
            @AuthenticationPrincipal Jwt jwt) {
        log.info("Delete MPIN request received");

        String keycloakId = jwt.getSubject();
        User user = userService.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        mpinService.deleteMpin(user.getId());

        return ResponseEntity.ok(
                ApiResponse.success("MPIN deleted successfully", "You will need to login with OTP")
        );
    }

    /**
     * Get MPIN status (protected endpoint)
     * Check if MPIN is set and valid
     */
    @GetMapping("/mpin/status")
    public ResponseEntity<ApiResponse<MpinStatusResponse>> getMpinStatus(
            @AuthenticationPrincipal Jwt jwt) {
        log.info("Get MPIN status request received");

        String keycloakId = jwt.getSubject();
        User user = userService.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        MpinService.MpinStatus status = mpinService.getMpinStatus(user.getId());

        MpinStatusResponse response = MpinStatusResponse.builder()
                .isSet(status.isSet())
                .isValid(status.isValid())
                .sessionExpiresAt(status.getSessionExpiresAt())
                .failedAttempts(status.getFailedAttempts())
                .lastUsedAt(status.getLastUsedAt())
                .build();

        return ResponseEntity.ok(
                ApiResponse.success(response)
        );
    }
}
