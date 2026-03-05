package com.zeroq.sensor.common.security;

import com.zeroq.sensor.common.exception.SensorException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

@Component
public class SensorRoleGuard {
    private static final String USER_ROLE_HEADER = "X-User-Role";
    private static final Set<String> MANAGER_OR_ADMIN = Set.of("MANAGER", "ADMIN");

    public void requireManagerOrAdmin(HttpServletRequest request) {
        String role = request.getHeader(USER_ROLE_HEADER);
        if (role == null || role.isBlank()) {
            throw new SensorException.ForbiddenException("Missing X-User-Role header");
        }

        String normalizedRole = role.toUpperCase(Locale.ROOT).trim();
        if (!MANAGER_OR_ADMIN.contains(normalizedRole)) {
            throw new SensorException.ForbiddenException("Required role: MANAGER or ADMIN");
        }
    }
}
