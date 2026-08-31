package com.tsix.immocrm.utils;

/**
 * Normalizes phone numbers for cross-column, format-agnostic lookups (see
 * {@code GET /api/hotline/recherche}). Legacy {@code CfgTier}/{@code ComProspection} phone data
 * is stored inconsistently — with/without the "+212" country code, with/without spaces —
 * confirmed empirically against the real {@code yesRems} database (e.g. "+212633659468",
 * "662653125" and "6 61 40 30 51" all refer to the same kind of number). Both the Java-side
 * search key and the JPQL query applied to each phone column (see
 * {@code CfgTierRepository#findByNormalizedPhone} and
 * {@code ComProspectionRepository#findByNormalizedTempTel}) strip everything but digits and keep
 * only the last 9 (a Moroccan local number, without country code), so "+212 6 12 34 56 78",
 * "212612345678" and "0612345678" all normalize to the same key.
 */
public final class PhoneNumberUtils {

    private static final int LOCAL_NUMBER_LENGTH = 9;

    private PhoneNumberUtils() {
    }

    /**
     * Strips everything but digits from {@code rawPhoneNumber} and keeps only the last
     * {@value #LOCAL_NUMBER_LENGTH} characters (returns the digits as-is if there are fewer).
     * Returns an empty string for {@code null} or a number with no digits at all.
     */
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
