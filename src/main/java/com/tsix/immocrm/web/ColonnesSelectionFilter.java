package com.tsix.immocrm.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Dépose {@code ?colonnes=} dans {@link ColonnesSelection} pour la requête. Filtre servlet et non
 * {@code ResponseBodyAdvice} : seul point qui garantit le nettoyage même si l'écriture échoue. Ne valide pas
 * (le contrôleur rend 400 via le {@code resoudre} de son catalogue) et ne connaît aucun catalogue.
 */
@Component
@Order(ColonnesSelectionFilter.ORDRE)
public class ColonnesSelectionFilter extends OncePerRequestFilter {

    /** Après la sécurité, avant le contrôleur : l'ordre exact importe peu, l'isolation oui. */
    static final int ORDRE = 100;

    private static final String PARAMETRE = "colonnes";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        ColonnesSelection.definir(new ColonnesSelection.Demande(nomsDemandes(request)));
        try {
            filterChain.doFilter(request, response);
        } finally {
            // Sur un thread de pool, un ThreadLocal oublié filtrerait les réponses des requêtes suivantes.
            ColonnesSelection.effacer();
        }
    }

    /** {@code null} si absent ou vide ; accepte {@code ?colonnes=a&colonnes=b} comme {@code ?colonnes=a,b}. */
    private static Set<String> nomsDemandes(HttpServletRequest request) {
        String[] valeurs = request.getParameterValues(PARAMETRE);
        if (valeurs == null) {
            return null;
        }
        Set<String> noms = Arrays.stream(valeurs)
                .flatMap(v -> Arrays.stream(v.split(",")))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return noms.isEmpty() ? null : noms;
    }
}
