package com.ct556.certification_service.dto.request;

import jakarta.validation.constraints.NotBlank;

public record InternalTimestampTokenRequest(
        @NotBlank String messageImprintBase64,
        @NotBlank String digestAlgorithm
) {
}
