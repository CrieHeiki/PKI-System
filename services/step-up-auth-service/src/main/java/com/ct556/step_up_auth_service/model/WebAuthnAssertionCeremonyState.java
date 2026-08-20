package com.ct556.step_up_auth_service.model;

import java.time.Instant;
import java.util.UUID;

public record WebAuthnAssertionCeremonyState(
        UUID ceremonyId,
        UUID userId,
        UUID registrationId,
        String requestJson,
        String action,
        String targetType,
        UUID targetId,
        Instant createdAt,
        Instant expiresAt
) {
}
