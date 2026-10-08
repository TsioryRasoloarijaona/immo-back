package com.tsix.immocrm.utils;

import org.springframework.util.StringUtils;

/**
 * Ce qui est vrai pour tout numéro, à l'écriture comme à l'envoi. La règle « mobile = 9 chiffres en 6/7 » reste dans
 * {@code NumeroWhatsApp} : l'imposer à l'écriture rejetterait les mobiles nord-américains (FEATURE-indicatif-telephone.md).
 */
public final class NumeroTelephone {

    /** Mobile national, au Maroc comme en France. */
    public static final int CHIFFRES_MOBILE_NATIONAL = 9;

    private NumeroTelephone() {
    }

    /** Chiffres seuls, zéros initiaux retirés tant que le numéro reste plus long qu'un national. */
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
     * Calquée sur la base : 9 chiffres ⇒ espacé {@code "6 61 22 48 57"} (forme qu'OMRAN affiche et réécrit), sinon
     * chiffres nus. {@code null} plutôt que {@code ''} : les requêtes du légataire ne les confondent pas.
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

    /** {@code "+212"} et {@code "212"} donnent {@code "212"} : la base stocke l'indicatif sans {@code +}. */
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
