package com.ct556.account_service.dto;

import java.util.UUID;

public record InternalAccountResponse(
        UUID id,
        String username,
        String email,
        String role,
        String status,
        String signatureImageBase64,
        String signatureImageContentType,
        boolean signatureImageOnly,
        int signatureBoxXPercent,
        int signatureBoxYPercent,
        int signatureBoxWidthPercent,
        int signatureBoxHeightPercent
) {
}
