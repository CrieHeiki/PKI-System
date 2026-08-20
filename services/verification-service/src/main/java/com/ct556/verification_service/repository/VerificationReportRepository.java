package com.ct556.verification_service.repository;

import com.ct556.verification_service.entity.VerificationReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VerificationReportRepository extends JpaRepository<VerificationReport, UUID> {
    Optional<VerificationReport> findFirstBySigningRequestIdOrderByVerifiedAtDesc(UUID signingRequestId);
    List<VerificationReport> findTop20ByOrderByVerifiedAtDesc();
}
