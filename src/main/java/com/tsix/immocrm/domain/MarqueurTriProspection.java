package com.tsix.immocrm.domain;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Marqueur de tri posé à la main dans {@code ComProspection.TempField1}, colonne de texte libre :
 * {@code null} si le contenu n'est pas un marqueur. Correspondance exacte après normalisation,
 * jamais par préfixe — voir {@code instructions/FEATURE-marqueurs-tri-pre-prospection.md} §2.
 */
public enum MarqueurTriProspection {

    /** Lead en doublon d'un tiers existant ; « doublan » est une faute de frappe répandue. */
    DOUBLON("doublon", "doublan"),

    SPAM("spam"),

    TRAITE("traite");

    private final String[] variantes;

    MarqueurTriProspection(String... variantes) {
        this.variantes = variantes;
    }

    public static MarqueurTriProspection depuis(String valeurBrute) {
        String normalisee = normaliser(valeurBrute);
        if (normalisee.isEmpty()) {
            return null;
        }
        for (MarqueurTriProspection marqueur : values()) {
            for (String variante : marqueur.variantes) {
                if (variante.equals(normalisee)) {
                    return marqueur;
                }
            }
        }
        return null;
    }

    /** {@code Locale.ROOT} : en turc, {@code "I".toLowerCase()} rend {@code "ı"}. */
    private static String normaliser(String valeur) {
        if (valeur == null) {
            return "";
        }
        String sansAccent = Normalizer.normalize(valeur.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return sansAccent.toLowerCase(Locale.ROOT);
    }
}
