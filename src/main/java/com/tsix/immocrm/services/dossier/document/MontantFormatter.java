package com.tsix.immocrm.services.dossier.document;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formats « 1 200 000.00 DH » et « dd/MM/yyyy » des documents de dossier. */
final class MontantFormatter {

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

    static String montant(BigDecimal value) {
        BigDecimal safe = value != null ? value : BigDecimal.ZERO;
        return MONTANT_FORMAT.format(safe) + " DH";
    }

    static String date(LocalDate date) {
        return date != null ? date.format(DATE_FORMAT) : "";
    }

    static String vide(String value) {
        return value != null ? value : "";
    }

    static String pourcentage(BigDecimal value) {
        BigDecimal safe = value != null ? value : BigDecimal.ZERO;
        return MONTANT_FORMAT.format(safe) + " %";
    }

    /** Pour les champs stockés en ratio (ex. PartPercent: 0.5 = 50%), contrairement à {@link #pourcentage}. */
    static String pourcentageRatio(BigDecimal ratio) {
        BigDecimal safe = (ratio != null ? ratio : BigDecimal.ZERO).multiply(BigDecimal.valueOf(100));
        return MONTANT_FORMAT.format(safe) + " %";
    }
}
