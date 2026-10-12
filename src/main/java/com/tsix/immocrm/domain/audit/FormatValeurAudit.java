package com.tsix.immocrm.domain.audit;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/** Texte d'une valeur scalaire tel qu'OMRAN l'écrit dans {@code Details} ({@code ToString()} .NET, culture fr). */
public final class FormatValeurAudit {

    static final DateTimeFormatter DATE_HEURE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private FormatValeurAudit() {
    }

    public static String texte(Object valeur) {
        return switch (valeur) {
            case null -> "";
            case String s -> s;
            case Boolean b -> Boolean.TRUE.equals(b) ? "True" : "False";
            case LocalDateTime d -> d.format(DATE_HEURE);
            case LocalDate d -> d.atStartOfDay().format(DATE_HEURE);
            // Point décimal (invariant) : c'est ce que montrent les lignes OMRAN (« 845000.0000000000 »).
            case BigDecimal n -> n.toPlainString();
            default -> valeur.toString();
        };
    }

    /**
     * Égalité après normalisation : deux nombres égaux en valeur ({@code 845000} et {@code 845000.000000}) ne sont
     * pas une modification. OMRAN retire tous les « 0 » et « . », ce qui confondrait 105 et 15 : non reproduit.
     */
    public static boolean equivalentes(String ancienne, String nouvelle) {
        if (Objects.equals(ancienne, nouvelle)) {
            return true;
        }
        BigDecimal a = nombre(ancienne);
        BigDecimal b = nombre(nouvelle);
        return a != null && b != null && a.compareTo(b) == 0;
    }

    private static BigDecimal nombre(String texte) {
        if (texte == null || texte.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(texte.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
