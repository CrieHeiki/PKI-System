package com.ct556.step_up_auth_service.dto.request;

import jakarta.validation.constraints.Size;

public record ReviewWebAuthnRecoveryRequest(
        @Size(max = 1000, message = "note must not exceed 1000 characters")
        String note
) {
}
