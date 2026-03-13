package com.zeroq.sensor.common.security;

import com.zeroq.sensor.common.exception.SensorException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class SensorRoleGuard {
    private static final String USER_ROLE_HEADER = "X-User-Role";
    private static final Set<String> MANAGER_OR_ADMIN = Set.of("MANAGER", "ADMIN");
    private static final Set<String> GATEWAY_OR_MANAGER_OR_ADMIN = Set.of("GATEWAY", "MANAGER", "ADMIN");
    private static final Set<String> AUTHENTICATED_ROLES = Set.of("USER", "MANAGER", "ADMIN", "GATEWAY");
    private static final Map<String, String> ROLE_PREFIX_ALIASES = Map.of(
            "ROLE_USER", "USER",
            "ROLE_MANAGER", "MANAGER",
            "ROLE_ADMIN", "ADMIN",
            "ROLE_GATEWAY", "GATEWAY"
    );

    public void requireManagerOrAdmin(HttpServletRequest request) {
        String normalizedRole = normalizeRole(request.getHeader(USER_ROLE_HEADER));
        if (!MANAGER_OR_ADMIN.contains(normalizedRole)) {
            throw new SensorException.ForbiddenException("Required role: MANAGER or ADMIN");
        }
    }

    public void requireGatewayOrManagerOrAdmin(HttpServletRequest request) {
        String normalizedRole = normalizeRole(request.getHeader(USER_ROLE_HEADER));
        if (!GATEWAY_OR_MANAGER_OR_ADMIN.contains(normalizedRole)) {
            throw new SensorException.ForbiddenException("Required role: GATEWAY, MANAGER, or ADMIN");
        }
    }

    public void requireAuthenticatedRole(HttpServletRequest request) {
        String normalizedRole = normalizeRole(request.getHeader(USER_ROLE_HEADER));
        if (!AUTHENTICATED_ROLES.contains(normalizedRole)) {
            throw new SensorException.ForbiddenException("Required role: USER, MANAGER, or ADMIN");
        }
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            throw new SensorException.ForbiddenException("Missing X-User-Role header");
        }

        String normalizedRole = role.toUpperCase(Locale.ROOT).trim();
        return ROLE_PREFIX_ALIASES.getOrDefault(normalizedRole, normalizedRole);
    }
}
