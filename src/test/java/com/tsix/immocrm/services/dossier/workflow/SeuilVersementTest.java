package com.tsix.immocrm.services.dossier.workflow;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Règles pures de {@code Sale.cs:VerifThreshold} (FEATURE-vente-workflow.md §5). */
class SeuilVersementTest {

    private static final BigDecimal PRIX = new BigDecimal("900000");
    private static final SeuilVersement.Regles SANS_REGLE = new SeuilVersement.Regles(false, false, false, null);

    private static SituationFinanciere verse(BigDecimal avanceReglee, BigDecimal credit) {
        return new SituationFinanciere(PRIX, credit, BigDecimal.ZERO, avanceReglee, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, List.of());
    }

    private static final LocalDateTime REMISE_PREVUE = LocalDateTime.of(2027, 6, 30, 0, 0);
    private static final SeuilVersement.DelaiMax DELAI_90 = new SeuilVersement.DelaiMax(true, 90);

    @Test
    void delaiMax_derniereEcheanceApresRemisePlusDelai_refuse() {
        assertThat(SeuilVersement.delaiMax(DELAI_90, REMISE_PREVUE.plusDays(91), REMISE_PREVUE, null))
                .contains("Vous avez dépassé le délai max de paiement !");
    }

    @Test
    void delaiMax_derniereEcheanceAuDernierJour_accepte() {
        assertThat(SeuilVersement.delaiMax(DELAI_90, REMISE_PREVUE.plusDays(90), REMISE_PREVUE, null)).isEmpty();
    }

    @Test
    void delaiMax_optionDesactivee_aucunControle() {
        SeuilVersement.DelaiMax sansOption = new SeuilVersement.DelaiMax(false, 90);

        assertThat(SeuilVersement.delaiMax(sansOption, REMISE_PREVUE.plusYears(5), REMISE_PREVUE, null)).isEmpty();
    }

    @Test
    void delaiMax_sansRemisePrevue_comparerALaRemiseEffective() {
        assertThat(SeuilVersement.delaiMax(DELAI_90, REMISE_PREVUE.plusDays(91), null, REMISE_PREVUE))
                .contains("Vous avez dépassé le délai max de paiement");
    }

    @Test
    void delaiMax_aucuneDateDeRemise_aucunControle() {
        assertThat(SeuilVersement.delaiMax(DELAI_90, REMISE_PREVUE, null, null)).isEmpty();
    }

    @Test
    void creditMax_creditAuDelaDuPourcentage_refuse() {
        SeuilVersement.Regles max70 = new SeuilVersement.Regles(false, false, true, new BigDecimal("0.7"));

        assertThat(SeuilVersement.creditMax(max70, new BigDecimal("700000"), PRIX))
                .contains("Vous avez dépassez le montant max du crédit !");
    }

    @Test
    void strictement_egalAuSeuil_refuse() {
        SeuilVersement.Configuration config = new SeuilVersement.Configuration(
                SeuilVersement.STRICTEMENT, new BigDecimal("0.125"), SeuilVersement.TOUJOURS);

        assertThat(SeuilVersement.controler(config, SANS_REGLE, verse(new BigDecimal("112500"), BigDecimal.ZERO),
                PRIX, PRIX))
                .hasValueSatisfying(m -> assertThat(m).contains("strictement supérieur à 12,50 %"));
    }

    @Test
    void sansOperateur_aucunControle() {
        SeuilVersement.Configuration config = new SeuilVersement.Configuration(
                SeuilVersement.SANS_SEUIL, BigDecimal.ONE, SeuilVersement.TOUJOURS);

        assertThat(SeuilVersement.controler(config, SANS_REGLE, verse(BigDecimal.ZERO, BigDecimal.ZERO), PRIX, PRIX))
                .isEmpty();
    }

    @Test
    void venteACreditSansCredit_refuse() {
        SeuilVersement.Configuration config = new SeuilVersement.Configuration(
                SeuilVersement.AU_MOINS, BigDecimal.ZERO, SeuilVersement.TOUJOURS);
        SeuilVersement.Regles credit = new SeuilVersement.Regles(false, true, false, null);

        assertThat(SeuilVersement.controler(config, credit, verse(BigDecimal.ZERO, BigDecimal.ZERO), PRIX, PRIX))
                .contains("Il faut que le montant de crédit soit strictement supérieur à 0");
    }

    @Test
    void auComptant_avecCredit_neControlePasLeSeuil() {
        SeuilVersement.Configuration config = new SeuilVersement.Configuration(
                SeuilVersement.AU_MOINS, BigDecimal.ONE, SeuilVersement.AU_COMPTANT);

        assertThat(SeuilVersement.controler(config, SANS_REGLE, verse(BigDecimal.ZERO, new BigDecimal("100000")),
                PRIX, PRIX)).isEmpty();
    }

    @Test
    void creditAuDelaDuPlafondDeLaTranche_refuse() {
        SeuilVersement.Regles plafond = new SeuilVersement.Regles(false, false, true, new BigDecimal("0.7"));

        assertThat(SeuilVersement.controler(null, plafond, verse(BigDecimal.ZERO, new BigDecimal("700000.01")),
                PRIX, PRIX)).contains("Vous avez dépassez le montant max du crédit !");
    }
}
