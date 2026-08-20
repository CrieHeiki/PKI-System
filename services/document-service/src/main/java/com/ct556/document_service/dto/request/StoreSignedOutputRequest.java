package com.ct556.document_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record StoreSignedOutputRequest(
        @NotNull(message = "signingRequestId is required")
        UUID signingRequestId,

        @NotBlank(message = "fileName is required")
        String fileName,

        @NotBlank(message = "contentType is required")
        String contentType,

        @NotBlank(message = "contentBase64 is required")
        String contentBase64
) {
}
