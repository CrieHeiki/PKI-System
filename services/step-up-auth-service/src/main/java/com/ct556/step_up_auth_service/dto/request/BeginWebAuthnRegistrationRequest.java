package com.ct556.step_up_auth_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BeginWebAuthnRegistrationRequest(
        @Size(max = 255, message = "issuer must not exceed 255 characters")
        String issuer,

        @NotBlank(message = "label is required")
        @Size(max = 255, message = "label must not exceed 255 characters")
        String label,

        @Size(max = 255, message = "enrollmentTicket must not exceed 255 characters")
        String enrollmentTicket,

        @Size(max = 255, message = "authorizationVerificationToken must not exceed 255 characters")
        String authorizationVerificationToken
) {
}
