package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
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
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Refus du PUT liste des onglets hors tests verrouillés (FEATURE-patrimoine-tranche-onglets.md §1) : 400 nommant la ligne. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TrancheOngletsRefusTest {

    private static final String DETAIL = "$.detail";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureTrancheOnglets onglets;

    private AdmUser admin;
    private CfgTranche tranche;
    private String jeton;

    @BeforeEach
    void setUp() {
        admin = fixture.administrateur();
        jeton = "P1BR" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        tranche = fixture.tranche("TR-" + jeton, fixture.projet("PRJ-" + jeton, fixture.entite("ENT-" + jeton, true)));
    }

    private ResultActions envoyer(String onglet, String corps) throws Exception {
        return mockMvc.perform(put("/api/tranches/{id}/{onglet}", tranche.getId(), onglet).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    @Test
    void idDUneLigneQuiNEstPasALaTranche_400() throws Exception {
        envoyer("terrains", lignes(corps().avec("id", UUID.randomUUID()).avec("terrainId", onglets.terrain("TER-" + jeton))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString("lignes[0].id")));
    }

    @Test
    void referenceInconnue_400() throws Exception {
        envoyer("etages", lignes(corps().avec("niveauTerrainId", UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString("lignes[0].niveauTerrainId")));
    }

    @Test
    void tauxTvaAuDelaDe1_400() throws Exception {
        envoyer("frais", lignes(corps().avec("categorieReglementId", onglets.categorieReglement("FR-" + jeton, true, true))
                .avec("montantHT", BigDecimal.TEN).avec("tauxTva", new BigDecimal("1.2"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString("lignes[0].tauxTva")));
    }

    @Test
    void corpsSansLignes_400() throws Exception {
        envoyer("plancher-couvert", "{}").andExpect(status().isBadRequest());
    }
}
