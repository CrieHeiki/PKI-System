package com.ct556.step_up_auth_service.dto.response;

import java.time.Instant;
import java.util.UUID;

public record InternalStepUpVerificationResponse(
        boolean valid,
        boolean consumed,
        UUID userId,
        String action,
        String targetType,
        UUID targetId,
        UUID registrationId,
        Instant verifiedAt,
        Instant expiresAt,
        String message
) {
}
