package com.ct556.certification_service.dto.response;

public record ApproveCertificateRequestResponse(
        String message,
        CertificateRequestResponse request,
        CertificateResponse certificate
) {
}
