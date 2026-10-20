package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureEntites;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureTerrains;
import com.tsix.immocrm.fixtures.FixtureTerrains.Contexte;
import com.tsix.immocrm.fixtures.FixtureTerrains.TypeFrais;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureTerrains.TERRAIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Suppression d'un terrain — FEATURE-patrimoine-terrains.md §1.2 « Suppression », décision 8 bis n° 2, §6.2
 * (SuppressionTerrainTest) : seul un frais validé bloque (409, compte explicite, yesRemsTest n'a pas de FK) ; les frais
 * non validés partent avec le terrain ; terrain occupé → 409.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SuppressionTerrainTest {

    private static final String TERRAIN_PAR_ID = "SELECT COUNT(*) FROM StkItem WHERE PKey = :id";
    private static final String FRAIS_DU_TERRAIN = "SELECT COUNT(*) FROM StkExpense WHERE StkItemId = :id";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureTerrains terrains;
    @Autowired private FixtureEntites entites;

    private AdmUser admin;
    private Contexte c;
    private TypeFrais type;
    private UUID fournisseur;

    @BeforeEach
    void setUp() {
        admin = fixture.administrateur();
        c = terrains.contexte("P7S");
        type = terrains.typeFrais(c.jeton(), true, true);
        fournisseur = terrains.fournisseur("FRN-" + c.jeton(), null);
    }

    private ResultActions supprimer(UUID terrain) throws Exception {
        return mockMvc.perform(delete(TERRAIN, terrain).with(connecte(admin)));
    }

    /** Frais validé tel qu'OMRAN le laisse : IsValidated = 1 et sa facture BuyDocument. */
    @Test
    void r1_2_fraisValide_409_terrainEtFraisIntacts() throws Exception {
        UUID terrain = terrains.terrain(c, "SFV-" + c.jeton());
        UUID valide = terrains.frais(terrain, type.id(), fournisseur, new BigDecimal("8000"), true);
        terrains.factureExistante(valide, "21/" + c.jeton());
        terrains.frais(terrain, type.id(), fournisseur, new BigDecimal("500"), false);

        supprimer(terrain).andExpect(status().isConflict());

        assertAll(
                () -> assertThat(terrains.compter(TERRAIN_PAR_ID, terrain)).isEqualTo(1),
                () -> assertThat(terrains.compter("SELECT COUNT(*) FROM StkExpense WHERE Pkey = :id", valide))
                        .isEqualTo(1),
                () -> assertThat(terrains.compter(FRAIS_DU_TERRAIN, terrain)).isEqualTo(2));
    }

    /** §1.2 : le contrôle est un compte explicite sur BuyDocument.StkExpenseId (facture orpheline OMRAN comprise). */
    @Test
    void r1_2_factureSurUnFraisNonValide_409() throws Exception {
        UUID terrain = terrains.terrain(c, "SFO-" + c.jeton());
        UUID orphelin = terrains.frais(terrain, type.id(), fournisseur, new BigDecimal("8000"), false);
        terrains.factureExistante(orphelin, "21/" + c.jeton());

        supprimer(terrain).andExpect(status().isConflict());

        assertAll(
                () -> assertThat(terrains.compter(TERRAIN_PAR_ID, terrain)).isEqualTo(1),
                () -> assertThat(terrains.compter(FRAIS_DU_TERRAIN, terrain)).isEqualTo(1));
    }

    /** Décision 8 bis n° 2 : les frais non validés partent avec le terrain (CASCADE OMRAN, codée pour yesRemsTest). */
    @Test
    void r1_2_fraisNonValidesSeulement_204_terrainEtFraisSupprimes() throws Exception {
        UUID terrain = terrains.terrain(c, "SNV-" + c.jeton());
        terrains.frais(terrain, type.id(), fournisseur, new BigDecimal("1000"), false);
        terrains.frais(terrain, type.id(), fournisseur, new BigDecimal("2000"), false);

        supprimer(terrain).andExpect(status().isNoContent());

        assertAll(
                () -> assertThat(terrains.compter(TERRAIN_PAR_ID, terrain)).isZero(),
                () -> assertThat(terrains.compter(FRAIS_DU_TERRAIN, terrain)).isZero());
    }

    @Test
    void r1_2_sansFrais_204() throws Exception {
        UUID terrain = terrains.terrain(c, "SSF-" + c.jeton());

        supprimer(terrain).andExpect(status().isNoContent());

        assertThat(terrains.compter(TERRAIN_PAR_ID, terrain)).isZero();
    }

    @Test
    void r1_2_occupe_409_terrainIntact() throws Exception {
        UUID terrain = terrains.terrain(c, "SOC-" + c.jeton());
        terrains.definir(terrain, "ComFolderId", fixture.dossier("DOS-" + c.jeton()).getId());

        supprimer(terrain).andExpect(status().isConflict());

        assertThat(terrains.compter(TERRAIN_PAR_ID, terrain)).isEqualTo(1);
    }

    @Test
    void r1_2_societeInactive_404_terrainIntact() throws Exception {
        UUID terrain = terrains.terrain(c, "SIN-" + c.jeton());
        mockMvc.perform(get(TERRAIN, terrain).with(connecte(admin))).andExpect(status().isOk());
        entites.actif(c.entite().getId(), false);

        supprimer(terrain).andExpect(status().isNotFound());

        assertThat(terrains.compter(TERRAIN_PAR_ID, terrain)).isEqualTo(1);
    }
}
