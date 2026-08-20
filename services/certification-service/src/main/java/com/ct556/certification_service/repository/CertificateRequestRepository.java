package com.ct556.certification_service.repository;

import com.ct556.certification_service.domain.CertificateRequestEntity;
import com.ct556.certification_service.domain.CertificateRequestStatus;
import com.ct556.certification_service.domain.CertificateRequestType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CertificateRequestRepository extends JpaRepository<CertificateRequestEntity, UUID> {

    List<CertificateRequestEntity> findAllByAccountIdOrderByRequestedAtDesc(UUID accountId);

    Optional<CertificateRequestEntity> findByIdAndAccountId(UUID id, UUID accountId);

    List<CertificateRequestEntity> findAllByOrderByRequestedAtDesc();

    List<CertificateRequestEntity> findAllByStatusOrderByRequestedAtDesc(CertificateRequestStatus status);

    List<CertificateRequestEntity> findAllByRequestTypeOrderByRequestedAtDesc(CertificateRequestType requestType);

    List<CertificateRequestEntity> findAllByStatusAndRequestTypeOrderByRequestedAtDesc(
            CertificateRequestStatus status,
            CertificateRequestType requestType
    );

    boolean existsByAccountIdAndRequestTypeAndStatus(
            UUID accountId,
            CertificateRequestType requestType,
            CertificateRequestStatus status
    );
}
