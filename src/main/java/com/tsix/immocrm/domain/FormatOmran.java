package com.tsix.immocrm.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Formats des textes qu'OMRAN calcule et stocke ({@code ComFolder.Description}, {@code ComDocumentDetail},
 * {@code StlSettlement.Description}), reproduits à l'octet près tels que relus en base.
 */
public final class FormatOmran {

    private static final DateTimeFormatter DATE_COURTE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private FormatOmran() {
    }

    /** {@code DateTime.ToShortDateString()} en culture française : {@code 11/09/2026}. */
    public static String dateCourte(LocalDateTime date) {
        return date == null ? "" : DATE_COURTE.format(date);
    }

    /** {@code decimal.ToString("c")} du poste OMRAN : {@code 637 000.00 DH} (espace simple, point décimal). */
    public static String montantDevise(BigDecimal montant) {
        DecimalFormatSymbols symboles = new DecimalFormatSymbols(Locale.ROOT);
        symboles.setGroupingSeparator(' ');
        symboles.setDecimalSeparator('.');
        DecimalFormat format = new DecimalFormat("#,##0.00", symboles);
        format.setRoundingMode(RoundingMode.HALF_UP);
        BigDecimal valeur = montant == null ? BigDecimal.ZERO : montant;
        return format.format(valeur) + " DH";
    }
}
