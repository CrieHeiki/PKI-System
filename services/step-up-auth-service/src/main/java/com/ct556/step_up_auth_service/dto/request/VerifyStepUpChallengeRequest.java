package com.ct556.step_up_auth_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyStepUpChallengeRequest(
        @NotBlank(message = "otpCode is required")
        @Pattern(regexp = "^\\d{6}$", message = "otpCode must be a 6-digit code")
        String otpCode
) {
}
