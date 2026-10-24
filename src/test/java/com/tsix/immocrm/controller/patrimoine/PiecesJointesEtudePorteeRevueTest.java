package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.fixtures.ApiFaisabilite;
import com.tsix.immocrm.fixtures.FixtureFaisabilite;
import com.tsix.immocrm.fixtures.FixtureFaisabiliteWorkflow;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.fiche;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.jeton;
import static com.tsix.immocrm.fixtures.FixtureFaisabiliteWorkflow.PIECE;
import static com.tsix.immocrm.fixtures.FixtureFaisabiliteWorkflow.PIECES;
import static com.tsix.immocrm.fixtures.FixtureFaisabiliteWorkflow.ROLE_DG;
import static com.tsix.immocrm.fixtures.FixtureFaisabiliteWorkflow.TYPE_ID;
import static com.tsix.immocrm.fixtures.FixtureFaisabiliteWorkflow.avecRole;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.guid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Revue P8b-2 (règle 6) : une pièce n'est servie ni supprimée que par l'URL de son étude (§4.3). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PiecesJointesEtudePorteeRevueTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureFaisabilite fixture;
    @Autowired private FixtureFaisabiliteWorkflow wf;

    private AdmUser admin;
    private UUID etudeA;
    private UUID etudeB;
    private UUID pieceDeB;

    @BeforeEach
    void setUp() throws Exception {
        fixture.parametrage();
        wf.typesEtat();
        admin = patrimoine.administrateur();
        ApiFaisabilite api = new ApiFaisabilite(mockMvc, admin);
        String jeton = jeton();
        CfgCompany societe = patrimoine.entite("ENP-" + jeton, true);
        etudeA = api.etude(fiche("PRA-" + jeton, societe.getId()));
        etudeB = api.etude(fiche("PRB-" + jeton, societe.getId()));
        UUID type = wf.typeDocument("PVP-" + jeton);
        MockMultipartFile pdf = new MockMultipartFile("fichier", "pv.pdf", MediaType.APPLICATION_PDF_VALUE,
                "PDF".getBytes(StandardCharsets.UTF_8));
        String json = mockMvc.perform(multipart(PIECES, etudeB).file(pdf).param(TYPE_ID, type.toString())
                        .with(connecte(admin)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        pieceDeB = UUID.fromString(JsonPath.read(json, "$.id"));
    }

    private List<String> idsDeB() throws Exception {
        String json = mockMvc.perform(get(PIECES, etudeB).with(connecte(admin))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.<List<String>>read(json, "$[*].id").stream().map(FixturePatrimoine::guid).toList();
    }

    @Test
    void fichier_pieceDUneAutreEtude_404() throws Exception {
        mockMvc.perform(get(PIECE + "/fichier", etudeA, pieceDeB).with(connecte(admin)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(PIECE + "/fichier", etudeA, pieceDeB).with(avecRole(patrimoine.administrateur(), ROLE_DG)))
                .andExpect(status().isNotFound());
    }

    @Test
    void suppression_pieceDUneAutreEtude_404_pieceIntacte() throws Exception {
        mockMvc.perform(delete(PIECE, etudeA, pieceDeB).with(connecte(admin))).andExpect(status().isNotFound());

        assertThat(idsDeB()).contains(guid(pieceDeB));
    }

    @Test
    void suppression_parLaDirectionGenerale_403_pieceIntacte() throws Exception {
        mockMvc.perform(delete(PIECE, etudeB, pieceDeB).with(avecRole(patrimoine.administrateur(), ROLE_DG)))
                .andExpect(status().isForbidden());

        assertThat(idsDeB()).contains(guid(pieceDeB));
    }
}
