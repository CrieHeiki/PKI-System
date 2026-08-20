package com.ct556.certification_service.vault.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record VaultTransitSignRequest(
        @JsonProperty("input")
        String input,

        @JsonProperty("hash_algorithm")
        String hashAlgorithm,

        @JsonProperty("signature_algorithm")
        String signatureAlgorithm
) {
}
