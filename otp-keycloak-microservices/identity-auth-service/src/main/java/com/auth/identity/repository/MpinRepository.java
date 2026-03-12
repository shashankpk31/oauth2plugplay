package com.auth.identity.repository;

import com.auth.identity.model.MpinRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MpinRepository extends JpaRepository<MpinRecord, UUID> {

    /**
     * Find active MPIN record by user ID
     */
    Optional<MpinRecord> findByUserIdAndIsActiveTrue(UUID userId);

    /**
     * Find active MPIN record by identifier (email or phone)
     */
    Optional<MpinRecord> findByIdentifierAndIsActiveTrue(String identifier);

    /**
     * Check if user has an active MPIN
     */
    boolean existsByUserIdAndIsActiveTrue(UUID userId);

    /**
     * Check if identifier has an active MPIN
     */
    boolean existsByIdentifierAndIsActiveTrue(String identifier);

    /**
     * Deactivate all MPIN records for a user
     * Used when user wants to remove MPIN
     */
    @Modifying
    @Query("UPDATE MpinRecord m SET m.isActive = false WHERE m.userId = :userId")
    void deactivateAllByUserId(UUID userId);

    /**
     * Delete expired MPIN sessions (cleanup job)
     */
    @Modifying
    @Query("DELETE FROM MpinRecord m WHERE m.sessionExpiresAt < :now")
    void deleteExpiredSessions(LocalDateTime now);

    /**
     * Find all MPIN records that have expired but not yet deleted
     * For logging/audit purposes
     */
    @Query("SELECT m FROM MpinRecord m WHERE m.sessionExpiresAt < :now AND m.isActive = true")
    java.util.List<MpinRecord> findExpiredActiveSessions(LocalDateTime now);
}
