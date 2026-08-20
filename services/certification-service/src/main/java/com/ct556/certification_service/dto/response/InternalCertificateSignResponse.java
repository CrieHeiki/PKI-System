package com.ct556.certification_service.dto.response;

public record InternalCertificateSignResponse(
        String digestAlgorithm,
        String signatureAlgorithm,
        String signatureValueBase64
) {
}
