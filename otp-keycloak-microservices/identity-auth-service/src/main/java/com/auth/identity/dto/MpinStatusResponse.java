package com.auth.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MpinStatusResponse {
    private boolean isSet;
    private boolean isValid;
    private LocalDateTime sessionExpiresAt;
    private Integer failedAttempts;
    private LocalDateTime lastUsedAt;
}
