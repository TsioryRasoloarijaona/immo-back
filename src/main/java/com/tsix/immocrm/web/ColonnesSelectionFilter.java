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
 * Dépose la demande {@code ?colonnes=} dans {@link ColonnesSelection} pour la durée de la
 * requête, et la retire ensuite.
 *
 * <p>Un filtre servlet plutôt qu'un {@code ResponseBodyAdvice} : la sérialisation a lieu
 * <em>après</em> le contrôleur, et c'est le seul point qui garantisse le nettoyage même quand la
 * réponse échoue en cours d'écriture.
 *
 * <p><strong>Ce filtre ne connaît aucun catalogue, et c'est délibéré.</strong> Il ne sait pas quel
 * DTO le contrôleur va renvoyer : appliquer ici les défauts d'un catalogue précis viderait les
 * listes servies par les autres. Il transmet donc la demande brute, et
 * {@link SelectionColonnesPropertyFilter} — une instance par DTO filtrable — applique le défaut
 * qui convient.
 *
 * <p><strong>Il ne valide pas non plus.</strong> Un nom inconnu doit rendre 400 avec la liste des
 * noms valides, ce que fait le contrôleur via le {@code resoudre(...)} de son catalogue. Ici, un
 * nom inconnu est conservé tel quel : il ne correspondra à aucune propriété, et la requête sera
 * de toute façon rejetée en amont de la sérialisation.
 */
@Component
@Order(ColonnesSelectionFilter.ORDRE)
public class ColonnesSelectionFilter extends OncePerRequestFilter {

    /** Après la sécurité, avant le contrôleur — l'ordre exact importe peu, l'isolation oui. */
    static final int ORDRE = 100;

    private static final String PARAMETRE = "colonnes";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        ColonnesSelection.definir(new ColonnesSelection.Demande(nomsDemandes(request)));
        try {
            filterChain.doFilter(request, response);
        } finally {
            // Indispensable : sur un thread de pool, un ThreadLocal oublié filtrerait
            // silencieusement les réponses des requêtes suivantes.
            ColonnesSelection.effacer();
        }
    }

    /**
     * Noms bruts demandés, ou {@code null} quand le paramètre est absent ou vide — auquel cas
     * chaque catalogue appliquera son propre défaut.
     *
     * <p>Accepte les deux écritures que Spring tolère déjà : paramètre répété
     * ({@code ?colonnes=a&colonnes=b}) et liste séparée par des virgules ({@code ?colonnes=a,b}).
     */
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
