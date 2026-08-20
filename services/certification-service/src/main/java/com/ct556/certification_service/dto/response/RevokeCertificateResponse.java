package com.ct556.certification_service.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RevokeCertificateResponse(
        String message,
        UUID certificateId,
        String status,
        OffsetDateTime revokedAt
) {
}