package com.ct556.account_service.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInMinutes
) {}