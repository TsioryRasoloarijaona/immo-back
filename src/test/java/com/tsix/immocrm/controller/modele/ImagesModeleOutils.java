package com.tsix.immocrm.controller.modele;

import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Images de test des modèles : génération et dépôt par {@code POST /api/modeles-documents/images}. */
final class ImagesModeleOutils {

    static final String IMAGES = "/api/modeles-documents/images";

    private ImagesModeleOutils() {
    }

    /** Image de bruit (incompressible) au format {@code png} ou {@code jpg} : {@code cote}² × 3 octets environ en PNG. */
    static byte[] image(int cote, String format) throws IOException {
        Random hasard = new Random(cote);
        BufferedImage image = new BufferedImage(cote, cote, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < cote; x++) {
            for (int y = 0; y < cote; y++) {
                image.setRGB(x, y, hasard.nextInt(0xFFFFFF));
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, format, out);
        return out.toByteArray();
    }

    static MockMultipartFile fichier(String nom, String contentType, byte[] octets) {
        return new MockMultipartFile("fichier", nom, contentType, octets);
    }

    /** Dépose un PNG et renvoie la balise qui le cite. */
    static String deposerPng(MockMvc mockMvc, JsonMapper json, int cote) throws Exception {
        String reponse = mockMvc.perform(multipart(IMAGES).file(fichier("logo.png", "image/png", image(cote, "png"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return "<img data-image=\"" + json.readTree(reponse).get("id").asString() + "\" alt=\"logo\">";
    }
}
