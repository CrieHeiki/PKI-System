package com.ct556.step_up_auth_service.dto.response;

import java.time.Instant;
import java.util.UUID;

public record StepUpVerificationResponse(
        boolean verified,
        UUID challengeId,
        String verificationToken,
        UUID registrationId,
        String action,
        String targetType,
        UUID targetId,
        Instant verifiedAt,
        Instant proofExpiresAt
) {
}
