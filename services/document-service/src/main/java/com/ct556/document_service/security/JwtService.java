package com.ct556.document_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
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

    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public CurrentUser extractCurrentUser(String token) {
        Claims claims = parseClaims(token);

        Object accountIdClaim = claims.get("accountId");
        if (accountIdClaim == null) {
            accountIdClaim = claims.get("account_id");
        }

        if (accountIdClaim == null) {
            String sub = claims.getSubject();
            try {
                UUID.fromString(sub);
                accountIdClaim = sub;
            } catch (Exception ignored) {
            }
        }

        if (accountIdClaim == null) {
            throw new IllegalArgumentException("JWT does not contain accountId claim");
        }

        String subject = claims.getSubject();
        String username = claims.get("username", String.class);
        if (StringUtils.hasText(username)) {
            subject = username;
        }

        String role = extractPrimaryRole(claims);

        return new CurrentUser(
                UUID.fromString(accountIdClaim.toString()),
                subject,
                role
        );
    }

    private String extractPrimaryRole(Claims claims) {
        List<String> roles = new ArrayList<>();
        appendRoles(roles, claims.get("roles"));
        appendRoles(roles, claims.get("authorities"));
        appendRoles(roles, claims.get("role"));

        if (roles.isEmpty()) {
            return "USER";
        }

        String normalized = roles.get(0).trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("ROLE_") ? normalized.substring("ROLE_".length()) : normalized;
    }

    private void appendRoles(List<String> roles, Object claimValue) {
        if (claimValue == null) {
            return;
        }

        if (claimValue instanceof Collection<?> collection) {
            for (Object item : collection) {
                if (item != null && StringUtils.hasText(String.valueOf(item))) {
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

        if (StringUtils.hasText(value)) {
            roles.add(value);
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
