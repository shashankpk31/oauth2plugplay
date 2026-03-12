package com.auth.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SetMpinRequest {

    @NotBlank(message = "MPIN is required")
    @Pattern(regexp = "^\\d{4,6}$", message = "MPIN must be 4-6 digits")
    private String mpin;

    @NotBlank(message = "Confirm MPIN is required")
    @Pattern(regexp = "^\\d{4,6}$", message = "Confirm MPIN must be 4-6 digits")
    private String confirmMpin;
}
