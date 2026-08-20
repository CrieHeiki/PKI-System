package com.ct556.signing_workflow_service.client.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class StoredSignedOutputResponse {
    private UUID documentId;
    private UUID signingRequestId;
    private String fileName;
    private String contentType;
    private Long size;
    private String objectKey;
}
