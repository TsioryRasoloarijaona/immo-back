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

import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.LignesJson.lignes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Onglet Étages ({@code CfgTrancheLevel.LndLevelId}) — FEATURE-patrimoine-tranche-onglets.md §1, §2.2 (TrancheEtagesTest). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TrancheEtagesTest {

    private static final String ETAGES = "/api/tranches/{id}/etages";
    private static final String NIVEAU_ID = "niveauTerrainId";
    private static final String LIGNE_DU_NIVEAU =
            "SELECT COUNT(*) FROM CfgTrancheLevel WHERE CfgTrancheId = :id AND LndLevelId = :autre";

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
        jeton = "P1BE" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        tranche = fixture.tranche("TR-" + jeton, fixture.projet("PRJ-" + jeton, fixture.entite("ENT-" + jeton, true)));
    }

    private ResultActions envoyer(String corps) throws Exception {
        return mockMvc.perform(put(ETAGES, tranche.getId()).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    @Test
    void r1_get_lignesDeLaTranche() throws Exception {
        UUID niveau = fiche.niveauTerrain("NIV-" + jeton);
        onglets.ligneEtage(tranche.getId(), niveau);

        mockMvc.perform(get(ETAGES, tranche.getId()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.." + NIVEAU_ID, hasItem(niveau.toString())));
    }

    @Test
    void r1_put_idConnuMaj_sansIdCree_absentSupprime() throws Exception {
        UUID n1 = fiche.niveauTerrain("NIV1-" + jeton);
        UUID n2 = fiche.niveauTerrain("NIV2-" + jeton);
        UUID n3 = fiche.niveauTerrain("NIV3-" + jeton);
        UUID n4 = fiche.niveauTerrain("NIV4-" + jeton);
        UUID modifiee = onglets.ligneEtage(tranche.getId(), n1);
        UUID retiree = onglets.ligneEtage(tranche.getId(), n2);

        envoyer(lignes(corps().avec("id", modifiee).avec(NIVEAU_ID, n4), corps().avec(NIVEAU_ID, n3)))
                .andExpect(status().isOk());

        assertAll(
                () -> assertThat(fiche.compter("SELECT COUNT(*) FROM CfgTrancheLevel WHERE PKey = :id", modifiee))
                        .isEqualTo(1),
                () -> assertThat(onglets.compter("SELECT COUNT(*) FROM CfgTrancheLevel WHERE PKey = :id AND LndLevelId = :autre",
                        modifiee, n4)).isEqualTo(1),
                () -> assertThat(fiche.compter("SELECT COUNT(*) FROM CfgTrancheLevel WHERE PKey = :id", retiree)).isZero(),
                () -> assertThat(onglets.compter(LIGNE_DU_NIVEAU, tranche.getId(), n3)).isEqualTo(1),
                () -> assertThat(fiche.compter("SELECT COUNT(*) FROM CfgTrancheLevel WHERE CfgTrancheId = :id",
                        tranche.getId())).isEqualTo(2));
    }

    @Test
    void r2_2_doublonDansLaListe_uneSeuleLigneGardee() throws Exception {
        UUID niveau = fiche.niveauTerrain("NIVX-" + jeton);

        envoyer(lignes(corps().avec(NIVEAU_ID, niveau), corps().avec(NIVEAU_ID, niveau))).andExpect(status().isOk());

        assertThat(onglets.compter(LIGNE_DU_NIVEAU, tranche.getId(), niveau)).isEqualTo(1);
    }

    @Test
    void r1_400_niveauManquant_champIndexe() throws Exception {
        UUID niveau = fiche.niveauTerrain("NIVV-" + jeton);

        envoyer(lignes(corps().avec(NIVEAU_ID, niveau), corps().avec(NIVEAU_ID, null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("lignes[1].niveauTerrainId")));
    }
}
