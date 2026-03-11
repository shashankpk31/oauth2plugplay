package com.auth.identity.controller;

import com.auth.identity.dto.*;
import com.auth.identity.model.User;
import com.auth.identity.service.AuthService;
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
}
