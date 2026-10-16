package com.tsix.immocrm.controller.modele;

import com.tsix.immocrm.entities.app.AppModeleImage;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.fixtures.FixtureActeReservation;
import com.tsix.immocrm.repositories.app.AppModeleImageRepository;
import com.tsix.immocrm.storage.StockageMemoire;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Dépôt, lecture et usage des images de modèles stockées dans S3 (FEATURE-modeles-documents.md §13). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "Administrateur")
class ModeleImagesTest {

    private static final String BASE = "/api/modeles-documents";
    private static final String IMAGE = ImagesModeleOutils.IMAGES + "/{id}";
    private static final String DETAIL = "$.detail";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JsonMapper json;
    @Autowired
    private AppModeleImageRepository imageRepository;
    @Autowired
    private StockageMemoire stockage;
    @Autowired
    private FixtureActeReservation fixtureActe;

    private JsonNode deposer(String nom, String contentType, byte[] octets) throws Exception {
        String reponse = mockMvc.perform(multipart(ImagesModeleOutils.IMAGES)
                        .file(ImagesModeleOutils.fichier(nom, contentType, octets)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(reponse);
    }

    private String modele(String code, String contenu) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("categorie", "DOSSIER_VENTE");
        m.put("code", code);
        m.put("libelle", code);
        m.put("contenu", contenu);
        return json.writeValueAsString(m);
    }

    @Test
    void depotPng_201LigneEtObjet() throws Exception {
        byte[] png = ImagesModeleOutils.image(20, "png");

        JsonNode reponse = deposer("logo.png", "image/png", png);

        UUID id = UUID.fromString(reponse.get("id").asString());
        assertThat(reponse.get("contentType").asString()).isEqualTo("image/png");
        assertThat(reponse.get("taille").asLong()).isEqualTo(png.length);
        AppModeleImage ligne = imageRepository.findById(id).orElseThrow();
        assertThat(ligne.getStorageKey()).endsWith("modeles/images/" + id + ".png");
        assertThat(ligne.getSizeBytes()).isEqualTo(png.length);
        assertThat(stockage.contenu(ligne.getStorageKey())).isEqualTo(png);
    }

    @Test
    void depotJpeg_201AvecSonType() throws Exception {
        // Nom et type déclarés trompeurs : seul le contenu compte.
        JsonNode reponse = deposer("photo.png", "image/png", ImagesModeleOutils.image(20, "jpg"));

        UUID id = UUID.fromString(reponse.get("id").asString());
        assertThat(reponse.get("contentType").asString()).isEqualTo("image/jpeg");
        assertThat(stockage.contient(imageRepository.findById(id).orElseThrow().getStorageKey())).isTrue();
        assertThat(imageRepository.findById(id).orElseThrow().getStorageKey()).endsWith(".jpg");
    }

    @Test
    void depotGif_400() throws Exception {
        mockMvc.perform(multipart(ImagesModeleOutils.IMAGES)
                        .file(ImagesModeleOutils.fichier("anim.gif", "image/gif", ImagesModeleOutils.image(20, "gif"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString("PNG ou JPEG seulement")));
    }

    @Test
    void fauxPngParLExtensionSeule_400() throws Exception {
        int avant = stockage.nombreObjets();
        mockMvc.perform(multipart(ImagesModeleOutils.IMAGES)
                        .file(ImagesModeleOutils.fichier("faux.png", "image/png", "<svg onload=alert(1)>".getBytes())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString("PNG ou JPEG seulement")));
        assertThat(stockage.nombreObjets()).isEqualTo(avant);
    }

    @Test
    void imageDePlusDe500Ko_400() throws Exception {
        byte[] lourde = Arrays.copyOf(ImagesModeleOutils.image(20, "png"), 500 * 1024 + 1);

        mockMvc.perform(multipart(ImagesModeleOutils.IMAGES).file(ImagesModeleOutils.fichier("lourd.png", "image/png", lourde)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString("500 Ko maximum")));
    }

    @Test
    @WithMockUser(roles = "Commercial")
    void depotParUnNonAdministrateur_403() throws Exception {
        mockMvc.perform(multipart(ImagesModeleOutils.IMAGES)
                        .file(ImagesModeleOutils.fichier("logo.png", "image/png", ImagesModeleOutils.image(20, "png"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void lectureDUneImageDeposee_octetsTypeEtCachePrive() throws Exception {
        byte[] png = ImagesModeleOutils.image(20, "png");
        String id = deposer("logo.png", "image/png", png).get("id").asString();

        mockMvc.perform(get(IMAGE, id))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(png))
                .andExpect(header().string("Cache-Control", containsString("private")));
    }

    @Test
    void lectureDUneImageInconnue_404() throws Exception {
        mockMvc.perform(get(IMAGE, UUID.randomUUID())).andExpect(status().isNotFound());
    }

    @Test
    void modeleAvecSrcEnData_400() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(modele("TST_IMG_SRC", "<p><img src=\"data:image/png;base64,iVBORw0KGgo=\"></p>")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString("src")));
    }

    @Test
    void modeleAvecImageInconnue_400() throws Exception {
        UUID inconnue = UUID.randomUUID();
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(modele("TST_IMG_INCONNUE", "<p><img data-image=\"" + inconnue + "\"></p>")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString("Image inconnue : " + inconnue)));
    }

    @Test
    void imageAbsenteDuStockage_500Explicite() throws Exception {
        ComFolder dossier = fixtureActe.dossier("IMG-ABSENTE", false, true);
        String balise = ImagesModeleOutils.deposerPng(mockMvc, json, 20);
        UUID imageId = UUID.fromString(balise.substring(balise.indexOf('"') + 1, balise.indexOf('"') + 37));
        String reponse = mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(modele("TST_IMG_ABSENTE", "<p>" + balise + "</p>")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        stockage.supprimer(imageRepository.findById(imageId).orElseThrow().getStorageKey());

        mockMvc.perform(get(BASE + "/{id}/generer", json.readTree(reponse).get("id").asString())
                        .param("ficheId", dossier.getId().toString()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath(DETAIL).value(containsString("Image " + imageId + " introuvable dans le stockage")));
    }
}
