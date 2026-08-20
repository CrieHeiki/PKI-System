package com.ct556.certification_service.vault.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record VaultSignCertificateRequest(
        @JsonProperty("csr")
        String csr,

        @JsonProperty("common_name")
        String commonName,

        @JsonProperty("ttl")
        String ttl,

        @JsonProperty("organization")
        String organization,

        @JsonProperty("country")
        String country
) {
}
