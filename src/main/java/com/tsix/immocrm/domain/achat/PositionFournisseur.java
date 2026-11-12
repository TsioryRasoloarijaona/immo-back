package com.tsix.immocrm.domain.achat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Position d'un fournisseur dans une société, copie de {@code TotalPositionLine.GetList} d'OMRAN : seul calcul des soldes
 * début / net à payer / réglé / solde fin (règle 6) — voir FEATURE-achat-impayes-soldes.md §3.2.
 */
public record PositionFournisseur(BigDecimal soldeDebut, BigDecimal netAPayer, BigDecimal regle, BigDecimal soldeFin) {

    private static final BigDecimal SEUIL_RESTE_ZERO = BigDecimal.ONE;

    public static PositionFournisseur de(Collection<MouvementPosition> mouvements, LocalDate du, LocalDate au) {
        Predicate<MouvementPosition> periode = m -> dansPeriode(m.date(), du, au);
        // Comme OMRAN : sans début de période, aucun solde de début ; sans fin, la fin couvre tout l'historique.
        BigDecimal debut = du == null ? BigDecimal.ZERO
                : solde(mouvements, m -> m.date() != null && m.date().isBefore(du));
        BigDecimal fin = solde(mouvements, m -> au == null || (m.date() != null && !m.date().isAfter(au)));
        return new PositionFournisseur(debut, somme(mouvements, periode.and(MouvementPosition::credit)),
                somme(mouvements, periode.and(m -> !m.credit())), fin);
    }

    /** Somme des lignes, champ à champ. */
    public static PositionFournisseur total(Collection<PositionFournisseur> positions) {
        return new PositionFournisseur(cumul(positions, PositionFournisseur::soldeDebut),
                cumul(positions, PositionFournisseur::netAPayer), cumul(positions, PositionFournisseur::regle),
                cumul(positions, PositionFournisseur::soldeFin));
    }

    /** Date dans {@code du..au} inclus, borne absente = ouverte ; une date nulle n'entre que sans aucune borne. */
    public static boolean dansPeriode(LocalDate date, LocalDate du, LocalDate au) {
        if (date == null) {
            return du == null && au == null;
        }
        return (du == null || !date.isBefore(du)) && (au == null || !date.isAfter(au));
    }

    /** « Masquer les fournisseurs avec reste à zéro » : solde fin entre −1 et 1 inclus. */
    public boolean resteZero() {
        return soldeFin.abs().compareTo(SEUIL_RESTE_ZERO) <= 0;
    }

    private static BigDecimal solde(Collection<MouvementPosition> mouvements, Predicate<MouvementPosition> retenu) {
        return somme(mouvements, retenu.and(MouvementPosition::credit))
                .subtract(somme(mouvements, retenu.and(m -> !m.credit())));
    }

    private static BigDecimal somme(Collection<MouvementPosition> mouvements, Predicate<MouvementPosition> retenu) {
        return mouvements.stream().filter(retenu).map(MouvementPosition::montant)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(3, RoundingMode.HALF_UP);
    }

    private static BigDecimal cumul(Collection<PositionFournisseur> positions, Function<PositionFournisseur, BigDecimal> champ) {
        return positions.stream().map(champ).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
