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
     * {@code Total} et {@code TotalSettled} de la vue (« Net à payer » et « Net à payer réglé » de la liste OMRAN),
     * {@code reste} = {@code TotalRest}. Même formule que le trop-perçu, qui n'en est que le reste négatif.
     */
    public record Bilan(BigDecimal total, BigDecimal totalRegle) {
        public BigDecimal reste() {
            return total.subtract(totalRegle);
        }
    }

    /**
     * @param prix          {@code ComFolder.Price}
     * @param modifications {@code ComFolder.ModificationsAmount}
     * @param sorti         statut d'annulation ou de résiliation ({@code IsCancellation} de la vue)
     */
    public static Bilan bilan(List<Ligne> lignes, BigDecimal prix, BigDecimal modifications, boolean sorti) {
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
        return new Bilan(total, regleHorsAnnulation.subtract(rembourse(lignes, Ligne::resiliation)));
    }

    /** Paramètres : ceux de {@link #bilan}. */
    public static Resultat calculer(List<Ligne> lignes, BigDecimal prix, BigDecimal modifications, boolean sorti) {
        BigDecimal reste = bilan(lignes, prix, modifications, sorti).reste();
        if (reste.signum() >= 0) {
            return Resultat.AUCUN;
        }
        // TropPercuRest = TropPercu − TropPercuSettled (remboursé, positif) ; voir FEATURE-dossier-durcissement.md §2.
        // La résiliation remboursée est déjà ôtée du réglé (bilan) : ne pas la retrancher une seconde fois.
        BigDecimal tropPercu = reste.negate();
        BigDecimal tropPercuReste = tropPercu
                .subtract(rembourse(lignes, l -> l.auDebit() && !l.resiliation()))
                .max(BigDecimal.ZERO);
        return new Resultat(tropPercu, tropPercuReste);
    }

    /**
     * Montant décaissé vers le client sur les échéances retenues : −Σ réglé signé (un décaissement est négatif),
     * quel que soit le sens de la catégorie, pour qu'un remboursement n'augmente jamais le trop-perçu.
     */
    private static BigDecimal rembourse(List<Ligne> lignes, Predicate<Ligne> filtre) {
        return regle(lignes, filtre).negate();
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
