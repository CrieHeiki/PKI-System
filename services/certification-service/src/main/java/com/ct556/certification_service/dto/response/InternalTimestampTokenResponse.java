package com.ct556.certification_service.dto.response;

import java.time.OffsetDateTime;

public record InternalTimestampTokenResponse(
        String timestampTokenBase64,
        String digestAlgorithm,
        OffsetDateTime generatedAt,
        String authoritySerialNumber,
        String authoritySubjectDn,
        String authorityCertificatePem
) {
}
