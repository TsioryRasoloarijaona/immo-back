package com.tsix.immocrm.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Runs after {@link JwtAuthenticationFilter}: while the authenticated caller still has a
 * temporary password ({@code mustChangePassword}) or hasn't submitted the "première connexion"
 * form ({@code !profileCompleted}), every route other than {@code /api/auth/**} and
 * {@code /api/mon-compte/**} is blocked with 403. The response body's {@code code} property (not
 * just prose) tells the frontend which onboarding screen to show. Written by hand rather than via
 * an injected {@code ObjectMapper} — {@code spring-boot-starter-webmvc} doesn't expose one as an
 * autowireable bean the way the classic {@code spring-boot-starter-web} did, and the payload
 * shape here is fixed and simple enough not to need Jackson at all.
 */
@Component
public class OnboardingGateFilter extends OncePerRequestFilter {

    private static final List<String> ALLOWED_PREFIXES = List.of("/api/auth/", "/api/mon-compte");

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String path = request.getRequestURI();
        boolean isAllowedPath = ALLOWED_PREFIXES.stream().anyMatch(path::startsWith);

        if (!isAllowedPath && auth != null && auth.getPrincipal() instanceof AuthenticatedUser user
                && (user.mustChangePassword() || !user.profileCompleted())) {
            String code = user.mustChangePassword() ? "MOT_DE_PASSE_TEMPORAIRE" : "PROFIL_INCOMPLET";
            String detail = user.mustChangePassword()
                    ? "Un changement de mot de passe est requis avant d'utiliser l'application."
                    : "Le profil doit être complété avant d'utiliser l'application.";
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType("application/problem+json");
            response.getWriter().write("""
                    {"type":"about:blank","title":"Forbidden","status":403,"detail":"%s","code":"%s"}"""
                    .formatted(escapeJson(detail), code));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
