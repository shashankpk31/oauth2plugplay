package com.example.demo.controller;

import com.example.demo.dto.ApiResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Public REST controller - no authentication required
 */
@Slf4j
@RestController
@RequestMapping("/api/public")
public class PublicController {

    /**
     * Public endpoint - accessible without authentication
     */
    @GetMapping("/hello")
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> hello() {
        log.info("Public hello endpoint accessed");

        Map<String, Object> response = Map.of(
                "message", "Hello from public endpoint!",
                "description", "This endpoint does not require authentication",
                "serverTime", LocalDateTime.now()
        );

        return ResponseEntity.ok(ApiResponseDto.success(response));
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponseDto<Map<String, String>>> health() {
        return ResponseEntity.ok(ApiResponseDto.success(
                Map.of(
                        "status", "UP",
                        "application", "Sample Backend with oidcplugplay"
                )
        ));
    }
}
