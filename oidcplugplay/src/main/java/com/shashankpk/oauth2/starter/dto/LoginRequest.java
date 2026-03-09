package com.shashankpk.oauth2.starter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for custom login (direct authentication)
 * Used when frontend has custom login page and sends credentials to backend
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    /**
     * Optional: Grant type (default: password)
     */
    private String grantType = "password";

    /**
     * Optional: Requested scopes
     */
    private String scope;
}
