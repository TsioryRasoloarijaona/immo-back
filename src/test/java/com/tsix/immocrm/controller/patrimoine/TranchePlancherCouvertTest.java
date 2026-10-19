package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureTrancheFiche;
import com.tsix.immocrm.fixtures.FixtureTrancheOnglets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.LignesJson.lignes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Onglet Plancher couvert ({@code CfgTrancheCoveredFloor}) — FEATURE-patrimoine-tranche-onglets.md §1, §2.3
 * (TranchePlancherCouvertTest) : Σ {@code Area} réécrit dans {@code CfgTranche.CoveredFloorArea} par le même PUT.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TranchePlancherCouvertTest {

    private static final String PLANCHER = "/api/tranches/{id}/plancher-couvert";
    private static final String CATEGORIE_ID = "categorieId";
    private static final String SURFACE = "surface";
    private static final String LIGNE_DE_LA_CATEGORIE =
            "SELECT COUNT(*) FROM CfgTrancheCoveredFloor WHERE CfgTrancheId = :id AND StkItemCategoryId = :autre";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureTrancheFiche fiche;
    @Autowired private FixtureTrancheOnglets onglets;

    private AdmUser admin;
    private CfgTranche tranche;
    private String jeton;

    @BeforeEach
    void setUp() {
        admin = fixture.administrateur();
        jeton = "P1BP" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        tranche = fixture.tranche("TR-" + jeton, fixture.projet("PRJ-" + jeton, fixture.entite("ENT-" + jeton, true)));
    }

    private UUID categorie(String prefixe) {
        return fixture.categorie(prefixe + "-" + jeton, fixture.natureBat(), false).getId();
    }

    private ResultActions envoyer(String corps) throws Exception {
        return mockMvc.perform(put(PLANCHER, tranche.getId()).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    /** L1 (100) gardée et passée à 80, L2 (50) retirée, une ligne de 20,5 ajoutée : Σ = 100,5. */
    private UUID[] reconcilier() throws Exception {
        UUID c1 = categorie("CP1");
        UUID c2 = categorie("CP2");
        UUID c3 = categorie("CP3");
        UUID l1 = onglets.lignePlancher(tranche.getId(), c1, new BigDecimal("100"));
        UUID l2 = onglets.lignePlancher(tranche.getId(), c2, new BigDecimal("50"));

        envoyer(lignes(
                corps().avec("id", l1).avec(CATEGORIE_ID, c1).avec(SURFACE, new BigDecimal("80")),
                corps().avec(CATEGORIE_ID, c3).avec(SURFACE, new BigDecimal("20.5"))))
                .andExpect(status().isOk());
        return new UUID[] {l1, l2, c3};
    }

    @Test
    void r1_get_lignesDeLaTranche() throws Exception {
        UUID c = categorie("CPG");
        onglets.lignePlancher(tranche.getId(), c, new BigDecimal("42"));

        mockMvc.perform(get(PLANCHER, tranche.getId()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.." + CATEGORIE_ID, hasItem(c.toString())));
    }

    @Test
    void r1_put_idConnuMaj_sansIdCree_absentSupprime() throws Exception {
        UUID[] ids = reconcilier();

        assertAll(
                () -> assertThat(onglets.decimal("SELECT Area FROM CfgTrancheCoveredFloor WHERE PKey = :id", ids[0]))
                        .isEqualByComparingTo("80"),
                () -> assertThat(fiche.compter("SELECT COUNT(*) FROM CfgTrancheCoveredFloor WHERE PKey = :id", ids[1]))
                        .isZero(),
                () -> assertThat(onglets.compter(LIGNE_DE_LA_CATEGORIE, tranche.getId(), ids[2])).isEqualTo(1));
    }

    @Test
    void r2_3_sommeDesSurfaces_ecriteDansCoveredFloorArea() throws Exception {
        reconcilier();

        assertThat(onglets.decimal("SELECT CoveredFloorArea FROM CfgTranche WHERE PKey = :id", tranche.getId()))
                .isEqualByComparingTo("100.5");
    }

    @Test
    void r2_3_ficheTranche_afficheLeNouveauPlancherCouvert() throws Exception {
        reconcilier();

        mockMvc.perform(get("/api/tranches/{id}", tranche.getId()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plancherCouvert", closeTo(100.5, 1e-9)));
    }

    @Test
    void r2_3_doublonDeCategorie_uneSeuleLigneGardee() throws Exception {
        UUID c = categorie("CPX");

        envoyer(lignes(corps().avec(CATEGORIE_ID, c).avec(SURFACE, BigDecimal.TEN),
                corps().avec(CATEGORIE_ID, c).avec(SURFACE, BigDecimal.TEN)))
                .andExpect(status().isOk());

        assertThat(onglets.compter(LIGNE_DE_LA_CATEGORIE, tranche.getId(), c)).isEqualTo(1);
    }

    @Test
    void r2_3_400_surfaceNegative_champIndexe() throws Exception {
        envoyer(lignes(corps().avec(CATEGORIE_ID, categorie("CPA")).avec(SURFACE, BigDecimal.TEN),
                corps().avec(CATEGORIE_ID, categorie("CPB")).avec(SURFACE, new BigDecimal("-1"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("lignes[1].surface")));
    }

    @Test
    void r2_3_400_categorieManquante_champIndexe() throws Exception {
        envoyer(lignes(corps().avec(SURFACE, BigDecimal.TEN)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("lignes[0].categorieId")));
    }
}
