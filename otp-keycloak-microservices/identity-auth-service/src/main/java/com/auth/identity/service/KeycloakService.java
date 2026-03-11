package com.auth.identity.service;

import com.auth.identity.dto.TokenResponse;
import com.auth.identity.exception.AuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.ws.rs.core.Response;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakService {

    private final Keycloak keycloak;
    private final WebClient.Builder webClientBuilder;

    @Value("${keycloak.server-url}")
    private String serverUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    public String createUser(String identifier, boolean isEmail) {
        try {
            UserRepresentation user = new UserRepresentation();
            user.setEnabled(true);
            user.setUsername(identifier);

            if (isEmail) {
                user.setEmail(identifier);
                user.setEmailVerified(true);
            }

            // Set a temporary password (will not be used for OTP login)
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(UUID.randomUUID().toString());
            credential.setTemporary(false);
            user.setCredentials(Collections.singletonList(credential));

            // Set additional attributes
            Map<String, List<String>> attributes = new HashMap<>();
            if (!isEmail) {
                attributes.put("phone", Collections.singletonList(identifier));
            }
            user.setAttributes(attributes);

            Response response = keycloak.realm(realm).users().create(user);

            if (response.getStatus() == 201) {
                String userId = extractUserIdFromLocation(response.getLocation().toString());
                log.info("User created in Keycloak with ID: {}", userId);
                return userId;
            } else {
                String errorMessage = response.readEntity(String.class);
                log.error("Failed to create user in Keycloak. Status: {}, Error: {}",
                        response.getStatus(), errorMessage);
                throw new AuthException("Failed to create user in Keycloak: " + errorMessage);
            }
        } catch (Exception e) {
            log.error("Error creating user in Keycloak: ", e);
            throw new AuthException("Failed to create user in Keycloak", e);
        }
    }

    public Map<String, Object> getTokenForUser(String identifier) {
        try {
            String tokenUrl = serverUrl + "/realms/" + realm + "/protocol/openid-connect/token";

            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("grant_type", "password");
            formData.add("client_id", clientId);
            formData.add("client_secret", clientSecret);
            formData.add("username", identifier);
            formData.add("password", UUID.randomUUID().toString()); // Dummy password

            // Note: This will fail with normal password grant, but we're using it as a workaround
            // In production, you should use a custom Keycloak provider or extension
            // For this demo, we'll use client credentials to get a token

            formData.clear();
            formData.add("grant_type", "client_credentials");
            formData.add("client_id", clientId);
            formData.add("client_secret", clientSecret);

            WebClient webClient = webClientBuilder.build();

            Map<String, Object> response = webClient.post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            log.info("Token obtained from Keycloak for user: {}", identifier);
            return response;

        } catch (Exception e) {
            log.error("Error getting token from Keycloak: ", e);
            throw new AuthException("Failed to obtain token from Keycloak", e);
        }
    }

    public Map<String, Object> refreshToken(String refreshToken) {
        try {
            String tokenUrl = serverUrl + "/realms/" + realm + "/protocol/openid-connect/token";

            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("grant_type", "refresh_token");
            formData.add("client_id", clientId);
            formData.add("client_secret", clientSecret);
            formData.add("refresh_token", refreshToken);

            WebClient webClient = webClientBuilder.build();

            Map<String, Object> response = webClient.post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            log.info("Token refreshed successfully");
            return response;

        } catch (Exception e) {
            log.error("Error refreshing token: ", e);
            throw new AuthException("Failed to refresh token", e);
        }
    }

    public UserRepresentation getUserByUsername(String username) {
        try {
            List<UserRepresentation> users = keycloak.realm(realm)
                    .users()
                    .search(username, true);

            if (users.isEmpty()) {
                return null;
            }

            return users.get(0);
        } catch (Exception e) {
            log.error("Error getting user from Keycloak: ", e);
            return null;
        }
    }

    private String extractUserIdFromLocation(String location) {
        String[] parts = location.split("/");
        return parts[parts.length - 1];
    }
}
