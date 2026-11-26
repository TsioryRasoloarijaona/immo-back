package com.tsix.immocrm.web;

import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.List;
import java.util.Set;

/**
 * Seules routes ouvertes à un compte agence (FEATURE-agences-vente.md R-V9, §6) : tout le reste de {@code /api/**} est
 * fermé. A2/A3 rouvrent une route en ajoutant une ligne, jamais en retirant le contrôle.
 */
final class ListeBlancheAgence {

    private static final Set<String> TOUTES = Set.of();
    private static final Set<String> GET = Set.of("GET");
    private static final Set<String> POST = Set.of("POST");
    private static final Set<String> POST_DELETE = Set.of("POST", "DELETE");

    private record Regle(Set<String> methodes, PathPattern motif) {
        boolean couvre(String methode, PathContainer chemin) {
            return (methodes.isEmpty() || methodes.contains(methode)) && motif.matches(chemin);
        }
    }

    private static final List<Regle> REGLES = List.of(
            // Connexion, profil, mot de passe
            regle(TOUTES, "/api/auth/**"),
            regle(TOUTES, "/api/mon-compte/**"),
            // Tableau de bord (ni annuel ni nouveaux pré-prospects), Ma journée, calendrier, notifications
            regle(GET, "/api/dashboard"),
            regle(GET, "/api/dashboard/relances"),
            regle(GET, "/api/actions-recentes"),
            regle(GET, "/api/commerciaux"),
            regle(GET, "/api/journee"),
            regle(GET, "/api/calendrier/evenements"),
            regle(TOUTES, "/api/notifications/**"),
            // Prospects, prospections, offres, clients
            regle(TOUTES, "/api/prospects/**"),
            regle(TOUTES, "/api/prospections/**"),
            regle(TOUTES, "/api/devis/**"),
            regle(GET, "/api/clients/**"),
            // Plans, fiche lot, fiche tranche (lecture)
            regle(GET, "/api/plans/**"),
            regle(GET, "/api/tranches/*/plans"),
            regle(GET, "/api/articles/**"),
            regle(GET, "/api/sous-articles/**"),
            regle(GET, "/api/tranches"),
            regle(GET, "/api/tranches/export"),
            regle(GET, "/api/tranches/*"),
            regle(GET, "/api/tranches/*/articles"),
            regle(GET, "/api/tranches/*/fiche.pdf"),
            // Dossiers : lectures, documents en lot, photo du chèque ; aucune autre écriture
            regle(GET, "/api/dossiers/**"),
            regle(POST, "/api/dossiers/documents/*"),
            regle(POST_DELETE, "/api/dossiers/*/reglements/*/pieces-jointes/**"),
            // Listes déroulantes
            regle(GET, "/api/referentiels/**"));

    private ListeBlancheAgence() {
    }

    /** {@code motif} = motif de route Spring ({@code /api/dossiers/{id}}) : un {@code *} y couvre une variable. */
    static boolean autorise(String methode, String motif) {
        PathContainer chemin = PathContainer.parsePath(motif);
        return REGLES.stream().anyMatch(r -> r.couvre(methode, chemin));
    }

    private static Regle regle(Set<String> methodes, String motif) {
        return new Regle(methodes, PathPatternParser.defaultInstance.parse(motif));
    }
}
