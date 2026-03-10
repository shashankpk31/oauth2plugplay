package com.shashankpk.otpauth.service;

import com.shashankpk.otpauth.dto.KeycloakUser;
import com.shashankpk.otpauth.dto.TokenResponse;
import com.shashankpk.otpauth.properties.OtpAuthProperties;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.ws.rs.core.Response;
import java.util.*;

@Slf4j
@Service
public class KeycloakService {

    @Autowired
    private OtpAuthProperties properties;

    private Keycloak keycloakAdminClient;
    private RestTemplate restTemplate;

    @PostConstruct
    public void init() {
        keycloakAdminClient = KeycloakBuilder.builder()
            .serverUrl(properties.getKeycloak().getServerUrl())
            .realm("master")
            .clientId("admin-cli")
            .username(properties.getKeycloak().getAdminUsername())
            .password(properties.getKeycloak().getAdminPassword())
            .build();

        restTemplate = new RestTemplate();
        log.info("Keycloak service initialized for realm: {}", properties.getKeycloak().getRealm());
    }

    /**
     * Find user by identifier (phone or email)
     */
    public KeycloakUser findUserByIdentifier(String identifier) {
        try {
            RealmResource realmResource = keycloakAdminClient.realm(properties.getKeycloak().getRealm());
            UsersResource usersResource = realmResource.users();

            List<UserRepresentation> users;

            if (identifier.contains("@")) {
                users = usersResource.search(null, null, null, identifier, 0, 1);
            } else {
                users = usersResource.search(identifier, true);

                if (users.isEmpty()) {
                    List<UserRepresentation> allUsers = usersResource.list();
                    users = allUsers.stream()
                        .filter(u -> {
                            Map<String, List<String>> attrs = u.getAttributes();
                            return attrs != null && attrs.containsKey("phoneNumber") &&
                                   attrs.get("phoneNumber").contains(identifier);
                        })
                        .limit(1)
                        .toList();
                }
            }

            if (users.isEmpty()) {
                log.info("User not found: {}", identifier);
                return null;
            }

            return mapToKeycloakUser(users.get(0));

        } catch (Exception e) {
            log.error("Error finding user: {}", identifier, e);
            throw new RuntimeException("Failed to find user", e);
        }
    }

