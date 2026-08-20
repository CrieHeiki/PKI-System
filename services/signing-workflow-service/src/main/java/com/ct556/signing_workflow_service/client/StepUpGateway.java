package com.ct556.signing_workflow_service.client;

import com.ct556.signing_workflow_service.client.dto.InternalStepUpVerificationResponse;

import java.util.UUID;

public interface StepUpGateway {
    InternalStepUpVerificationResponse consumeVerification(String verificationToken,
                                                           UUID userId,
                                                           String action,
                                                           String targetType,
                                                           UUID targetId,
                                                           boolean consume);
}
