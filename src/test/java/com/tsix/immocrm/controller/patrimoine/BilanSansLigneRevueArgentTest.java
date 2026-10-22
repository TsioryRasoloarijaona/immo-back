package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.fixtures.ApiFaisabilite;
import com.tsix.immocrm.fixtures.FixtureFaisabilite;
import com.tsix.immocrm.fixtures.FixtureFaisabiliteFinance;
import com.tsix.immocrm.fixtures.FixtureFaisabiliteFinance.Categories;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ApiFaisabilite.d;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.*;
import static com.tsix.immocrm.fixtures.FixtureFaisabiliteFinance.*;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.decimal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Seconde revue argent P8b-1 : le recalcul garde le bilan stocké quand l'arbre est vide (RecalculFaisabiliteService.ecrire).
 * Une étude qui AVAIT des prévisions puis les perd, ou qui n'a qu'un crédit, doit avoir ses colonnes de bilan recalculées.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BilanSansLigneRevueArgentTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureFaisabilite fixture;
    @Autowired private FixtureFaisabiliteFinance finance;

    private ApiFaisabilite api;
    private Categories cat;
    private UUID etude;
    private UUID unitePv;

    @BeforeEach
    void setUp() throws Exception {
        fixture.parametrage();
        api = new ApiFaisabilite(mockMvc, patrimoine.administrateur());
        String jeton = jeton();
        CfgCompany societe = patrimoine.entite("ENT-" + jeton, true);
        cat = finance.categoriesDeTest(jeton);
        etude = api.etude(yestest(fiche("BSL-" + jeton, societe.getId())));
        UUID uniteBase = api.unite("PV-" + jeton, false, null);
        api.unites(etude, lignes(ligneUniteCorps(1, uniteBase, d("2970")))).andExpect(status().isOk());
        unitePv = fixture.ligneUnite(etude, uniteBase);
    }

    /** Toutes les lignes retirées : coût global et marges doivent retomber à 0, comme les totaux de l'arbre. */
    @Test
    void toutesLesPrevisionsRetirees_bilanStockeRemisAZero() throws Exception {
        api.remplacer(PREVISIONS, etude, lignes(parUnite(cat.grosOeuvres(), unitePv, "2000", "0.2"))).andExpect(status().isOk());
        assertThat(decimal(fixture.etude(etude).get("TotalCostHT"))).isEqualByComparingTo("5940000");

        api.remplacer(PREVISIONS, etude, lignes(new String[0])).andExpect(status().isOk());

        assertAll(
                () -> assertThat(fixture.compter("SELECT COUNT(*) FROM FSPrevision WHERE FeasibilityStudyId = :id", etude)).isZero(),
                () -> assertThat(decimal(fixture.etude(etude).get("TotalCostHT"))).as("TotalCostHT").isEqualByComparingTo("0"),
                () -> assertThat(decimal(fixture.etude(etude).get("TotalCost"))).as("TotalCost").isEqualByComparingTo("0"),
                () -> assertThat(decimal(fixture.etude(etude).get("GrossProfitMargin"))).as("GrossProfitMargin")
                        .isEqualByComparingTo("0"));
    }

    /** Crédit seul (aucune prévision) : coût du financement = Σ intérêts, fonds propres = −crédit (§2.2). */
    @Test
    void creditSansPrevision_coutFinancementEtAutofinancementEcrits() throws Exception {
        api.remplacer(FINANCEMENTS, etude, lignes(credit("2026-01-15", "1000000", 1, 1, 0, "0.1")
                .avec(REMBOURSEMENTS, List.of(remboursement("2026-02-15", "1000000", "0.1"))))).andExpect(status().isOk());

        assertAll(
                () -> assertThat(decimal(fixture.etude(etude).get("TotalCostFunding"))).as("TotalCostFunding")
                        .isEqualByComparingTo("100000"),
                () -> assertThat(decimal(fixture.etude(etude).get("AutofinancementAmount"))).as("AutofinancementAmount")
                        .isEqualByComparingTo("-1000000"));
    }
}
