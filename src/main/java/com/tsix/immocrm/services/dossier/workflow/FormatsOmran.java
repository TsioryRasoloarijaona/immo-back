package com.tsix.immocrm.services.dossier.workflow;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Formats {@code "p"} et {@code "c"} de .NET dans les messages d'OMRAN : {@code 12,50 %}, {@code 112 500,00}. */
final class FormatsOmran {

    private FormatsOmran() {
    }

    static String pourcentage(BigDecimal ratio) {
        return format().format(ratio.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_EVEN)) + " %";
    }

    static String montant(BigDecimal montant) {
        return format().format(montant.setScale(2, RoundingMode.HALF_EVEN));
    }

    private static DecimalFormat format() {
        DecimalFormatSymbols symboles = new DecimalFormatSymbols(Locale.FRANCE);
        symboles.setGroupingSeparator(' ');
        symboles.setDecimalSeparator(',');
        return new DecimalFormat("#,##0.00", symboles);
    }
}
