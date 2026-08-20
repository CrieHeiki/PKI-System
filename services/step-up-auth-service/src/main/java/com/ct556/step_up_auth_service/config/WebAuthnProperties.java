package com.ct556.step_up_auth_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.step-up.webauthn")
public record WebAuthnProperties(
        String rpId,
        String rpName,
        List<String> allowedOrigins,
        long timeoutMillis
) {
}
