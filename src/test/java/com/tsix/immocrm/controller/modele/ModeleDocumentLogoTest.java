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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Logo dans l'en-tête : mêmes limites que le contenu, 2 Mo d'images par version au total (FEATURE-modeles-documents.md §3). */
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

    /** PNG de bruit (incompressible) : {@code cote}² × 3 octets environ. */
    private static String png(int cote) throws IOException {
        Random hasard = new Random(cote);
        BufferedImage image = new BufferedImage(cote, cote, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < cote; x++) {
            for (int y = 0; y < cote; y++) {
                image.setRGB(x, y, hasard.nextInt(0xFFFFFF));
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return "<img src=\"data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray()) + "\" alt=\"logo\">";
    }

    private String modele(String code, String contenu, String enTete) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("categorie", "DOSSIER_VENTE");
        m.put("code", code);
        m.put("libelle", code);
        m.put("contenu", contenu);
        m.put("miseEnPage", Map.of("enTete", enTete));
        return json.writeValueAsString(m);
    }

    /** Modèle dont l'en-tête porte un logo de plus de 100 Ko ; renvoie son id. */
    private String creerAvecLogo(String code) throws Exception {
        String logo = png(180);
        assertThat(logo.length()).isGreaterThan(100 * 1024);
        String reponse = mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(modele(code, "<p>Corps</p>", "<p>" + logo + " En-tête</p>")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.miseEnPage.enTete").value(org.hamcrest.Matchers.containsString("data:image/png")))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(reponse).get("id").asString();
    }

    @Test
    void logoDeCentKoDansLEnTete_enregistreEtRenduEnPdf() throws Exception {
        ComFolder dossier = fixtureActe.dossier("LOGO", false, true);
        String id = creerAvecLogo("TST_LOGO_PDF");

        byte[] pdf = mockMvc.perform(get(GENERER, id).param(FICHE_ID, dossier.getId().toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(document.getPage(0).getResources().getXObjectNames()).isNotEmpty();
        }
    }

    @Test
    void logoDeCentKoDansLEnTete_renduEnWord() throws Exception {
        ComFolder dossier = fixtureActe.dossier("LOGO-DOCX", false, true);
        String id = creerAvecLogo("TST_LOGO_DOCX");

        byte[] docx = mockMvc.perform(get(GENERER, id).param(FICHE_ID, dossier.getId().toString()).param("format", "docx"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            assertThat(document.getHeaderList()).isNotEmpty()
                    .anySatisfy((XWPFHeader h) -> assertThat(h.getAllPictures()).isNotEmpty());
        }
    }

    @Test
    void imageDansLeCorps_renduEnWordDansLeCorps() throws Exception {
        ComFolder dossier = fixtureActe.dossier("IMG-CORPS", false, true);
        String reponse = mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(modele("TST_IMAGE_CORPS", "<p>" + png(60) + "</p>", "<p>En-tête</p>")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = json.readTree(reponse).get("id").asString();

        byte[] docx = mockMvc.perform(get(GENERER, id).param(FICHE_ID, dossier.getId().toString()).param("format", "docx"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            assertThat(document.getAllPictures()).isNotEmpty();
        }
    }

    @Test
    void enTeteEtContenuAuDelaDe2Mo_400() throws Exception {
        String image = png(390);
        // Contenu seul : 4 × ~456 Ko, sous les 2 Mo ; c'est l'image de l'en-tête qui fait déborder la version.
        String contenu = ("<p>" + image + "</p>").repeat(4);

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(modele("TST_LOGO_LOURD", contenu, "<p>" + image + "</p>")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("2 Mo maximum")));
    }
}
