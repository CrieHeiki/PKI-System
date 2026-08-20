package com.ct556.signing_workflow_service.client.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class DocumentMetadataResponse {
    private UUID documentId;
    private UUID ownerAccountId;
    private Integer currentVersion;
    private String status;
}
