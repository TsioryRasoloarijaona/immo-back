package com.tsix.immocrm.domain.projet;

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

    /** {@code avanceSurDecompte} : amortir le reste des avances sur décompte (prévisions de paiement seulement). */
    public DecompteSimule(BigDecimal tauxAvance, BigDecimal plafondAvance, Retenues retenues, boolean avanceSurDecompte) {
        this.tauxAvance = tauxAvance;
        this.plafondAvance = plafondAvance;
        this.retenues = retenues;
        this.resteAvanceSurDecompte = avanceSurDecompte ? retenues.avanceSurDecompte().max(ZERO) : ZERO;
    }

    /** S6 : décompte réel, montants stockés ; il ne compte que dans les cumuls. */
    public void precedent(BigDecimal ttc) {
        cumul = cumul.add(ttc);
    }

    /** S2-S5 : net à payer du décompte simulé de TTC {@code montant}. */
    public BigDecimal net(BigDecimal montant) {
        BigDecimal avant = netCumule(cumul);
        cumul = cumul.add(montant);
        BigDecimal net = netCumule(cumul).subtract(avant);
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
        return montant.signum() != 0 ? retenue.min(montant) : retenue;
    }
}
