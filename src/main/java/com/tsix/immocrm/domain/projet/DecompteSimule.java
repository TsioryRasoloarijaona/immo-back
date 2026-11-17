package com.tsix.immocrm.domain.projet;

import com.tsix.immocrm.domain.projet.EntreesSimulation.DecompteReel;
import com.tsix.immocrm.domain.projet.EntreesSimulation.RetenueStockee;
import com.tsix.immocrm.domain.projet.EntreesSimulation.RetenuesStockees;
import com.tsix.immocrm.domain.projet.EntreesSimulation.Retenues;

import java.math.BigDecimal;

import static java.math.BigDecimal.ZERO;

/**
 * Suite de décomptes simulés d'un lot (OMRAN {@code Yes.Buy.Tdb/WorkingOut.cs}, FEATURE-lot-previsions.md §3.2 S1-S6) :
 * chaque décompte porte un TTC, son net = net cumulé − net cumulé du précédent − avance sur décompte amortie.
 */
public final class DecompteSimule {

    private final BigDecimal tauxAvance;
    private final BigDecimal plafondAvance;
    private final Retenues retenues;
    private BigDecimal resteAvanceSurDecompte;
    private BigDecimal cumul = ZERO;
    private BigDecimal netAvant = ZERO;
    private RetenuesStockees stockees = RetenuesStockees.AUCUNES;

    /** {@code avanceSurDecompte} : amortir le reste des avances sur décompte (prévisions de paiement seulement). */
    public DecompteSimule(BigDecimal tauxAvance, BigDecimal plafondAvance, Retenues retenues, boolean avanceSurDecompte) {
        this.tauxAvance = tauxAvance;
        this.plafondAvance = plafondAvance;
        this.retenues = retenues;
        this.resteAvanceSurDecompte = avanceSurDecompte ? retenues.avanceSurDecompte().max(ZERO) : ZERO;
    }

    /**
     * S6, C2 : décompte réel ; son net cumulé se calcule avec les taux stockés du dernier réel (montants cumulés si
     * « basé sur le montant ») et sert d'« avant » au premier décompte simulé.
     */
    public void precedent(DecompteReel reel) {
        cumul = cumul.add(reel.ttc());
        stockees = stockees.puis(reel.retenues());
        RetenueStockee a = stockees.avance();
        BigDecimal avance = (a.surMontant() ? a.montant() : a.taux().multiply(cumul)).min(a.plafond().max(ZERO));
        netAvant = cumul.subtract(avance).subtract(stockee(stockees.garantie())).subtract(stockee(stockees.prorata()))
                .subtract(stockee(stockees.dec())).subtract(stockee(stockees.trc()));
    }

    private BigDecimal stockee(RetenueStockee r) {
        return r.surMontant() ? r.montant() : plafonne(r.taux(), r.plafond(), cumul);
    }

    /** S2-S5 : net à payer du décompte simulé de TTC {@code montant}. */
    public BigDecimal net(BigDecimal montant) {
        cumul = cumul.add(montant);
        BigDecimal cumule = netCumule(cumul);
        BigDecimal net = cumule.subtract(netAvant);
        netAvant = cumule;
        if (net.signum() > 0 && resteAvanceSurDecompte.signum() > 0) {
            BigDecimal amortie = resteAvanceSurDecompte.min(net);
            resteAvanceSurDecompte = resteAvanceSurDecompte.subtract(amortie);
            net = net.subtract(amortie);
        }
        return net;
    }

    private BigDecimal netCumule(BigDecimal c) {
        BigDecimal avance = tauxAvance.multiply(c).min(plafondAvance.max(ZERO));
        return c.subtract(avance).subtract(retenues.garantie().multiply(c)).subtract(retenues.prorata().multiply(c))
                .subtract(plafonne(retenues.dec(), retenues.decMontant(), c))
                .subtract(plafonne(retenues.trc(), retenues.trcMontant(), c));
    }

    private static BigDecimal plafonne(BigDecimal ratio, BigDecimal montant, BigDecimal c) {
        BigDecimal retenue = ratio.multiply(c);
        return montant.signum() == 0 ? retenue : retenue.min(montant);
    }
}
