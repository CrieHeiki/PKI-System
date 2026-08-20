package com.ct556.step_up_auth_service.model;

import java.time.Instant;
import java.util.UUID;

public record StepUpChallengeState(
        UUID challengeId,
        UUID userId,
        UUID registrationId,
        String mfaType,
        String action,
        String targetType,
        UUID targetId,
        String otpCode,
        Instant createdAt,
        Instant expiresAt,
        int attemptCount,
        int maxAttempts,
        String deliveryHint
) {
    public StepUpChallengeState withAttemptCount(int newAttemptCount) {
        return new StepUpChallengeState(
                challengeId,
                userId,
                registrationId,
                mfaType,
                action,
                targetType,
                targetId,
                otpCode,
                createdAt,
                expiresAt,
                newAttemptCount,
                maxAttempts,
                deliveryHint
        );
    }
}
