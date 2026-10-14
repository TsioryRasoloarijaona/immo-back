package com.tsix.immocrm.services.dossier.workflow;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
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
