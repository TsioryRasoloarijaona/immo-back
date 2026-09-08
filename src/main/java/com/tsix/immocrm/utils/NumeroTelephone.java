package com.tsix.immocrm.utils;

import org.springframework.util.StringUtils;

/**
 * Primitives de manipulation d'un numéro de téléphone, communes à l'enregistrement d'une fiche
 * et à l'envoi d'un message.
 *
 * <p><b>Ce qui est ici est ce qui est vrai partout</b> : extraire les chiffres, retirer le zéro
 * initial, mettre au format de stockage. Ce qui dépend d'un usage reste chez l'appelant — la
 * règle « un mobile marocain fait 9 chiffres et commence par 6 ou 7 » vit dans
 * {@code NumeroWhatsApp}, parce qu'elle sert à décider d'un <i>envoi</i> et n'a de valeur
 * mesurée que pour le Maroc et la France. L'imposer à l'écriture rejetterait les 47 fiches
 * réelles portant un mobile nord-américain à 10 chiffres.
 *
 * <p>Extrait de {@code NumeroWhatsApp} au moment d'ajouter la validation à l'écriture. En
 * recopier une variante était le piège évident : ce dépôt porte déjà la trace de
 * {@code ProspectionListItemDto} et de ses trois mappeurs identiques, qui ont fini par diverger.
 */
public final class NumeroTelephone {

    /** Longueur d'un numéro national mobile au Maroc comme en France. */
    public static final int CHIFFRES_MOBILE_NATIONAL = 9;

    private NumeroTelephone() {
    }

    /**
     * Ne garde que les chiffres, puis retire le ou les zéros initiaux tant que le numéro reste
     * plus long qu'un national.
     *
     * <p>Le retrait conditionnel est ce qui compte : sur {@code "06 61 22 48 57"} il ramène au
     * {@code "661224857"} que porte la base, mais sur un numéro déjà court il ne mange rien.
     * Mesuré : <b>aucune ligne réelle ne contient de caractère parasite</b> hors espaces — ni
     * tiret, ni point, ni parenthèse — donc cette fonction est en pratique un simple retrait
     * d'espaces, et le reste est une précaution contre les saisies futures.
     */
    public static String chiffres(String valeur) {
        if (!StringUtils.hasText(valeur)) {
            return "";
        }
        StringBuilder propre = new StringBuilder();
        for (char c : valeur.toCharArray()) {
            if (Character.isDigit(c)) {
                propre.append(c);
            }
        }
        while (propre.length() > CHIFFRES_MOBILE_NATIONAL && propre.charAt(0) == '0') {
            propre.deleteCharAt(0);
        }
        return propre.toString();
    }

    /**
     * Forme de stockage, calquée sur ce que la base contient déjà.
     *
     * <ul>
     *   <li><b>9 chiffres</b> ⇒ espacé {@code "6 61 22 48 57"} — la forme de
     *       <b>14 285 des 14 458</b> fiches (98,8 %), celle que l'application Windows affiche et
     *       réécrit. Nos écritures en restent indiscernables, comme pour le libellé
     *       d'interaction ;</li>
     *   <li><b>toute autre longueur</b> ⇒ chiffres nus, ce que la base fait déjà pour les
     *       numéros nord-américains ({@code "5145829397"}).</li>
     * </ul>
     *
     * @return {@code null} si rien d'exploitable, jamais une chaîne vide — une colonne legacy
     *         doit porter {@code NULL} et non {@code ''}, les deux se confondant à la lecture
     *         mais pas dans les requêtes du légataire.
     */
    public static String versFormatDeStockage(String valeur) {
        String chiffres = chiffres(valeur);
        if (chiffres.isEmpty()) {
            return null;
        }
        if (chiffres.length() != CHIFFRES_MOBILE_NATIONAL) {
            return chiffres;
        }
        return chiffres.charAt(0)
                + " " + chiffres.substring(1, 3)
                + " " + chiffres.substring(3, 5)
                + " " + chiffres.substring(5, 7)
                + " " + chiffres.substring(7, 9);
    }

    /**
     * Indicatif réduit à ses chiffres — {@code "+212"} et {@code "212"} donnent la même chose.
     * Sert aussi bien à comparer au référentiel qu'à stocker, la base portant l'indicatif sans
     * le {@code +} sur 14 505 fiches.
     */
    public static String indicatifNormalise(String indicatif) {
        if (!StringUtils.hasText(indicatif)) {
            return null;
        }
        StringBuilder propre = new StringBuilder();
        for (char c : indicatif.toCharArray()) {
            if (Character.isDigit(c)) {
                propre.append(c);
            }
        }
        return propre.isEmpty() ? null : propre.toString();
    }
}
