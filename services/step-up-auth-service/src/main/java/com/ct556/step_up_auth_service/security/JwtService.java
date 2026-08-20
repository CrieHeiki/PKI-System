package com.ct556.step_up_auth_service.security;

import com.ct556.step_up_auth_service.config.JwtProperties;
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

    public CurrentUser extractCurrentUser(String token) {
        Claims claims = extractAllClaims(token);

        UUID accountId = extractAccountId(claims);
        String username = extractUsername(claims);
        List<String> roles = extractRoles(claims);

        return new CurrentUser(accountId, username, roles);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .setAllowedClockSkewSeconds(jwtProperties.clockSkewSeconds())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    private UUID extractAccountId(Claims claims) {
        Object value = firstNonNull(
                claims.get("accountId"),
                claims.get("account_id"),
                claims.get("userId"),
                claims.get("user_id")
        );

        if (value == null) {
            throw new IllegalArgumentException("JWT does not contain accountId/account_id claim");
        }

        return UUID.fromString(String.valueOf(value));
    }

    private String extractUsername(Claims claims) {
        if (StringUtils.hasText(claims.getSubject())) {
            return claims.getSubject();
        }

        Object username = firstNonNull(claims.get("username"), claims.get("email"));
        return username == null ? "unknown" : String.valueOf(username);
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
        return normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized;
    }

    private Object firstNonNull(Object... values) {
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
