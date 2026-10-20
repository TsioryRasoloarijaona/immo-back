package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.storage.StockageMemoire;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Liste blanche d'extensions des dépôts — FEATURE-patrimoine-article-onglets.md §11 (Q1) : globale, dans
 * {@code PieceJointeSupport.deposer} (dossier et règlement compris) ; admises {@code pdf, jpg, jpeg, jfif, png, gif, webp,
 * heic, doc, docx, xls, xlsx, txt} ; ni svg ni dwg ; extension refusée → 400 avec la liste admise, rien de stocké.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ListeBlancheExtensionsTest {

    private static final String DETAIL = "$.detail";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private StockageMemoire stockage;

    private String jeton;
    private AdmUser admin;
    private CfgTranche tranche;

    @BeforeEach
    void setUp() {
        jeton = "P2BLB" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
        admin = patrimoine.administrateur();
        tranche = patrimoine.tranche("TR-" + jeton, patrimoine.projet("PRJ-" + jeton,
                patrimoine.entite("ENT-" + jeton, true)));
    }

    private static MockMultipartFile fichier(String nom, String typeMime) {
        return new MockMultipartFile("fichier", nom, typeMime, "CONTENU".getBytes(StandardCharsets.UTF_8));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "plan.pdf, application/pdf",
            "photo.jpg, image/jpeg",
            "photo.jpeg, image/jpeg",
            "photo.jfif, image/jpeg",
            "plan.png, image/png",
            "anim.gif, image/gif",
            "photo.webp, image/webp",
            "photo.heic, image/heic",
            "contrat.doc, application/msword",
            "contrat.docx, application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "tableau.xls, application/vnd.ms-excel",
            "tableau.xlsx, application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "notes.txt, text/plain"})
    void q1_extensionAdmise_depotTranche201(String nom, String typeMime) throws Exception {
        mockMvc.perform(multipart("/api/tranches/{id}/pieces-jointes", tranche.getId())
                        .file(fichier(nom, typeMime)).with(connecte(admin)))
                .andExpect(status().isCreated());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "plan.svg, image/svg+xml",
            "plan.dwg, application/acad",
            "outil.exe, application/octet-stream",
            "page.html, text/html"})
    void q1_extensionRefusee_depotTranche400_avecLaListeAdmise_rienDeStocke(String nom, String typeMime)
            throws Exception {
        int avant = stockage.nombreObjets();

        mockMvc.perform(multipart("/api/tranches/{id}/pieces-jointes", tranche.getId())
                        .file(fichier(nom, typeMime)).with(connecte(admin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(allOf(containsString("pdf"), containsString("xlsx"),
                        containsString("heic"))));

        assertThat(stockage.nombreObjets()).isEqualTo(avant);
    }

    /** Q1 = global : le dépôt de dossier (comportement actuel : tout accepté) refuse aussi. */
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"plan.svg", "plan.dwg", "outil.exe"})
    void q1_global_depotDossier_extensionRefusee400(String nom) throws Exception {
        ComFolder dossier = patrimoine.dossier("DOS-" + jeton);

        mockMvc.perform(multipart("/api/dossiers/{id}/pieces-jointes", dossier.getId())
                        .file(fichier(nom, MediaType.APPLICATION_OCTET_STREAM_VALUE)).with(connecte(admin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString("pdf")));
    }
}
