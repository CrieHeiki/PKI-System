package com.ct556.signing_workflow_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public CurrentUser parseToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        String issuer = claims.getIssuer();
        if (jwtProperties.getIssuer() != null && !jwtProperties.getIssuer().isBlank()) {
            if (!jwtProperties.getIssuer().equals(issuer)) {
                throw new IllegalArgumentException("Invalid token issuer");
            }
        }

        Object accountIdRaw = firstNonNull(
                claims.get("accountId"),
                claims.get("account_id")
        );
        if (accountIdRaw == null) {
            throw new IllegalArgumentException("Token does not contain accountId");
        }

        UUID accountId = UUID.fromString(String.valueOf(accountIdRaw));
        String username = extractUsername(claims);

        List<String> roles = extractRoles(claims);

        return new CurrentUser(accountId, username, roles);
    }

    private List<String> extractRoles(Claims claims) {
        List<String> roles = new ArrayList<>();
        appendRoles(roles, claims.get("roles"));
        appendRoles(roles, claims.get("authorities"));
        appendRoles(roles, claims.get("role"));

        return roles.stream()
                .filter(StringUtils::hasText)
                .map(this::normalizeRole)
                .distinct()
                .toList();
    }

    private String extractUsername(Claims claims) {
        String username = claims.get("username", String.class);
        if (StringUtils.hasText(username)) {
            return username;
        }

        if (StringUtils.hasText(claims.getSubject())) {
            return claims.getSubject();
        }

        return "unknown";
    }

    private void appendRoles(List<String> roles, Object claimValue) {
        if (claimValue == null) {
            return;
        }

        if (claimValue instanceof Collection<?> collection) {
            for (Object item : collection) {
                if (item != null) {
                    roles.add(String.valueOf(item));
                }
            }
            return;
        }

        String value = String.valueOf(claimValue);
        if (value.contains(",")) {
            for (String item : value.split(",")) {
                if (StringUtils.hasText(item)) {
                    roles.add(item.trim());
                }
            }
            return;
        }

        roles.add(value);
    }

    private String normalizeRole(String role) {
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("ROLE_")) {
            return normalized;
        }
        return "ROLE_" + normalized;
    }

    private Object firstNonNull(Object... values) {
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
