package com.ct556.step_up_auth_service.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record WebAuthnRecoveryRequestResponse(
        UUID id,
        UUID userId,
        String status,
        String reason,
        String decisionNote,
        UUID reviewedByAdminId,
        LocalDateTime requestedAt,
        LocalDateTime reviewedAt,
        LocalDateTime completedAt,
        String recoveryTicket,
        LocalDateTime recoveryTicketIssuedAt,
        LocalDateTime recoveryTicketExpiresAt,
        UUID completedRegistrationId
) {
}
