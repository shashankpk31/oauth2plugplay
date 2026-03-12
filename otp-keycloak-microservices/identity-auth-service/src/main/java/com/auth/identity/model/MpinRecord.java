package com.auth.identity.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing MPIN (Mobile PIN) for quick login
 * Similar to banking apps where users can set a PIN for faster authentication
 * after initial OTP-based login
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "mpin_records", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_identifier", columnList = "identifier")
})
public class MpinRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Reference to the user
     */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * User identifier (email or phone) - for quick lookup during MPIN login
     */
    @Column(name = "identifier", nullable = false)
    private String identifier;

    /**
     * Encrypted MPIN (4-6 digits)
     * NEVER store plaintext MPIN!
     */
    @Column(name = "encrypted_mpin", nullable = false)
    private String encryptedMpin;

    /**
     * Failed MPIN attempts counter
     * After max attempts, user must login with OTP again
     */
    @Column(name = "failed_attempts", nullable = false)
    @Builder.Default
    private Integer failedAttempts = 0;

    /**
     * MPIN session expiry timestamp
     * After this time, user must login with OTP again to renew MPIN session
     * Default: 30 days (like bank apps)
     */
    @Column(name = "session_expires_at", nullable = false)
    private LocalDateTime sessionExpiresAt;

    /**
     * Last successful MPIN login timestamp
     */
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    /**
     * MPIN active status
     * Can be disabled without deleting the record
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Check if MPIN session has expired
     */
    public boolean isSessionExpired() {
        return LocalDateTime.now().isAfter(sessionExpiresAt);
    }

    /**
     * Check if MPIN is blocked due to too many failed attempts
     */
    public boolean isBlocked(int maxAttempts) {
        return failedAttempts >= maxAttempts;
    }

    /**
     * Increment failed attempts counter
     */
    public void incrementFailedAttempts() {
        this.failedAttempts++;
    }

    /**
     * Reset failed attempts counter after successful login
     */
    public void resetFailedAttempts() {
        this.failedAttempts = 0;
    }

    /**
     * Update last used timestamp
     */
    public void updateLastUsed() {
        this.lastUsedAt = LocalDateTime.now();
    }

    /**
     * Extend MPIN session (renew expiry)
     */
    public void extendSession(int days) {
        this.sessionExpiresAt = LocalDateTime.now().plusDays(days);
    }
}
