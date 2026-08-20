package com.ct556.certification_service.repository;

import com.ct556.certification_service.domain.TrustedTimestampAuthorityEntity;
import com.ct556.certification_service.domain.TrustedTimestampAuthorityStatus;
import com.ct556.certification_service.util.X509CertificateUtils;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TrustedTimestampAuthorityRepository extends JpaRepository<TrustedTimestampAuthorityEntity, UUID> {

    Optional<TrustedTimestampAuthorityEntity> findFirstByStatusOrderByCreatedAtDesc(TrustedTimestampAuthorityStatus status);

    Optional<TrustedTimestampAuthorityEntity> findBySerialNumber(String serialNumber);

    default Optional<TrustedTimestampAuthorityEntity> findByNormalizedSerialNumber(String normalizedSerial) {
        if (normalizedSerial == null) {
            return Optional.empty();
        }

        return findAll().stream()
                .filter(authority -> X509CertificateUtils.serialNumbersMatch(authority.getSerialNumber(), normalizedSerial))
                .findFirst();
    }
}
