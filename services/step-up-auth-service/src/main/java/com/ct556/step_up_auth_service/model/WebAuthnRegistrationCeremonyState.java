package com.ct556.step_up_auth_service.model;

import java.time.Instant;
import java.util.UUID;

public record WebAuthnRegistrationCeremonyState(
        UUID ceremonyId,
        UUID userId,
        String username,
        String label,
        String issuer,
        String optionsJson,
        UUID recoveryRequestId,
        Instant createdAt,
        Instant expiresAt
) {
}
