package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stl.StlSettlementType;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementTypeRepository;
import com.tsix.immocrm.services.dossier.ParametresVente;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code FEATURE-impayes.md} P4 sur la vraie base de test : {@code AdmParamsDecimal} y manque, {@code RecoveryAge}
 * et {@code DefaultComPenaltyDocumentType} n'y sont pas semés — défauts, sans erreur SQL.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ParametresVenteTest {

    @Autowired private ParametresVente parametres;
    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDossierVente fixture;
    @Autowired private ComDocumentRepository documentRepository;
    @Autowired private StlSettlementTypeRepository settlementTypeRepository;
    @Autowired private EntityManager em;
    @Autowired private Clock clock;

    @Test
    void parametresAbsents_valeursParDefaut() {
        assertAll(
                () -> assertThat(parametres.tauxPenalite()).isEqualByComparingTo("0"),
                () -> assertThat(parametres.categoriePenalite()).isEmpty(),
                () -> assertThat(parametres.ageRecouvrement()).isEqualTo(90));
    }

    @Test
    void reglementSansParametresDePenalite_aucunePenaliteNiErreur() throws Exception {
        String code = "PAR" + UUID.randomUUID().toString().substring(0, 8);
        ComFolder dossier = fixture.dossier(code, fixture.tranche(code, true), StatutDossier.FORMALISE);
        ComSettlementCategory avance = fixture.categorieEcheance("AVANCE-" + code, true, false, false, null);
        avance.setIsWithPenalty(true);
        ComDocument echeance = fixture.echeance(dossier, avance, new BigDecimal("12000"),
                LocalDate.now(clock).minusDays(30).atStartOfDay());
        StlSettlementType type = new StlSettlementType();
        type.setId(UUID.randomUUID());
        type.setCode("VIR-" + code);
        settlementTypeRepository.save(type);
        long avant = documentRepository.count();

        mockMvc.perform(post("/api/dossiers/{id}/echeancier/{echeanceId}/reglements", dossier.getId(), echeance.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montant\":1000,\"dateEcheance\":\"2026-09-01T10:00:00\",\"typeId\":\""
                                + type.getId() + "\"}"))
                .andExpect(status().isCreated());

        em.flush();
        em.clear();
        assertThat(documentRepository.findById(echeance.getId()).orElseThrow().getComPenaltyDocument()).isNull();
        assertThat(documentRepository.count()).isEqualTo(avant);
    }
}
