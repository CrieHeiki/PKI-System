package com.ct556.signing_workflow_service.client.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CertificateSignResponse {
    private String digestAlgorithm;
    private String signatureAlgorithm;
    private String signatureValueBase64;
}
