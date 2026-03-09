package com.shashankpk.oauth2.starter.config;

import com.shashankpk.oauth2.starter.properties.OAuth2Properties;
import com.shashankpk.oauth2.starter.provider.OAuth2Provider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Auto-configuration for OAuth2/OIDC Starter
 * Automatically configures OAuth2 when this starter is added to classpath
 *
 * To enable: Add this dependency and configure properties
 * To disable: Set oauth2.enabled=false
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.security.oauth2.jwt.JwtDecoder")
@ConditionalOnProperty(prefix = "oauth2", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(OAuth2Properties.class)
@ComponentScan(basePackages = "com.shashankpk.oauth2.starter")
public class OAuth2AutoConfiguration {

    public OAuth2AutoConfiguration(OAuth2Properties oauth2Properties) {
        OAuth2Provider provider = OAuth2Provider.fromString(oauth2Properties.getProvider());

        log.info("=".repeat(80));
        log.info("OAuth2/OIDC Spring Boot Starter - Auto-Configuration");
        log.info("=".repeat(80));
        log.info("Provider: {}", provider.getDisplayName());
        log.info("Client ID: {}", oauth2Properties.getClientId());
        log.info("Issuer URI: {}", oauth2Properties.getIssuerUri());
        log.info("Custom Login Enabled: {}", oauth2Properties.isCustomLoginEnabled());
        log.info("CORS Enabled: {}", oauth2Properties.getCors().isEnabled());
        log.info("=".repeat(80));

        validateConfiguration(oauth2Properties, provider);
    }

    /**
     * WebClient builder bean for HTTP requests to OAuth2 provider
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    /**
     * Validate OAuth2 configuration
     */
    private void validateConfiguration(OAuth2Properties properties, OAuth2Provider provider) {
        if (properties.getClientId() == null || properties.getClientId().isBlank()) {
            throw new IllegalStateException("OAuth2 client-id must be configured");
        }

        if (properties.getClientSecret() == null || properties.getClientSecret().isBlank()) {
            throw new IllegalStateException("OAuth2 client-secret must be configured");
        }

        if (properties.getIssuerUri() == null || properties.getIssuerUri().isBlank()) {
            throw new IllegalStateException("OAuth2 issuer-uri must be configured");
        }

        if (properties.isCustomLoginEnabled()) {
            log.warn("Custom login is enabled. Ensure your OAuth2 provider supports Resource Owner Password Credentials Grant");
            log.warn("For Keycloak: Enable 'Direct Access Grants' in client settings");
            log.warn("For Okta: This grant type may require additional configuration");
        }

        // Warn about missing JWK Set URI for providers that don't auto-configure
        if (provider == OAuth2Provider.CUSTOM && properties.getJwkSetUri() == null) {
            log.warn("JWK Set URI not configured for CUSTOM provider. JWT validation may fail.");
            log.warn("Please set oauth2.jwk-set-uri property");
        }

        log.info("OAuth2 configuration validation completed successfully");
    }
}
