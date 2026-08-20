package com.ct556.document_service.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class StoreSignedOutputResponse {
    private UUID documentId;
    private UUID signingRequestId;
    private String fileName;
    private String contentType;
    private Long size;
    private String objectKey;
}
