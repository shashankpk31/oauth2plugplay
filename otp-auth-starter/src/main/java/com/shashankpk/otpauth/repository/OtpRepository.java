package com.shashankpk.otpauth.repository;

import com.shashankpk.otpauth.model.OtpRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<OtpRecord, Long> {

    Optional<OtpRecord> findByIdentifierAndOtpCodeAndVerifiedFalse(String identifier, String otpCode);

    @Modifying
    @Transactional
    @Query("UPDATE OtpRecord o SET o.verified = true WHERE o.identifier = :identifier AND o.verified = false")
    void invalidatePreviousOtps(String identifier);
}
