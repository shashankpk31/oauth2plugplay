package com.auth.identity.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "otp_records")
public class OtpRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "identifier", nullable = false)
    private String identifier;

    @Column(name = "otp", nullable = false, length = 6)
    private String otp;

    @Column(name = "attempts", nullable = false)
    @Builder.Default
    private Integer attempts = 0;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "verified", nullable = false)
    @Builder.Default
    private Boolean verified = false;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public void incrementAttempts() {
        this.attempts++;
    }
}
