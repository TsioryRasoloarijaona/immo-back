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
 * terrains rattachés à une tranche dont la vocation n'est pas PROJET ({@code LotLandList.cs:168-189}). Depuis
 * FEATURE-patrimoine-lotissements.md §1.1, un lot porte aussi l'entité « article » (StrEntity StkItem).
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
        fixture.entiteStkItem();
        tranche = fixture.tranche("TR-" + c.jeton(), fixture.projet("PRJ-" + c.jeton(), c.entite()));
    }

    private static String ligne(UUID id) {
        return "$.content[?(@.id=='" + id + "')]";
    }

    private ResultActions lister() throws Exception {
        return mockMvc.perform(get(LOTISSEMENTS).param("size", "500").with(connecte(admin)))
                .andExpect(status().isOk());
    }

    /** Terrain de tranche portant l'entité « article » : un lot au sens de §1.1. */
    private UUID lot(String code, UUID vocationId) {
        UUID id = terrains.terrain(c, code, tranche.getId(), vocationId);
        terrains.definir(id, "StrEntityId", FixturePatrimoine.ENTITE_STK_ITEM);
        return id;
    }

    @Test
    void r4_terrainDeTrancheVocationVente_liste() throws Exception {
        UUID lot = lot("LOT-" + c.jeton(), FixturePatrimoine.VOCATION_VENTE);

        lister().andExpect(jsonPath(ligne(lot)).isNotEmpty());
    }

    @Test
    void r4_terrainDeTrancheVocationProjet_exclu() throws Exception {
        UUID lot = lot("LOT-" + c.jeton(), FixturePatrimoine.VOCATION_VENTE);
        UUID projet = lot("LOP-" + c.jeton(), FixturePatrimoine.VOCATION_PROJET);

        lister().andExpect(jsonPath(ligne(lot)).isNotEmpty()).andExpect(jsonPath(ligne(projet)).isEmpty());
    }

    @Test
    void r4_terrainSansTranche_exclu() throws Exception {
        UUID lot = lot("LOT-" + c.jeton(), FixturePatrimoine.VOCATION_VENTE);
        UUID libre = terrains.terrain(c, "LOS-" + c.jeton(), null, FixturePatrimoine.VOCATION_VENTE);

        lister().andExpect(jsonPath(ligne(lot)).isNotEmpty()).andExpect(jsonPath(ligne(libre)).isEmpty());
    }

    @Test
    void r4_articleBatiDeTranche_exclu() throws Exception {
        UUID lot = lot("LOT-" + c.jeton(), FixturePatrimoine.VOCATION_VENTE);
        UUID bati = lot("LOB-" + c.jeton(), FixturePatrimoine.VOCATION_VENTE);
        terrains.definir(bati, "StkKindId", fixture.natureBat().getId());

        lister().andExpect(jsonPath(ligne(lot)).isNotEmpty()).andExpect(jsonPath(ligne(bati)).isEmpty());
    }

    /** §4 : visible = TrancheVisibleRepository.trancheVisible — tranche d'une société inactive exclue. */
    @Test
    void r4_trancheDUneSocieteInactive_exclu() throws Exception {
        UUID lot = lot("LOT-" + c.jeton(), FixturePatrimoine.VOCATION_VENTE);
        lister().andExpect(jsonPath(ligne(lot)).isNotEmpty());
        entites.actif(c.entite().getId(), false);

        lister().andExpect(jsonPath(ligne(lot)).isEmpty());
    }

    /** FEATURE-patrimoine-lotissements §1.1 : un terrain LandProject rattaché à une tranche ne devient pas lot. */
    @Test
    void l1_1_terrainLandProjectDeTranche_exclu() throws Exception {
        UUID lot = lot("LOT-" + c.jeton(), FixturePatrimoine.VOCATION_VENTE);
        UUID landProject = terrains.terrain(c, "LLP-" + c.jeton(), tranche.getId(), FixturePatrimoine.VOCATION_VENTE);

        lister().andExpect(jsonPath(ligne(lot)).isNotEmpty()).andExpect(jsonPath(ligne(landProject)).isEmpty());
    }

    /** Réponse 1 du manager (27/10) : filtre {@code trancheId} sur la liste des lotissements. */
    @Test
    void reponse1_filtreTrancheId_neRendQueLesLotsDeLaTranche() throws Exception {
        UUID ici = lot("LTI-" + c.jeton(), FixturePatrimoine.VOCATION_VENTE);
        CfgTranche autre = fixture.tranche("TR2-" + c.jeton(), fixture.projet("PRJ2-" + c.jeton(), c.entite()));
        UUID ailleurs = terrains.terrain(c, "LTA-" + c.jeton(), autre.getId(), FixturePatrimoine.VOCATION_VENTE);
        terrains.definir(ailleurs, "StrEntityId", FixturePatrimoine.ENTITE_STK_ITEM);

        mockMvc.perform(get(LOTISSEMENTS).param("trancheId", tranche.getId().toString()).param("size", "500")
                        .with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(ici)).isNotEmpty())
                .andExpect(jsonPath(ligne(ailleurs)).isEmpty());
    }
}
