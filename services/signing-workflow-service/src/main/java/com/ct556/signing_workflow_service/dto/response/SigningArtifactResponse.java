package com.ct556.signing_workflow_service.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class SigningArtifactResponse {
    private UUID id;
    private UUID signingRequestId;
    private UUID signerAccountId;
    private UUID certificateId;
    private String fileName;
    private String contentType;
    private String digestAlgorithm;
    private String signatureAlgorithm;
    private String documentDigestBase64;
    private String signatureValueBase64;
    private Long signedPayloadLength;
    private String outputObjectKey;
    private String outputFileName;
    private String outputContentType;
    private Long outputFileSize;
    private LocalDateTime createdAt;
}
