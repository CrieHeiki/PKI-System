package com.ct556.certification_service.dto.request;

import jakarta.validation.constraints.NotBlank;

public record InternalCertificateSignRequest(
        @NotBlank(message = "contentBase64 is required")
        String contentBase64
) {
}
