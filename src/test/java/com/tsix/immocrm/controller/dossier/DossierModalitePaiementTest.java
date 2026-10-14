package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComDocumentPaymentMethod;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.repositories.com.ComDocumentPaymentMethodRepository;
import com.tsix.immocrm.services.dossier.modalite.ModalitePaiementService;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Modalité de paiement : copie du schéma, instantané au statut effectif (FEATURE-vente-echeancier.md §2). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class DossierModalitePaiementTest {

    private static final String MODALITE = "/api/dossiers/{id}/modalite-paiement";

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private FixtureDossierVente fixture;
    @Autowired private ComDocumentPaymentMethodRepository modaliteRepository;
    @Autowired private ModalitePaiementService modaliteService;

    private ComFolder dossier;
    private ComSettlementCategory avance;

    @BeforeEach
    void dossierAvecSchema() {
        dossier = fixture.dossier("DOS-MODAL", fixture.tranche("TR-MODAL", true), StatutDossier.RESERVATION);
        avance = fixture.categorieEcheance("AVANCE-MODAL", true, false, false, null);
        ComSettlementCategory autofinancement = fixture.categorieEcheance("AUTOFIN-MODAL", false, true, false, null);
        ComDocument a = fixture.echeance(dossier, avance, new BigDecimal("300000"), LocalDateTime.of(2026, 10, 1, 0, 0));
        a.setVATRatio(new BigDecimal("0.2"));
        a.setDiscountRatio(BigDecimal.ZERO);
        fixture.echeance(dossier, autofinancement, new BigDecimal("700000"), LocalDateTime.of(2027, 3, 1, 0, 0));
    }

    @Test
    void copierSchema_uneLigneParEcheance_HTRecalcule() throws Exception {
        mockMvc.perform(post(MODALITE + "/copier-schema", dossier.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].montant").value(300000))
                .andExpect(jsonPath("$[1].montantHT").value(250000));
    }

    @Test
    void instantane_statutEffectif_copieUneSeuleFois() {
        entityManager.flush();
        entityManager.createNativeQuery("UPDATE StrStatus SET BoolField001 = 1 WHERE PKey = :id")
                .setParameter("id", dossier.getStrStatus().getId()).executeUpdate();

        boolean premier = modaliteService.instantaneSiEffectif(dossier);
        boolean second = modaliteService.instantaneSiEffectif(dossier);

        List<ComDocumentPaymentMethod> lignes = modaliteRepository.findByComFolderIdOrderByDocDateDesc(dossier.getId());
        assertAll(
                () -> assertThat(premier).isTrue(),
                () -> assertThat(second).isFalse(),
                () -> assertThat(lignes).hasSize(2));
    }

    @Test
    void instantane_statutNonEffectif_rienNEstCopie() {
        entityManager.flush();
        entityManager.createNativeQuery("UPDATE StrStatus SET BoolField001 = 0 WHERE PKey = :id")
                .setParameter("id", dossier.getStrStatus().getId()).executeUpdate();

        assertAll(
                () -> assertThat(modaliteService.instantaneSiEffectif(dossier)).isFalse(),
                () -> assertThat(modaliteRepository.findByComFolderIdOrderByDocDateDesc(dossier.getId())).isEmpty());
    }

    @Test
    void ajoutPuisSuppression() throws Exception {
        String json = mockMvc.perform(post(MODALITE, dossier.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"2026-12-01T00:00:00\",\"categorieId\":\"%s\",\"montant\":1200}"
                                .formatted(avance.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].categorie").value("AVANCE-MODAL"))
                .andReturn().getResponse().getContentAsString();
        String ligneId = com.jayway.jsonpath.JsonPath.read(json, "$[0].id");

        mockMvc.perform(delete(MODALITE + "/{l}", dossier.getId(), ligneId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void ligneInconnue_404() throws Exception {
        mockMvc.perform(delete(MODALITE + "/{l}", dossier.getId(), UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
