package com.tsix.immocrm.services.dossier.document;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Converts a monetary amount into French words — mirrors the legacy Stimulsoft template's
 * {@code DecimalToLiteral(montant, CurrencyLabel.CodeValue, CurrencyFraction.CodeValue)} function
 * (e.g. "cent mille dirhams et cinquante centimes"), used by "Fiche Trop Perçu"
 * ({@code ComSaleFicheTropPercu.yes}). {@code CurrencyLabel}/{@code CurrencyFraction} are
 * {@code AdmParamsString} rows confirmed on real data as "dirham"/"centime" — hardcoded here
 * rather than mapping a new entity for two static params, same precedent as
 * {@link MontantFormatter}'s hardcoded " DH" suffix.
 */
final class NombreEnLettresFr {

    private static final String[] UNITES = {
            "", "un", "deux", "trois", "quatre", "cinq", "six", "sept", "huit", "neuf",
            "dix", "onze", "douze", "treize", "quatorze", "quinze", "seize",
            "dix-sept", "dix-huit", "dix-neuf"
    };
    private static final String[] DIZAINES = {
            "", "", "vingt", "trente", "quarante", "cinquante", "soixante", "soixante-dix", "quatre-vingt", "quatre-vingt-dix"
    };

    private NombreEnLettresFr() {
    }

    /** e.g. {@code montant(1250000, "dirham", "centime")} → "un million deux cent cinquante mille dirhams". */
    static String montant(BigDecimal valeur, String deviseSingulier, String deviseFractionSingulier) {
        BigDecimal safe = (valeur != null ? valeur : BigDecimal.ZERO).abs().setScale(2, RoundingMode.HALF_UP);
        long partieEntiere = safe.longValue();
        int centimes = safe.subtract(BigDecimal.valueOf(partieEntiere)).movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValue();

        StringBuilder sb = new StringBuilder();
        sb.append(enToutesLettres(partieEntiere)).append(' ').append(pluriel(deviseSingulier, partieEntiere));
        if (centimes > 0) {
            sb.append(" et ").append(enToutesLettres(centimes)).append(' ').append(pluriel(deviseFractionSingulier, centimes));
        }
        return sb.toString();
    }

    private static String pluriel(String mot, long quantite) {
        if (mot == null || mot.isBlank()) {
            return "";
        }
        return quantite > 1 && !mot.endsWith("s") ? mot + "s" : mot;
    }

    static String enToutesLettres(long nombre) {
        if (nombre == 0) {
            return "zéro";
        }
        if (nombre < 0) {
            return "moins " + enToutesLettres(-nombre);
        }
        long milliards = nombre / 1_000_000_000L;
        long millions = (nombre / 1_000_000L) % 1000;
        long milliers = (nombre / 1000L) % 1000;
        long reste = nombre % 1000;

        // "cent"/"quatre-vingts" ne prennent le pluriel que dans le tout dernier groupe de 3
        // chiffres (rien après) — convention usuelle des documents administratifs/financiers :
        // "quatre-vingt mille dirhams" (invariable) mais "cent quatre-vingts dirhams" (pluriel).
        StringBuilder sb = new StringBuilder();
        if (milliards > 0) {
            sb.append(centaines(milliards, false)).append(milliards > 1 ? " milliards " : " milliard ");
        }
        if (millions > 0) {
            sb.append(centaines(millions, false)).append(millions > 1 ? " millions " : " million ");
        }
        if (milliers > 0) {
            sb.append(milliers == 1 ? "mille " : centaines(milliers, false) + " mille ");
        }
        if (reste > 0) {
            sb.append(centaines(reste, true));
        }
        return sb.toString().trim();
    }

    /** 0-999. {@code dernierGroupe}: true quand rien ne suit ce groupe dans le nombre entier. */
    private static String centaines(long n, boolean dernierGroupe) {
        if (n < 20) {
            return UNITES[(int) n];
        }
        if (n < 100) {
            return dizaines(n, dernierGroupe);
        }
        long c = n / 100;
        long r = n % 100;
        StringBuilder sb = new StringBuilder();
        if (c == 1) {
            sb.append("cent");
        } else {
            sb.append(UNITES[(int) c]).append(" cent");
            if (r == 0 && dernierGroupe) {
                sb.append('s'); // "deux cents" mais "deux cent cinquante" ou "deux cent mille" (pas de 's' si suivi)
            }
        }
        if (r > 0) {
            sb.append(' ').append(dizaines(r, dernierGroupe));
        }
        return sb.toString();
    }

    /** 0-99. */
    private static String dizaines(long n, boolean dernierGroupe) {
        if (n < 20) {
            return UNITES[(int) n];
        }
        long d = n / 10;
        long u = n % 10;
        if (d == 7 || d == 9) {
            // soixante-dix (60+10..19) / quatre-vingt-dix (80+10..19)
            long baseDizaine = d - 1;
            if (d == 7 && u == 1) {
                return DIZAINES[(int) baseDizaine] + " et " + UNITES[(int) (10 + u)]; // "soixante et onze" (exception)
            }
            return DIZAINES[(int) baseDizaine] + "-" + UNITES[(int) (10 + u)];
        }
        String base = DIZAINES[(int) d];
        if (u == 0) {
            return (d == 8 && dernierGroupe) ? base + "s" : base; // "quatre-vingts" pluriel quand rond ET final
        }
        if (u == 1 && d != 8) {
            return base + " et un"; // "vingt et un"... mais "quatre-vingt-un" (pas de "et")
        }
        return base + "-" + UNITES[(int) u];
    }
}
