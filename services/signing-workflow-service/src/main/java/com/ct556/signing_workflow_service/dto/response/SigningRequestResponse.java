package com.ct556.signing_workflow_service.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class SigningRequestResponse {
    private UUID id;
    private UUID documentId;
    private UUID requesterAccountId;
    private UUID signerAccountId;
    private UUID certificateId;
    private String status;
    private LocalDateTime requestedAt;
    private LocalDateTime decidedAt;
    private LocalDateTime signedAt;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<SigningEventResponse> events;
    private SigningArtifactResponse artifact;
}