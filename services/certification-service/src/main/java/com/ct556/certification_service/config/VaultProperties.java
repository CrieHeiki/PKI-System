package com.ct556.certification_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "vault")
public record VaultProperties(
        String url,
        String token,
        String mountPath,
        String issueRole,
        String transitMountPath,
        String transitKeyPrefix,
        Duration connectTimeout,
        Duration readTimeout
) {
}
