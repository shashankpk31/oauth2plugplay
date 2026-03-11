package com.auth.identity.service;

import com.auth.identity.exception.AuthException;
import com.auth.identity.model.OtpRecord;
import com.auth.identity.repository.OtpRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpRepository otpRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${otp.expiry-minutes:5}")
    private Integer otpExpiryMinutes;

    @Value("${otp.max-attempts:3}")
    private Integer maxAttempts;

    @Value("${otp.length:6}")
    private Integer otpLength;

    @Transactional
    public String generateAndSaveOtp(String identifier) {
        // Delete any existing unverified OTPs for this identifier
        otpRepository.deleteUnverifiedOtpsByIdentifier(identifier);

        // Generate OTP
        String otp = generateOtp();

        // Create OTP record
        OtpRecord otpRecord = OtpRecord.builder()
                .identifier(identifier)
                .otp(otp)
                .expiresAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes))
                .attempts(0)
                .verified(false)
                .build();

        otpRepository.save(otpRecord);

        // Log OTP to console (for testing)
        log.info("==========================================");
        log.info("[OTP] Code for {}: {}", identifier, otp);
        log.info("[OTP] Expires at: {}", otpRecord.getExpiresAt());
        log.info("==========================================");

        return otp;
    }

    @Transactional
    public boolean verifyOtp(String identifier, String otp) {
        OtpRecord otpRecord = otpRepository
                .findTopByIdentifierAndVerifiedFalseOrderByCreatedAtDesc(identifier)
                .orElseThrow(() -> new AuthException("No OTP found for this identifier"));

        // Check if expired
        if (otpRecord.isExpired()) {
            throw new AuthException("OTP has expired. Please request a new one");
        }

        // Check attempts
        if (otpRecord.getAttempts() >= maxAttempts) {
            throw new AuthException("Maximum OTP verification attempts exceeded. Please request a new OTP");
        }

        // Increment attempts
        otpRecord.incrementAttempts();

        // Verify OTP
        if (!otpRecord.getOtp().equals(otp)) {
            otpRepository.save(otpRecord);
            throw new AuthException("Invalid OTP. Attempts remaining: " +
                    (maxAttempts - otpRecord.getAttempts()));
        }

        // Mark as verified
        otpRecord.setVerified(true);
        otpRepository.save(otpRecord);

        log.info("OTP verified successfully for identifier: {}", identifier);
        return true;
    }

    private String generateOtp() {
        int bound = (int) Math.pow(10, otpLength);
        int otp = secureRandom.nextInt(bound);
        return String.format("%0" + otpLength + "d", otp);
    }

    @Scheduled(fixedRate = 3600000) // Run every hour
    @Transactional
    public void cleanupExpiredOtps() {
        log.info("Cleaning up expired OTPs");
        otpRepository.deleteExpiredOtps(LocalDateTime.now());
    }

    public Integer getOtpExpiryMinutes() {
        return otpExpiryMinutes;
    }
}
