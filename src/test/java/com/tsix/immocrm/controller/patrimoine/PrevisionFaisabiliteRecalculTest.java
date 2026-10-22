package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
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
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.ID;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.fiche;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.jeton;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.ligneUniteCorps;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.lignes;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.yestest;
import static com.tsix.immocrm.fixtures.FixtureFaisabiliteFinance.*;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.decimal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Recalcul unique (FEATURE-patrimoine-faisabilite-workflow.md §3) : lignes recréées par le serveur et PUT des unités,
 * sur une étude sans tranche (la Verif P8a impose qu'une unité ventilée garde sa quantité ventilée).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PrevisionFaisabiliteRecalculTest {

    private static final String PREVISIONS_ETUDE = "SELECT COUNT(*) FROM FSPrevision WHERE FeasibilityStudyId = :id";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureFaisabilite fixture;
    @Autowired private FixtureFaisabiliteFinance finance;

    private ApiFaisabilite api;
    private Categories cat;
    private UUID etude;
    private UUID uniteBase;
    private UUID unitePv;

    @BeforeEach
    void setUp() throws Exception {
        fixture.parametrage();
        api = new ApiFaisabilite(mockMvc, patrimoine.administrateur());
        String jeton = jeton();
        CfgCompany societe = patrimoine.entite("ENT-" + jeton, true);
        cat = finance.categoriesDeTest(jeton);
        etude = api.etude(yestest(fiche("REC-" + jeton, societe.getId())));
        uniteBase = api.unite("PV-" + jeton, false, null);
        api.unites(etude, lignes(ligneUniteCorps(1, uniteBase, d("2970")))).andExpect(status().isOk());
        unitePv = fixture.ligneUnite(etude, uniteBase);
    }

    private String enregistrer() throws Exception {
        return api.remplacer(PREVISIONS, etude, lignes(parUnite(cat.grosOeuvres(), unitePv, "2000", "0.2"),
                        enPourcentage(cat.architecte(), cat.grosOeuvres(), "0.025", "0.2")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    @Test
    void putUnites_quantiteDivisee_bilanRecalcule() throws Exception {
        enregistrer();

        api.unites(etude, lignes(ligneUniteCorps(1, uniteBase, d("1485")).avec(ID, unitePv))).andExpect(status().isOk());

        assertAll(
                () -> assertThat(decimal(finance.prevision(etude, cat.grosOeuvres()).get("AmountHT"))).isEqualByComparingTo("2970000"),
                () -> assertThat(decimal(fixture.etude(etude).get("TotalCostHT"))).isEqualByComparingTo("3044250"));
    }

    /** Le front n'envoie ni les ancêtres ni les lignes verrouillées : le serveur les recrée, il ne les supprime pas. */
    @Test
    void lignesVerrouilleesEtAncetres_absentesDuCorps_recreeesAChaqueEnregistrement() throws Exception {
        UUID terrain = finance.depense("TER-" + UUID.randomUUID().toString().substring(0, 6), RACINE_DEPENSE);
        finance.relier(RELATION_PRIX_TERRAIN, terrain);
        enregistrer();

        String json = enregistrer();

        List<Object> verrou = JsonPath.read(json, "$.lignes[?(@.categorieId == '" + terrain + "')]." + VERROUILLEE);
        assertAll(
                () -> assertThat(verrou).containsExactly(true),
                () -> assertThat(decimal(finance.prevision(etude, terrain).get("AmountHT"))).isEqualByComparingTo("4500000"),
                () -> assertThat(finance.prevision(etude, cat.travaux())).isNotNull(),
                () -> assertThat(finance.prevision(etude, RACINE_DEPENSE)).isNotNull(),
                () -> assertThat(fixture.compter(PREVISIONS_ETUDE, etude)).isEqualTo(6));
    }
}
