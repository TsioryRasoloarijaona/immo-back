package com.tsix.immocrm.security;

import com.tsix.immocrm.domain.PorteeCleApi;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UrlPathHelper;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * {@code X-Api-Key} des clients machine, placé avant le JWT (FEATURE-cle-api-n8n.md §5). Clé présente mais invalide
 * = 401 sans lire le cookie (sinon une clé révoquée marcherait depuis un navigateur connecté) ; hors portée = 403
 * {@code HORS_PORTEE}. Même principal {@link AuthenticatedUser} que le JWT : périmètre et rôles s'appliquent pareil.
 */
@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String EN_TETE = "X-Api-Key";
    public static final String CODE_HORS_PORTEE = "HORS_PORTEE";

    private final CleApiService cleApiService;

    public ApiKeyAuthenticationFilter(CleApiService cleApiService) {
        this.cleApiService = cleApiService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        String cle = request.getHeader(EN_TETE);
        if (!StringUtils.hasText(cle)) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<CleApiService.ClientResolu> resolu = cleApiService.resoudre(cle);
        if (resolu.isEmpty()) {
            refuser(response, HttpStatus.UNAUTHORIZED, "CLE_INVALIDE", "Clé d'API inconnue ou révoquée.");
            return;
        }

        CleApiService.ClientResolu client = resolu.get();
        Optional<String> chemin = cheminNormalise(request);
        if (chemin.isEmpty() || !PorteeCleApi.autorise(client.portees(), request.getMethod(), chemin.get())) {
            refuser(response, HttpStatus.FORBIDDEN, CODE_HORS_PORTEE,
                    "La clé « " + client.client().getNom() + " » n'autorise pas "
                            + request.getMethod() + " " + request.getRequestURI() + ".");
            return;
        }

        List<GrantedAuthority> authorities = client.roles().stream()
                .<GrantedAuthority>map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(client.principal(), null, authorities));
        cleApiService.tracerUsage(client.client());
        filterChain.doFilter(request, response);
    }

    // Chemin décodé comme Spring MVC le route (getRequestURI reste encodé : %2D contournait les exclusions).
    // Tout chemin ambigu (;, //, .., %2F, \, double encodage) est refusé plutôt que deviné.
    private static Optional<String> cheminNormalise(HttpServletRequest request) {
        String brut = request.getRequestURI().toLowerCase(Locale.ROOT);
        if (Stream.of(";", "//", "\\", "%2f", "%5c").anyMatch(brut::contains)) {
            return Optional.empty();
        }
        String decode = UrlPathHelper.defaultInstance.getPathWithinApplication(request);
        if (decode.contains("%") || decode.contains("..")) {
            return Optional.empty();
        }
        return Optional.of(decode);
    }

    private static void refuser(HttpServletResponse response, HttpStatus statut, String code, String detail)
            throws IOException {
        response.setStatus(statut.value());
        response.setContentType("application/problem+json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                "{\"type\":\"about:blank\",\"title\":\"%s\",\"status\":%d,\"detail\":\"%s\",\"code\":\"%s\"}"
                .formatted(statut.getReasonPhrase(), statut.value(), echapper(detail), code));
    }

    private static String echapper(String valeur) {
        return valeur.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
