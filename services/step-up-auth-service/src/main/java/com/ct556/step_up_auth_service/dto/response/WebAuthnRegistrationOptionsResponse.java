package com.ct556.step_up_auth_service.dto.response;

import java.time.Instant;
import java.util.UUID;

public record WebAuthnRegistrationOptionsResponse(
        UUID ceremonyId,
        String optionsJson,
        Instant expiresAt
) {
}
