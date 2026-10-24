package com.tsix.immocrm.controller.rapport;

import com.jayway.jsonpath.JsonPath;
import org.assertj.core.api.SoftAssertions;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.colonnesAvecTotal;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.nombre;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.somme;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.totaux;

/** Lectures communes des tests R3a (FEATURE-tdb-vente-rapports-commercial.md §10). */
final class LecturesRapportsCommerciaux {

    static final String SOMME = "SOMME";
    static final String AUTRES = "AUTRES < 2%";
    static final String NON_DEFINIE = "Non définie";

    private LecturesRapportsCommerciaux() {
    }

    /** Lignes dont la colonne {@code nom} contient {@code jeton} (noms affichés : compte, employé ou code). */
    static List<Map<String, Object>> lignesContenant(Map<String, Object> section, String nom, String jeton) {
        return lignes(section).stream().filter(l -> String.valueOf(l.get(nom)).contains(jeton)).toList();
    }

    /** Valeur de {@code colonne} sur l'unique ligne contenant {@code jeton} ; 0 si aucune ligne. */
    static BigDecimal valeur(Map<String, Object> section, String nom, String jeton, String colonne) {
        List<Map<String, Object>> trouvees = lignesContenant(section, nom, jeton);
        if (trouvees.isEmpty()) {
            return BigDecimal.ZERO;
        }
        if (trouvees.size() > 1) {
            throw new AssertionError("plusieurs lignes pour " + jeton + " dans " + section.get("id"));
        }
        BigDecimal v = nombre(trouvees.get(0).get(colonne));
        return v == null ? BigDecimal.ZERO : v;
    }

    /** libellé → nombre d'une section de répartition (colonnes {@code libelle}, {@code nombre}), dans l'ordre. */
    static Map<String, Long> repartition(Map<String, Object> section) {
        Map<String, Long> parLibelle = new LinkedHashMap<>();
        lignes(section).forEach(l -> parLibelle.put(String.valueOf(l.get("libelle")), entier(l.get("nombre"))));
        return parLibelle;
    }

    /** Effectif lu sur le fil (30, 30.0…) ; {@code null} si absent. */
    static Long entier(Object valeur) {
        BigDecimal v = nombre(valeur);
        return v == null ? null : v.longValueExact();
    }

    /** Type d'un paramètre du descripteur ; {@code null} si absent. */
    static String typeParametre(String descripteur, String nom) {
        List<String> types = JsonPath.read(descripteur, "$.parametres[?(@.nom == '" + nom + "')].type");
        return types.isEmpty() ? null : types.get(0);
    }

    /** Toute colonne SOMME d'une section non vide : total serveur = somme des lignes. */
    static void totauxEgauxSommeDesLignes(SoftAssertions soft, List<Map<String, Object>> sections) {
        for (Map<String, Object> s : sections) {
            if (lignes(s).isEmpty()) {
                continue;
            }
            for (String nom : colonnesAvecTotal(s, SOMME)) {
                soft.assertThat(totaux(s)).as("totaux de %s", s.get("id")).isNotNull();
                if (totaux(s) != null) {
                    soft.assertThat(nombre(totaux(s).get(nom))).as("%s.%s", s.get("id"), nom)
                            .isEqualByComparingTo(somme(lignes(s), nom));
                }
            }
        }
    }

    /** Types des graphiques d'une section. */
    @SuppressWarnings("unchecked")
    static List<String> typesGraphiques(Map<String, Object> section) {
        List<Map<String, Object>> graphiques = (List<Map<String, Object>>) section.get("graphiques");
        return graphiques == null ? List.of() : graphiques.stream().map(g -> String.valueOf(g.get("type"))).toList();
    }

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> graphiques(Map<String, Object> section) {
        List<Map<String, Object>> graphiques = (List<Map<String, Object>>) section.get("graphiques");
        return graphiques == null ? List.of() : graphiques;
    }
}
