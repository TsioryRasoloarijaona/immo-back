package com.tsix.immocrm.services.patrimoine.importation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Set;

/** Conversion d'une cellule lue vers la valeur du DTO d'écriture (FEATURE-patrimoine-import-export.md §1.2). */
enum TypeCellule {
    TEXTE, NOMBRE, ENTIER, DATE, BOOLEEN,
    /** Case « Indisponible … » : la valeur du DTO est l'inverse. */
    BOOLEEN_INVERSE,
    /** Fraction ; une valeur &gt; 1 est un pourcentage (÷ 100). */
    TVA,
    /** Code (ou libellé) d'un référentiel, résolu en id par {@link ResolutionCodes}. */
    REFERENCE;

    private static final DateTimeFormatter JJ_MM_AAAA = DateTimeFormatter.ofPattern("dd/MM/uuuu");
    private static final Set<String> VRAI = Set.of("oui", "vrai", "1", "x", "true");
    private static final Set<String> FAUX = Set.of("non", "faux", "0", "false");
    static final BigDecimal CENT = BigDecimal.valueOf(100);

    /** Valeur illisible pour ce type. */
    static final class Illisible extends RuntimeException {
        Illisible() {
            super(null, null, false, false);
        }
    }

    Object lire(Object brut) {
        return switch (this) {
            case TEXTE, REFERENCE -> texte(brut);
            case NOMBRE -> nombre(brut);
            case ENTIER -> entier(brut);
            case DATE -> date(brut);
            case BOOLEEN -> booleen(brut);
            case BOOLEEN_INVERSE -> !booleen(brut);
            case TVA -> tva(nombre(brut));
        };
    }

    static String texte(Object brut) {
        return switch (brut) {
            case BigDecimal d -> d.stripTrailingZeros().toPlainString();
            case LocalDateTime d -> d.format(JJ_MM_AAAA);
            case Boolean b -> Boolean.TRUE.equals(b) ? "Oui" : "Non";
            default -> brut.toString();
        };
    }

    static BigDecimal tva(BigDecimal taux) {
        return taux.compareTo(BigDecimal.ONE) > 0 ? taux.divide(CENT) : taux;
    }

    private static BigDecimal nombre(Object brut) {
        if (brut instanceof BigDecimal d) {
            return d;
        }
        if (!(brut instanceof String s)) {
            throw new Illisible();
        }
        try {
            return new BigDecimal(s.replace(" ", "").replace(" ", "").replace(" ", "").replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new Illisible();
        }
    }

    private static Integer entier(Object brut) {
        try {
            return nombre(brut).intValueExact();
        } catch (ArithmeticException e) {
            throw new Illisible();
        }
    }

    private static LocalDateTime date(Object brut) {
        if (brut instanceof LocalDateTime d) {
            return d;
        }
        try {
            return LocalDate.parse(texte(brut), JJ_MM_AAAA).atStartOfDay();
        } catch (DateTimeParseException e) {
            throw new Illisible();
        }
    }

    private static boolean booleen(Object brut) {
        if (brut instanceof Boolean b) {
            return b;
        }
        String s = texte(brut).toLowerCase(Locale.ROOT);
        if (VRAI.contains(s)) {
            return true;
        }
        if (FAUX.contains(s)) {
            return false;
        }
        throw new Illisible();
    }
}
