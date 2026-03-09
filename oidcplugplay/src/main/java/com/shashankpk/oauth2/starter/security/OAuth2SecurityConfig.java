package com.shashankpk.oauth2.starter.security;

import com.shashankpk.oauth2.starter.properties.OAuth2Properties;
import com.shashankpk.oauth2.starter.provider.OAuth2Provider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Security configuration for OAuth2/OIDC
 * Configures JWT token validation and CORS
 */
@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "oauth2", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OAuth2SecurityConfig {

    private final OAuth2Properties oauth2Properties;

    /**
     * Configure security filter chain
     * Permits OAuth2 endpoints and validates JWT for protected resources
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring OAuth2 security filter chain");

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                        		"/api/public/**",
                                "/oauth2/authorize",
                                "/oauth2/token",
                                "/oauth2/login",
                                "/oauth2/refresh",
                                "/oauth2/health",
                                "/error"
                        ).permitAll()
                        // Protected endpoints
                        .requestMatchers("/oauth2/userinfo", "/oauth2/validate").authenticated()
                        // All other endpoints require authentication by default
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.decoder(jwtDecoder()))
                );

        log.info("OAuth2 security configuration completed");
        return http.build();
    }

    /**
     * JWT decoder for token validation
     * Validates tokens against the configured OAuth2 provider
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        OAuth2Provider provider = OAuth2Provider.fromString(oauth2Properties.getProvider());
        String jwkSetUri = determineJwkSetUri(provider);

        log.info("Configuring JWT decoder for provider: {} with JWK Set URI: {}",
                provider.getDisplayName(), jwkSetUri);

        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }

    /**
     * CORS configuration
     * Allows cross-origin requests from configured origins
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        if (!oauth2Properties.getCors().isEnabled()) {
            log.info("CORS is disabled");
            return request -> null;
        }

        log.info("Configuring CORS with allowed origins: {}",
                Arrays.toString(oauth2Properties.getCors().getAllowedOrigins()));

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(oauth2Properties.getCors().getAllowedOrigins()));
        configuration.setAllowedMethods(Arrays.asList(oauth2Properties.getCors().getAllowedMethods()));
        configuration.setAllowedHeaders(Arrays.asList(oauth2Properties.getCors().getAllowedHeaders()));
        configuration.setExposedHeaders(Arrays.asList(oauth2Properties.getCors().getExposedHeaders()));
        configuration.setAllowCredentials(oauth2Properties.getCors().isAllowCredentials());
        configuration.setMaxAge(oauth2Properties.getCors().getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    /**
     * Determine JWK Set URI based on provider and configuration
     */
    private String determineJwkSetUri(OAuth2Provider provider) {
        if (oauth2Properties.getJwkSetUri() != null) {
            return oauth2Properties.getJwkSetUri();
        }

        String uri = provider.buildJwkSetUri(oauth2Properties.getIssuerUri());
        if (uri == null) {
            throw new IllegalStateException(
                    "JWK Set URI not configured for provider: " + provider.getDisplayName() +
                            ". Please set oauth2.jwk-set-uri property."
            );
        }

        return uri;
    }
}
