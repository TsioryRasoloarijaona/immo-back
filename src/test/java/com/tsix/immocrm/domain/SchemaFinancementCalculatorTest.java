package com.tsix.immocrm.domain;

import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.cfg.CfgTrancheSettlementCategory;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class SchemaFinancementCalculatorTest {

    /**
     * Cas réel yesRemsProd « YES 2022/00200 » : prix 756 000, tranche YES alors configurée avec pré-réservation
     * 5 000, avance au montant 100 000, frais de dossier 7 500, syndic 600 × 12. Lignes en base :
     * PRÉ-RÉSERVATION 5 000, AVANCE 95 000, AUTO-FINANCEMENT 656 000, FRAIS DOSSIER 7 500, FRAIS SYNDIC 7 200.
     */
    @Test
    void casReelYes2022_00200_avanceAuMontant_preReservation_fraisDeDossier() {
        CfgTranche tranche = new CfgTranche();
        tranche.setBeforeReservationAmount(new BigDecimal("5000"));
        tranche.setAdvanceBasedOnAmount(true);
        tranche.setAdvanceAmount(new BigDecimal("100000"));
        tranche.setAdvancePercent(new BigDecimal("0.2"));
        tranche.setDefaultExpenseAmount(new BigDecimal("7500"));
        tranche.setSyndicAmount(new BigDecimal("600"));
        tranche.setSyndicPeriod(12);

        List<SchemaFinancementCalculator.Ligne> lignes =
                SchemaFinancementCalculator.calculer(tranche, new BigDecimal("756000"));

        assertThat(lignes)
                .extracting(SchemaFinancementCalculator.Ligne::categorieCode, l -> l.montant().stripTrailingZeros())
                .containsExactly(
                        tuple("PRÉ-RÉSERVATION", new BigDecimal("5000").stripTrailingZeros()),
                        tuple("AVANCE", new BigDecimal("95000").stripTrailingZeros()),
                        tuple("AUTO-FINANCEMENT", new BigDecimal("656000").stripTrailingZeros()),
                        tuple("FRAIS DOSSIER", new BigDecimal("7500").stripTrailingZeros()),
                        tuple("FRAIS SYNDIC", new BigDecimal("7200").stripTrailingZeros()));
    }

    /** Cas réel « APF » : 30 %, syndic 700 × 24, prix 1 548 000 — inchangé par le lot. */
    @Test
    void casReelApf_avanceAuTaux_sansFraisDeDossier() {
        CfgTranche tranche = new CfgTranche();
        tranche.setAdvancePercent(new BigDecimal("0.300000"));
        tranche.setSyndicAmount(new BigDecimal("700"));
        tranche.setSyndicPeriod(24);
        tranche.setBeforeReservationAmount(BigDecimal.ZERO);
        tranche.setDefaultExpenseAmount(BigDecimal.ZERO);

        assertThat(SchemaFinancementCalculator.calculer(tranche, new BigDecimal("1548000")))
                .extracting(SchemaFinancementCalculator.Ligne::categorieCode, l -> l.montant().stripTrailingZeros())
                .containsExactly(
                        tuple("AVANCE", new BigDecimal("464400").stripTrailingZeros()),
                        tuple("AUTO-FINANCEMENT", new BigDecimal("1083600").stripTrailingZeros()),
                        tuple("FRAIS SYNDIC", new BigDecimal("16800").stripTrailingZeros()));
    }

    /** Taux d'avance à 0 en base (APO, ART…) : OMRAN n'écrit pas de ligne d'avance, tout est autofinancement. */
    @Test
    void avanceNulle_pasDeLigneDAvance() {
        CfgTranche tranche = new CfgTranche();
        tranche.setAdvancePercent(BigDecimal.ZERO);

        assertThat(SchemaFinancementCalculator.calculer(tranche, new BigDecimal("500000")))
                .extracting(SchemaFinancementCalculator.Ligne::categorieCode)
                .containsExactly("AUTO-FINANCEMENT");
    }

    /** Pré-réservation au moins égale à l'avance : pas de ligne d'avance, l'autofinancement s'en déduit. */
    @Test
    void preReservationSuperieureALAvance_pasDAvance() {
        CfgTranche tranche = new CfgTranche();
        tranche.setBeforeReservationAmount(new BigDecimal("50000"));
        tranche.setAdvanceBasedOnAmount(true);
        tranche.setAdvanceAmount(new BigDecimal("40000"));

        assertThat(SchemaFinancementCalculator.calculer(tranche, new BigDecimal("300000")))
                .extracting(SchemaFinancementCalculator.Ligne::categorieCode, l -> l.montant().stripTrailingZeros())
                .containsExactly(
                        tuple("PRÉ-RÉSERVATION", new BigDecimal("50000").stripTrailingZeros()),
                        tuple("AUTO-FINANCEMENT", new BigDecimal("250000").stripTrailingZeros()));
    }

    @Test
    void lignesDeTranche_pourcentageDuPrixOuMontantFixe_categorieDejaPresenteSautee() {
        CfgTranche tranche = new CfgTranche();
        tranche.setAdvancePercent(new BigDecimal("0.3"));
        tranche.setSyndicAmount(new BigDecimal("100"));
        tranche.setSyndicPeriod(12);
        tranche.getCategoriesReglement().add(ligneTranche("FRAIS NOTAIRE", new BigDecimal("0.01"), null));
        tranche.getCategoriesReglement().add(ligneTranche("FRAIS D'ENREGISTREMENT", BigDecimal.ZERO, new BigDecimal("2500")));
        tranche.getCategoriesReglement().add(ligneTranche("FRAIS SYNDIC", BigDecimal.ZERO, new BigDecimal("9999")));

        assertThat(SchemaFinancementCalculator.calculer(tranche, new BigDecimal("800000")))
                .extracting(SchemaFinancementCalculator.Ligne::categorieCode, l -> l.montant().stripTrailingZeros())
                .containsExactly(
                        tuple("AVANCE", new BigDecimal("240000").stripTrailingZeros()),
                        tuple("AUTO-FINANCEMENT", new BigDecimal("560000").stripTrailingZeros()),
                        tuple("FRAIS SYNDIC", new BigDecimal("1200").stripTrailingZeros()),
                        tuple("FRAIS NOTAIRE", new BigDecimal("8000").stripTrailingZeros()),
                        tuple("FRAIS D'ENREGISTREMENT", new BigDecimal("2500").stripTrailingZeros()));
    }

    /** Invariant : avance + pré-réservation + autofinancement = prix, quelle que soit la configuration. */
    @Test
    void lesLignesDuPrixSommentAuPrix() {
        CfgTranche tranche = new CfgTranche();
        tranche.setBeforeReservationAmount(new BigDecimal("4000"));
        tranche.setAdvancePercent(new BigDecimal("0.237"));
        tranche.setDefaultExpenseAmount(new BigDecimal("7500"));

        BigDecimal prix = new BigDecimal("885600");
        BigDecimal somme = SchemaFinancementCalculator.calculer(tranche, prix).stream()
                .filter(l -> !l.categorieCode().startsWith("FRAIS"))
                .map(SchemaFinancementCalculator.Ligne::montant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(somme).isEqualByComparingTo(prix);
    }

    private static CfgTrancheSettlementCategory ligneTranche(String code, BigDecimal pourcentage, BigDecimal montant) {
        ComSettlementCategory categorie = new ComSettlementCategory();
        categorie.setId(UUID.randomUUID());
        categorie.setCode(code);
        CfgTrancheSettlementCategory ligne = new CfgTrancheSettlementCategory();
        ligne.setId(UUID.randomUUID());
        ligne.setComSettlementCategory(categorie);
        ligne.setPercentOf(pourcentage);
        ligne.setAmountTTC(montant);
        return ligne;
    }
}
