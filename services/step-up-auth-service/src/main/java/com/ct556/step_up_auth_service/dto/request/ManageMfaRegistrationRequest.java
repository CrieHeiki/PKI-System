package com.ct556.step_up_auth_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ManageMfaRegistrationRequest(
        @NotBlank(message = "authorizationVerificationToken is required")
        @Size(max = 255, message = "authorizationVerificationToken must not exceed 255 characters")
        String authorizationVerificationToken
) {
}
