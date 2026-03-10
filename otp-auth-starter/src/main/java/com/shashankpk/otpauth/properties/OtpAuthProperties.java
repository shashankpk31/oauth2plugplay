package com.shashankpk.otpauth.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "otp.auth")
public class OtpAuthProperties {

    /**
     * Enable/disable OTP authentication
     */
    private boolean enabled = true;

    /**
     * Keycloak Configuration
     */
    private Keycloak keycloak = new Keycloak();

    /**
     * OTP Configuration
     */
    private Otp otp = new Otp();

    /**
     * Email Configuration (optional - uses spring.mail if not specified)
     */
    private Email email = new Email();

    /**
     * SMS Configuration (optional - for future use)
     */
    private Sms sms = new Sms();

    /**
     * CORS Configuration
     */
    private Cors cors = new Cors();

    @Data
    public static class Keycloak {
        /**
         * Keycloak server URL (e.g., http://localhost:8180)
         */
        private String serverUrl;

        /**
         * Realm name
         */
        private String realm;

        /**
         * Client ID
         */
        private String clientId;

        /**
         * Client Secret
         */
        private String clientSecret;

        /**
         * Admin username
         */
        private String adminUsername = "admin";

        /**
         * Admin password
         */
        private String adminPassword = "admin";

        /**
         * Default role to assign to new users
         */
        private String defaultRole = "user";
    }

    @Data
    public static class Otp {
        /**
         * Testing mode - uses fixed OTP and logs to console/file
         */
        private boolean testingMode = true;

        /**
         * Fixed OTP for testing (only works when testingMode=true)
         */
        private String fixedOtp = "123456";

        /**
         * OTP length (default: 6 digits)
         */
        private int length = 6;

        /**
         * OTP expiry in seconds (default: 5 minutes)
         */
        private int expirySeconds = 300;

        /**
         * Maximum attempts before OTP becomes invalid
         */
        private int maxAttempts = 3;

        /**
         * Enable file logging for OTPs
         */
        private boolean enableFileLogging = true;

        /**
         * Email address to send test OTPs to
         */
        private String testEmailRecipient;
    }

    @Data
    public static class Email {
        /**
         * Enable email OTP
         */
        private boolean enabled = true;
    }

    @Data
    public static class Sms {
        /**
         * Enable SMS OTP
         */
        private boolean enabled = false;

        /**
         * SMS provider (twilio, aws-sns, custom)
         */
        private String provider = "none";

        /**
         * Twilio configuration
         */
        private Twilio twilio = new Twilio();

        @Data
        public static class Twilio {
            private String accountSid;
            private String authToken;
            private String fromNumber;
        }
    }

    @Data
    public static class Cors {
        /**
         * Enable CORS
         */
        private boolean enabled = true;

        /**
         * Allowed origins
         */
        private String[] allowedOrigins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:19006"};

        /**
         * Allowed methods
         */
        private String[] allowedMethods = {"GET", "POST", "PUT", "DELETE", "OPTIONS"};

        /**
         * Allowed headers
         */
        private String[] allowedHeaders = {"*"};

        /**
         * Allow credentials
         */
        private boolean allowCredentials = true;

        /**
         * Max age in seconds
         */
        private long maxAge = 3600;
    }
}
