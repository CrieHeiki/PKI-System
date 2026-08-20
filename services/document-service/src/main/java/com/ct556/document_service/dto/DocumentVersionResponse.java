package com.ct556.document_service.dto;

import java.time.Instant;
import java.util.UUID;

public record DocumentVersionResponse(
        UUID id,
        Integer versionNumber,
        String fileName,
        String contentType,
        Long fileSize,
        String sha256Hash,
        Instant createdAt
) {
}