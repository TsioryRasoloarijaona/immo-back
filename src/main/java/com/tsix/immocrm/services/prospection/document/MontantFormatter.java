package com.tsix.immocrm.services.prospection.document;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Formats "1 200 000.00 DH" / "20.00 %" / "dd/MM/yyyy" — seul formateur du PDF, du mail et du
 * WhatsApp de l'offre, pour qu'un même envoi n'affiche pas deux prix différents.
 */
public final class MontantFormatter {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final DecimalFormat MONTANT_FORMAT;
    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRANCE);
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator('.');
        MONTANT_FORMAT = new DecimalFormat("#,##0.00", symbols);
    }

    private MontantFormatter() {
    }

    public static String montant(BigDecimal value) {
        BigDecimal safe = value != null ? value : BigDecimal.ZERO;
        return MONTANT_FORMAT.format(safe) + " DH";
    }

    static String date(LocalDate date) {
        return date != null ? date.format(DATE_FORMAT) : "";
    }

    static String vide(String value) {
        return value != null ? value : "";
    }

    public static String surface(BigDecimal value) {
        return value != null ? MONTANT_FORMAT.format(value) : "";
    }

    /** Ratio (ex. 0.20) → "20.00 %". */
    static String pourcentageRatio(BigDecimal ratio) {
        BigDecimal safe = (ratio != null ? ratio : BigDecimal.ZERO).multiply(BigDecimal.valueOf(100));
        return MONTANT_FORMAT.format(safe.setScale(2, RoundingMode.HALF_UP)) + " %";
    }

    /** Valeur déjà en %, ex. 20.00 → "20.00 %". */
    public static String pourcentage(BigDecimal percent) {
        BigDecimal safe = percent != null ? percent : BigDecimal.ZERO;
        return MONTANT_FORMAT.format(safe) + " %";
    }
}
