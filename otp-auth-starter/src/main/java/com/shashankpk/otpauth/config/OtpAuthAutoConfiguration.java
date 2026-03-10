package com.shashankpk.otpauth.config;

import com.shashankpk.otpauth.properties.OtpAuthProperties;
import com.shashankpk.otpauth.service.*;
import com.shashankpk.otpauth.controller.OtpAuthController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.PostConstruct;

@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(OtpAuthProperties.class)
@ConditionalOnProperty(prefix = "otp.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "com.shashankpk.otpauth")
public class OtpAuthAutoConfiguration {

    private final OtpAuthProperties properties;

    public OtpAuthAutoConfiguration(OtpAuthProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║        OTP Authentication Starter Initialized                  ║");
        log.info("╠════════════════════════════════════════════════════════════════╣");
        log.info("║ Keycloak Server: {}", String.format("%-43s", properties.getKeycloak().getServerUrl()) + "║");
        log.info("║ Realm:           {}", String.format("%-43s", properties.getKeycloak().getRealm()) + "║");
        log.info("║ Client ID:       {}", String.format("%-43s", properties.getKeycloak().getClientId()) + "║");
        log.info("║ Testing Mode:    {}", String.format("%-43s", properties.getOtp().isTestingMode() ? "ENABLED ⚠️" : "DISABLED") + "║");
        log.info("║ OTP Expiry:      {}", String.format("%-43s", properties.getOtp().getExpirySeconds() + " seconds") + "║");
        log.info("╚════════════════════════════════════════════════════════════════╝");

        if (properties.getOtp().isTestingMode()) {
            log.warn("⚠️  OTP Testing Mode is ENABLED - All OTPs will use: {}", properties.getOtp().getFixedOtp());
            log.warn("⚠️  Disable testing mode in production by setting: otp.auth.otp.testing-mode=false");
        }
    }

    @Bean
    @ConditionalOnProperty(prefix = "otp.auth.cors", name = "enabled", havingValue = "true", matchIfMissing = true)
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                log.info("📡 Configuring CORS for OTP Auth endpoints");

                registry.addMapping("/auth/**")
                    .allowedOrigins(properties.getCors().getAllowedOrigins())
                    .allowedMethods(properties.getCors().getAllowedMethods())
                    .allowedHeaders(properties.getCors().getAllowedHeaders())
                    .allowCredentials(properties.getCors().isAllowCredentials())
                    .maxAge(properties.getCors().getMaxAge());
            }
        };
    }

    @Bean
    public OtpService otpService() {
        log.debug("Creating OtpService bean");
        return new OtpService();
    }

    @Bean
    public KeycloakService keycloakService() {
        log.debug("Creating KeycloakService bean");
        return new KeycloakService();
    }

    @Bean
    public UserService userService() {
        log.debug("Creating UserService bean");
        return new UserService();
    }

    @Bean
    public EmailService emailService() {
        log.debug("Creating EmailService bean");
        return new EmailService();
    }

    @Bean
    public OtpAuthController otpAuthController() {
        log.debug("Creating OtpAuthController bean");
        return new OtpAuthController();
    }
}
