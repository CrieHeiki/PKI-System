package com.ct556.account_service.dto;

import java.util.UUID;

public record AccountResponse(
        UUID id,
        String username,
        String email,
        String role,
        String status,
        boolean signatureImageConfigured,
        boolean signatureImageOnly,
        int signatureBoxXPercent,
        int signatureBoxYPercent,
        int signatureBoxWidthPercent,
        int signatureBoxHeightPercent
) {}
