package com.tsix.immocrm.domain.projet;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Entrées de {@link SimulationPaiementsLot}, lues par l'appelant (FEATURE-lot-previsions.md §3.1). */
public record EntreesSimulation(
        Etat etat,
        Planning planning,
        Montants montants,
        Retenues retenues,
        List<Point> points,
        List<Point> pointsBase,
        List<Echeance> echeancesRg,
        List<DecompteReel> decomptesReels,
        List<BigDecimal> avances,
        List<LigneFixe> avenants,
        List<LigneFixe> bonsCommande
) {

    /** {@code annule} = annulé ou résilié ; {@code termine} = annulé, résilié ou clos ; {@code gestionnaire} = lot prorata du projet. */
    public record Etat(boolean multi, boolean annule, boolean termine, boolean gestionnaire, boolean rgBloquee) {
    }

    /** {@code fin} = {@code debut} + {@code duree} ; {@code finBase} = {@code EndDateManual}. */
    public record Planning(LocalDate debut, int duree, LocalDate debutBase, int dureeBase, LocalDate finBase,
                           LocalDate signature, int delaiPaiement, BigDecimal moisGarantie) {

        public LocalDate fin() {
            return debut.plusDays(duree);
        }
    }

    /**
     * Grandeurs de {@code PrjMarketMonitoringView} : {@code base}, {@code avanceBase}, {@code rgBase}, {@code prorataBase} =
     * {@code Prevision*} ; {@code montant} = {@code Amount} ; {@code cout*} = {@code Cost*} ; {@code avancement} = {@code Achievement}.
     */
    public record Montants(BigDecimal base, BigDecimal avanceBase, BigDecimal rgBase, BigDecimal prorataBase,
                           BigDecimal montant, BigDecimal coutHT, BigDecimal coutTTC, BigDecimal coutRg,
                           BigDecimal coutRemboursement, BigDecimal rgValidee, BigDecimal prorataRembourse,
                           BigDecimal avancement, BigDecimal pourMulti) {
    }

    /** Taux du lot ; {@code avanceSurDecompte} = reste à amortir des avances sur décompte. */
    public record Retenues(BigDecimal avance, BigDecimal garantie, BigDecimal prorata, BigDecimal dec, BigDecimal decMontant,
                           BigDecimal trc, BigDecimal trcMontant, BigDecimal avanceSurDecompte) {
    }

    /** Point d'avancement ; {@code temps} = fraction de la durée. */
    public record Point(LocalDate date, BigDecimal avancement, BigDecimal temps) {
    }

    /** Échéance de remboursement de la RG : {@code date} nulle = fin + {@code delai} jours. */
    public record Echeance(BigDecimal ratio, LocalDate date, int delai, String libelle) {
    }

    /** Décompte facturé (multi-fournisseur : facture, sans retenues) : date, avancement cumulé, TTC, retenues stockées. */
    public record DecompteReel(LocalDate date, BigDecimal avancement, BigDecimal ttc, RetenuesStockees retenues) {
    }

    /**
     * Retenue stockée d'un décompte réel (FEATURE-lot-previsions-reel.md C1) : taux, plafond (avance : Σ des avances
     * datées au plus tard au décompte ; DEC / TRC : montant maximal, 0 = sans), montant, case « basé sur le montant ».
     */
    public record RetenueStockee(BigDecimal taux, BigDecimal plafond, BigDecimal montant, boolean surMontant) {

        static final RetenueStockee AUCUNE = new RetenueStockee(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false);

        /** Taux et plafond du décompte suivant, montants cumulés. */
        RetenueStockee puis(RetenueStockee suivante) {
            return new RetenueStockee(suivante.taux, suivante.plafond, montant.add(suivante.montant), suivante.surMontant);
        }
    }

    public record RetenuesStockees(RetenueStockee avance, RetenueStockee garantie, RetenueStockee prorata,
                                   RetenueStockee dec, RetenueStockee trc) {

        public static final RetenuesStockees AUCUNES = new RetenuesStockees(RetenueStockee.AUCUNE, RetenueStockee.AUCUNE,
                RetenueStockee.AUCUNE, RetenueStockee.AUCUNE, RetenueStockee.AUCUNE);

        RetenuesStockees puis(RetenuesStockees s) {
            return new RetenuesStockees(avance.puis(s.avance), garantie.puis(s.garantie), prorata.puis(s.prorata),
                    dec.puis(s.dec), trc.puis(s.trc));
        }
    }

    /** Avenant non facturé ou bon de commande à payer, repris tel quel. */
    public record LigneFixe(LocalDate date, String libelle, BigDecimal montant, BigDecimal ht, BigDecimal ttc) {
    }
}
