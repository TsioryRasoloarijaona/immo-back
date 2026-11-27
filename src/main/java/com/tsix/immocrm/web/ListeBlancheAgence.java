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
    private static final Set<String> GET_POST_DELETE = Set.of("GET", "POST", "DELETE");
    private static final String ONGLETS_DOSSIER = "fiche|historique|montants|echeancier|reglements|pieces-jointes"
            + "|payeurs|actions|compteurs|visites-techniques|cadeaux|modifications|modalite-paiement|sous-articles"
            + "|acquereurs";
    // Choix des onglets d'administration de la tranche, servis sous /api/tranches/{id} sans id.
    private static final String CHOIX_TRANCHE = "terrains-disponibles|categories-frais|entites-finalisation";
    private static final String COLONNES_CACHEES = "encaissements|impayes|suivi-actions|etat-des-ventes"
            + "|suivi-affectations|suivi-commerciaux";

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
            // Formulaire de prospection : programmes et tranches du périmètre agence (ProjetService)
            regle(GET, "/api/projets"),
            regle(GET, "/api/projets/*/tranches"),
            // Dossiers : liste, fiche et ses onglets, documents, photo du chèque ; ni désistement ni autre écriture
            regle(GET, "/api/dossiers"),
            regle(GET, "/api/dossiers/*"),
            regle(GET, "/api/dossiers/*/{onglet:" + ONGLETS_DOSSIER + "}"),
            regle(GET, "/api/dossiers/*/echeancier/*/reglements"),
            regle(GET, "/api/dossiers/*/echeancier/schema"),
            regle(GET, "/api/dossiers/*/statut/etapes-suivantes"),
            regle(GET, "/api/dossiers/*/cadeaux/{choix:catalogue|raisons}"),
            regle(GET, "/api/dossiers/*/pieces-jointes/*/fichier"),
            regle(GET, "/api/dossiers/*/documents/*"),
            regle(GET, "/api/dossiers/*/reglements/*/documents/*"),
            regle(POST, "/api/dossiers/documents/*"),
            regle(POST, "/api/dossiers"),
            regle(GET_POST_DELETE, "/api/dossiers/*/reglements/*/pieces-jointes/**"),
            // Listes déroulantes
            regle(GET, "/api/referentiels/**"));

    // Écrans cachés servis sous un préfixe ouvert : refusés avant la liste blanche.
    private static final List<Regle> REFUS = List.of(
            regle(POST, "/api/prospections/*/transformer"),
            regle(GET, "/api/tranches/{choix:" + CHOIX_TRANCHE + "}"),
            regle(GET, "/api/referentiels/colonnes/{ecran:" + COLONNES_CACHEES + "}"),
            regle(GET, "/api/referentiels/motifs-desistement"));

    private ListeBlancheAgence() {
    }

    /** {@code motif} = motif de route Spring ({@code /api/dossiers/{id}}) : un {@code *} y couvre une variable. */
    static boolean autorise(String methode, String motif) {
        PathContainer chemin = PathContainer.parsePath(motif);
        return REFUS.stream().noneMatch(r -> r.couvre(methode, chemin))
                && REGLES.stream().anyMatch(r -> r.couvre(methode, chemin));
    }

    private static Regle regle(Set<String> methodes, String motif) {
        return new Regle(methodes, PathPatternParser.defaultInstance.parse(motif));
    }
}
