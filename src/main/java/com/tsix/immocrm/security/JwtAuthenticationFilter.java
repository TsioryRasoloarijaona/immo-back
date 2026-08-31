package com.tsix.immocrm.security;

import com.tsix.immocrm.controller.auth.AuthController;
import com.tsix.immocrm.repositories.app.AppUserCredentialRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Reads the {@code access_token} httpOnly cookie, verifies its signature/expiry, and — only if
 * its embedded {@code tokenVersion} claim still matches {@code AppUserCredential.tokenVersion} in
 * DB (a password change/admin reset bumps that, instantly invalidating older tokens without a
 * blacklist table) — populates the {@link SecurityContextHolder} with an
 * {@link AuthenticatedUser} principal and {@code ROLE_<AdmRole.Code>} authorities.
 * <p>
 * Never throws or rejects the request itself: a missing/invalid/stale-version cookie simply
 * leaves the security context empty, and {@code anyRequest().authenticated()} in
 * {@code SecurityConfig} is what turns that into a 401 — this filter's only job is "identify the
 * caller if possible".
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AppUserCredentialRepository credentialRepository;

    public JwtAuthenticationFilter(JwtService jwtService, AppUserCredentialRepository credentialRepository) {
        this.jwtService = jwtService;
        this.credentialRepository = credentialRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        extractCookie(request).ifPresent(this::authenticate);
        filterChain.doFilter(request, response);
    }

    private Optional<String> extractCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(c -> AuthController.COOKIE_NAME.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    private void authenticate(String token) {
        try {
            Claims claims = jwtService.parse(token);
            UUID admUserId = UUID.fromString(claims.getSubject());
            Integer tokenVersion = claims.get(JwtService.CLAIM_TOKEN_VERSION, Integer.class);

            credentialRepository.findByAdmUserId(admUserId)
                    .filter(credential -> Objects.equals(credential.getTokenVersion(), tokenVersion))
                    .ifPresent(credential -> {
                        @SuppressWarnings("unchecked")
                        List<String> roles = claims.get(JwtService.CLAIM_ROLES, List.class);
                        String employeeIdClaim = claims.get(JwtService.CLAIM_EMPLOYEE_ID, String.class);

                        AuthenticatedUser principal = new AuthenticatedUser(
                                admUserId,
                                claims.get(JwtService.CLAIM_USERNAME, String.class),
                                employeeIdClaim != null ? UUID.fromString(employeeIdClaim) : null,
                                Boolean.TRUE.equals(claims.get(JwtService.CLAIM_MUST_CHANGE_PASSWORD, Boolean.class)),
                                Boolean.TRUE.equals(claims.get(JwtService.CLAIM_PROFILE_COMPLETED, Boolean.class)),
                                tokenVersion != null ? tokenVersion : 0
                        );
                        List<GrantedAuthority> authorities = (roles == null ? List.<String>of() : roles).stream()
                                .<GrantedAuthority>map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                                .toList();
                        SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(principal, null, authorities));
                    });
        } catch (JwtException | IllegalArgumentException ex) {
            // Malformed/expired/bad-signature token — leave the request unauthenticated.
        }
    }
}
