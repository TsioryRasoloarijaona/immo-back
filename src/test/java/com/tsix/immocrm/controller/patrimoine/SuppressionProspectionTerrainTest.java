package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.fixtures.ApiProspectionsTerrain;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureProspectionsTerrain;
import com.tsix.immocrm.fixtures.FixtureProspectionsTerrain.Contexte;
import com.tsix.immocrm.fixtures.FixtureTerrains;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.tsix.immocrm.fixtures.ApiProspectionsTerrain.d;
import static com.tsix.immocrm.fixtures.FixtureProspectionsTerrain.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Suppression — FEATURE-patrimoine-prospections-terrain.md §5.8 (SuppressionProspectionTerrainTest) : 204 avec zones,
 * historique, pièces jointes et notes ; 409 terrain lié (code du terrain), 409 étude liée (nombre), contrôles codés
 * explicitement (yesRemsTest n'a aucune FK). Le 409 CanDelete est dans TransitionsProspectionTerrainTest.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SuppressionProspectionTerrainTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureProspectionsTerrain fixture;
    @Autowired private FixtureTerrains terrains;

    private ApiProspectionsTerrain api;
    private Contexte c;

    @BeforeEach
    void setUp() {
        api = new ApiProspectionsTerrain(mockMvc, patrimoine.administrateur());
        c = fixture.contexte("P7D");
    }

    private int compter(String sql, UUID id) {
        return fixture.compter(sql, id);
    }

    /** §5.8 : 204 ; la prospection, ses zones, son historique, ses pièces jointes et ses notes sont supprimés. */
    @Test
    void r5_8_suppression_204_avecZonesHistoriquePiecesEtNotes() throws Exception {
        String code = "SUP-" + c.jeton();
        UUID id = fixture.prospection(c, code);
        fixture.zone(id, "Z1", d("300"));
        fixture.zone(id, "Z2", d("200"));
        fixture.historique(id, "Création");
        fixture.pieceJointe(id);
        fixture.note(id);

        api.supprimer(id).andExpect(status().isNoContent());

        assertAll(
                () -> assertThat(fixture.prospectionsDeCode(code)).isZero(),
                () -> assertThat(compter("SELECT COUNT(*) FROM LndLandZone WHERE ProspectionId = :id", id)).isZero(),
                () -> assertThat(compter("SELECT COUNT(*) FROM StrHistory WHERE ObjectId = :id", id)).isZero(),
                () -> assertThat(compter("SELECT COUNT(*) FROM AdmAttachedFile WHERE CategoryId = :id", id)).isZero(),
                () -> assertThat(compter("SELECT COUNT(*) FROM AdmMessage WHERE CategoryId = :id", id)).isZero());
    }

    /** §5.8 : un terrain StkItem porte LndLandId = la prospection → 409 nommant le code du terrain, rien supprimé. */
    @Test
    void r5_8_terrainLie_409_nommantLeTerrain() throws Exception {
        String code = "STL-" + c.jeton();
        UUID id = fixture.prospection(c, code);
        fixture.zone(id, "Z1", d("300"));
        FixtureTerrains.Contexte t = terrains.contexte("P7T");
        UUID terrain = terrains.terrain(t, "TER-" + c.jeton());
        terrains.definir(terrain, "LndLandId", id);

        api.supprimer(id)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("TER-" + c.jeton())));

        assertAll(
                () -> assertThat(fixture.prospectionsDeCode(code)).isEqualTo(1),
                () -> assertThat(compter("SELECT COUNT(*) FROM LndLandZone WHERE ProspectionId = :id", id)).isEqualTo(1));
    }

    /** §5.8 : études de faisabilité liées → 409 avec leur nombre dans le message, rien supprimé. */
    @Test
    void r5_8_etudesLiees_409_avecLeNombre() throws Exception {
        String code = "SET-" + c.jeton();
        UUID id = fixture.prospection(c, code);
        fixture.etudeLiee(id, "ET1-" + c.jeton());
        fixture.etudeLiee(id, "ET2-" + c.jeton());

        api.supprimer(id)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("2")));

        assertThat(fixture.prospectionsDeCode(code)).isEqualTo(1);
    }
}
