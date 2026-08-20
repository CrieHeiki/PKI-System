package com.ct556.certification_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCertificateRequestRequest(
        @NotBlank(message = "commonName is required")
        @Size(max = 255, message = "commonName must be <= 255 characters")
        String commonName,

        @Size(max = 50, message = "ttl must be <= 50 characters")
        String ttl,

        @Size(max = 255, message = "organization must be <= 255 characters")
        String organization,

        @Size(max = 10, message = "country must be <= 10 characters")
        String country
) {
}