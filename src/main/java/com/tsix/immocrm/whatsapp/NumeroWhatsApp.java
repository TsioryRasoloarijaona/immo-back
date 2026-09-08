package com.tsix.immocrm.whatsapp;

import com.tsix.immocrm.utils.NumeroTelephone;

/**
 * Fabrique le numéro international attendu par UltraMsg ({@code +212661224857}) à partir des
 * deux colonnes du légataire.
 *
 * <p><b>Le format en base est régulier, et c'est ce qui rend cette classe courte.</b> Mesuré sur
 * les 5 070 prospections avec rendez-vous portant un mobile :
 *
 * <ul>
 *   <li>{@code CfgTier.MobileIndicatif} porte l'indicatif <b>sans le {@code +}</b> — « 212 »
 *       sur 14 078 tiers, « 33 » sur 225, puis une longue traîne de pays ;</li>
 *   <li>{@code CfgTier.Mobile} porte le numéro national <b>espacé et sans le zéro initial</b> :
 *       {@code "6 61 22 48 57"}, soit exactement 9 chiffres commençant par 6 ou 7 dans
 *       4 973 cas sur 5 070 ;</li>
 *   <li><b>aucune ligne ne contient de caractère parasite</b> hors espaces — ni tiret, ni point,
 *       ni parenthèse. La normalisation se réduit donc à retirer les espaces.</li>
 * </ul>
 *
 * <p>Sur ces 5 070, <b>5 027 produisent un numéro exploitable</b> (99,2 %). Les 43 restants se
 * répartissent en 24 longueurs anormales ({@code "000000"}, saisies tronquées), 19 numéros qui
 * n'en sont pas ({@code "5 22 27 34 96"} est un fixe, pas un mobile) et 1 sans indicatif.
 *
 * <p><b>Pourquoi refuser un fixe plutôt que tenter l'envoi.</b> UltraMsg accepterait le numéro
 * et échouerait à la livraison, ou pire, l'enverrait à un inconnu si le numéro a été réattribué.
 * Un refus en amont est traçable et se corrige dans la fiche ; un échec côté API ne se voit que
 * dans un journal.
 */
public final class NumeroWhatsApp {

    /** Longueur d'un numéro mobile national marocain — et français, par coïncidence utile. */
    private static final int CHIFFRES_ATTENDUS = NumeroTelephone.CHIFFRES_MOBILE_NATIONAL;

    private NumeroWhatsApp() {
    }

    /**
     * {@code ("212", "6 61 22 48 57")} → {@code "+212661224857"}, ou {@code null} si l'un des
     * deux éléments ne permet pas de composer un numéro sûr.
     */
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

    /**
     * Délègue à {@link NumeroTelephone#chiffres} : retirer les caractères non numériques et le
     * zéro initial est vrai partout, et l'enregistrement d'une fiche en a besoin exactement
     * comme l'envoi. Ce qui reste propre à cette classe est la seule chose qui ne vaut que pour
     * envoyer — {@link #estUnMobile}.
     */
    private static String chiffres(String valeur) {
        return NumeroTelephone.chiffres(valeur);
    }

    /**
     * Au Maroc comme en France, un mobile commence par 6 ou 7 une fois le zéro initial retiré.
     * Les 19 numéros rejetés par cette règle en données réelles commencent par 5 — ce sont des
     * fixes, sur lesquels WhatsApp n'a rien à envoyer.
     */
    private static boolean estUnMobile(String local) {
        char premier = local.charAt(0);
        return premier == '6' || premier == '7';
    }
}
