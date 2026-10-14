package com.tsix.immocrm.controller.dossier;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComGift;
import com.tsix.immocrm.entities.com.ComGiftReason;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.repositories.com.ComGiftReasonRepository;
import com.tsix.immocrm.repositories.com.ComGiftRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Cadeaux : date de livraison obligatoire, prix et stock du catalogue, droit toolAddDeleteGifts (FEATURE-vente-echeancier.md §2). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "Commercial Chef")
class DossierCadeauxTest {

    private static final String CADEAUX = "/api/dossiers/{id}/cadeaux";

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private FixtureDossierVente fixture;
    @Autowired private ComGiftRepository catalogueRepository;
    @Autowired private ComGiftReasonRepository raisonRepository;

    private ComFolder dossier;
    private ComGift refrigerateur;
    private ComGiftReason ancienClient;
    private ComGiftReason parrainage;

    /** Catalogue réel de yesRemsProd : « Réfrigérateur » à 5 000, deux en stock. */
    @BeforeEach
    void catalogue() {
        dossier = fixture.dossier("DOS-CADEAU", fixture.tranche("TR-CADEAU", true), StatutDossier.RESERVATION);
        refrigerateur = new ComGift();
        refrigerateur.setId(UUID.randomUUID());
        refrigerateur.setCode("Réfrigérateur-TST");
        refrigerateur.setDescription("Réfrigérateur");
        refrigerateur.setPrice(new BigDecimal("5000"));
        refrigerateur = catalogueRepository.save(refrigerateur);
        ancienClient = raison("Ancien client");
        parrainage = raison("Parrainage");
        entityManager.flush();
        entityManager.createNativeQuery("INSERT INTO ComGiftQuantity (PKey, ComGiftId, Quantity) VALUES (:id, :cadeau, 2)")
                .setParameter("id", UUID.randomUUID()).setParameter("cadeau", refrigerateur.getId()).executeUpdate();
    }

    @Test
    void ajout_prixEtDisponibiliteDuCatalogue() throws Exception {
        mockMvc.perform(post(CADEAUX, dossier.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(corps(refrigerateur.getId(), ancienClient.getId(), false, null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].prix").value(5000))
                .andExpect(jsonPath("$[0].disponible").value(true))
                .andExpect(jsonPath("$[0].raison").value("Ancien client"));
    }

    @Test
    void livreSansDateDeLivraison_400() throws Exception {
        mockMvc.perform(post(CADEAUX, dossier.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(corps(refrigerateur.getId(), ancienClient.getId(), true, null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("date de livraison")));
    }

    @Test
    void livreAvecDate_ok() throws Exception {
        mockMvc.perform(post(CADEAUX, dossier.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(corps(refrigerateur.getId(), ancienClient.getId(), true, "\"2026-11-02T10:00:00\"")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].livre").value(true))
                .andExpect(jsonPath("$[0].dateLivraison").value("2026-11-02T00:00:00"));
    }

    @Test
    @WithMockUser(roles = "Commercial")
    void ajout_sansLeDroit_403() throws Exception {
        mockMvc.perform(post(CADEAUX, dossier.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(corps(refrigerateur.getId(), ancienClient.getId(), false, null)))
                .andExpect(status().isForbidden());
    }

    @Test
    void modification_changementDeRaisonSansLeDroit_403_sinonOk() throws Exception {
        String json = mockMvc.perform(post(CADEAUX, dossier.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(corps(refrigerateur.getId(), ancienClient.getId(), false, null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String cadeauId = JsonPath.read(json, "$[0].id");

        mockMvc.perform(put(CADEAUX + "/{c}", dossier.getId(), cadeauId)
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                .user("commercial").roles("Commercial"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps(refrigerateur.getId(), parrainage.getId(), false, null)))
                .andExpect(status().isForbidden());
        mockMvc.perform(put(CADEAUX + "/{c}", dossier.getId(), cadeauId)
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                .user("commercial").roles("Commercial"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps(refrigerateur.getId(), ancienClient.getId(), true, "\"2026-12-01T00:00:00\"")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].livre").value(true));
        mockMvc.perform(delete(CADEAUX + "/{c}", dossier.getId(), cadeauId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void cadeauInconnu_404() throws Exception {
        mockMvc.perform(delete(CADEAUX + "/{c}", dossier.getId(), UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    private static String corps(UUID cadeau, UUID raison, boolean livre, String dateLivraison) {
        return """
                {"cadeauId":"%s","raisonId":"%s","livre":%s,"dateLivraison":%s}"""
                .formatted(cadeau, raison, livre, dateLivraison);
    }

    private ComGiftReason raison(String code) {
        ComGiftReason r = new ComGiftReason();
        r.setId(UUID.randomUUID());
        r.setCode(code + "-" + UUID.randomUUID().toString().substring(0, 6));
        r.setDescription(code);
        return raisonRepository.save(r);
    }
}
