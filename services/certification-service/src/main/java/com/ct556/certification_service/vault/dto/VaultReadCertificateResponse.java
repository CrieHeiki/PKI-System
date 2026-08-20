package com.ct556.certification_service.vault.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VaultReadCertificateResponse(
        Data data
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(
            @JsonProperty("certificate")
            String certificate,

            @JsonProperty("revocation_time")
            Long revocationTime,

            @JsonProperty("issuer_id")
            String issuerId
    ) {
    }
}