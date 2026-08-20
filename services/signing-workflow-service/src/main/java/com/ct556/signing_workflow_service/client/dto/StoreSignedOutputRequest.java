package com.ct556.signing_workflow_service.client.dto;

import java.util.UUID;

public record StoreSignedOutputRequest(
        UUID signingRequestId,
        String fileName,
        String contentType,
        String contentBase64
) {
}
