package com.auth.business.controller;

import com.auth.business.service.BusinessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BusinessController {

    private final BusinessService businessService;

    @GetMapping("/data")
    public ResponseEntity<Map<String, Object>> getData(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {

        log.info("Get data request received");

        String userId = jwt.getSubject();

        // Read user context from headers (injected by Gateway)
        String userIdFromHeader = request.getHeader("X-User-Id");
        String userEmail = request.getHeader("X-User-Email");
        String userPhone = request.getHeader("X-User-Phone");

        log.info("User ID from JWT: {}", userId);
        log.info("User ID from Header: {}", userIdFromHeader);
        log.info("User Email from Header: {}", userEmail);
        log.info("User Phone from Header: {}", userPhone);

        Map<String, Object> data = businessService.getBusinessData(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);
        response.put("userContext", Map.of(
                "userId", userIdFromHeader != null ? userIdFromHeader : "",
                "email", userEmail != null ? userEmail : "",
                "phone", userPhone != null ? userPhone : ""
        ));
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/profile")
    public ResponseEntity<Map<String, Object>> getUserProfile() {
        log.info("Get user profile request received");

        Map<String, Object> profile = businessService.getUserProfile();

        return ResponseEntity.ok(profile);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserById(@PathVariable UUID userId) {
        log.info("Get user by ID request received: {}", userId);

        Map<String, Object> user = businessService.getUserProfileById(userId);

        return ResponseEntity.ok(user);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "Business Service");
        health.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(health);
    }
}
