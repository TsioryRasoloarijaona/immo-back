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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Onglet Terrains ({@code CfgTrancheStkItem}) — FEATURE-patrimoine-tranche-onglets.md §1, §2.1, §8.2
 * (TrancheTerrainsTest). Le format de la réponse (tableau ou {@code {lignes}}) n'est pas fixé : lecture en base ou
 * par recherche profonde {@code $..}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TrancheTerrainsTest {

    private static final String TERRAINS = "/api/tranches/{id}/terrains";
    private static final String TERRAIN_ID = "terrainId";
    private static final String LIGNES_DE_LA_TRANCHE = "SELECT COUNT(*) FROM CfgTrancheStkItem WHERE CfgTrancheId = :id";
    private static final String LIGNE_DU_TERRAIN =
            "SELECT COUNT(*) FROM CfgTrancheStkItem WHERE CfgTrancheId = :id AND StkItemId = :autre";

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
        jeton = "P1BT" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        tranche = fixture.tranche("TR-" + jeton, fixture.projet("PRJ-" + jeton, fixture.entite("ENT-" + jeton, true)));
    }

    private ResultActions envoyer(String corps) throws Exception {
        return mockMvc.perform(put(TERRAINS, tranche.getId()).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    // ------------------------------------------------------------------ §1 GET

    @Test
    void r1_get_lignesDeLaTranche_avecLeCodeDuTerrain() throws Exception {
        UUID terrain = onglets.terrain("TER-" + jeton);
        onglets.lienTerrain(tranche.getId(), terrain, BigDecimal.ONE, 1);

        mockMvc.perform(get(TERRAINS, tranche.getId()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.." + TERRAIN_ID, hasItem(terrain.toString())))
                .andExpect(content().string(containsString("TER-" + jeton)));
    }

    // ------------------------------------------------------------------ §1 PUT liste : réconciliation

    @Test
    void r1_put_idConnuMaj_sansIdCree_absentSupprime() throws Exception {
        UUID t1 = onglets.terrain("TER1-" + jeton);
        UUID t2 = onglets.terrain("TER2-" + jeton);
        UUID t3 = onglets.terrain("TER3-" + jeton);
        UUID gardee = onglets.lienTerrain(tranche.getId(), t1, BigDecimal.ONE, 1);
        UUID retiree = onglets.lienTerrain(tranche.getId(), t2, BigDecimal.ONE, 2);

        envoyer(lignes(
                corps().avec("id", gardee).avec(TERRAIN_ID, t1).avec("ratio", new BigDecimal("0.4")),
                corps().avec(TERRAIN_ID, t3).avec("ratio", new BigDecimal("0.6"))))
                .andExpect(status().isOk());

        assertAll(
                () -> assertThat(onglets.decimal("SELECT Ratio FROM CfgTrancheStkItem WHERE PKey = :id", gardee))
                        .isEqualByComparingTo("0.4"),
                () -> assertThat(fiche.compter("SELECT COUNT(*) FROM CfgTrancheStkItem WHERE PKey = :id", retiree))
                        .isZero(),
                () -> assertThat(onglets.compter(LIGNE_DU_TERRAIN, tranche.getId(), t3)).isEqualTo(1),
                () -> assertThat(fiche.compter(LIGNES_DE_LA_TRANCHE, tranche.getId())).isEqualTo(2));
    }

    @Test
    void r1_put_reponse200_listeRelue() throws Exception {
        UUID terrain = onglets.terrain("TERR-" + jeton);

        envoyer(lignes(corps().avec(TERRAIN_ID, terrain)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.." + TERRAIN_ID, hasItem(terrain.toString())));
    }

    @Test
    void r2_1_ratioAbsent_vaut1() throws Exception {
        UUID terrain = onglets.terrain("TERD-" + jeton);

        envoyer(lignes(corps().avec(TERRAIN_ID, terrain))).andExpect(status().isOk());

        assertThat(onglets.decimal("SELECT Ratio FROM CfgTrancheStkItem WHERE CfgTrancheId = :id", tranche.getId()))
                .isEqualByComparingTo("1");
    }

    @Test
    void r2_1_doublonDansLaListe_uneSeuleLigneGardee() throws Exception {
        UUID terrain = onglets.terrain("TERX-" + jeton);

        envoyer(lignes(corps().avec(TERRAIN_ID, terrain), corps().avec(TERRAIN_ID, terrain)))
                .andExpect(status().isOk());

        assertThat(onglets.compter(LIGNE_DU_TERRAIN, tranche.getId(), terrain)).isEqualTo(1);
    }

    @Test
    void r1_400_terrainManquant_champIndexe() throws Exception {
        UUID terrain = onglets.terrain("TERV-" + jeton);

        envoyer(lignes(corps().avec(TERRAIN_ID, terrain), corps().avec("ratio", BigDecimal.ONE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("lignes[1].terrainId")));
    }

    // ------------------------------------------------------------------ §2.1 liste de choix

    @Test
    void r2_1_terrainsDisponibles_natureTerrainEtVocationProjetSeulement() throws Exception {
        String terrain = "TEROK-" + jeton;
        String batiment = "BATPRJ-" + jeton;
        String terrainVente = "TERVTE-" + jeton;
        onglets.terrain(terrain);
        onglets.article(batiment, null, fixture.natureBat(), fixture.vocationProjet());
        onglets.article(terrainVente, null, onglets.natureTerrain(), fixture.vocationVente());

        mockMvc.perform(get("/api/tranches/terrains-disponibles").with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(terrain)))
                .andExpect(content().string(not(containsString(batiment))))
                .andExpect(content().string(not(containsString(terrainVente))));
    }
}
