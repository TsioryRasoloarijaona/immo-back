package com.tsix.immocrm.services.dossier.edition;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Plafonds de remise d'un article ({@code Sale.cs:VerifDiscount}) : niveau 1 = {@code StkItem.DiscountMax},
 * niveau 2 = {@code DiscountMax2}, ratios 0–1 comparés arrondis à 4 décimales (devise à 2 + 2).
 */
public final class PlafondRemise {

    /** Niveau de remise que le rôle de l'appelant autorise (droits {@code toolModifyAmounts} / {@code …1}). */
    public enum Niveau { AUCUN, PREMIER, DEUXIEME }

    private static final int DECIMALES = 4;
    /** Rôles qui détiennent {@code toolModifyAmounts1} dans AdmRight (yesRemsProd). */
    private static final Set<String> ROLES_NIVEAU_2 = Set.of("Administrateur", "Assistante Commerciale",
            "Commercial Chef", "Direction fonciere", "Direction Générale", "Niveua remise 2",
            "Responsable marketing et communication");
    /** Rôles qui détiennent {@code toolModifyAmounts} sans {@code toolModifyAmounts1}. */
    private static final Set<String> ROLES_NIVEAU_1 = Set.of("Commercial");

    private PlafondRemise() {
    }

    public static Niveau niveauDe(Collection<String> rolesAppelant) {
        if (rolesAppelant.stream().anyMatch(ROLES_NIVEAU_2::contains)) {
            return Niveau.DEUXIEME;
        }
        return rolesAppelant.stream().anyMatch(ROLES_NIVEAU_1::contains) ? Niveau.PREMIER : Niveau.AUCUN;
    }

    /** Message OMRAN si {@code taux} dépasse le plafond du niveau, vide sinon. */
    public static Optional<String> depassement(BigDecimal taux, BigDecimal max1, BigDecimal max2, Niveau niveau) {
        BigDecimal t = arrondi(taux);
        return switch (niveau) {
            case DEUXIEME -> t.compareTo(arrondi(max2)) > 0
                    ? Optional.of("Vous ne pouvez pas dépasser le deuxième niveau de remise pour cet article ("
                    + pourcent(max2) + ") !")
                    : Optional.empty();
            case PREMIER -> t.compareTo(arrondi(max1)) > 0
                    ? Optional.of("Vous ne pouvez pas dépasser le premier niveau de remise pour cet article ("
                    + pourcent(max1) + ") !")
                    : Optional.empty();
            case AUCUN -> t.signum() > 0 ? Optional.of(messageSansRemise(max2)) : Optional.empty();
        };
    }

    static String messageSansRemise(BigDecimal max2) {
        return "Vous ne pouvez pas appliquer une remise pour cet article (" + pourcent(max2) + ") !";
    }

    /** Remise verrouillée ({@code Sale.cs:Sale_Load}) : article sans premier plafond et appelant sous le niveau 2. */
    public static boolean verrouillee(BigDecimal max1, Niveau niveau) {
        return niveau != Niveau.DEUXIEME && (max1 == null || max1.signum() == 0);
    }

    private static BigDecimal arrondi(BigDecimal v) {
        return (v != null ? v : BigDecimal.ZERO).setScale(DECIMALES, RoundingMode.HALF_UP);
    }

    /** Format {@code "p"} .NET en culture française : {@code 7,41 %}. */
    static String pourcent(BigDecimal ratio) {
        DecimalFormat format = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.FRANCE));
        BigDecimal valeur = (ratio != null ? ratio : BigDecimal.ZERO).multiply(BigDecimal.valueOf(100));
        return format.format(valeur.setScale(2, RoundingMode.HALF_UP)) + " %";
    }
}
