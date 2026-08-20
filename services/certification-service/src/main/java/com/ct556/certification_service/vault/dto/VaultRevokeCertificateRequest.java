package com.ct556.certification_service.vault.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record VaultRevokeCertificateRequest(
        @JsonProperty("serial_number")
        String serialNumber
) {
}