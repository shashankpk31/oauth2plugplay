package com.auth.business.service;

import com.auth.business.client.IdentityServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessService {

    private final IdentityServiceClient identityServiceClient;

    public Map<String, Object> getBusinessData(String userId) {
        log.info("Fetching business data for user: {}", userId);

        Map<String, Object> data = new HashMap<>();
        data.put("message", "This is protected business data");
        data.put("timestamp", LocalDateTime.now().toString());
        data.put("userId", userId);
        data.put("dataType", "business-metrics");

        return data;
    }

    public Map<String, Object> getUserProfile() {
        log.info("Fetching user profile from Identity Service via FeignClient");

        try {
            // This call will propagate the JWT token automatically
            Map<String, Object> userProfile = identityServiceClient.getCurrentUser();
            log.info("Successfully fetched user profile from Identity Service");
            return userProfile;
        } catch (Exception e) {
            log.error("Error fetching user profile from Identity Service: ", e);
            throw new RuntimeException("Failed to fetch user profile", e);
        }
    }

    public Map<String, Object> getUserProfileById(UUID userId) {
        log.info("Fetching user profile for userId: {} from Identity Service", userId);

        try {
            Map<String, Object> userProfile = identityServiceClient.getUserById(userId);
            log.info("Successfully fetched user profile from Identity Service");
            return userProfile;
        } catch (Exception e) {
            log.error("Error fetching user profile from Identity Service: ", e);
            throw new RuntimeException("Failed to fetch user profile", e);
        }
    }
}
