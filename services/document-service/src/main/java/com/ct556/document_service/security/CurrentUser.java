package com.ct556.document_service.security;

import java.util.UUID;

public record CurrentUser(
        UUID accountId,
        String subject,
        String role
) {
    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role) || "ROLE_ADMIN".equalsIgnoreCase(role);
    }
}