package com.tsix.immocrm.security;

import java.util.UUID;

/**
 * The Spring Security principal populated by {@link JwtAuthenticationFilter} from the
 * {@code access_token} cookie's claims — retrievable in any controller via
 * {@code @AuthenticationPrincipal AuthenticatedUser}.
 */
public record AuthenticatedUser(
        UUID admUserId,
        String username,
        UUID employeeId,
        boolean mustChangePassword,
        boolean profileCompleted,
        int tokenVersion
) {
}
