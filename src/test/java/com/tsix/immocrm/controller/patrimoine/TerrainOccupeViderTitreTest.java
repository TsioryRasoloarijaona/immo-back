package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureTerrains;
import com.tsix.immocrm.fixtures.FixtureTerrains.Contexte;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureTerrains.TERRAIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Revue P7a — §1.2 : le n° de titre d'un terrain occupé est protégé (409). Le vider par {@code champsAVider}
 * doit être refusé comme le modifier.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TerrainOccupeViderTitreTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureTerrains terrains;

    @Test
    void occupe_viderTitreFoncierParChampsAVider_409_titreConserve() throws Exception {
        AdmUser admin = fixture.administrateur();
        Contexte c = terrains.contexte("P7R");
        UUID terrain = terrains.terrain(c, "OCV-" + c.jeton());
        String titre = "TF-" + c.jeton();
        terrains.definir(terrain, "TitleNumber", titre);
        terrains.definir(terrain, "ComFolderId", fixture.dossier("DOS-" + c.jeton()).getId());

        mockMvc.perform(patch(TERRAIN, terrain).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(corps().avec("champsAVider", List.of("titreFoncier")).json()))
                .andExpect(status().isConflict());

        assertThat(terrains.ligne("SELECT * FROM StkItem WHERE PKey = :id", terrain).get("TitleNumber"))
                .isEqualTo(titre);
    }
}
