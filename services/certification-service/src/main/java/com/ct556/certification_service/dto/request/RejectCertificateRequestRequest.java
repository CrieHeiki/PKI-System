package com.ct556.certification_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectCertificateRequestRequest(
        @NotBlank(message = "reason is required")
        @Size(max = 1000, message = "reason must be <= 1000 characters")
        String reason
) {
}