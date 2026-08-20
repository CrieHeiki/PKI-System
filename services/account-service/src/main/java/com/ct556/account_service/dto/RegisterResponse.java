package com.ct556.account_service.dto;

import java.util.UUID;

public record RegisterResponse(
        UUID id,
        String username,
        String email,
        String role,
        String status
) {}