    /**
     * Create new user in Keycloak
     */
    public KeycloakUser createUser(String identifier, String phoneNumber, String email, String name) {
        try {
            log.info("Creating new user: {}", identifier);

            RealmResource realmResource = keycloakAdminClient.realm(properties.getKeycloak().getRealm());
            UsersResource usersResource = realmResource.users();

            UserRepresentation user = new UserRepresentation();
            user.setEnabled(true);
            user.setUsername(identifier);

            if (email != null && !email.isEmpty()) {
                user.setEmail(email);
                user.setEmailVerified(true);
            }

            if (name != null && !name.isEmpty()) {
                String[] parts = name.trim().split("\\s+", 2);
                user.setFirstName(parts[0]);
                if (parts.length > 1) {
                    user.setLastName(parts[1]);
                }
            }

            Map<String, List<String>> attributes = new HashMap<>();
            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                attributes.put("phoneNumber", List.of(phoneNumber));
                attributes.put("phoneNumberVerified", List.of("true"));
            }
            attributes.put("createdVia", List.of("OTP"));
            user.setAttributes(attributes);

            Response response = usersResource.create(user);

            if (response.getStatus() != 201) {
                throw new RuntimeException("Failed to create user: " + response.getStatusInfo());
            }

            String locationPath = response.getLocation().getPath();
            String userId = locationPath.substring(locationPath.lastIndexOf('/') + 1);

            log.info("User created with ID: {}", userId);

            assignDefaultRole(userId);

            KeycloakUser keycloakUser = new KeycloakUser();
            keycloakUser.setId(userId);
            keycloakUser.setUsername(identifier);
            keycloakUser.setEmail(email);
            keycloakUser.setPhoneNumber(phoneNumber);
            keycloakUser.setName(name);
            keycloakUser.setNewlyCreated(true);

            return keycloakUser;

        } catch (Exception e) {
            log.error("Error creating user", e);
            throw new RuntimeException("Failed to create user", e);
        }
    }

    /**
     * Generate tokens for user
     */
    public TokenResponse generateTokensForUser(String userId) {
        try {
            String tokenUrl = properties.getKeycloak().getServerUrl() + "/realms/" +
                properties.getKeycloak().getRealm() + "/protocol/openid-connect/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "client_credentials");
            body.add("client_id", properties.getKeycloak().getClientId());
            body.add("client_secret", properties.getKeycloak().getClientSecret());
            body.add("scope", "openid profile email");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> tokenData = response.getBody();

                TokenResponse tokenResponse = new TokenResponse();
                tokenResponse.setAccessToken((String) tokenData.get("access_token"));
                tokenResponse.setRefreshToken((String) tokenData.get("refresh_token"));
                tokenResponse.setExpiresIn((Integer) tokenData.get("expires_in"));
                tokenResponse.setTokenType("Bearer");

                return tokenResponse;
            }

            throw new RuntimeException("Failed to generate tokens");

        } catch (Exception e) {
            log.error("Failed to generate tokens", e);
            throw new RuntimeException("Token generation failed", e);
        }
    }

    /**
     * Refresh access token
     */
    public TokenResponse refreshAccessToken(String refreshToken) {
        try {
            String tokenUrl = properties.getKeycloak().getServerUrl() + "/realms/" +
                properties.getKeycloak().getRealm() + "/protocol/openid-connect/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "refresh_token");
            body.add("client_id", properties.getKeycloak().getClientId());
            body.add("client_secret", properties.getKeycloak().getClientSecret());
            body.add("refresh_token", refreshToken);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

            Map<String, Object> tokenData = response.getBody();

            TokenResponse tokenResponse = new TokenResponse();
            tokenResponse.setAccessToken((String) tokenData.get("access_token"));
            tokenResponse.setRefreshToken((String) tokenData.get("refresh_token"));
            tokenResponse.setExpiresIn((Integer) tokenData.get("expires_in"));
            tokenResponse.setTokenType("Bearer");

            return tokenResponse;

        } catch (Exception e) {
            log.error("Failed to refresh token", e);
            throw new RuntimeException("Token refresh failed", e);
        }
    }

    /**
     * Assign default role to user
     */
    private void assignDefaultRole(String userId) {
        try {
            RealmResource realmResource = keycloakAdminClient.realm(properties.getKeycloak().getRealm());
            RoleRepresentation userRole = realmResource.roles()
                .get(properties.getKeycloak().getDefaultRole()).toRepresentation();

            realmResource.users().get(userId).roles().realmLevel()
                .add(Collections.singletonList(userRole));

            log.info("Assigned '{}' role to user: {}", properties.getKeycloak().getDefaultRole(), userId);

        } catch (Exception e) {
            log.warn("Failed to assign default role: {}", e.getMessage());
        }
    }

    /**
     * Map UserRepresentation to KeycloakUser
     */
    private KeycloakUser mapToKeycloakUser(UserRepresentation user) {
        KeycloakUser keycloakUser = new KeycloakUser();
        keycloakUser.setId(user.getId());
        keycloakUser.setUsername(user.getUsername());
        keycloakUser.setEmail(user.getEmail());

        String firstName = user.getFirstName() != null ? user.getFirstName() : "";
        String lastName = user.getLastName() != null ? user.getLastName() : "";
        keycloakUser.setName((firstName + " " + lastName).trim());

        if (user.getAttributes() != null && user.getAttributes().containsKey("phoneNumber")) {
            List<String> phones = user.getAttributes().get("phoneNumber");
            if (!phones.isEmpty()) {
                keycloakUser.setPhoneNumber(phones.get(0));
            }
        }

        keycloakUser.setNewlyCreated(false);
        return keycloakUser;
    }
}
