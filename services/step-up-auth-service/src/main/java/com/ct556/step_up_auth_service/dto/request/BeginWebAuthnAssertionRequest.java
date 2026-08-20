package com.ct556.step_up_auth_service.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record BeginWebAuthnAssertionRequest(
        UUID registrationId,

        @NotBlank(message = "action is required")
        String action,

        String targetType,

        UUID targetId
) {
}
