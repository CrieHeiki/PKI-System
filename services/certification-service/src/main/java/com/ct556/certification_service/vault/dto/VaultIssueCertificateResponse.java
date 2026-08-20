package com.ct556.certification_service.vault.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VaultIssueCertificateResponse(
        Data data
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(
            @JsonProperty("certificate")
            String certificate,

            @JsonProperty("issuing_ca")
            String issuingCa,

            @JsonProperty("serial_number")
            String serialNumber,

            @JsonProperty("expiration")
            Long expiration
    ) {
    }
}
