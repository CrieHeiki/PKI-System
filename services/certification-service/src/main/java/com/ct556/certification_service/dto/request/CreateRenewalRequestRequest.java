package com.ct556.certification_service.dto.request;

import jakarta.validation.constraints.Size;

public record CreateRenewalRequestRequest(
        @Size(max = 50, message = "ttl must be <= 50 characters")
        String ttl,

        @Size(max = 255, message = "organization must be <= 255 characters")
        String organization,

        @Size(max = 10, message = "country must be <= 10 characters")
        String country,

        @Size(max = 1000, message = "reason must be <= 1000 characters")
        String reason
) {
}