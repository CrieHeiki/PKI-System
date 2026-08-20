package com.ct556.certification_service.vault.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VaultTransitSignResponse(
        Data data
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(
            @JsonProperty("signature")
            String signature
    ) {
    }
}
