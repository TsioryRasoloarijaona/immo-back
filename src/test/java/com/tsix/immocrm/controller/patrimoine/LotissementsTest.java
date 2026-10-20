package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.FixtureEntites;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureTerrains;
import com.tsix.immocrm.fixtures.FixtureTerrains.Contexte;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureTerrains.LOTISSEMENTS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lotissements — FEATURE-patrimoine-terrains.md §4 et §6.2 (LotissementsTest) : {@code /api/lotissements} liste les
 * terrains rattachés à une tranche dont la vocation n'est pas PROJET ({@code LotLandList.cs:168-189}).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotissementsTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureTerrains terrains;
    @Autowired private FixtureEntites entites;

    private AdmUser admin;
    private Contexte c;
    private CfgTranche tranche;

    @BeforeEach
    void setUp() {
        admin = fixture.administrateur();
        c = terrains.contexte("P7LO");
        tranche = fixture.tranche("TR-" + c.jeton(), fixture.projet("PRJ-" + c.jeton(), c.entite()));
    }

    private static String ligne(UUID id) {
        return "$.content[?(@.id=='" + id + "')]";
    }

    private ResultActions lister() throws Exception {
        return mockMvc.perform(get(LOTISSEMENTS).param("size", "500").with(connecte(admin)))
                .andExpect(status().isOk());
    }

    @Test
    void r4_terrainDeTrancheVocationVente_liste() throws Exception {
        UUID lot = terrains.terrain(c, "LOT-" + c.jeton(), tranche.getId(), FixturePatrimoine.VOCATION_VENTE);

        lister().andExpect(jsonPath(ligne(lot)).isNotEmpty());
    }

    @Test
    void r4_terrainDeTrancheVocationProjet_exclu() throws Exception {
        UUID lot = terrains.terrain(c, "LOT-" + c.jeton(), tranche.getId(), FixturePatrimoine.VOCATION_VENTE);
        UUID projet = terrains.terrain(c, "LOP-" + c.jeton(), tranche.getId(), FixturePatrimoine.VOCATION_PROJET);

        lister().andExpect(jsonPath(ligne(lot)).isNotEmpty()).andExpect(jsonPath(ligne(projet)).isEmpty());
    }

    @Test
    void r4_terrainSansTranche_exclu() throws Exception {
        UUID lot = terrains.terrain(c, "LOT-" + c.jeton(), tranche.getId(), FixturePatrimoine.VOCATION_VENTE);
        UUID libre = terrains.terrain(c, "LOS-" + c.jeton(), null, FixturePatrimoine.VOCATION_VENTE);

        lister().andExpect(jsonPath(ligne(lot)).isNotEmpty()).andExpect(jsonPath(ligne(libre)).isEmpty());
    }

    @Test
    void r4_articleBatiDeTranche_exclu() throws Exception {
        UUID lot = terrains.terrain(c, "LOT-" + c.jeton(), tranche.getId(), FixturePatrimoine.VOCATION_VENTE);
        UUID bati = terrains.terrain(c, "LOB-" + c.jeton(), tranche.getId(), FixturePatrimoine.VOCATION_VENTE);
        terrains.definir(bati, "StkKindId", fixture.natureBat().getId());

        lister().andExpect(jsonPath(ligne(lot)).isNotEmpty()).andExpect(jsonPath(ligne(bati)).isEmpty());
    }

    /** §4 : visible = TrancheVisibleRepository.trancheVisible — tranche d'une société inactive exclue. */
    @Test
    void r4_trancheDUneSocieteInactive_exclu() throws Exception {
        UUID lot = terrains.terrain(c, "LOT-" + c.jeton(), tranche.getId(), FixturePatrimoine.VOCATION_VENTE);
        lister().andExpect(jsonPath(ligne(lot)).isNotEmpty());
        entites.actif(c.entite().getId(), false);

        lister().andExpect(jsonPath(ligne(lot)).isEmpty());
    }
}
