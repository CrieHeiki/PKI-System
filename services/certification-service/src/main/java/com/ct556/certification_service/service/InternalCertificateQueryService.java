package com.ct556.certification_service.service;

import com.ct556.certification_service.domain.CertificateEntity;
import com.ct556.certification_service.domain.CertificateStatus;
import com.ct556.certification_service.dto.response.InternalCertificateMetadataResponse;
import com.ct556.certification_service.exception.ResourceNotFoundException;
import com.ct556.certification_service.repository.CertificateRepository;
import com.ct556.certification_service.util.X509CertificateUtils;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class InternalCertificateQueryService {

    private final CertificateRepository certificateRepository;

    public InternalCertificateQueryService(CertificateRepository certificateRepository) {
        this.certificateRepository = certificateRepository;
    }

    public InternalCertificateMetadataResponse getCertificateMetadata(UUID certificateId) {
        CertificateEntity certificate = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found: " + certificateId));

        return toMetadata(certificate);
    }

    public InternalCertificateMetadataResponse getCertificateMetadataBySerial(String serialNumber) {
        String normalizedSerial = X509CertificateUtils.normalizeSerialLookup(serialNumber);
        CertificateEntity certificate = certificateRepository.findByNormalizedSerialNumber(normalizedSerial)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found with serial: " + normalizedSerial));

        return toMetadata(certificate);
    }

    private InternalCertificateMetadataResponse toMetadata(CertificateEntity certificate) {
        return InternalCertificateMetadataResponse.builder()
                .certificateId(certificate.getId())
                .ownerAccountId(certificate.getAccountId())
                .subjectDn(certificate.getSubjectDn())
                .serialNumber(certificate.getSerialNumber())
                .certificatePem(certificate.getCertificatePem())
                .issuingCaPem(certificate.getIssuingCaPem())
                .status(resolveDisplayStatus(certificate).name())
                .revoked(isRevoked(certificate))
                .validFrom(certificate.getIssuedAt())
                .validTo(certificate.getExpiresAt())
                .revokedAt(certificate.getRevokedAt())
                .build();
    }

    private boolean isRevoked(CertificateEntity certificate) {
        return certificate.getStatus() == CertificateStatus.REVOKED || certificate.getRevokedAt() != null;
    }

    private CertificateStatus resolveDisplayStatus(CertificateEntity certificate) {
        if (certificate.getStatus() == CertificateStatus.ACTIVE
                && certificate.getExpiresAt() != null
                && certificate.getExpiresAt().isBefore(OffsetDateTime.now(ZoneOffset.UTC))) {
            return CertificateStatus.EXPIRED;
        }
        return certificate.getStatus();
    }
}
