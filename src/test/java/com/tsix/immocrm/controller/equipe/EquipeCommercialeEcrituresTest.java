package com.tsix.immocrm.controller.equipe;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.str.StrHistory;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.str.StrHistoryRepository;
import com.tsix.immocrm.services.equipe.RolesEquipe;
import com.tsix.immocrm.services.prospection.DiscriminantPreProspection;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Écritures de {@code /api/equipe/*} et leurs refus (FEATURE-equipe-commerciale.md §2). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EquipeCommercialeEcrituresTest {

    private static final String CHEF = RolesEquipe.CHEF;
    private static final String COMMERCIAL = RolesEquipe.COMMERCIAL;
    private static final String ADMIN = "Administrateur";

    @Autowired private MockMvc mockMvc;
    @Autowired private EquipeOutils outils;
    @Autowired private ComProspectionRepository prospectionRepository;
    @Autowired private StrHistoryRepository historyRepository;
    @Autowired private EntityManager em;

    private CfgProject projetA;
    private CfgProject projetB;
    private CfgProject projetC;
    private CfgTranche trancheA;
    private CfgTranche trancheB;
    private AdmUser chef;
    private AdmUser commercialA;
    private AdmUser commercialA2;
    private AdmUser commercialB;
    private AdmUser commercialC;
    private LocalDateTime hier;

    @BeforeEach
    void setUp() {
        projetA = outils.projet("EQ-ECR-A");
        projetB = outils.projet("EQ-ECR-B");
        projetC = outils.projet("EQ-ECR-C");
        trancheA = outils.tranche("EQ-ECR-TA", projetA);
        trancheB = outils.tranche("EQ-ECR-TB", projetB);
        hier = LocalDateTime.now().minusDays(1);

        chef = outils.compte("eq.ecr.chef", CHEF);
        outils.affecter(chef, projetA, projetC);
        commercialA = outils.compte("eq.ecr.coma", COMMERCIAL);
        outils.affecter(commercialA, projetA);
        commercialA2 = outils.compte("eq.ecr.coma2", COMMERCIAL);
        outils.affecter(commercialA2, projetA);
        commercialB = outils.compte("eq.ecr.comb", COMMERCIAL);
        outils.affecter(commercialB, projetB);
        commercialC = outils.compte("eq.ecr.comc", COMMERCIAL);
        outils.affecter(commercialC, projetC);
    }

    // ---------------------------------------------------------------- distribuer

    @Test
    void distribuer_poseLeCommercialSansTransformerNiExigerDeProspect() throws Exception {
        ComProspection pre = outils.preProspection("EQ-ECR-D1", trancheA, hier);
        pre.setCfgProspect(null);
        em.flush();

        distribuer(chef, List.of(pre.getId()), commercialA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value(1))
                .andExpect(jsonPath("$.lignes[0].code").value("EQ-ECR-D1"));

        em.flush();
        em.clear();
        ComProspection apres = prospectionRepository.findById(pre.getId()).orElseThrow();
        assertThat(apres.getCfgCommercial().getId()).isEqualTo(commercialA.getCfgEmployee().getId());
        assertThat(DiscriminantPreProspection.estPreProspection(apres)).isTrue();
        assertThat(historyRepository.findByObjectIdInOrderByDocDateDesc(List.of(pre.getId())))
                .extracting(StrHistory::getDescription)
                .anyMatch(d -> d.startsWith("Commercial assigné : "));
    }

    @Test
    void distribuer_redistributionTraceAncienEtNouveau() throws Exception {
        ComProspection pre = outils.preProspection("EQ-ECR-D7", trancheA, hier);
        pre.setCfgCommercial(commercialA.getCfgEmployee());
        em.flush();

        distribuer(chef, List.of(pre.getId()), commercialA2).andExpect(status().isOk());

        em.flush();
        assertThat(historyRepository.findByObjectIdInOrderByDocDateDesc(List.of(pre.getId())))
                .extracting(StrHistory::getDescription)
                .anyMatch(d -> d.startsWith("Commercial réaffecté : "));
    }

    @Test
    void distribuer_ligneCloturee_409() throws Exception {
        ComProspection pre = outils.preProspection("EQ-ECR-D8", trancheA, hier);
        pre.setIsClosed(true);
        em.flush();

        distribuer(chef, List.of(pre.getId()), commercialA).andExpect(status().isConflict());
    }

    @Test
    void distribuer_ligneDUnProjetHorsPerimetre_403() throws Exception {
        ComProspection pre = outils.preProspection("EQ-ECR-D2", trancheB, hier);

        distribuer(chef, List.of(pre.getId()), commercialA).andExpect(status().isForbidden());
    }

    @Test
    void distribuer_versUnCommercialHorsEquipe_409() throws Exception {
        ComProspection pre = outils.preProspection("EQ-ECR-D3", trancheA, hier);

        distribuer(chef, List.of(pre.getId()), commercialB).andExpect(status().isConflict());
    }

    @Test
    void distribuer_versUnCommercialNonAffecteAuProjetDeLaLigne_409() throws Exception {
        ComProspection pre = outils.preProspection("EQ-ECR-D4", trancheA, hier);

        distribuer(chef, List.of(pre.getId()), commercialC).andExpect(status().isConflict());
    }

    @Test
    void distribuer_lotPartiellementInvalide_rienNEstEcrit() throws Exception {
        ComProspection pre = outils.preProspection("EQ-ECR-D5", trancheA, hier);
        ComProspection dejaProspection = outils.prospection("EQ-ECR-D6", trancheA, commercialA.getCfgEmployee(), hier);

        distribuer(chef, List.of(pre.getId(), dejaProspection.getId()), commercialA2)
                .andExpect(status().isConflict());

        em.flush();
        em.clear();
        ComProspection apres = prospectionRepository.findById(pre.getId()).orElseThrow();
        assertThat(apres.getCfgCommercial()).isNull();
        assertThat(DiscriminantPreProspection.estPreProspection(apres)).isTrue();
    }

    @Test
    void distribuer_ligneInconnue_404() throws Exception {
        distribuer(chef, List.of(UUID.randomUUID()), commercialA).andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------- réaffecter

    @Test
    void reaffecter_changeLeCommercialEtTraceLHistorique() throws Exception {
        ComProspection p = outils.prospection("EQ-ECR-R1", trancheA, commercialA.getCfgEmployee(), hier);

        reaffecter(chef, List.of(p.getId()), commercialA2)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value(1));

        em.flush();
        em.clear();
        assertThat(prospectionRepository.findById(p.getId()).orElseThrow().getCfgCommercial().getId())
                .isEqualTo(commercialA2.getCfgEmployee().getId());
        assertThat(historyRepository.findByObjectIdInOrderByDocDateDesc(List.of(p.getId())))
                .extracting(StrHistory::getDescription)
                .anyMatch(d -> d.startsWith("Commercial réaffecté : "));
    }

    @Test
    void reaffecter_unePreProspection_409() throws Exception {
        ComProspection pre = outils.preProspection("EQ-ECR-R2", trancheA, hier);

        reaffecter(chef, List.of(pre.getId()), commercialA2).andExpect(status().isConflict());
    }

    @Test
    void reaffecter_uneProspectionCloturee_409() throws Exception {
        ComProspection p = outils.prospection("EQ-ECR-R3", trancheA, commercialA.getCfgEmployee(), hier);
        p.setIsClosed(true);
        em.flush();

        reaffecter(chef, List.of(p.getId()), commercialA2).andExpect(status().isConflict());
    }

    @Test
    void reaffecter_ligneHorsPerimetre_403() throws Exception {
        ComProspection p = outils.prospection("EQ-ECR-R4", trancheB, commercialB.getCfgEmployee(), hier);

        reaffecter(chef, List.of(p.getId()), commercialA).andExpect(status().isForbidden());
    }

    @Test
    void reaffecter_commercialNonAffecteAuProjetDeLaLigne_409() throws Exception {
        ComProspection p = outils.prospection("EQ-ECR-R5", trancheA, commercialA.getCfgEmployee(), hier);

        reaffecter(chef, List.of(p.getId()), commercialC).andExpect(status().isConflict());
    }

    // ---------------------------------------------------------------- affectations

    @Test
    void affectations_chefAjouteSonProjetAUnCommercial() throws Exception {
        affectations(chef, commercialB, "{\"ajouter\":[\"" + projetA.getId() + "\"]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projetIds[0]").value(projetA.getId().toString()));

        em.flush();
        assertThat(outils.projetsDe(commercialB)).containsExactlyInAnyOrder(projetA.getId(), projetB.getId());
    }

    @Test
    void affectations_projetQuiNEstPasAuChef_403() throws Exception {
        affectations(chef, commercialB, "{\"retirer\":[\"" + projetB.getId() + "\"]}")
                .andExpect(status().isForbidden());
    }

    @Test
    void affectations_retirerLeDernierProjet_409() throws Exception {
        affectations(chef, commercialA, "{\"retirer\":[\"" + projetA.getId() + "\"]}")
                .andExpect(status().isConflict());
    }

    @Test
    void affectations_cibleAutreChef_403() throws Exception {
        AdmUser autreChef = outils.compte("eq.ecr.chef2", CHEF, COMMERCIAL);
        outils.affecter(autreChef, projetA);

        affectations(chef, autreChef, "{\"ajouter\":[\"" + projetC.getId() + "\"]}")
                .andExpect(status().isForbidden());
    }

    @Test
    void affectations_cibleAdministrateur_403() throws Exception {
        AdmUser admin = outils.compte("eq.ecr.admin", ADMIN);

        affectations(chef, admin, "{\"ajouter\":[\"" + projetA.getId() + "\"]}")
                .andExpect(status().isForbidden());
    }

    @Test
    void affectations_compteInconnu_404() throws Exception {
        mockMvc.perform(put("/api/equipe/affectations/{id}", UUID.randomUUID())
                        .with(EquipeOutils.connecte(chef, CHEF))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ajouter\":[\"" + projetA.getId() + "\"]}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void affectations_administrateurPasseSurUnChef() throws Exception {
        AdmUser admin = outils.compte("eq.ecr.admin2", ADMIN);
        AdmUser autreChef = outils.compte("eq.ecr.chef3", CHEF, COMMERCIAL);
        outils.affecter(autreChef, projetA);

        affectations(admin, autreChef, "{\"ajouter\":[\"" + projetB.getId() + "\"]}", ADMIN)
                .andExpect(status().isOk());
    }

    // ---------------------------------------------------------------- outils

    private ResultActions distribuer(AdmUser appelant, List<UUID> ids, AdmUser destinataire) throws Exception {
        return mockMvc.perform(post("/api/equipe/distribuer")
                .with(EquipeOutils.connecte(appelant, CHEF))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"preProspectionIds\":" + json(ids) + ",\"commercialId\":\""
                        + destinataire.getCfgEmployee().getId() + "\"}"));
    }

    private ResultActions reaffecter(AdmUser appelant, List<UUID> ids, AdmUser destinataire) throws Exception {
        return mockMvc.perform(post("/api/equipe/reaffecter")
                .with(EquipeOutils.connecte(appelant, CHEF))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"prospectionIds\":" + json(ids) + ",\"commercialId\":\""
                        + destinataire.getCfgEmployee().getId() + "\",\"motif\":\"test\"}"));
    }

    private ResultActions affectations(AdmUser appelant, AdmUser cible, String corps) throws Exception {
        return affectations(appelant, cible, corps, CHEF);
    }

    private ResultActions affectations(AdmUser appelant, AdmUser cible, String corps, String role) throws Exception {
        return mockMvc.perform(put("/api/equipe/affectations/{id}", cible.getId())
                .with(EquipeOutils.connecte(appelant, role))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corps));
    }

    private static String json(List<UUID> ids) {
        return ids.stream().map(id -> "\"" + id + "\"").toList().toString();
    }
}
