package com.tsix.immocrm.whatsapp;

import com.tsix.immocrm.utils.NumeroTelephone;

/**
 * Numéro international UltraMsg depuis {@code MobileIndicatif} (sans {@code +}) et {@code Mobile} (espacé, sans zéro
 * initial) ; format régulier en base (FEATURE-mail-confirmation-visite.md §4 bis). Un fixe est refusé en amont :
 * UltraMsg l'accepterait puis échouerait, ou écrirait à un inconnu si le numéro a été réattribué.
 */
public final class NumeroWhatsApp {

    /** Mobile national marocain, et français par coïncidence. */
    private static final int CHIFFRES_ATTENDUS = NumeroTelephone.CHIFFRES_MOBILE_NATIONAL;

    private NumeroWhatsApp() {
    }

    /** {@code ("212", "6 61 22 48 57")} → {@code "+212661224857"}, ou {@code null} si le numéro n'est pas sûr. */
    public static String international(String indicatif, String numeroNational) {
        String pays = chiffres(indicatif);
        String local = chiffres(numeroNational);

        if (pays.isEmpty() || pays.length() > 4) {
            return null;
        }
        if (local.length() != CHIFFRES_ATTENDUS || !estUnMobile(local)) {
            return null;
        }
        return "+" + pays + local;
    }

    private static String chiffres(String valeur) {
        return NumeroTelephone.chiffres(valeur);
    }

    /** Maroc comme France : un mobile commence par 6 ou 7 sans le zéro ; les 5 sont des fixes. */
    private static boolean estUnMobile(String local) {
        char premier = local.charAt(0);
        return premier == '6' || premier == '7';
    }
}
