package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.fixtures.ApiFaisabilite;
import com.tsix.immocrm.fixtures.FixtureFaisabilite;
import com.tsix.immocrm.fixtures.FixtureFaisabiliteWorkflow;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.fiche;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.jeton;
import static com.tsix.immocrm.fixtures.FixtureFaisabiliteWorkflow.*;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.guid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pièces jointes d'une étude — FEATURE-patrimoine-faisabilite-workflow.md §4.3 et décision 3 (22/10) : porteur
 * {@code PorteurPieceJointe.ETUDE} (« etude », Category « Yes.Stk.StkFeasibilityStudy »), type choisi au dépôt, décompte par
 * {@code AdmAttachedFile.CategoryId}, écriture Administrateur, lecture Administrateur + DG, étude bloquée → 409.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PiecesJointesEtudeTest {

    private static final String PIECES_SQL = "SELECT * FROM AdmAttachedFile WHERE CategoryId = :id";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureFaisabilite fixture;
    @Autowired private FixtureFaisabiliteWorkflow wf;

    private AdmUser admin;
    private ApiFaisabilite api;
    private String jeton;
    private UUID etude;
    private UUID type;

    @BeforeEach
    void setUp() throws Exception {
        fixture.parametrage();
        wf.typesEtat();
        admin = patrimoine.administrateur();
        api = new ApiFaisabilite(mockMvc, admin);
        jeton = jeton();
        CfgCompany societe = patrimoine.entite("ENT-" + jeton, true);
        etude = api.etude(fiche("PJE-" + jeton, societe.getId()));
        type = wf.typeDocument("PV-" + jeton);
    }

    private static MockMultipartFile pdf() {
        return new MockMultipartFile("fichier", "pv.pdf", MediaType.APPLICATION_PDF_VALUE,
                "PDF".getBytes(StandardCharsets.UTF_8));
    }

    private ResultActions deposer(UUID etudeId, RequestPostProcessor qui) throws Exception {
        return mockMvc.perform(multipart(PIECES, etudeId).file(pdf()).param(TYPE_ID, type.toString()).with(qui));
    }

    private UUID depose() throws Exception {
        String json = deposer(etude, connecte(admin)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return UUID.fromString(JsonPath.read(json, "$.id"));
    }

    private List<String> idsListes(RequestPostProcessor qui) throws Exception {
        String json = mockMvc.perform(get(PIECES, etude).with(qui)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.<List<String>>read(json, "$[*].id").stream().map(FixturePatrimoine::guid).toList();
    }

    /** §4.3 : dépôt → 201, AdmAttachedFile rattachée à l'étude (CategoryId), Category OMRAN, type choisi. */
    @Test
    void r4_3_depot_201_ligneRattacheeALEtude() throws Exception {
        UUID piece = depose();

        List<Tuple> lignes = fixture.lignes(PIECES_SQL, etude);
        assertThat(lignes).singleElement().satisfies(l -> assertAll(
                () -> assertThat(guid(l.get("PKey"))).isEqualTo(guid(piece)),
                () -> assertThat(l.get("Category")).isEqualTo(CATEGORIE_ETUDE),
                () -> assertThat(guid(l.get("AdmAttachedFileTypeId"))).isEqualTo(guid(type))));
    }

    /** §4.3 : la liste rend la pièce déposée (Administrateur et Direction Générale). */
    @Test
    void r4_3_liste_rendLaPiece_adminEtDg() throws Exception {
        UUID piece = depose();

        assertAll(
                () -> assertThat(idsListes(connecte(admin))).contains(guid(piece)),
                () -> assertThat(idsListes(avecRole(patrimoine.administrateur(), ROLE_DG))).contains(guid(piece)));
    }

    /** §4.3 : suppression → 204, la pièce sort de la liste. */
    @Test
    void r4_3_suppression_204_retireeDeLaListe() throws Exception {
        UUID piece = depose();
        UUID autre = depose();

        mockMvc.perform(delete(PIECE, etude, piece).with(connecte(admin))).andExpect(status().isNoContent());

        assertThat(idsListes(connecte(admin))).contains(guid(autre)).doesNotContain(guid(piece));
    }

    /** §4.3 : étude bloquée → dépôt 409, rien n'est écrit. */
    @Test
    void r4_3_etudeBloquee_depot_409() throws Exception {
        api.bloquer(etude).andExpect(status().isOk());

        deposer(etude, connecte(admin)).andExpect(status().isConflict());

        assertThat(fixture.lignes(PIECES_SQL, etude)).isEmpty();
    }

    /** §4.3 : étude bloquée → suppression 409, la pièce reste listée. */
    @Test
    void r4_3_etudeBloquee_suppression_409() throws Exception {
        UUID piece = depose();
        api.bloquer(etude).andExpect(status().isOk());

        mockMvc.perform(delete(PIECE, etude, piece).with(connecte(admin))).andExpect(status().isConflict());

        assertThat(idsListes(connecte(admin))).contains(guid(piece));
    }

    /** §4.3 : écriture Administrateur seul — la Direction Générale ne dépose pas (403). */
    @Test
    void r4_3_depot_directionGenerale_403() throws Exception {
        deposer(etude, avecRole(patrimoine.administrateur(), ROLE_DG)).andExpect(status().isForbidden());

        assertThat(fixture.lignes(PIECES_SQL, etude)).isEmpty();
    }

    /** §4.3 : étude inconnue → 404 (liste et dépôt). */
    @Test
    void r4_3_etudeInconnue_404() throws Exception {
        UUID inconnue = UUID.fromString("7D8E9F0A-1B2C-4D3E-8F4A-6B7C8D9E0F1A");

        mockMvc.perform(get(PIECES, inconnue).with(connecte(admin))).andExpect(status().isNotFound());
        deposer(inconnue, connecte(admin)).andExpect(status().isNotFound());
    }

    /** §4.3 + §4.1 contrôle 3 : une pièce déposée par l'API satisfait le document exigé par la transition. */
    @Test
    void r4_3_pieceDeposee_satisfaitLeDocumentDeLaTransition() throws Exception {
        UUID depart = wf.statut("DEP-" + jeton, 921);
        UUID arrivee = wf.statut("ARR-" + jeton, 922);
        UUID t = wf.transition(depart, arrivee, "En cours-" + jeton, "", wf.roleAdministrateur());
        wf.documentDeTransition(t, type, true);
        wf.placer(etude, depart);
        depose();

        mockMvc.perform(post(TRANSITIONS, etude).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(corps().avec(TRANSITION_ID, t).json()))
                .andExpect(status().isOk());

        assertThat(wf.statutCourant(etude)).isEqualTo(guid(arrivee));
    }
}
