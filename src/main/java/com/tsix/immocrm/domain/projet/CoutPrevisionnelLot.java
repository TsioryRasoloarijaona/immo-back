package com.tsix.immocrm.domain.projet;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Seul calcul des montants de la liste des lots, repris de {@code PrjMarketMonitoringView} (branche « lot non terminé ») :
 * coût HT / TTC, net à payer, réglé, reste et avancement. Voir FEATURE-lots-liste-parite.md R1, R2.
 */
public record CoutPrevisionnelLot(BigDecimal ht, BigDecimal ttc, BigDecimal netAPayer, BigDecimal regle, BigDecimal reste,
                                  BigDecimal avancement) {

    public static final CoutPrevisionnelLot ZERO = calculer(Composantes.vides(false));

    /** Paire HT / TTC ; nul = 0. */
    public record Paire(BigDecimal ht, BigDecimal ttc) {
        public static final Paire NULLE = new Paire(BigDecimal.ZERO, BigDecimal.ZERO);

        public Paire {
            ht = Objects.requireNonNullElse(ht, BigDecimal.ZERO);
            ttc = Objects.requireNonNullElse(ttc, BigDecimal.ZERO);
        }

        Paire plus(Paire p) {
            return new Paire(ht.add(p.ht), ttc.add(p.ttc));
        }
    }

    /** Retenues non remboursables du lot ({@code PrjMarket.ProportionRatio}, {@code Insurance*}) ; nul = 0. */
    public record Taux(BigDecimal prorata, BigDecimal decRatio, BigDecimal decMontant, BigDecimal trcRatio,
                       BigDecimal trcMontant) {
        public static final Taux AUCUN = new Taux(null, null, null, null, null);

        /** {@code EstimatedNRRAmount} / {@code OfferedNRRAmount} de la vue, sur le TTC donné. */
        BigDecimal nonRemboursables(BigDecimal ttc) {
            if (ttc.signum() == 0) {
                return BigDecimal.ZERO;
            }
            return z(prorata).multiply(ttc).add(assurance(decMontant, decRatio, ttc)).add(assurance(trcMontant, trcRatio, ttc));
        }

        private static BigDecimal assurance(BigDecimal montant, BigDecimal ratio, BigDecimal ttc) {
            return z(montant).signum() != 0 ? montant : z(ratio).multiply(ttc);
        }
    }

    /**
     * Entrées lues en base. {@code offert} = bordereau (lot) ou commandes validées (multi-fournisseur) ; {@code factureHT},
     * {@code depassementFactureHT}, {@code supplementairesFactureHT}, {@code avoirsHT} servent à l'avancement.
     */
    public record Composantes(boolean multi, Paire offert, Paire estime, Paire depassement, Paire supplementaires,
                              Paire fluctuations, Taux taux, BigDecimal retenuesDecomptes, BigDecimal regularisation,
                              BigDecimal regle, BigDecimal factureHT, BigDecimal depassementFactureHT,
                              BigDecimal supplementairesFactureHT, BigDecimal avoirsHT) {

        public static Composantes vides(boolean multi) {
            return new Composantes(multi, Paire.NULLE, Paire.NULLE, Paire.NULLE, Paire.NULLE, Paire.NULLE, Taux.AUCUN,
                    null, null, null, null, null, null, null);
        }
    }

    public static CoutPrevisionnelLot calculer(Composantes c) {
        Paire cout = cout(c);
        boolean sansOffre = c.offert().ht().signum() == 0;
        BigDecimal nonRemboursables = sansOffre ? c.taux().nonRemboursables(c.estime().ttc())
                : c.taux().nonRemboursables(c.offert().ttc()).add(z(c.retenuesDecomptes()));
        BigDecimal netAPayer = arrondi(cout.ttc().add(z(c.regularisation())).subtract(nonRemboursables));
        BigDecimal regle = arrondi(z(c.regle()));
        return new CoutPrevisionnelLot(arrondi(cout.ht()), arrondi(cout.ttc()), netAPayer, regle, netAPayer.subtract(regle),
                avancement(c));
    }

    private static Paire cout(Composantes c) {
        Paire realise = c.offert().plus(c.depassement()).plus(c.supplementaires());
        if (c.multi()) {
            return realise.ht().compareTo(c.estime().ht()) >= 0 ? realise : c.estime();
        }
        return c.offert().ht().signum() == 0 ? c.estime() : realise.plus(c.fluctuations());
    }

    private static BigDecimal avancement(Composantes c) {
        BigDecimal marche = c.offert().ht();
        if (marche.signum() == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal realise = z(c.factureHT()).subtract(z(c.depassementFactureHT())).subtract(z(c.supplementairesFactureHT()))
                .subtract(c.multi() ? z(c.avoirsHT()) : BigDecimal.ZERO);
        return realise.divide(marche, MathContext.DECIMAL64).setScale(6, RoundingMode.HALF_UP);
    }

    private static BigDecimal arrondi(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal z(BigDecimal v) {
        return Objects.requireNonNullElse(v, BigDecimal.ZERO);
    }
}
