package com.tsix.immocrm.controller.equipe;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.services.equipe.RolesEquipe;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Chemins de refus de FEATURE-equipe-commerciale.md §2 non couverts par les tests de l'implémentation (revue). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EquipeCommercialeRevueTest {

    private static final String CHEF = RolesEquipe.CHEF;
    private static final String COMMERCIAL = RolesEquipe.COMMERCIAL;

    @Autowired private MockMvc mockMvc;
    @Autowired private EquipeOutils outils;
    @Autowired private ComProspectionRepository prospectionRepository;
    @Autowired private CfgTierRepository tierRepository;
    @Autowired private EntityManager em;

    private CfgProject projetA;
    private CfgProject projetC;
    private CfgTranche trancheA;
    private AdmUser chef;
    private AdmUser commercialA;
    private AdmUser commercialA2;
    private AdmUser commercialB;
    private LocalDateTime hier;

    @BeforeEach
    void setUp() {
        projetA = outils.projet("EQ-REV-A");
        CfgProject projetB = outils.projet("EQ-REV-B");
        projetC = outils.projet("EQ-REV-C");
        trancheA = outils.tranche("EQ-REV-TA", projetA);
        hier = LocalDateTime.now().minusDays(1);

        chef = outils.compte("eq.rev.chef", CHEF);
        outils.affecter(chef, projetA, projetC);
        commercialA = outils.compte("eq.rev.coma", COMMERCIAL);
        outils.affecter(commercialA, projetA);
        commercialA2 = outils.compte("eq.rev.coma2", COMMERCIAL);
        outils.affecter(commercialA2, projetA);
        commercialB = outils.compte("eq.rev.comb", COMMERCIAL);
        outils.affecter(commercialB, projetB);
    }

    @Test
    void affectations_cibleDirectionGeneraleAvecRoleCommercial_403() throws Exception {
        AdmUser dg = outils.compte("eq.rev.dg", "Direction Générale", COMMERCIAL);
        outils.affecter(dg, projetA);

        affectations(dg, "{\"ajouter\":[\"" + projetC.getId() + "\"]}").andExpect(status().isForbidden());
    }

    @Test
    void affectations_cibleSansAucuneRestriction_403() throws Exception {
        AdmUser libre = outils.compte("eq.rev.libre", COMMERCIAL);

        affectations(libre, "{\"ajouter\":[\"" + projetA.getId() + "\"]}").andExpect(status().isForbidden());
        em.flush();
        assertThat(outils.projetsDe(libre)).isEmpty();
    }

    @Test
    void affectations_commercialAideSansRoleChef_403() throws Exception {
        AdmUser aide = outils.compte("eq.rev.aide", RolesEquipe.COMMERCIAL_AIDE);
        outils.affecter(aide, projetA);

        mockMvc.perform(put("/api/equipe/affectations/{id}", commercialA.getId())
                        .with(EquipeOutils.connecte(aide, RolesEquipe.COMMERCIAL_AIDE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ajouter\":[\"" + projetC.getId() + "\"]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void aDistribuer_projetHorsPerimetre_403() throws Exception {
        mockMvc.perform(get("/api/equipe/a-distribuer").param("projet", UUID.randomUUID().toString())
                        .with(EquipeOutils.connecte(chef, CHEF)))
                .andExpect(status().isForbidden());
    }

    @Test
    void reaffecter_versUnCommercialHorsEquipe_409() throws Exception {
        ComProspection p = outils.prospection("EQ-REV-R1", trancheA, commercialA.getCfgEmployee(), hier);

        reaffecter(List.of(p.getId()), commercialB).andExpect(status().isConflict());
    }

    @Test
    void reaffecter_lotPartiellementInvalide_rienNEstEcrit() throws Exception {
        ComProspection valide = outils.prospection("EQ-REV-R2", trancheA, commercialA.getCfgEmployee(), hier);
        ComProspection close = outils.prospection("EQ-REV-R3", trancheA, commercialA.getCfgEmployee(), hier);
        close.setIsClosed(true);
        em.flush();

        reaffecter(List.of(valide.getId(), close.getId()), commercialA2).andExpect(status().isConflict());

        em.flush();
        em.clear();
        assertThat(prospectionRepository.findById(valide.getId()).orElseThrow().getCfgCommercial().getId())
                .isEqualTo(commercialA.getCfgEmployee().getId());
    }

    @Test
    void reaffecter_neTouchePasLeCommercialReferentDuProspect() throws Exception {
        ComProspection p = outils.prospection("EQ-REV-R4", trancheA, commercialA.getCfgEmployee(), hier);
        CfgTier prospect = p.getCfgProspect();
        prospect.setCfgCommercial(commercialA.getCfgEmployee());
        tierRepository.save(prospect);
        em.flush();

        reaffecter(List.of(p.getId()), commercialA2).andExpect(status().isOk());

        em.flush();
        em.clear();
        assertThat(tierRepository.findById(prospect.getId()).orElseThrow().getCfgCommercial().getId())
                .isEqualTo(commercialA.getCfgEmployee().getId());
    }

    /** {ajouter:[C], retirer:[C]} : la réponse dit « pas C » mais la base reçoit C. */
    @Test
    void affectations_ajouterEtRetirerLeMemeProjet_laBaseConcordeAvecLaReponse() throws Exception {
        affectations(commercialA, "{\"ajouter\":[\"" + projetC.getId() + "\"],\"retirer\":[\"" + projetC.getId() + "\"]}");

        em.flush();
        assertThat(outils.projetsDe(commercialA)).containsExactly(projetA.getId());
    }

    /** Swagger annonce 400 pour `du` > `au` ; une équipe vide répond 200 []. */
    @Test
    void commerciaux_periodeInverseeEquipeVide_400() throws Exception {
        AdmUser chefSansProjet = outils.compte("eq.rev.chef0", CHEF);

        mockMvc.perform(get("/api/equipe/commerciaux").param("du", "2026-10-10").param("au", "2026-10-01")
                        .with(EquipeOutils.connecte(chefSansProjet, CHEF)))
                .andExpect(status().isBadRequest());
    }

    private ResultActions affectations(AdmUser cible, String corps) throws Exception {
        return mockMvc.perform(put("/api/equipe/affectations/{id}", cible.getId())
                .with(EquipeOutils.connecte(chef, CHEF))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corps));
    }

    private ResultActions reaffecter(List<UUID> ids, AdmUser destinataire) throws Exception {
        String json = ids.stream().map(id -> "\"" + id + "\"").toList().toString();
        return mockMvc.perform(post("/api/equipe/reaffecter")
                .with(EquipeOutils.connecte(chef, CHEF))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"prospectionIds\":" + json + ",\"commercialId\":\""
                        + destinataire.getCfgEmployee().getId() + "\"}"));
    }
}
