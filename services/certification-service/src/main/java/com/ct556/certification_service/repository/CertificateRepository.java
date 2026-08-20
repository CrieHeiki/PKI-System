package com.ct556.certification_service.repository;

import com.ct556.certification_service.domain.CertificateEntity;
import com.ct556.certification_service.util.X509CertificateUtils;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CertificateRepository extends JpaRepository<CertificateEntity, UUID> {

    List<CertificateEntity> findAllByAccountIdOrderByIssuedAtDesc(UUID accountId);

    List<CertificateEntity> findAllByOrderByIssuedAtDesc();

    Optional<CertificateEntity> findBySerialNumber(String serialNumber);

    Optional<CertificateEntity> findBySerialNumberAndAccountId(String serialNumber, UUID accountId);

    boolean existsByRenewedFromCertificateId(UUID renewedFromCertificateId);

    default Optional<CertificateEntity> findByNormalizedSerialNumber(String normalizedSerial) {
        if (normalizedSerial == null) {
            return Optional.empty();
        }

        return findAll().stream()
                .filter(certificate -> X509CertificateUtils.serialNumbersMatch(certificate.getSerialNumber(), normalizedSerial))
                .findFirst();
    }

    default Optional<CertificateEntity> findByNormalizedSerialNumberAndAccountId(String normalizedSerial, UUID accountId) {
        if (normalizedSerial == null || accountId == null) {
            return Optional.empty();
        }

        return findAllByAccountIdOrderByIssuedAtDesc(accountId).stream()
                .filter(certificate -> X509CertificateUtils.serialNumbersMatch(certificate.getSerialNumber(), normalizedSerial))
                .findFirst();
    }
}
