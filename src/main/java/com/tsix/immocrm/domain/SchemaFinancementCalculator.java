package com.tsix.immocrm.domain;

import com.tsix.immocrm.entities.cfg.CfgTranche;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure calcul du "Schéma de financement" (AUTO-FINANCEMENT / AVANCE / FRAIS SYNDIC) — confirmé
 * sur données réelles (onglet "Schéma de financement" Omran). Ne fait aucun accès DB ni
 * persistance: partagé tel quel entre {@code DossierService} (création réelle du Dossier +
 * prévisualisation {@code POST /api/dossiers/simulation-financement}) et {@code DevisService}
 * (aperçu sur la Fiche Offre vente, {@code GAP-07}) pour que les trois n'affichent jamais des
 * montants différents pour un même (tranche, prix).
 */
public final class SchemaFinancementCalculator {

    /**
     * Ratio d'avance de secours quand la tranche n'a pas de {@code AdvancePercent} configuré —
     * décision métier ("l'avance c'est 1/3 par défaut"). Les tranches réelles portent leur propre
     * {@code AdvancePercent} (ex. 0.30 confirmé sur les tranches APF/ARS), qui prime toujours.
     */
    private static final BigDecimal DEFAULT_ADVANCE_PERCENT =
            BigDecimal.ONE.divide(BigDecimal.valueOf(3), 6, RoundingMode.HALF_UP);

    private SchemaFinancementCalculator() {
    }

    /** Une ligne calculée — code catégorie + montant, avant toute résolution/persistance DB. */
    public record Ligne(String categorieCode, BigDecimal montant) {
    }

    /**
     * Calcule les lignes du "Schéma de financement" pour un {@code prix} donné:
     * <ul>
     *   <li>{@code AVANCE} = prix × {@code CfgTranche.AdvancePercent} (retombe sur 1/3 si la
     *       tranche n'en a pas)</li>
     *   <li>{@code AUTO-FINANCEMENT} = prix − avance</li>
     *   <li>{@code FRAIS SYNDIC} = {@code CfgTranche.SyndicAmount} × {@code SyndicPeriod}
     *       (confirmé: 700.00 × 24 = 16 800.00 sur un dossier réel) — omis si la tranche n'a pas
     *       les deux configurés</li>
     * </ul>
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
