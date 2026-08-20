package com.ct556.certification_service.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CertificateResponse(
        UUID id,
        UUID accountId,
        String commonName,
        String subjectDn,
        String serialNumber,
        String vaultCertificatePath,
        String certificatePem,
        String issuingCaPem,
        String status,
        UUID renewedFromCertificateId,
        OffsetDateTime issuedAt,
        OffsetDateTime expiresAt,
        OffsetDateTime revokedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}