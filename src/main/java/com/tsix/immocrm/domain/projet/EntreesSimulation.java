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

    /** Décompte facturé (multi-fournisseur : facture) : date, avancement cumulé, TTC. */
    public record DecompteReel(LocalDate date, BigDecimal avancement, BigDecimal ttc) {
    }

    /** Avenant non facturé ou bon de commande à payer, repris tel quel. */
    public record LigneFixe(LocalDate date, String libelle, BigDecimal montant, BigDecimal ht, BigDecimal ttc) {
    }
}
