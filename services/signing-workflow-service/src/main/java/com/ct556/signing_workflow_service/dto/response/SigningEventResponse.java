package com.ct556.signing_workflow_service.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class SigningEventResponse {
    private UUID id;
    private String eventType;
    private UUID actorAccountId;
    private LocalDateTime eventTime;
    private String detail;
}