package com.ct556.step_up_auth_service.dto.response;

import java.time.Instant;
import java.util.UUID;

public record StepUpChallengeResponse(
        UUID challengeId,
        UUID registrationId,
        String mfaType,
        String action,
        String targetType,
        UUID targetId,
        String deliveryHint,
        Instant expiresAt,
        int maxAttempts,
        String challengeCode
) {
}
