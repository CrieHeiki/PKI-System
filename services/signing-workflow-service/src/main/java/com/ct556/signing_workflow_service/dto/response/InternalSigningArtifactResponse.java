package com.ct556.signing_workflow_service.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class InternalSigningArtifactResponse {
    private UUID signingRequestId;
    private UUID documentId;
    private String status;
    private UUID certificateId;
    private String fileName;
    private String contentType;
    private String digestAlgorithm;
    private String signatureAlgorithm;
    private String outputObjectKey;
    private String outputFileName;
    private String outputContentType;
    private Long outputFileSize;
}
