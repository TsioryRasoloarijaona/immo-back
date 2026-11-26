package com.tsix.immocrm.web;

import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;

/**
 * Ferme à un compte agence toute route absente de {@link ListeBlancheAgence} (FEATURE-agences-vente.md R-V9) : 403
 * avant tout chargement et avant les gardes de rôle des contrôleurs. Le siège n'est jamais touché.
 */
@Component
public class RoutesFermeesAgenceInterceptor implements HandlerInterceptor {

    private static final String FERMEE = "Cette fonction n'est pas ouverte aux agences";

    private final PerimetreCommercialService perimetreService;
    private final ReponseProbleme reponseProbleme;

    public RoutesFermeesAgenceInterceptor(PerimetreCommercialService perimetreService,
                                          ReponseProbleme reponseProbleme) {
        this.perimetreService = perimetreService;
        this.reponseProbleme = reponseProbleme;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                             @NonNull Object handler) throws IOException {
        if (CorsUtils.isPreFlightRequest(request)) {
            return true;
        }
        String motif = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (motif == null || ListeBlancheAgence.autorise(request.getMethod(), motif)) {
            return true;
        }
        // Utilisateurs et rôles se relisent sur la source (tables absentes de la copie) : le rôle aussi.
        boolean surSource = motif.startsWith("/api/administration/") || motif.startsWith("/api/utilisateurs");
        return !refuser(surSource ? perimetreService.estAgenceCourantSurSource() : perimetreService.estAgenceCourant(),
                response);
    }

    /**
     * Aussi appelé par le refus de la chaîne de sécurité ({@code SecurityConfig}) : une route réservée à
     * l'Administrateur y répond 403 avant MVC, et doit porter le même motif pour une agence.
     */
    public boolean refuserSiAgence(HttpServletResponse response) throws IOException {
        return refuser(perimetreService.estAgenceCourant(), response);
    }

    private boolean refuser(boolean agence, HttpServletResponse response) throws IOException {
        if (agence) {
            reponseProbleme.ecrire(ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, FERMEE), response);
        }
        return agence;
    }
}
