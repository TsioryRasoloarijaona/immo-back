package com.tsix.immocrm.utils;

/**
 * Clé de recherche hotline : chiffres seuls, 9 derniers gardés (numéro marocain sans indicatif), comme la requête
 * appliquée aux colonnes ({@code CfgTierRepository#findByNormalizedPhone}) ; les deux doivent rester identiques.
 */
public final class PhoneNumberUtils {

    private static final int LOCAL_NUMBER_LENGTH = 9;

    private PhoneNumberUtils() {
    }

    /** Moins de 9 chiffres : rendus tels quels ; {@code null} ou aucun chiffre : chaîne vide. */
    public static String normalize(String rawPhoneNumber) {
        if (rawPhoneNumber == null) {
            return "";
        }
        String digitsOnly = rawPhoneNumber.replaceAll("[^0-9]", "");
        if (digitsOnly.length() <= LOCAL_NUMBER_LENGTH) {
            return digitsOnly;
        }
        return digitsOnly.substring(digitsOnly.length() - LOCAL_NUMBER_LENGTH);
    }
}
