package com.ct556.certification_service.service;

import com.ct556.certification_service.domain.CertificateEntity;
import com.ct556.certification_service.domain.CertificateStatus;
import com.ct556.certification_service.dto.request.InternalCertificateSignRequest;
import com.ct556.certification_service.dto.response.InternalCertificateSignResponse;
import com.ct556.certification_service.exception.BadRequestException;
import com.ct556.certification_service.exception.ResourceNotFoundException;
import com.ct556.certification_service.repository.CertificateRepository;
import com.ct556.certification_service.vault.VaultPkiClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.UUID;

@Service
public class InternalCertificateSigningService {

    private static final String DIGEST_ALGORITHM = "SHA-256";
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    private final CertificateRepository certificateRepository;
    private final VaultPkiClient vaultPkiClient;

    public InternalCertificateSigningService(CertificateRepository certificateRepository,
                                             VaultPkiClient vaultPkiClient) {
        this.certificateRepository = certificateRepository;
        this.vaultPkiClient = vaultPkiClient;
    }

    public InternalCertificateSignResponse sign(UUID certificateId, InternalCertificateSignRequest request) {
        CertificateEntity certificate = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found: " + certificateId));

        validateSigningCertificate(certificate);
        validateSigningMaterial(certificate);

        byte[] content = decodeContent(request.contentBase64());
        byte[] signatureValue = vaultPkiClient.signWithTransitKey(certificate.getVaultTransitKeyName(), content);

        return new InternalCertificateSignResponse(
                DIGEST_ALGORITHM,
                SIGNATURE_ALGORITHM,
                Base64.getEncoder().encodeToString(signatureValue)
        );
    }

    private void validateSigningCertificate(CertificateEntity certificate) {
        if (certificate.getStatus() == CertificateStatus.REVOKED || certificate.getRevokedAt() != null) {
            throw new BadRequestException("Certificate has been revoked");
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (certificate.getIssuedAt() != null && now.isBefore(certificate.getIssuedAt())) {
            throw new BadRequestException("Certificate is not yet valid");
        }
        if (certificate.getExpiresAt() != null && now.isAfter(certificate.getExpiresAt())) {
            throw new BadRequestException("Certificate has expired");
        }
    }

    private void validateSigningMaterial(CertificateEntity certificate) {
        if (!StringUtils.hasText(certificate.getVaultTransitKeyName())) {
            throw new BadRequestException("Certificate does not have Vault Transit key reference for signing");
        }
    }

    private byte[] decodeContent(String contentBase64) {
        try {
            return Base64.getDecoder().decode(contentBase64);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("contentBase64 is not valid Base64");
        }
    }

}
