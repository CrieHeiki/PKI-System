package com.ct556.certification_service.vault.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VaultTransitReadKeyResponse(
        Data data
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(
            @JsonProperty("latest_version")
            Integer latestVersion,

            @JsonProperty("keys")
            Map<String, KeyVersion> keys
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KeyVersion(
            @JsonProperty("public_key")
            String publicKey
    ) {
    }
}
