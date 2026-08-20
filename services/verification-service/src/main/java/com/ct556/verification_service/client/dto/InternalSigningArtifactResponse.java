package com.ct556.verification_service.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
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
