package com.auth.identity.service;

import com.auth.identity.dto.OtpResponse;
import com.auth.identity.dto.RegisterRequest;
import com.auth.identity.dto.TokenResponse;
import com.auth.identity.dto.UserDto;
import com.auth.identity.exception.AuthException;
import com.auth.identity.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final OtpService otpService;
    private final KeycloakService keycloakService;
    private final UserService userService;

    public OtpResponse register(RegisterRequest request) {
        String identifier = request.getIdentifier();
        boolean isEmail = request.getIdentifierType() == RegisterRequest.IdentifierType.EMAIL;

        // Check if user already exists
        if (userService.existsByIdentifier(identifier)) {
            throw new AuthException("User with this " +
                    (isEmail ? "email" : "phone") + " already exists");
        }

        // Generate and send OTP
        otpService.generateAndSaveOtp(identifier);

        log.info("Registration OTP sent to: {}", identifier);

        return OtpResponse.builder()
                .identifier(identifier)
                .expiresIn(otpService.getOtpExpiryMinutes() * 60)
                .build();
    }

    public OtpResponse sendOtp(String identifier) {
        // Check if user exists
        if (!userService.existsByIdentifier(identifier)) {
            throw new AuthException("User not found. Please register first");
        }

        // Generate and send OTP
        otpService.generateAndSaveOtp(identifier);

        log.info("Login OTP sent to: {}", identifier);

        return OtpResponse.builder()
                .identifier(identifier)
                .expiresIn(otpService.getOtpExpiryMinutes() * 60)
                .build();
    }

    public TokenResponse verifyOtp(String identifier, String otp) {
        // Verify OTP
        otpService.verifyOtp(identifier, otp);

        // Check if user exists in our database
        Optional<User> existingUser = userService.findByIdentifier(identifier);

        User user;
        if (existingUser.isEmpty()) {
            // New user - create in Keycloak and our database
            boolean isEmail = identifier.contains("@");
            String keycloakId = keycloakService.createUser(identifier, isEmail);

            user = userService.createUser(
                    keycloakId,
                    isEmail ? identifier : null,
                    isEmail ? null : identifier
            );

            log.info("New user created with ID: {}", user.getId());
        } else {
            user = existingUser.get();
            log.info("Existing user authenticated: {}", user.getId());
        }

        // Get token from Keycloak
        Map<String, Object> tokenData = keycloakService.getTokenForUser(identifier);

        // Build response
        return TokenResponse.builder()
                .accessToken((String) tokenData.get("access_token"))
                .refreshToken((String) tokenData.get("refresh_token"))
                .expiresIn((Integer) tokenData.get("expires_in"))
                .tokenType("Bearer")
                .user(userService.toDto(user))
                .build();
    }

    public TokenResponse refreshToken(String refreshToken) {
        Map<String, Object> tokenData = keycloakService.refreshToken(refreshToken);

        return TokenResponse.builder()
                .accessToken((String) tokenData.get("access_token"))
                .refreshToken((String) tokenData.get("refresh_token"))
                .expiresIn((Integer) tokenData.get("expires_in"))
                .tokenType("Bearer")
                .build();
    }
}
