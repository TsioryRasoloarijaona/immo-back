package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureProspectionsTerrain;
import com.tsix.immocrm.fixtures.FixtureProspectionsTerrain.Contexte;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureProspectionsTerrain.PIECES;
import static com.tsix.immocrm.fixtures.FixtureProspectionsTerrain.PORTEUR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Porteur « prospection-terrain » — FEATURE-patrimoine-prospections-terrain.md §5.6 (PorteursPatrimoineProspectionTest) :
 * notes, audit et pièces jointes acceptent une prospection terrain ; id inconnu → 404.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PorteursPatrimoineProspectionTest {

    private static final String NOTES = "/api/notes";
    private static final String AUDIT = "/api/audit";
    private static final UUID INCONNU = UUID.fromString("2A3B4C5D-6E7F-4801-9A2B-3C4D5E6F7A8B");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureProspectionsTerrain fixture;

    private AdmUser admin;
    private UUID prospection;

    @BeforeEach
    void setUp() {
        admin = patrimoine.administrateur();
        Contexte c = fixture.contexte("P7N");
        prospection = fixture.prospection(c, "POR-" + c.jeton());
    }

    @Test
    void r5_6_notes_lecture_200_porteurProspectionTerrain() throws Exception {
        fixture.note(prospection);

        mockMvc.perform(get(NOTES).param("porteur", PORTEUR).param("id", prospection.toString()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void r5_6_notes_creation_201_rattacheeALaProspection() throws Exception {
        mockMvc.perform(post(NOTES).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(corps().avec("porteur", PORTEUR).avec("id", prospection).avec("texte", "Note P7b").json()))
                .andExpect(status().isCreated());

        assertThat(fixture.compter("SELECT COUNT(*) FROM AdmMessage WHERE CategoryId = :id", prospection)).isEqualTo(1);
    }

    @Test
    void r5_6_notes_prospectionInconnue_404() throws Exception {
        mockMvc.perform(get(NOTES).param("porteur", PORTEUR).param("id", INCONNU.toString()).with(connecte(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    void r5_6_audit_200_pourUneProspection() throws Exception {
        mockMvc.perform(get(AUDIT).param("objetId", prospection.toString()).with(connecte(admin)))
                .andExpect(status().isOk());
    }

    @Test
    void r5_6_audit_objetInconnu_404() throws Exception {
        mockMvc.perform(get(AUDIT).param("objetId", INCONNU.toString()).with(connecte(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    void r5_6_piecesJointes_lecture_200() throws Exception {
        fixture.pieceJointe(prospection);

        mockMvc.perform(get(PIECES, prospection).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void r5_6_piecesJointes_prospectionInconnue_404() throws Exception {
        mockMvc.perform(get(PIECES, INCONNU).with(connecte(admin))).andExpect(status().isNotFound());
    }
}
