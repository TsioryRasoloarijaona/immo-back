package com.tsix.immocrm.controller.modele;

import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureFormulairesBarid;
import com.tsix.immocrm.fixtures.FixtureFormulairesBarid.Adresse;
import com.tsix.immocrm.repositories.app.AppModeleImageRepository;
import com.tsix.immocrm.storage.StockageMemoire;
import com.tsix.immocrm.utils.LecturePdf;
import com.tsix.immocrm.utils.LecturePdf.Dessin;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fond de page (FEATURE-vente-formulaire-barid.md §3.1, §8.1 « fond ») : {@code miseEnPage.fondImageId} (image déposée,
 * mêmes règles que §13 des modèles) et {@code fondPleinePage} (défaut vrai) ; le fond est dessiné sur chaque page, plein
 * format, derrière le texte. Dossier de test : un acquéreur et un co-acquéreur (deux pages).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "Administrateur")
class FondDePageTest {

    private static final String BASE = "/api/modeles-documents";
    private static final String GENERER = BASE + "/{id}/generer";
    private static final String FICHE_ID = "ficheId";
    private static final String FOND_IMAGE_ID = "fondImageId";
    private static final String CONTENU = "<div data-repeter=\"destinataires\" data-saut-page>"
            + "<p>Fond de <span data-champ=\"destinataire.nom\">Nom</span></p></div>";
    private static final float TOLERANCE_PT = 2f;

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper json;
    @Autowired private FixtureFormulairesBarid fixture;
    @Autowired private FixtureEtatVentes ev;
    @Autowired private AppModeleImageRepository imageRepository;
    @Autowired private StockageMemoire stockage;

    private ComFolder dossier;

    @BeforeEach
    void setUp() {
        String p = FixtureEtatVentes.prefixe("BAMF");
        CfgTranche t = fixture.tranche("T-" + p, true, fixture.pays("PAYS-" + p));
        dossier = fixture.dossier(t, "DOS-" + p, ev.statutEnCours("ENC-" + p),
                fixture.client("FONDUN" + p, null, Adresse.aucune()));
        fixture.coAcquereur(dossier, fixture.client("FONDDEUX" + p, null, Adresse.aucune()),
                LocalDateTime.of(2025, 1, 1, 0, 0));
        fixture.relire();
    }

    private UUID deposerJpeg() throws Exception {
        String reponse = mockMvc.perform(multipart(ImagesModeleOutils.IMAGES)
                        .file(ImagesModeleOutils.fichier("fond.jpg", "image/jpeg", ImagesModeleOutils.image(40, "jpg"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(json.readTree(reponse).get("id").asString());
    }

    private ResultActions creer(Map<String, Object> miseEnPage) throws Exception {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("categorie", "DOSSIER_VENTE");
        m.put("code", "TST_FOND_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        m.put("libelle", "Fond");
        m.put("contenu", CONTENU);
        m.put("miseEnPage", miseEnPage);
        return mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(m)));
    }

    private String idCree(UUID fond) throws Exception {
        String reponse = creer(Map.of(FOND_IMAGE_ID, fond.toString())).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(reponse).get("id").asString();
    }

    @Test
    void fondInconnu_400ALEnregistrement() throws Exception {
        UUID inconnue = UUID.randomUUID();

        creer(Map.of(FOND_IMAGE_ID, inconnue.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString(inconnue.toString())));
    }

    @Test
    void fondEnregistre_relu_pleinePageParDefaut() throws Exception {
        UUID fond = deposerJpeg();

        mockMvc.perform(get(BASE + "/{id}", idCree(fond)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.miseEnPage.fondImageId").value(fond.toString()))
                .andExpect(jsonPath("$.miseEnPage.fondPleinePage").value(true));
    }

    @Test
    void fondPresent_surChaquePage_pleinFormat_derriereLeTexte() throws Exception {
        String id = idCree(deposerJpeg());

        byte[] pdf = mockMvc.perform(get(GENERER, id).param(FICHE_ID, dossier.getId().toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(LecturePdf.pages(pdf)).as("pages").isEqualTo(2);
        for (int page = 1; page <= 2; page++) {
            float[] format = LecturePdf.format(pdf, page);
            List<Dessin> dessins = LecturePdf.dessins(pdf, page);
            List<Dessin> images = dessins.stream().filter(Dessin::image).toList();
            soft.assertThat(images).as("fond de la page %d", page).hasSize(1);
            soft.assertThat(dessins).as("page %d : fond dessiné avant tout texte", page).isNotEmpty()
                    .first().matches(Dessin::image, "image");
            if (images.size() == 1) {
                soft.assertThat(images.get(0).largeur()).as("largeur page %d", page)
                        .isCloseTo(format[0], within(TOLERANCE_PT));
                soft.assertThat(images.get(0).hauteur()).as("hauteur page %d", page)
                        .isCloseTo(format[1], within(TOLERANCE_PT));
            }
        }
        soft.assertAll();
    }

    @Test
    void fondAbsentDuStockage_500Explicite() throws Exception {
        UUID fond = deposerJpeg();
        String id = idCree(fond);
        stockage.supprimer(imageRepository.findById(fond).orElseThrow().getStorageKey());

        mockMvc.perform(get(GENERER, id).param(FICHE_ID, dossier.getId().toString()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value(containsString(fond.toString())));
    }
}
