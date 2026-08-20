package com.ct556.step_up_auth_service.dto.request;

import com.ct556.step_up_auth_service.entity.MfaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateMfaRegistrationRequest(
        @NotNull(message = "mfaType is required")
        MfaType mfaType,

        @Size(max = 255, message = "issuer must not exceed 255 characters")
        String issuer,

        @NotBlank(message = "label is required")
        @Size(max = 255, message = "label must not exceed 255 characters")
        String label
) {
}
