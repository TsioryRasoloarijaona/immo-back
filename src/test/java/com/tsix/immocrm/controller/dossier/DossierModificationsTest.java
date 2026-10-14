package com.tsix.immocrm.controller.dossier;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Modifications : {@code ModificationsAmount(HT)}, prix avec modifications (FEATURE-vente-echeancier.md §2). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class DossierModificationsTest {

    private static final String MODIFICATIONS = "/api/dossiers/{id}/modifications";
    private static final String ACCEPTEE = """
            {"description":"Cloison","prixHT":10000,"prixTTC":12000,"cout":8000,"etat":"ACCEPTEE"}""";

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private FixtureDossierVente fixture;
    @Autowired private ComFolderRepository folderRepository;

    private ComFolder dossier;

    @BeforeEach
    void dossier() {
        dossier = fixture.dossier("DOS-MODIF", fixture.tranche("TR-MODIF", true), StatutDossier.RESERVATION);
        dossier.setPriceAfterDiscount(new BigDecimal("1000000"));
        dossier.setPriceAfterDiscountHT(new BigDecimal("833333.33"));
        dossier.setPrice(new BigDecimal("1000000"));
        dossier = folderRepository.save(dossier);
    }

    @Test
    void acceptee_metAJourModificationsAmount_prixInchangeHorsIntegration() throws Exception {
        mockMvc.perform(post(MODIFICATIONS, dossier.getId()).contentType(MediaType.APPLICATION_JSON).content(ACCEPTEE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.montantTTC").value(12000))
                .andExpect(jsonPath("$.ecartSchema").value(12000))
                .andExpect(jsonPath("$.modifications[0].etatLibelle").value("Acceptée"));

        ComFolder f = relire();
        assertAll(
                () -> assertThat(f.getModificationsAmount()).isEqualByComparingTo("12000"),
                () -> assertThat(f.getModificationsAmountHT()).isEqualByComparingTo("10000"),
                () -> assertThat(f.getPrice()).isEqualByComparingTo("1000000"));
    }

    @Test
    void integreeAuPrix_prixDeVenteInclutLesModifications() throws Exception {
        dossier.setPriceWithModification(true);
        folderRepository.save(dossier);

        mockMvc.perform(post(MODIFICATIONS, dossier.getId()).contentType(MediaType.APPLICATION_JSON).content(ACCEPTEE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ecartSchema").doesNotExist());

        ComFolder f = relire();
        assertAll(
                () -> assertThat(f.getPrice()).isEqualByComparingTo("1012000"),
                () -> assertThat(f.getPriceHT()).isEqualByComparingTo("843333.33"));
    }

    @Test
    void enCours_nonCompte_puisSuppression() throws Exception {
        String json = mockMvc.perform(post(MODIFICATIONS, dossier.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Carrelage\",\"prixTTC\":5000}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.montantTTC").value(0))
                .andExpect(jsonPath("$.modifications[0].etat").value("ENCOURS"))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(json, "$.modifications[0].id");

        mockMvc.perform(put(MODIFICATIONS + "/{m}", dossier.getId(), id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Carrelage\",\"prixTTC\":5000,\"etat\":\"REALISED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montantTTC").value(5000));

        mockMvc.perform(delete(MODIFICATIONS + "/{m}", dossier.getId(), id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montantTTC").value(0));
        assertThat(relire().getModificationsAmount()).isEqualByComparingTo("0");
    }

    @Test
    void modificationInconnue_404() throws Exception {
        mockMvc.perform(delete(MODIFICATIONS + "/{m}", dossier.getId(), UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    private ComFolder relire() {
        entityManager.flush();
        entityManager.clear();
        return folderRepository.findById(dossier.getId()).orElseThrow();
    }
}
