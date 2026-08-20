package com.ct556.certification_service.dto.response;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CertificateRequestResponse(
        UUID id,
        UUID accountId,
        String requestType,
        JsonNode requestPayload,
        String requestReason,
        String status,
        UUID resultCertificateId,
        OffsetDateTime requestedAt,
        OffsetDateTime reviewedAt,
        UUID reviewedByAccountId,
        String rejectionReason,
        OffsetDateTime updatedAt
) {
}
