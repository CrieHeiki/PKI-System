package com.ct556.step_up_auth_service.model;

import java.time.Instant;
import java.util.UUID;

public record WebAuthnEnrollmentTicketState(
        String enrollmentTicket,
        UUID userId,
        UUID issuedByAdminId,
        Instant issuedAt,
        Instant expiresAt,
        UUID recoveryRequestId
) {
}
