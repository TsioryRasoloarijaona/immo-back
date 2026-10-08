package com.tsix.immocrm.domain;

import com.tsix.immocrm.entities.cfg.CfgTranche;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Calcul du « Schéma de financement », partagé par {@code DossierService} (création, simulation) et
 * {@code DevisService} (fiche offre) : un même (tranche, prix) ne doit jamais donner deux montants.
 */
public final class SchemaFinancementCalculator {

    /** Avance par défaut (1/3, décision métier) si la tranche n'a pas d'{@code AdvancePercent}. */
    private static final BigDecimal DEFAULT_ADVANCE_PERCENT =
            BigDecimal.ONE.divide(BigDecimal.valueOf(3), 6, RoundingMode.HALF_UP);

    private SchemaFinancementCalculator() {
    }

    public record Ligne(String categorieCode, BigDecimal montant) {
    }

    /**
     * {@code AVANCE} = prix × {@code AdvancePercent} ; {@code AUTO-FINANCEMENT} = prix − avance ;
     * {@code FRAIS SYNDIC} = {@code SyndicAmount} × {@code SyndicPeriod}, omis si l'un manque.
     */
    public static List<Ligne> calculer(CfgTranche tranche, BigDecimal prix) {
        BigDecimal advancePercent = tranche != null && tranche.getAdvancePercent() != null
                ? tranche.getAdvancePercent()
                : DEFAULT_ADVANCE_PERCENT;

        BigDecimal avance = prix.multiply(advancePercent).setScale(2, RoundingMode.HALF_UP);
        BigDecimal autoFinancement = prix.subtract(avance);

        List<Ligne> lignes = new ArrayList<>();
        lignes.add(new Ligne("AUTO-FINANCEMENT", autoFinancement));
        lignes.add(new Ligne("AVANCE", avance));

        if (tranche != null && tranche.getSyndicAmount() != null && tranche.getSyndicPeriod() != null
                && tranche.getSyndicPeriod() > 0) {
            BigDecimal fraisSyndic = tranche.getSyndicAmount()
                    .multiply(BigDecimal.valueOf(tranche.getSyndicPeriod()))
                    .setScale(2, RoundingMode.HALF_UP);
            lignes.add(new Ligne("FRAIS SYNDIC", fraisSyndic));
        }
        return lignes;
    }
}
