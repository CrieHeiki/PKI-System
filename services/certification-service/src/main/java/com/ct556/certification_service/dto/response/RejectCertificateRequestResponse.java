package com.ct556.certification_service.dto.response;

public record RejectCertificateRequestResponse(
        String message,
        CertificateRequestResponse request
) {
}