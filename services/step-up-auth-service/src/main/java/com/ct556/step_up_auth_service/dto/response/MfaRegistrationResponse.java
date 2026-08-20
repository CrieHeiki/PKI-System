package com.ct556.step_up_auth_service.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record MfaRegistrationResponse(
        UUID id,
        UUID userId,
        String mfaType,
        String issuer,
        String label,
        String registrationStatus,
        LocalDateTime createdAt,
        LocalDateTime activatedAt,
        LocalDateTime disabledAt
) {
}
