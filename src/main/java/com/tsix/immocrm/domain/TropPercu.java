package com.tsix.immocrm.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Predicate;

/**
 * Trop-perçu d'un dossier, formule de la vue {@code ComSaleFolderView} (PLAN-vente.md, invariant 5), seule
 * source pour {@code GET .../montants} et la fiche trop-perçu. Montants rendus positifs : la vue les
 * écrit en négatif ; un dossier débiteur n'a ni trop-perçu ni reste à rembourser.
 */
public final class TropPercu {

    private TropPercu() {
    }

    /**
     * Une échéance : {@code montant} brut ({@code ComDocument.Amount}), {@code regle} déjà signé par le sens
     * du règlement, {@code sens} = {@code ComSettlementCategory.IsCredit} (NULL possible), puis ses drapeaux.
     */
    public record Ligne(BigDecimal montant, BigDecimal regle, Boolean sens, boolean frais, boolean avance,
                        boolean autoFinancement, boolean creditBancaire, boolean modification, boolean penalite,
                        boolean resiliation) {

        boolean auCredit() {
            return Boolean.TRUE.equals(sens);
        }

        boolean auDebit() {
            return Boolean.FALSE.equals(sens);
        }
    }

    public record Resultat(BigDecimal tropPercu, BigDecimal tropPercuReste) {
        public static final Resultat AUCUN = new Resultat(BigDecimal.ZERO, BigDecimal.ZERO);
    }

    /**
     * @param prix          {@code ComFolder.Price}
     * @param modifications {@code ComFolder.ModificationsAmount}
     * @param sorti         statut d'annulation ou de résiliation ({@code IsCancellation} de la vue)
     */
    public static Resultat calculer(List<Ligne> lignes, BigDecimal prix, BigDecimal modifications, boolean sorti) {
        BigDecimal regleHorsAnnulation = regle(lignes, Ligne::avance)
                .add(regle(lignes, Ligne::creditBancaire))
                .add(regle(lignes, Ligne::autoFinancement))
                .add(regle(lignes, Ligne::modification))
                .add(regle(lignes, Ligne::frais))
                .add(regle(lignes, Ligne::penalite));
        BigDecimal montantAnnulation = somme(lignes, l -> l.resiliation() && l.auDebit());
        BigDecimal total = sorti
                ? regleHorsAnnulation.subtract(montantAnnulation)
                : nonNul(prix).add(nonNul(modifications))
                        .add(somme(lignes, l -> l.frais() && l.auCredit()))
                        .add(penalites(lignes));
        BigDecimal totalRegle = regleHorsAnnulation.subtract(regle(lignes, Ligne::resiliation));
        BigDecimal reste = total.subtract(totalRegle);
        if (reste.signum() >= 0) {
            return Resultat.AUCUN;
        }
        // TropPercuRest = TropPercu − TropPercuSettled, TropPercuSettled = −Σ réglé des échéances au débit.
        BigDecimal tropPercu = reste.negate();
        BigDecimal tropPercuRegleVue = regle(lignes, Ligne::auDebit).negate();
        return new Resultat(tropPercu, tropPercu.add(tropPercuRegleVue));
    }

    private static BigDecimal penalites(List<Ligne> lignes) {
        return lignes.stream()
                .filter(Ligne::penalite)
                .map(l -> l.auCredit() ? nonNul(l.montant()) : nonNul(l.montant()).negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal somme(List<Ligne> lignes, Predicate<Ligne> filtre) {
        return lignes.stream().filter(filtre).map(l -> nonNul(l.montant())).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal regle(List<Ligne> lignes, Predicate<Ligne> filtre) {
        return lignes.stream().filter(filtre).map(l -> nonNul(l.regle())).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal nonNul(BigDecimal valeur) {
        return valeur != null ? valeur : BigDecimal.ZERO;
    }
}
