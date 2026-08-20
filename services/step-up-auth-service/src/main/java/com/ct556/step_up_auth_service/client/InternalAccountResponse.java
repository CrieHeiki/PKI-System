package com.ct556.step_up_auth_service.client;

import java.util.UUID;

public record InternalAccountResponse(
        UUID id,
        String username,
        String email,
        String role,
        String status
) {
}
