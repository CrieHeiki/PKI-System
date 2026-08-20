package com.ct556.step_up_auth_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ConsumeStepUpVerificationRequest(
        @NotBlank(message = "verificationToken is required")
        String verificationToken,

        @NotNull(message = "userId is required")
        UUID userId,

        @NotBlank(message = "action is required")
        @Size(max = 100, message = "action must not exceed 100 characters")
        String action,

        @Size(max = 100, message = "targetType must not exceed 100 characters")
        String targetType,

        UUID targetId,

        Boolean consume
) {
}
