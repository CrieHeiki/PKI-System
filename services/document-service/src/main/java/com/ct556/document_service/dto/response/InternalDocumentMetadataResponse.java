package com.ct556.document_service.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class InternalDocumentMetadataResponse {
    private UUID documentId;
    private UUID ownerAccountId;
    private Integer currentVersion;
    private String status;
}
