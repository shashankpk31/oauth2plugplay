package com.auth.identity.repository;

import com.auth.identity.model.OtpRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpRepository extends JpaRepository<OtpRecord, UUID> {

    Optional<OtpRecord> findTopByIdentifierAndVerifiedFalseOrderByCreatedAtDesc(String identifier);

    @Modifying
    @Query("DELETE FROM OtpRecord o WHERE o.expiresAt < :now")
    void deleteExpiredOtps(LocalDateTime now);

    @Modifying
    @Query("DELETE FROM OtpRecord o WHERE o.identifier = :identifier AND o.verified = false")
    void deleteUnverifiedOtpsByIdentifier(String identifier);
}
