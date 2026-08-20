package com.ct556.signing_workflow_service.client;

import com.ct556.signing_workflow_service.client.dto.DocumentMetadataResponse;
import com.ct556.signing_workflow_service.client.dto.DocumentPayloadResponse;
import com.ct556.signing_workflow_service.client.dto.StoredSignedOutputResponse;

import java.util.UUID;

public interface DocumentGateway {
    DocumentMetadataResponse getDocumentMetadata(UUID documentId);
    DocumentPayloadResponse getDocumentForSigning(UUID documentId);
    byte[] getSignedOutput(String objectKey);
    StoredSignedOutputResponse storeSignedOutput(UUID documentId,
                                                 UUID signingRequestId,
                                                 String fileName,
                                                 String contentType,
                                                 String contentBase64);
}
