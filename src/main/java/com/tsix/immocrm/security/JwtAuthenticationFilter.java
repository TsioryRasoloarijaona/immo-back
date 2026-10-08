package com.tsix.immocrm.security;

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
 * Lit le cookie JWT ; n'authentifie que si son {@code tokenVersion} égale celui d'{@code AppUserCredential}
 * (un changement ou une réinitialisation de mot de passe l'incrémente : les anciens jetons meurent sans liste noire).
 * Ne rejette jamais lui-même : contexte vide, et c'est {@code anyRequest().authenticated()} qui rend 401.
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
                .filter(c -> CookieJwt.NOM.equals(c.getName()))
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
            // Jeton malformé, expiré ou mal signé : la requête reste anonyme.
        }
    }
}
