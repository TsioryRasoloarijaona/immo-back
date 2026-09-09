package com.tsix.immocrm.web;

import com.tsix.immocrm.domain.ColonneProspection;
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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Dépose la sélection {@code ?colonnes=} dans {@link ColonnesSelection} pour la durée de la
 * requête, et la retire ensuite.
 *
 * <p>Un filtre servlet plutôt qu'un {@code ResponseBodyAdvice} : la sérialisation a lieu
 * <em>après</em> le contrôleur, et c'est le seul point qui garantisse le nettoyage même quand la
 * réponse échoue en cours d'écriture.
 *
 * <p><strong>Ce filtre ne valide pas.</strong> Un nom inconnu doit rendre 400 avec la liste des
 * noms valides, ce que fait le contrôleur via {@link ColonneProspection#resoudre(List)}. Ici, un
 * nom inconnu est simplement conservé tel quel : il ne correspondra à aucune propriété, et la
 * requête sera de toute façon rejetée en amont de la sérialisation.
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
        // Toujours définie, y compris sans paramètre : la sélection par défaut reproduit
        // exactement les colonnes servies avant ce lot. Sans cela, les 14 champs d'activité
        // apparaîtraient à `null` dans toutes les réponses, et « identique à l'existant »
        // serait faux.
        ColonnesSelection.definir(selectionDemandee(request));
        try {
            filterChain.doFilter(request, response);
        } finally {
            // Indispensable : sur un thread de pool, un ThreadLocal oublié filtrerait
            // silencieusement les réponses des requêtes suivantes.
            ColonnesSelection.effacer();
        }
    }

    /**
     * Sélection par défaut quand le paramètre est absent ou vide, sinon les noms demandés.
     *
     * <p>Accepte les deux écritures que Spring tolère déjà : paramètre répété
     * ({@code ?colonnes=a&colonnes=b}) et liste séparée par des virgules ({@code ?colonnes=a,b}).
     */
    private static Set<String> selectionDemandee(HttpServletRequest request) {
        String[] valeurs = request.getParameterValues(PARAMETRE);
        if (valeurs == null) {
            return nomsParDefaut();
        }
        Set<String> noms = Arrays.stream(valeurs)
                .flatMap(v -> Arrays.stream(v.split(",")))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (noms.isEmpty()) {
            return nomsParDefaut();
        }
        // `id` est toujours sérialisé : sans lui la ligne n'est plus cliquable côté front.
        noms.add(ColonneProspection.ID.nom());
        return noms;
    }

    private static Set<String> nomsParDefaut() {
        return ColonneProspection.parDefautSet().stream()
                .map(ColonneProspection::nom)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
