package com.tsix.immocrm.security;

import java.util.UUID;

/** Principal Spring Security, posé par {@link JwtAuthenticationFilter} ou {@link ApiKeyAuthenticationFilter}. */
public record AuthenticatedUser(
        UUID admUserId,
        String username,
        UUID employeeId,
        boolean mustChangePassword,
        boolean profileCompleted,
        int tokenVersion
) {
}
