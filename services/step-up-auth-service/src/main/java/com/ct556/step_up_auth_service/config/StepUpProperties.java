package com.ct556.step_up_auth_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.step-up")
public record StepUpProperties(
        int challengeTtlSeconds,
        int proofTtlSeconds,
        int enrollmentTicketTtlSeconds,
        int maxAttempts,
        int challengeCodeLength,
        boolean demoReturnCode
) {
}
