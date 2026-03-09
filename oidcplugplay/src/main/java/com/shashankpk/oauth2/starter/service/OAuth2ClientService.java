package com.shashankpk.oauth2.starter.service;

import com.shashankpk.oauth2.starter.dto.LoginRequest;
import com.shashankpk.oauth2.starter.dto.TokenResponse;
import com.shashankpk.oauth2.starter.dto.UserInfo;
import com.shashankpk.oauth2.starter.exception.OAuth2AuthenticationException;
import com.shashankpk.oauth2.starter.properties.OAuth2Properties;
import com.shashankpk.oauth2.starter.provider.OAuth2Provider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Service for OAuth2 client operations
 * Handles token exchange, refresh, and user info retrieval
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2ClientService {

    private final OAuth2Properties oauth2Properties;
    private final WebClient.Builder webClientBuilder;

    /**
     * Exchange authorization code for access token
     * Used in standard OAuth2 authorization code flow
     */
    public TokenResponse exchangeCodeForToken(String code, String redirectUri) {
        log.info("Exchanging authorization code for token");

        OAuth2Provider provider = OAuth2Provider.fromString(oauth2Properties.getProvider());
        String tokenUri = determineTokenUri(provider);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("code", code);
        formData.add("client_id", oauth2Properties.getClientId());
        formData.add("client_secret", oauth2Properties.getClientSecret());
        formData.add("redirect_uri", redirectUri);

        return executeTokenRequest(tokenUri, formData);
    }

    /**
     * Authenticate with username and password (direct authentication)
     * Used for custom login pages - requires Resource Owner Password Credentials Grant
     * Note: This grant type must be enabled in your OAuth2 provider
     */
    public TokenResponse authenticateWithPassword(LoginRequest loginRequest) {
        log.info("Authenticating with password for user: {}", loginRequest.getUsername());

        if (!oauth2Properties.isCustomLoginEnabled()) {
            throw new OAuth2AuthenticationException(
                "Custom login is not enabled. Set oauth2.custom-login-enabled=true in properties"
            );
        }

        OAuth2Provider provider = OAuth2Provider.fromString(oauth2Properties.getProvider());
        String tokenUri = oauth2Properties.getDirectAuthTokenUri() != null
                ? oauth2Properties.getDirectAuthTokenUri()
                : determineTokenUri(provider);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("username", loginRequest.getUsername());
        formData.add("password", loginRequest.getPassword());
        formData.add("client_id", oauth2Properties.getClientId());
        formData.add("client_secret", oauth2Properties.getClientSecret());

        if (loginRequest.getScope() != null) {
            formData.add("scope", loginRequest.getScope());
        } else {
            formData.add("scope", String.join(" ", oauth2Properties.getScopes()));
        }

        return executeTokenRequest(tokenUri, formData);
    }

    /**
     * Refresh access token using refresh token
     */
    public TokenResponse refreshToken(String refreshToken) {
        log.info("Refreshing access token");

        OAuth2Provider provider = OAuth2Provider.fromString(oauth2Properties.getProvider());
        String tokenUri = determineTokenUri(provider);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "refresh_token");
        formData.add("refresh_token", refreshToken);
        formData.add("client_id", oauth2Properties.getClientId());
        formData.add("client_secret", oauth2Properties.getClientSecret());

        return executeTokenRequest(tokenUri, formData);
    }

    /**
     * Retrieve user information using access token
     */
    public UserInfo getUserInfo(String accessToken) {
        log.info("Retrieving user information");

        OAuth2Provider provider = OAuth2Provider.fromString(oauth2Properties.getProvider());
        String userInfoUri = determineUserInfoUri(provider);

        try {
            WebClient webClient = webClientBuilder.build();

            UserInfo userInfo = webClient.get()
                    .uri(userInfoUri)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .bodyToMono(UserInfo.class)
                    .block();

            log.info("Successfully retrieved user info for subject: {}", userInfo.getSubject());
            return userInfo;

        } catch (WebClientResponseException e) {
            log.error("Failed to retrieve user info. Status: {}, Response: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new OAuth2AuthenticationException("Failed to retrieve user information", e);
        } catch (Exception e) {
            log.error("Error retrieving user info", e);
            throw new OAuth2AuthenticationException("Failed to retrieve user information", e);
        }
    }

    /**
     * Execute token request to OAuth2 provider
     */
    private TokenResponse executeTokenRequest(String tokenUri, MultiValueMap<String, String> formData) {
        try {
            WebClient webClient = webClientBuilder.build();

            TokenResponse response = webClient.post()
                    .uri(tokenUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .bodyToMono(TokenResponse.class)
                    .block();

            if (response == null) {
                throw new OAuth2AuthenticationException("Empty response from token endpoint");
            }

            log.info("Successfully obtained access token");
            return response;

        } catch (WebClientResponseException e) {
            log.error("Token request failed. Status: {}, Response: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new OAuth2AuthenticationException(
                "Authentication failed: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e
            );
        } catch (Exception e) {
            log.error("Error during token request", e);
            throw new OAuth2AuthenticationException("Authentication failed", e);
        }
    }

    /**
     * Determine token URI based on provider and configuration
     */
    private String determineTokenUri(OAuth2Provider provider) {
        if (oauth2Properties.getTokenUri() != null) {
            return oauth2Properties.getTokenUri();
        }
        String uri = provider.buildTokenUri(oauth2Properties.getIssuerUri());
        if (uri == null) {
            throw new OAuth2AuthenticationException(
                "Token URI not configured for provider: " + provider.getDisplayName()
            );
        }
        return uri;
    }

    /**
     * Determine user info URI based on provider and configuration
     */
    private String determineUserInfoUri(OAuth2Provider provider) {
        if (oauth2Properties.getUserInfoUri() != null) {
            return oauth2Properties.getUserInfoUri();
        }
        String uri = provider.buildUserInfoUri(oauth2Properties.getIssuerUri());
        if (uri == null) {
            throw new OAuth2AuthenticationException(
                "User info URI not configured for provider: " + provider.getDisplayName()
            );
        }
        return uri;
    }

    /**
     * Get authorization URL for OAuth2 flow
     */
    public String getAuthorizationUrl(String state, String redirectUri) {
        OAuth2Provider provider = OAuth2Provider.fromString(oauth2Properties.getProvider());
        String authUri = oauth2Properties.getAuthorizationUri() != null
                ? oauth2Properties.getAuthorizationUri()
                : provider.buildAuthorizationUri(oauth2Properties.getIssuerUri());

        if (authUri == null) {
            throw new OAuth2AuthenticationException(
                "Authorization URI not configured for provider: " + provider.getDisplayName()
            );
        }

        StringBuilder url = new StringBuilder(authUri);
        url.append("?response_type=code");
        url.append("&client_id=").append(oauth2Properties.getClientId());
        url.append("&redirect_uri=").append(redirectUri);
        url.append("&scope=").append(String.join(" ", oauth2Properties.getScopes()));
        if (state != null) {
            url.append("&state=").append(state);
        }

        return url.toString();
    }
}
