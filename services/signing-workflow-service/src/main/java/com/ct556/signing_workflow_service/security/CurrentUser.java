package com.ct556.signing_workflow_service.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class CurrentUser {
    private UUID accountId;
    private String username;
    private List<String> roles;

    public boolean isAdmin() {
        return roles != null && roles.stream().anyMatch(r ->
                "ADMIN".equalsIgnoreCase(r) || "ROLE_ADMIN".equalsIgnoreCase(r)
        );
    }
}
