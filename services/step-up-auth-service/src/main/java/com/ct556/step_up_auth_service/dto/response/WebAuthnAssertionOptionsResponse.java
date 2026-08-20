package com.ct556.step_up_auth_service.dto.response;

import java.time.Instant;
import java.util.UUID;

public record WebAuthnAssertionOptionsResponse(
        UUID ceremonyId,
        UUID registrationId,
        String action,
        String targetType,
        UUID targetId,
        String optionsJson,
        Instant expiresAt
) {
}
