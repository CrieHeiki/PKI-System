package com.ct556.audit_service.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "immudb")
public record ImmudbProperties(
        boolean enabled,
        @NotBlank String host,
        @Min(1) int port,
        @NotBlank String database,
        @NotBlank String username,
        @NotBlank String password
) {
}
