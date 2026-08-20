package com.ct556.certification_service.vault.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record VaultMountRequest(
        @JsonProperty("type")
        String type
) {
}
