package com.ct556.certification_service.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
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
