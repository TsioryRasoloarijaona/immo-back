package com.tsix.immocrm.controller.projet;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.fixtures.FixtureTresorerieProjet;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Lecture de {@code TresorerieProjetDto} (FEATURE-n5-tableaux-de-bord-projet.md, DTO) : arbres en {@code Map}. */
final class LectureTresorerie {

    static final String TRESORERIE = "/api/projet/tresorerie";
    static final String CELLULE = "/api/projet/tresorerie/cellule";
    static final String EXPORT = "/api/projet/tresorerie/export";
    static final String ONGLET_PROJET = "projet";
    static final String ONGLET_FOURNISSEUR = "fournisseur";
    static final String ANNEE = "annee";
    static final String PROJETS = "projets";
    static final String SOCIETE = "societe";
    static final String MOIS = "mois";
    static final String NOEUD = "noeud";
    static final String ANNEE_TEST = String.valueOf(FixtureTresorerieProjet.ANNEE);

    private LectureTresorerie() {
    }

    static List<Map<String, Object>> racines(String corps, String onglet) {
        return JsonPath.read(corps, "$.onglets." + onglet);
    }

    static Optional<Map<String, Object>> racine(String corps, String onglet, boolean credit) {
        return racines(corps, onglet).stream().filter(n -> Boolean.valueOf(credit).equals(n.get("estCredit"))).findFirst();
    }

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> enfants(Map<String, Object> noeud) {
        Object e = noeud.get("enfants");
        return e == null ? List.of() : (List<Map<String, Object>>) e;
    }

    /** Premier descendant (profondeur d'abord) de libellé donné. */
    static Optional<Map<String, Object>> descendant(Map<String, Object> noeud, String libelle) {
        for (Map<String, Object> enfant : enfants(noeud)) {
            if (libelle.equals(enfant.get("libelle"))) {
                return Optional.of(enfant);
            }
            Optional<Map<String, Object>> trouve = descendant(enfant, libelle);
            if (trouve.isPresent()) {
                return trouve;
            }
        }
        return Optional.empty();
    }

    /** Nœud de libellé donné sous la racine Encaissement ({@code credit}) ou Décaissement de l'onglet. */
    static Map<String, Object> noeud(String corps, String onglet, boolean credit, String libelle) {
        Map<String, Object> r = racine(corps, onglet, credit).orElseThrow(() -> new AssertionError("racine absente"));
        return descendant(r, libelle).orElseThrow(() -> new AssertionError("nœud absent : " + libelle));
    }

    /** Tous les libellés des deux onglets, tous niveaux. */
    static List<String> libelles(String corps) {
        List<String> tous = new ArrayList<>();
        for (String onglet : List.of(ONGLET_PROJET, ONGLET_FOURNISSEUR)) {
            racines(corps, onglet).forEach(r -> collecter(r, tous));
        }
        return tous;
    }

    private static void collecter(Map<String, Object> noeud, List<String> tous) {
        tous.add(String.valueOf(noeud.get("libelle")));
        enfants(noeud).forEach(e -> collecter(e, tous));
    }

    static BigDecimal nombre(Object valeur) {
        return valeur == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(valeur));
    }

    static BigDecimal total(Map<String, Object> noeud) {
        return nombre(noeud.get("total"));
    }

    /** Montant du mois {@code m} (1 = janvier). */
    static BigDecimal mois(Map<String, Object> noeud, int m) {
        List<?> montants = (List<?>) noeud.get("montants");
        return nombre(montants.get(m - 1));
    }

    /** Somme des totaux des racines de l'onglet (0 si aucune racine). */
    static BigDecimal totalOnglet(String corps, String onglet) {
        return racines(corps, onglet).stream().map(LectureTresorerie::total).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
