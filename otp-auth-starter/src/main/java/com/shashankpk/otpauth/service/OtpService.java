package com.shashankpk.otpauth.service;

import com.shashankpk.otpauth.model.OtpRecord;
import com.shashankpk.otpauth.properties.OtpAuthProperties;
import com.shashankpk.otpauth.repository.OtpRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class OtpService {

    @Autowired
    private OtpRepository otpRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private OtpAuthProperties properties;

    private static final SecureRandom random = new SecureRandom();

    /**
     * Generate and send OTP
     */
    public String generateAndSendOtp(String identifier, OtpRecord.OtpType type) {
        log.info("Generating OTP for: {} (type: {})", identifier, type);

        // Generate OTP
        String otp = properties.getOtp().isTestingMode()
            ? properties.getOtp().getFixedOtp()
            : generateRandomOtp();

        // Invalidate previous OTPs
        otpRepository.invalidatePreviousOtps(identifier);

        // Save OTP
        OtpRecord record = new OtpRecord();
        record.setIdentifier(identifier);
        record.setOtpCode(otp);
        record.setType(type);
        record.setExpiresAt(LocalDateTime.now().plusSeconds(properties.getOtp().getExpirySeconds()));
        record.setAttempts(0);
        record.setVerified(false);
        otpRepository.save(record);

        // Send OTP
        if (type == OtpRecord.OtpType.PHONE) {
            sendOtpViaSms(identifier, otp);
        } else if (type == OtpRecord.OtpType.EMAIL) {
            sendOtpViaEmail(identifier, otp);
        }

        log.info("OTP generated successfully for: {}", identifier);

        return properties.getOtp().isTestingMode() ? otp : null;
    }

    /**
     * Verify OTP
     */
    public boolean verifyOtp(String identifier, String otp) {
        log.info("Verifying OTP for: {}", identifier);

        OtpRecord record = otpRepository.findByIdentifierAndOtpCodeAndVerifiedFalse(identifier, otp)
            .orElse(null);

        if (record == null) {
            log.warn("OTP not found for: {}", identifier);
            return false;
        }

        if (record.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("OTP expired for: {}", identifier);
            return false;
        }

        if (record.getAttempts() >= properties.getOtp().getMaxAttempts()) {
            log.warn("Max attempts exceeded for: {}", identifier);
            return false;
        }

        record.setAttempts(record.getAttempts() + 1);

        if (record.getOtpCode().equals(otp)) {
            record.setVerified(true);
            otpRepository.save(record);
            log.info("OTP verified successfully for: {}", identifier);
            return true;
        }

        otpRepository.save(record);
        return false;
    }

    /**
     * Generate random OTP
     */
    private String generateRandomOtp() {
        int length = properties.getOtp().getLength();
        int bound = (int) Math.pow(10, length);
        String format = "%0" + length + "d";
        return String.format(format, random.nextInt(bound));
    }

    /**
     * Send OTP via SMS
     */
    private void sendOtpViaSms(String phoneNumber, String otp) {
        if (properties.getOtp().isTestingMode()) {
            log.info("┌─────────────────────────────────────┐");
            log.info("│     🔐 OTP TESTING MODE            │");
            log.info("├─────────────────────────────────────┤");
            log.info("│ Phone: {}           │", phoneNumber);
            log.info("│ OTP:   {}                      │", otp);
            log.info("│ Valid: {} minutes              │", properties.getOtp().getExpirySeconds() / 60);
            log.info("└─────────────────────────────────────┘");

            if (properties.getOtp().isEnableFileLogging()) {
                saveOtpToFile(phoneNumber, otp);
            }

            if (properties.getOtp().getTestEmailRecipient() != null) {
                try {
                    emailService.sendEmail(
                        properties.getOtp().getTestEmailRecipient(),
                        "Test OTP for " + phoneNumber,
                        buildTestOtpEmail(phoneNumber, otp)
                    );
                } catch (Exception e) {
                    log.warn("Failed to send test OTP email: {}", e.getMessage());
                }
            }
        } else {
            // TODO: Production SMS sending
            log.info("SMS would be sent to: {} in production mode", phoneNumber);
        }
    }

    /**
     * Send OTP via Email
     */
    private void sendOtpViaEmail(String email, String otp) {
        emailService.sendEmail(email, "Your OTP Code", buildOtpEmailTemplate(otp));
        log.info("OTP email sent to: {}", email);
    }

    /**
     * Save OTP to file
     */
    private void saveOtpToFile(String identifier, String otp) {
        try {
            String filename = "otp_logs/otp_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".txt";

            Path directory = Paths.get("otp_logs");
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
            }

            String logEntry = String.format("[%s] %s -> %s%n",
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                identifier, otp);

            Files.write(Paths.get(filename), logEntry.getBytes(),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        } catch (Exception e) {
            log.error("Failed to save OTP to file", e);
        }
    }

    /**
     * Build test OTP email
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
            <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 0;">
                <div style="max-width: 600px; margin: 40px auto; background: white; border-radius: 8px;">
                    <div style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); padding: 30px; text-align: center; border-radius: 8px 8px 0 0;">
                        <h1 style="color: white; margin: 0; font-size: 28px;">Verify Your Identity</h1>
                    </div>
                    <div style="padding: 40px 30px;">
                        <p style="color: #333; font-size: 16px;">Your OTP is:</p>
                        <div style="background: #f8f9fa; padding: 25px; border-radius: 8px; text-align: center; margin: 25px 0;">
                            <div style="font-size: 48px; font-weight: bold; color: #4CAF50; letter-spacing: 8px;">%s</div>
                        </div>
                        <div style="background: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0;">
                            <p style="margin: 0; color: #856404; font-size: 14px;">⚠️ Valid for %d minutes only.</p>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """, otp, properties.getOtp().getExpirySeconds() / 60);
    }
}
