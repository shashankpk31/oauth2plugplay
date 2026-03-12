package com.auth.identity.service;

import com.auth.identity.exception.AuthException;
import com.auth.identity.model.MpinRecord;
import com.auth.identity.model.User;
import com.auth.identity.repository.MpinRepository;
import com.auth.identity.repository.UserRepository;
import com.auth.identity.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing MPIN (Mobile PIN) functionality
 * MPIN allows users to login quickly after initial OTP authentication
 * Similar to banking apps where PIN is valid for a limited time
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MpinService {

    private final MpinRepository mpinRepository;
    private final UserRepository userRepository;
    private final EncryptionUtil encryptionUtil;
    private final KeycloakService keycloakService;

    @Value("${mpin.session-expiry-days:30}")
    private int sessionExpiryDays;

    @Value("${mpin.max-attempts:3}")
    private int maxAttempts;

    @Value("${mpin.min-length:4}")
    private int minLength;

    @Value("${mpin.max-length:6}")
    private int maxLength;

    /**
     * Set or update MPIN for a user
     * User must be authenticated (have valid OTP session) to set MPIN
     *
     * @param userId User ID
     * @param mpin MPIN (4-6 digits)
     * @return Created or updated MpinRecord
     */
    @Transactional
    public MpinRecord setMpin(UUID userId, String mpin) {
        // Validate MPIN format
        validateMpinFormat(mpin);

        // Get user
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AuthException("User not found"));

        // Get identifier (email or phone)
        String identifier = user.getEmail() != null ? user.getEmail() : user.getPhone();
        if (identifier == null) {
            throw new AuthException("User has no email or phone");
        }

        // Encrypt MPIN
        String encryptedMpin = encryptionUtil.encrypt(mpin);

        // Check if MPIN already exists
        Optional<MpinRecord> existingMpin = mpinRepository.findByUserIdAndIsActiveTrue(userId);

        MpinRecord mpinRecord;
        if (existingMpin.isPresent()) {
            // Update existing MPIN
            mpinRecord = existingMpin.get();
            mpinRecord.setEncryptedMpin(encryptedMpin);
            mpinRecord.setFailedAttempts(0);
            mpinRecord.extendSession(sessionExpiryDays);
            log.info("Updated MPIN for user: {}", userId);
        } else {
            // Create new MPIN
            mpinRecord = MpinRecord.builder()
                .userId(userId)
                .identifier(identifier)
                .encryptedMpin(encryptedMpin)
                .failedAttempts(0)
                .sessionExpiresAt(LocalDateTime.now().plusDays(sessionExpiryDays))
                .isActive(true)
                .build();
            log.info("Created new MPIN for user: {}", userId);
        }

        return mpinRepository.save(mpinRecord);
    }

    /**
     * Validate MPIN and return JWT tokens if valid
     * This is the quick login method using MPIN
     *
     * @param identifier Email or phone
     * @param mpin MPIN to validate
     * @return JWT tokens from Keycloak
     */
    @Transactional
    public MpinValidationResult validateMpin(String identifier, String mpin) {
        // Find active MPIN record
        MpinRecord mpinRecord = mpinRepository.findByIdentifierAndIsActiveTrue(identifier)
            .orElseThrow(() -> new AuthException("MPIN not set for this user"));

        // Check if session has expired
        if (mpinRecord.isSessionExpired()) {
            log.warn("MPIN session expired for identifier: {}", identifier);
            mpinRecord.setIsActive(false);
            mpinRepository.save(mpinRecord);
            throw new AuthException("MPIN session expired. Please login with OTP.");
        }

        // Check if MPIN is blocked due to too many failed attempts
        if (mpinRecord.isBlocked(maxAttempts)) {
            log.warn("MPIN blocked due to too many failed attempts for identifier: {}", identifier);
            throw new AuthException("MPIN blocked due to too many failed attempts. Please login with OTP.");
        }

        // Validate MPIN
        boolean isValid = encryptionUtil.matches(mpin, mpinRecord.getEncryptedMpin());

        if (!isValid) {
            // Increment failed attempts
            mpinRecord.incrementFailedAttempts();
            mpinRepository.save(mpinRecord);

            int remainingAttempts = maxAttempts - mpinRecord.getFailedAttempts();
            log.warn("Invalid MPIN for identifier: {}. Remaining attempts: {}", identifier, remainingAttempts);

            throw new AuthException("Invalid MPIN. Remaining attempts: " + remainingAttempts);
        }

        // MPIN is valid - reset failed attempts and update last used
        mpinRecord.resetFailedAttempts();
        mpinRecord.updateLastUsed();
        mpinRepository.save(mpinRecord);

        log.info("MPIN validated successfully for identifier: {}", identifier);

        // Get JWT tokens from Keycloak
        var tokens = keycloakService.getTokenForUser(identifier);

        // Get user details
        User user = userRepository.findById(mpinRecord.getUserId())
            .orElseThrow(() -> new AuthException("User not found"));

        return MpinValidationResult.builder()
            .success(true)
            .tokens(tokens)
            .user(user)
            .sessionExpiresAt(mpinRecord.getSessionExpiresAt())
            .build();
    }

    /**
     * Delete MPIN for a user (logout from MPIN)
     *
     * @param userId User ID
     */
    @Transactional
    public void deleteMpin(UUID userId) {
        mpinRepository.deactivateAllByUserId(userId);
        log.info("Deactivated MPIN for user: {}", userId);
    }

    /**
     * Check if MPIN session is valid for a user
     *
     * @param userId User ID
     * @return MpinStatus with details
     */
    @Transactional(readOnly = true)
    public MpinStatus getMpinStatus(UUID userId) {
        Optional<MpinRecord> mpinRecord = mpinRepository.findByUserIdAndIsActiveTrue(userId);

        if (mpinRecord.isEmpty()) {
            return MpinStatus.builder()
                .isSet(false)
                .isValid(false)
                .build();
        }

        MpinRecord record = mpinRecord.get();
        boolean isValid = record.isActive &&
                         !record.isSessionExpired() &&
                         !record.isBlocked(maxAttempts);

        return MpinStatus.builder()
            .isSet(true)
            .isValid(isValid)
            .sessionExpiresAt(record.getSessionExpiresAt())
            .failedAttempts(record.getFailedAttempts())
            .lastUsedAt(record.getLastUsedAt())
            .build();
    }

    /**
     * Extend MPIN session after successful OTP login
     * This renews the MPIN validity
     *
     * @param userId User ID
     */
    @Transactional
    public void extendMpinSession(UUID userId) {
        Optional<MpinRecord> mpinRecord = mpinRepository.findByUserIdAndIsActiveTrue(userId);
        if (mpinRecord.isPresent()) {
            MpinRecord record = mpinRecord.get();
            record.extendSession(sessionExpiryDays);
            record.resetFailedAttempts();
            mpinRepository.save(record);
            log.info("Extended MPIN session for user: {}", userId);
        }
    }

    /**
     * Scheduled job to clean up expired MPIN sessions
     * Runs daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();
        var expiredSessions = mpinRepository.findExpiredActiveSessions(now);

        if (!expiredSessions.isEmpty()) {
            log.info("Cleaning up {} expired MPIN sessions", expiredSessions.size());
            mpinRepository.deleteExpiredSessions(now);
        }
    }

    /**
     * Validate MPIN format
     */
    private void validateMpinFormat(String mpin) {
        if (mpin == null || mpin.isBlank()) {
            throw new AuthException("MPIN cannot be empty");
        }

        if (!mpin.matches("^\\d+$")) {
            throw new AuthException("MPIN must contain only digits");
        }

        if (mpin.length() < minLength || mpin.length() > maxLength) {
            throw new AuthException(
                String.format("MPIN must be between %d and %d digits", minLength, maxLength)
            );
        }
    }

    // Result classes
    @lombok.Data
    @lombok.Builder
    public static class MpinValidationResult {
        private boolean success;
        private java.util.Map<String, Object> tokens;
        private User user;
        private LocalDateTime sessionExpiresAt;
    }

    @lombok.Data
    @lombok.Builder
    public static class MpinStatus {
        private boolean isSet;
        private boolean isValid;
        private LocalDateTime sessionExpiresAt;
        private Integer failedAttempts;
        private LocalDateTime lastUsedAt;
    }
}
