package com.ct556.signing_workflow_service.client.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class TimestampTokenResponse {
    private String timestampTokenBase64;
    private String digestAlgorithm;
    private OffsetDateTime generatedAt;
    private String authoritySerialNumber;
    private String authoritySubjectDn;
    private String authorityCertificatePem;
}
