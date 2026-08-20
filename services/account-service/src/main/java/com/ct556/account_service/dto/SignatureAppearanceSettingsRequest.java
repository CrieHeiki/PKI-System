package com.ct556.account_service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record SignatureAppearanceSettingsRequest(
        boolean imageOnly,
        @Min(0) @Max(95) int xPercent,
        @Min(0) @Max(95) int yPercent,
        @Min(5) @Max(100) int widthPercent,
        @Min(5) @Max(100) int heightPercent
) {
}
