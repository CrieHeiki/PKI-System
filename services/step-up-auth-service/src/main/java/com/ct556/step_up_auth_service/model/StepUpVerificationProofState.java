package com.ct556.step_up_auth_service.model;

import java.time.Instant;
import java.util.UUID;

public record StepUpVerificationProofState(
        String verificationToken,
        UUID challengeId,
        UUID userId,
        UUID registrationId,
        String action,
        String targetType,
        UUID targetId,
        Instant verifiedAt,
        Instant expiresAt
) {
}
