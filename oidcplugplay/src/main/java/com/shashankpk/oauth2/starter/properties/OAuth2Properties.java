package com.shashankpk.oauth2.starter.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import java.util.HashMap;
import java.util.Map;

// These are in case user didn't give those properties themselves
@Data
@Validated
@ConfigurationProperties(prefix = "oauth2")
public class OAuth2Properties {

    /**
     * Enable/disable OAuth2 auto-configuration
     */
    private boolean enabled = true;

    /**
     * OAuth2 provider type (KEYCLOAK, OKTA, GOOGLE, META, GITHUB, CUSTOM)
     */
    @NotBlank(message = "OAuth2 provider must be specified")
    private String provider = "KEYCLOAK";

    /**
     * OAuth2 client ID
     */
    @NotBlank(message = "OAuth2 client-id must be specified")
    private String clientId;

    /**
     * OAuth2 client secret
     */
    @NotBlank(message = "OAuth2 client-secret must be specified")
    private String clientSecret;

    /**
     * OAuth2 issuer URI (authorization server base URL)
     */
    @NotBlank(message = "OAuth2 issuer-uri must be specified")
    private String issuerUri;

    /**
     * Authorization endpoint (auto-detected if not provided)
     */
    private String authorizationUri;

    /**
     * Token endpoint (auto-detected if not provided)
     */
    private String tokenUri;

    /**
     * User info endpoint (auto-detected if not provided)
     */
    private String userInfoUri;

    /**
     * JWK Set URI for token validation (auto-detected if not provided)
     */
    private String jwkSetUri;

    /**
     * Redirect URI after successful authentication
     */
    private String redirectUri = "/login/oauth2/code";

    /**
     * Logout URI
     */
    private String logoutUri;

    /**
     * Post logout redirect URI
     */
    private String postLogoutRedirectUri;

    /**
     * OAuth2 scopes
     */
    private String[] scopes = {"openid", "profile", "email"};

    /**
     * Enable custom login page support (password grant / direct authentication)
     */
    private boolean customLoginEnabled = false;

    /**
     * Token endpoint for direct authentication (if different from main token endpoint)
     */
    private String directAuthTokenUri;

    /**
     * JWT token validation settings
     */
    private JwtProperties jwt = new JwtProperties();

    /**
     * CORS settings for OAuth2 endpoints
     */
    private CorsProperties cors = new CorsProperties();

    /**
     * Additional custom properties for specific provider configurations
     */
    private Map<String, String> customProperties = new HashMap<>();

    @Data
    public static class JwtProperties {
        /**
         * JWT token expiration time in seconds (default: 3600 = 1 hour)
         */
        private long accessTokenValidity = 3600;

        /**
         * JWT refresh token expiration time in seconds (default: 86400 = 24 hours)
         */
        private long refreshTokenValidity = 86400;

        /**
         * Validate token issuer
         */
        private boolean validateIssuer = true;

        /**
         * Validate token audience
         */
        private boolean validateAudience = false;

        /**
         * Expected audience value
         */
        private String audience;
    }

    @Data
    public static class CorsProperties {
        /**
         * Enable CORS for OAuth2 endpoints
         */
        private boolean enabled = true;

        /**
         * Allowed origins (default: all)
         */
        private String[] allowedOrigins = {"*"};

        /**
         * Allowed methods
         */
        private String[] allowedMethods = {"GET", "POST", "PUT", "DELETE", "OPTIONS"};

        /**
         * Allowed headers
         */
        private String[] allowedHeaders = {"*"};

        /**
         * Exposed headers
         */
        private String[] exposedHeaders = {"Authorization", "Content-Type"};

        /**
         * Allow credentials
         */
        private boolean allowCredentials = true;

        /**
         * Max age for preflight requests
         */
        private long maxAge = 3600;
    }
}
