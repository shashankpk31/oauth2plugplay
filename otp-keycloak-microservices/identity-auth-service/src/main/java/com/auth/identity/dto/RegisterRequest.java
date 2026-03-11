package com.auth.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Identifier is required")
    private String identifier;

    @NotNull(message = "Identifier type is required")
    private IdentifierType identifierType;

    public enum IdentifierType {
        EMAIL, PHONE
    }
}
