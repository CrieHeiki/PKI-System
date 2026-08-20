package com.ct556.signing_workflow_service.client.dto;

public record TimestampTokenRequest(
        String messageImprintBase64,
        String digestAlgorithm
) {
}
