package com.ct556.signing_workflow_service.client.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class DocumentPayloadResponse {
    private UUID documentId;
    private String fileName;
    private String contentType;
    private String contentBase64;
    private Long size;
    private String objectKey;
}