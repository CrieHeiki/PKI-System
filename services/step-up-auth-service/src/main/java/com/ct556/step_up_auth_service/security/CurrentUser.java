package com.ct556.step_up_auth_service.security;

import java.util.List;
import java.util.UUID;

public record CurrentUser(
        UUID accountId,
        String username,
        List<String> roles
) {
    public CurrentUser {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }

    public boolean isAdmin() {
        return roles.stream().anyMatch("ROLE_ADMIN"::equals);
    }
}
