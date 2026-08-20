package com.ct556.step_up_auth_service.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateWebAuthnEnrollmentTicketRequest(
        @NotNull(message = "userId is required")
        UUID userId
) {
}
