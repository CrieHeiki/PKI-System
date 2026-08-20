package com.ct556.certification_service.dto.response;

public record IssueCertificateResponse(
        String message,
        CertificateResponse certificate
) {
}
