package com.shashankpk.oauth2.starter.service;

import com.shashankpk.oauth2.starter.exception.OAuth2AuthenticationException;
import com.shashankpk.oauth2.starter.properties.OAuth2Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

/**
 * Service for JWT token validation and inspection
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenValidationService {

    private final JwtDecoder jwtDecoder;
    private final OAuth2Properties oauth2Properties;

    /**
     * Validate and decode JWT token
     */
    public Jwt validateToken(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            log.info("Token validated successfully for subject: {}", jwt.getSubject());
            return jwt;
        } catch (JwtException e) {
            log.error("Token validation failed: {}", e.getMessage());
            throw new OAuth2AuthenticationException("Invalid token: " + e.getMessage(), e);
        }
    }

    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(Jwt token) {
        Instant expiresAt = token.getExpiresAt();
        return expiresAt != null && expiresAt.isBefore(Instant.now());
    }

    /**
     * Extract claims from token
     */
    public Map<String, Object> extractClaims(String token) {
        Jwt jwt = validateToken(token);
        return jwt.getClaims();
    }

    /**
     * Extract username from token
     */
    public String extractUsername(String token) {
        Jwt jwt = validateToken(token);
        String username = jwt.getClaimAsString("preferred_username");
        if (username == null) {
            username = jwt.getSubject();
        }
        return username;
    }

    /**
     * Validate token and return user subject
     */
    public String validateAndGetSubject(String token) {
        Jwt jwt = validateToken(token);
        if (isTokenExpired(jwt)) {
            throw new OAuth2AuthenticationException("Token has expired");
        }
        return jwt.getSubject();
    }
}
