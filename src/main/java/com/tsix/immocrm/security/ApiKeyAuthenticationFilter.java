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

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Authentification par clé d'API ({@code X-Api-Key}) pour les clients machine — n8n d'abord.
 * Voir {@code instructions/FEATURE-cle-api-n8n.md} §5.
 *
 * <p>Placé <b>avant</b> {@link JwtAuthenticationFilter}. Trois issues :
 * <ul>
 *   <li>pas d'en-tête : rien ne se passe, le cookie JWT est lu comme avant ;</li>
 *   <li>clé inconnue, révoquée ou compte désactivé : <b>401</b> immédiat — la clé présente fait
 *       foi, un cookie éventuel n'est pas consulté (sinon une clé révoquée « marcherait » encore
 *       depuis un navigateur connecté, et l'administrateur ne le verrait jamais) ;</li>
 *   <li>clé valide mais route hors portée : <b>403</b> {@code HORS_PORTEE}, écrit ici comme le
 *       fait {@link OnboardingGateFilter} ; sinon le principal est posé et la chaîne continue.</li>
 * </ul>
 *
 * <p>Le principal est le même {@link AuthenticatedUser} que celui du JWT : périmètre, auteur
 * ({@link UtilisateurAffiche}) et {@code @PreAuthorize} n'ont rien à savoir de la clé.
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
        String chemin = request.getRequestURI();
        if (!PorteeCleApi.autorise(client.portees(), request.getMethod(), chemin)) {
            refuser(response, HttpStatus.FORBIDDEN, CODE_HORS_PORTEE,
                    "La clé « " + client.client().getNom() + " » n'autorise pas "
                            + request.getMethod() + " " + chemin + ".");
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

    private static void refuser(HttpServletResponse response, HttpStatus statut, String code, String detail)
            throws IOException {
        response.setStatus(statut.value());
        response.setContentType("application/problem+json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("""
                {"type":"about:blank","title":"%s","status":%d,"detail":"%s","code":"%s"}"""
                .formatted(statut.getReasonPhrase(), statut.value(), echapper(detail), code));
    }

    private static String echapper(String valeur) {
        return valeur.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
