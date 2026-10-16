package com.tsix.immocrm.controller.modele;

import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.fixtures.FixtureActeReservation;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayInputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Logo d'en-tête et image du corps déposés dans le stockage, rendus en PDF et en Word (FEATURE-modeles-documents.md §13). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "Administrateur")
class ModeleDocumentLogoTest {

    private static final String BASE = "/api/modeles-documents";
    private static final String GENERER = BASE + "/{id}/generer";
    private static final String FICHE_ID = "ficheId";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JsonMapper json;
    @Autowired
    private FixtureActeReservation fixtureActe;

    private String modele(String code, String contenu, String enTete) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("categorie", "DOSSIER_VENTE");
        m.put("code", code);
        m.put("libelle", code);
        m.put("contenu", contenu);
        m.put("miseEnPage", Map.of("enTete", enTete));
        return json.writeValueAsString(m);
    }

    /** Modèle avec un logo d'environ 100 Ko dans l'en-tête et une image dans le corps ; renvoie son id. */
    private String creerAvecImages(String code) throws Exception {
        String logo = ImagesModeleOutils.deposerPng(mockMvc, json, 190);
        String image = ImagesModeleOutils.deposerPng(mockMvc, json, 60);
        String reponse = mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(modele(code, "<p>Corps " + image + "</p>", "<p>" + logo + " En-tête</p>")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.miseEnPage.enTete").value(org.hamcrest.Matchers.containsString("data-image")))
                .andReturn().getResponse().getContentAsString();
        assertThat(reponse).doesNotContain("src=");
        return json.readTree(reponse).get("id").asString();
    }

    @Test
    void imagesDansLEnTeteEtLeCorps_renduesEnPdf() throws Exception {
        ComFolder dossier = fixtureActe.dossier("LOGO", false, true);
        String id = creerAvecImages("TST_LOGO_PDF");

        byte[] pdf = mockMvc.perform(get(GENERER, id).param(FICHE_ID, dossier.getId().toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(document.getPage(0).getResources().getXObjectNames()).hasSizeGreaterThanOrEqualTo(2);
        }
    }

    @Test
    void imagesDansLEnTeteEtLeCorps_rendueEnWord() throws Exception {
        ComFolder dossier = fixtureActe.dossier("LOGO-DOCX", false, true);
        String id = creerAvecImages("TST_LOGO_DOCX");

        byte[] docx = mockMvc.perform(get(GENERER, id).param(FICHE_ID, dossier.getId().toString()).param("format", "docx"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            assertThat(document.getHeaderList()).isNotEmpty()
                    .anySatisfy((XWPFHeader h) -> assertThat(h.getAllPictures()).isNotEmpty());
            assertThat(document.getParagraphs()).anySatisfy(p -> assertThat(p.getRuns())
                    .anySatisfy(r -> assertThat(r.getEmbeddedPictures()).isNotEmpty()));
        }
    }

    @Test
    void texteDeLaVersionAuDelaDe1Mo_400() throws Exception {
        // Contenu seul sous 1 Mo ; c'est le texte de l'en-tête qui fait déborder la version.
        String contenu = "<p>" + "a".repeat(700 * 1024) + "</p>";
        String enTete = "<p>" + "b".repeat(400 * 1024) + "</p>";

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(modele("TST_TEXTE_LOURD", contenu, enTete)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("1 Mo maximum")));
    }
}
