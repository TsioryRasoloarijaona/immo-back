package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.fixtures.FixtureExportPatrimoine;
import com.tsix.immocrm.fixtures.FixtureLotissements;
import com.tsix.immocrm.fixtures.FixtureLotissements.Jeu;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureLotissements.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureLotissements.FICHE_PDF;
import static com.tsix.immocrm.fixtures.FixtureLotissements.LOTISSEMENT;
import static com.tsix.immocrm.fixtures.LectureExport.compact;
import static com.tsix.immocrm.fixtures.LectureExport.estPdf;
import static com.tsix.immocrm.fixtures.LectureExport.executer;
import static com.tsix.immocrm.fixtures.LectureExport.textePdf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fiche d'un lot — FEATURE-patrimoine-lotissements.md §1.1, §1.2, §1.8, §2, règles L2, L3, L16 ; réponse 3 du manager
 * (27/10, {@code FicheLotissement}) ; décision Q5 (champs spécifiques, porteur {@code lotissement}). Données validées pour
 * de bon ({@link FixtureLotissements}), effacées en {@code @AfterEach}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LotissementFicheTest {

    private static final String CHAMPS_SPECIFIQUES = "/api/champs-specifiques/lotissement/{id}";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotissements fixture;

    private Jeu j;

    @BeforeEach
    void setUp() {
        j = fixture.preparer("P7CF");
    }

    @AfterEach
    void nettoyer() {
        fixture.nettoyer(j);
    }

    private int statutDetail(UUID id) throws Exception {
        return mockMvc.perform(get(LOTISSEMENT, id).with(connecte(j.admin()))).andReturn().getResponse().getStatus();
    }

    private int statutPdf(UUID id) throws Exception {
        return executer(mockMvc, get(FICHE_PDF, id).with(connecte(j.admin()))).getResponse().getStatus();
    }

    // ------------------------------------------------------------------ détail

    /** §2 : LotissementDetailDto d'un lot libre ; estOccupe faux. */
    @Test
    void l2_lotLibre_200_codeEtEstOccupeFaux() throws Exception {
        String code = j.code("LF");
        UUID lot = fixture.lot(j, code);

        mockMvc.perform(get(LOTISSEMENT, lot).with(connecte(j.admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(lot.toString()))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.estOccupe").value(false));
    }

    /** §2 : estOccupe vrai quand le lot porte un dossier de vente (ArticleService.estOccupe). */
    @Test
    void l2_lotOccupe_estOccupeVrai() throws Exception {
        UUID lot = fixture.lot(j, j.code("LO"));
        fixture.occuper(j, lot);

        mockMvc.perform(get(LOTISSEMENT, lot).with(connecte(j.admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estOccupe").value(true));
    }

    /** §2 : GET ouvert à tous les rôles de la liste. */
    @Test
    void l1_commercial_lecture_200() throws Exception {
        UUID lot = fixture.lot(j, j.code("LC"));

        mockMvc.perform(get(LOTISSEMENT, lot).with(FixtureExportPatrimoine.commercial(j.admin())))
                .andExpect(status().isOk());
    }

    @Test
    void l2_terrainSansTranche_404() throws Exception {
        UUID terrain = fixture.terrainSansTranche(j, j.code("LST"));

        assertThat(statutDetail(terrain)).isEqualTo(404);
    }

    @Test
    void l2_articleBatiDeLaTranche_404() throws Exception {
        UUID article = fixture.article(j, j.code("LAB"));

        assertThat(statutDetail(article)).isEqualTo(404);
    }

    /** §1.1 : un terrain LandProject rattaché à une tranche n'est pas un lot. */
    @Test
    void l1_1_terrainLandProjectDeTranche_404() throws Exception {
        UUID terrain = fixture.terrainLandProjectDeTranche(j, j.code("LLP"));

        assertThat(statutDetail(terrain)).isEqualTo(404);
    }

    @Test
    void l2_idInconnu_404() throws Exception {
        assertThat(statutDetail(UUID.randomUUID())).isEqualTo(404);
    }

    // ------------------------------------------------------------------ L3 entité inactive

    /** L3 : lot d'une tranche d'entité inactive — GET, PATCH, DELETE, PDF ⇒ 404, lot intact. */
    @Test
    void l3_entiteInactive_getPatchDeletePdf_404_lotIntact() throws Exception {
        String code = j.code("LIN");
        UUID lot = fixture.lot(j, code);
        assertThat(statutDetail(lot)).isEqualTo(200);
        fixture.desactiverEntite(j);

        int lecture = statutDetail(lot);
        int pdf = statutPdf(lot);
        int modification = mockMvc.perform(patch(LOTISSEMENT, lot).with(connecte(j.admin()))
                        .contentType(MediaType.APPLICATION_JSON).content(corps().avec(DESCRIPTION, "Renomme").json()))
                .andReturn().getResponse().getStatus();
        int suppression = mockMvc.perform(delete(LOTISSEMENT, lot).with(connecte(j.admin())))
                .andReturn().getResponse().getStatus();

        assertAll(
                () -> assertThat(lecture).as("GET").isEqualTo(404),
                () -> assertThat(pdf).as("PDF").isEqualTo(404),
                () -> assertThat(modification).as("PATCH").isEqualTo(404),
                () -> assertThat(suppression).as("DELETE").isEqualTo(404),
                () -> assertThat(fixture.ligne(lot)).containsEntry("Description", FixtureLotissements.description(code)));
    }

    // ------------------------------------------------------------------ L16 fiche PDF

    /** L16 : 200, PDF inline, texte portant le code du lot et la tranche. */
    @Test
    void l16_fichePdf_200_inline_codeEtTranche() throws Exception {
        String code = j.code("LPDF");
        UUID lot = fixture.lot(j, code);

        MvcResult resultat = executer(mockMvc, get(FICHE_PDF, lot).with(connecte(j.admin())));

        byte[] contenu = resultat.getResponse().getContentAsByteArray();
        assertAll(
                () -> assertThat(resultat.getResponse().getStatus()).isEqualTo(200),
                () -> assertThat(resultat.getResponse().getContentType()).startsWith(MediaType.APPLICATION_PDF_VALUE),
                () -> assertThat(resultat.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION)).startsWith("inline"),
                () -> assertThat(estPdf(contenu)).isTrue(),
                () -> assertThat(compact(textePdf(contenu))).contains(code).contains("TR-" + j.jeton()));
    }

    @Test
    void l16_fichePdf_terrainSansTranche_404() throws Exception {
        assertThat(statutPdf(fixture.terrainSansTranche(j, j.code("PST")))).isEqualTo(404);
    }

    @Test
    void l16_fichePdf_article_404() throws Exception {
        assertThat(statutPdf(fixture.article(j, j.code("PAB")))).isEqualTo(404);
    }

    // ------------------------------------------------------------------ Q5 champs spécifiques

    /** Décision Q5 / carte (d) : porteur {@code lotissement} ({@code Yes.Stk.Land.Land}) des champs spécifiques. */
    @Test
    void q5_champsSpecifiques_porteurLotissement_200() throws Exception {
        UUID lot = fixture.lot(j, j.code("LCS"));

        mockMvc.perform(get(CHAMPS_SPECIFIQUES, lot).with(connecte(j.admin()))).andExpect(status().isOk());
    }

    @Test
    void q5_champsSpecifiques_lotDUneEntiteInactive_404() throws Exception {
        UUID lot = fixture.lot(j, j.code("LCI"));
        fixture.desactiverEntite(j);

        mockMvc.perform(get(CHAMPS_SPECIFIQUES, lot).with(connecte(j.admin()))).andExpect(status().isNotFound());
    }
}
