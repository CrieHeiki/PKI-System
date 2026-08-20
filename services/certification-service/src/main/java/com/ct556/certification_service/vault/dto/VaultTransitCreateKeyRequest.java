package com.ct556.certification_service.vault.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record VaultTransitCreateKeyRequest(
        @JsonProperty("type")
        String type,

        @JsonProperty("exportable")
        boolean exportable,

        @JsonProperty("allow_plaintext_backup")
        boolean allowPlaintextBackup
) {
}
