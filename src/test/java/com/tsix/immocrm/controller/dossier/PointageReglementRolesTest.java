package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pointer ou annuler un règlement est réservé aux rôles du droit OMRAN CanAcceptDocument : FEATURE-pointage-reglement-role.md. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PointageReglementRolesTest {

    private static final String STATUT = "/api/dossiers/{id}/reglements/{reglementId}/statut";
    private static final String EN_COURS = "IN_WORK";

    @Autowired private MockMvc mockMvc;
    @Autowired private ComFolderRepository folderRepository;
    @Autowired private ComDocumentRepository documentRepository;
    @Autowired private StkVocationRepository stkVocationRepository;
    @Autowired private StlSettlementRepository settlementRepository;
    @Autowired private StlSettlementLineRepository settlementLineRepository;

    private ComFolder dossier;
    private StlSettlement reglement;

    @BeforeEach
    void reglementEnCours() {
        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-PTG-" + UUID.randomUUID());
        stkVocationRepository.save(vocation);

        dossier = new ComFolder();
        dossier.setId(UUID.randomUUID());
        dossier.setCode("DOS-PTG-" + UUID.randomUUID().toString().substring(0, 8));
        dossier.setStkVocation(vocation);
        folderRepository.save(dossier);

        ComDocument echeance = new ComDocument();
        echeance.setId(UUID.randomUUID());
        echeance.setComFolder(dossier);
        echeance.setAmount(new BigDecimal("100000"));
        documentRepository.save(echeance);

        reglement = new StlSettlement();
        reglement.setId(UUID.randomUUID());
        reglement.setCode("REG-PTG-" + UUID.randomUUID());
        reglement.setDocDate(LocalDateTime.of(2026, 9, 1, 10, 0));
        reglement.setAmount(new BigDecimal("50000"));
        reglement.setStatus(EN_COURS);
        reglement.setIsCredit(true);
        settlementRepository.save(reglement);

        StlSettlementLine ligne = new StlSettlementLine();
        ligne.setId(UUID.randomUUID());
        ligne.setStlSettlement(reglement);
        ligne.setComDocument(echeance);
        ligne.setAmount(new BigDecimal("50000"));
        settlementLineRepository.save(ligne);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Commercial", "Commercial Aide", "Comptabilité", "Assistante Commerciale", "USER"})
    void horsDroitDePointage_returns403_etLeReglementNeBougePas(String role) throws Exception {
        mockMvc.perform(patch(STATUT, dossier.getId(), reglement.getId())
                        .with(user("sans-droit").roles(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statut\":\"POINTED\",\"dateReglement\":\"2026-09-05T09:00:00\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch(STATUT, dossier.getId(), reglement.getId())
                        .with(user("sans-droit").roles(role))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"statut\":\"CANCELED\"}"))
                .andExpect(status().isForbidden());

        StlSettlement relu = settlementRepository.findById(reglement.getId()).orElseThrow();
        assertThat(relu.getStatus()).isEqualTo(EN_COURS);
        assertThat(relu.getSettlementDate()).isNull();
    }

    /** Le refus passe avant la recherche : un règlement inconnu ne doit pas répondre 404 à un rôle sans droit. */
    @ParameterizedTest
    @ValueSource(strings = {"Commercial", "USER"})
    void horsDroit_reglementInconnu_returns403PasLe404(String role) throws Exception {
        mockMvc.perform(patch(STATUT, dossier.getId(), UUID.randomUUID())
                        .with(user("sans-droit").roles(role))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"statut\":\"CANCELED\"}"))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Administrateur", "Commercial Chef", "Direction Générale"})
    void avecLeDroit_annulationAcceptee(String role) throws Exception {
        mockMvc.perform(patch(STATUT, dossier.getId(), reglement.getId())
                        .with(user("siege").roles(role))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"statut\":\"CANCELED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("CANCELED"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Commercial Chef", "Direction Générale"})
    void avecLeDroit_pointageAtteintLesControlesMetier(String role) throws Exception {
        // Sans preuve de paiement : 409 métier, preuve que la garde de rôle a laissé passer.
        mockMvc.perform(patch(STATUT, dossier.getId(), reglement.getId())
                        .with(user("siege").roles(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statut\":\"POINTED\",\"dateReglement\":\"2026-09-05T09:00:00\"}"))
                .andExpect(status().isConflict());
    }
}
