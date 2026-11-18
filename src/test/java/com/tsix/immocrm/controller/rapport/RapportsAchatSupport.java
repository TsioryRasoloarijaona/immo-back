package com.tsix.immocrm.controller.rapport;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Lecture des résultats des rapports Achat (FEATURE-n7-etats-achat.md R12-R15). Le spec ne nomme ni les sections ni
 * certaines colonnes (mois, prêt, montants de R13/R15) : les lignes se lisent toutes sections confondues, et un montant
 * non nommé se cherche par sa valeur dans la ligne.
 */
final class RapportsAchatSupport {

    static final String ACHAT = "achat";
    static final String ANNEE = "annee";
    static final String DU = "du";
    static final String AU = "au";
    static final String FORMAT = "format";
    static final String XLSX = "xlsx";
    static final String PDF = "pdf";

    private RapportsAchatSupport() {
    }

    /** Lignes de toutes les sections, dans l'ordre. */
    static List<Map<String, Object>> toutesLesLignes(List<Map<String, Object>> sections) {
        return sections.stream().flatMap(s -> AppelsRapports.lignes(s).stream()).toList();
    }

    /** Lignes (toutes sections) qui portent la colonne {@code nom}. */
    static List<Map<String, Object>> lignesAvec(List<Map<String, Object>> sections, String nom) {
        return toutesLesLignes(sections).stream().filter(l -> l.containsKey(nom)).toList();
    }

    /** Lignes dont une valeur texte contient {@code marqueur}. */
    static List<Map<String, Object>> lignesMentionnant(List<Map<String, Object>> sections, String marqueur) {
        return toutesLesLignes(sections).stream()
                .filter(l -> l.values().stream().anyMatch(v -> v instanceof String s && s.contains(marqueur)))
                .toList();
    }

    /** Vrai si une valeur numérique de la ligne vaut {@code montant}. */
    static boolean porteMontant(Map<String, Object> ligne, String montant) {
        BigDecimal attendu = new BigDecimal(montant);
        return ligne.values().stream()
                .anyMatch(v -> v instanceof Number n && new BigDecimal(n.toString()).compareTo(attendu) == 0);
    }

    /** Vrai si une des lignes porte {@code montant}. */
    static boolean unePorte(List<Map<String, Object>> lignes, String montant) {
        return lignes.stream().anyMatch(l -> porteMontant(l, montant));
    }
}
