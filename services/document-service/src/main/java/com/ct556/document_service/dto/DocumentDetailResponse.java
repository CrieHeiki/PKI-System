package com.ct556.document_service.dto;

import java.time.Instant;
import java.util.UUID;

public record DocumentDetailResponse(
        UUID id,
        UUID ownerAccountId,
        String title,
        String description,
        Integer currentVersion,
        String status,
        Instant createdAt,
        Instant updatedAt,
        DocumentVersionResponse currentVersionDetail
) {
}