package com.ct556.step_up_auth_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record FinishWebAuthnRegistrationRequest(
        @NotNull(message = "ceremonyId is required")
        UUID ceremonyId,

        @NotBlank(message = "credentialJson is required")
        String credentialJson
) {
}
