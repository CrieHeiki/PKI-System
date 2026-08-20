package com.ct556.verification_service.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class VerificationReportResponse {
    private UUID id;
    private UUID documentId;
    private Integer documentVersion;
    private UUID signingRequestId;
    private UUID certificateId;
    private UUID verifiedBy;
    private String verificationStatus;
    private String signedObjectKey;
    private String signedFileName;
    private String originalFileName;
    private String signerCertificateSerial;
    private String signerSubjectDn;
    private String issuerDn;
    private String trustSource;
    private String revocationSource;
    private String digestAlgorithm;
    private String signatureAlgorithm;
    private boolean integrityValid;
    private boolean signatureValid;
    private boolean trustChainValid;
    private boolean expirationValid;
    private boolean revocationValid;
    private String overallMessage;
    private LocalDateTime verifiedAt;
    private List<VerificationCheckResponse> checks;
}
