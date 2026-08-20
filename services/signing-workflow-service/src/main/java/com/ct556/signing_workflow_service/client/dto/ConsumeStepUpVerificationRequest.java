package com.ct556.signing_workflow_service.client.dto;

import java.util.UUID;

public record ConsumeStepUpVerificationRequest(
        String verificationToken,
        UUID userId,
        String action,
        String targetType,
        UUID targetId,
        Boolean consume
) {
}
