package com.ct556.verification_service.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class InternalCertificateMetadataResponse {
    private UUID certificateId;
    private UUID ownerAccountId;
    private String subjectDn;
    private String serialNumber;
    private String certificatePem;
    private String issuingCaPem;
    private String status;
    private boolean revoked;
    private OffsetDateTime validFrom;
    private OffsetDateTime validTo;
    private OffsetDateTime revokedAt;
}
