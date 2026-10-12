package com.tsix.immocrm.controller.equipe;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.services.equipe.RolesEquipe;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Lectures de {@code /api/equipe/*} (FEATURE-equipe-commerciale.md §2, §4). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EquipeCommercialeLecturesTest {

    private static final String CHEF = RolesEquipe.CHEF;
    private static final String COMMERCIAL = RolesEquipe.COMMERCIAL;
    private static final String ADMIN = "Administrateur";

    @Autowired private MockMvc mockMvc;
    @Autowired private EquipeOutils outils;
    @Autowired private ComFolderRepository folderRepository;
    @Autowired private StkVocationRepository vocationRepository;
    @Autowired private EntityManager em;

    private CfgProject projetA;
    private CfgProject projetB;
    private CfgTranche trancheA;
    private CfgTranche trancheB;
    private AdmUser chef;
    private AdmUser commercialA;
    private AdmUser admin;

    @BeforeEach
    void setUp() {
        projetA = outils.projet("EQ-LEC-A");
        projetB = outils.projet("EQ-LEC-B");
        trancheA = outils.tranche("EQ-LEC-TA", projetA);
        trancheB = outils.tranche("EQ-LEC-TB", projetB);

        chef = outils.compte("eq.lec.chef", CHEF);
        outils.affecter(chef, projetA);
        commercialA = outils.compte("eq.lec.coma", COMMERCIAL);
        outils.affecter(commercialA, projetA);
        AdmUser commercialB = outils.compte("eq.lec.comb", COMMERCIAL);
        outils.affecter(commercialB, projetB);
        AdmUser commercialDesactive = outils.compte("eq.lec.off", outils.tier("EMP-OFF"), true, COMMERCIAL);
        outils.affecter(commercialDesactive, projetA);
        admin = outils.compte("eq.lec.admin", ADMIN);
    }

    @Test
    void commercialSansRoleChef_403() throws Exception {
        mockMvc.perform(get("/api/equipe/commerciaux").with(EquipeOutils.connecte(commercialA, COMMERCIAL)))
                .andExpect(status().isForbidden());
    }

    @Test
    void chef_commerciaux_seulementSesProjetsEtComptesActifs() throws Exception {
        mockMvc.perform(get("/api/equipe/commerciaux").with(EquipeOutils.connecte(chef, CHEF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code", hasItem("eq.lec.coma")))
                .andExpect(jsonPath("$[*].code", not(hasItem("eq.lec.comb"))))
                .andExpect(jsonPath("$[*].code", not(hasItem("eq.lec.off"))));
    }

    @Test
    void chef_projetHorsPerimetre_403() throws Exception {
        mockMvc.perform(get("/api/equipe/entonnoir").param("projet", projetB.getId().toString())
                        .with(EquipeOutils.connecte(chef, CHEF)))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_commerciaux_voitTousLesCommerciauxActifs() throws Exception {
        mockMvc.perform(get("/api/equipe/commerciaux").with(EquipeOutils.connecte(admin, ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code", hasItem("eq.lec.coma")))
                .andExpect(jsonPath("$[*].code", hasItem("eq.lec.comb")))
                .andExpect(jsonPath("$[*].code", not(hasItem("eq.lec.off"))));
    }

    @Test
    void chef_portefeuille_compteOuvertesEtSansAction() throws Exception {
        LocalDateTime ilYa10Jours = LocalDateTime.now().minusDays(10);
        outils.prospection("EQ-LEC-P1", trancheA, commercialA.getCfgEmployee(), ilYa10Jours);
        ComProspection close = outils.prospection("EQ-LEC-P2", trancheA, commercialA.getCfgEmployee(), ilYa10Jours);
        close.setIsClosed(true);
        // Ligne d'un autre projet : hors du périmètre du chef.
        outils.prospection("EQ-LEC-P3", trancheB, commercialA.getCfgEmployee(), ilYa10Jours);
        em.flush();

        mockMvc.perform(get("/api/equipe/commerciaux").with(EquipeOutils.connecte(chef, CHEF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code == 'eq.lec.coma')].portefeuilleOuvert").value(1))
                .andExpect(jsonPath("$[?(@.code == 'eq.lec.coma')].sansActionDepuis7Jours").value(1))
                .andExpect(jsonPath("$[?(@.code == 'eq.lec.coma')].relancesEnRetard").value(0))
                .andExpect(jsonPath("$[?(@.code == 'eq.lec.coma')].projets[0].code").value("EQ-LEC-A"));
    }

    @Test
    void chef_entonnoir_cohorteDeSesProjets() throws Exception {
        LocalDateTime arrivee = LocalDateTime.now().minusDays(4);
        outils.preProspection("EQ-LEC-L1", trancheA, arrivee);
        outils.prospection("EQ-LEC-L2", trancheA, commercialA.getCfgEmployee(), arrivee);
        ComProspection avecDossier = outils.prospection("EQ-LEC-L3", trancheA, commercialA.getCfgEmployee(), arrivee);
        dossier("EQ-LEC-D3", avecDossier, arrivee.plusDays(2));
        outils.preProspection("EQ-LEC-L4", trancheB, arrivee);
        em.flush();

        mockMvc.perform(get("/api/equipe/entonnoir").with(EquipeOutils.connecte(chef, CHEF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapes[0].code").value("ARRIVES"))
                .andExpect(jsonPath("$.etapes[0].nombre").value(3))
                .andExpect(jsonPath("$.etapes[1].nombre").value(2))
                .andExpect(jsonPath("$.etapes[1].tauxPassage").value(0.667))
                .andExpect(jsonPath("$.etapes[4].nombre").value(1))
                .andExpect(jsonPath("$.etapes[5].code").value("DOSSIERS"))
                .andExpect(jsonPath("$.etapes[5].nombre").value(1))
                .andExpect(jsonPath("$.delaiMedianJours").value(2));
    }

    @Test
    void chef_aDistribuer_preProspectionsSansCommercialDeSesProjets() throws Exception {
        outils.preProspection("EQ-LEC-A1", trancheA, LocalDateTime.now().minusDays(9));
        outils.preProspection("EQ-LEC-A2", trancheB, LocalDateTime.now().minusDays(20));
        em.flush();

        mockMvc.perform(get("/api/equipe/a-distribuer").with(EquipeOutils.connecte(chef, CHEF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value(1))
                .andExpect(jsonPath("$.agePlusAncienneJours").value(9));
    }

    @Test
    void chef_affectations_equipeEtAffectables() throws Exception {
        AdmUser autreChef = outils.compte("eq.lec.chef2", CHEF, COMMERCIAL);
        outils.affecter(autreChef, projetB);
        em.flush();

        mockMvc.perform(get("/api/equipe/affectations").with(EquipeOutils.connecte(chef, CHEF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projets[*].code", hasItem("EQ-LEC-A")))
                .andExpect(jsonPath("$.projets[*].code", not(hasItem("EQ-LEC-B"))))
                .andExpect(jsonPath("$.commerciaux[?(@.code == 'eq.lec.coma')].projetIds[0]").value(projetA.getId().toString()))
                .andExpect(jsonPath("$.affectables[*].code", hasItem("eq.lec.comb")))
                .andExpect(jsonPath("$.affectables[*].code", not(hasItem("eq.lec.chef2"))));
    }

    private void dossier(String code, ComProspection prospection, LocalDateTime date) {
        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-" + code);
        vocationRepository.save(vocation);

        ComFolder dossier = new ComFolder();
        dossier.setId(UUID.randomUUID());
        dossier.setCode(code);
        dossier.setStkVocation(vocation);
        dossier.setDocDate(date);
        dossier.setComProspection(prospection);
        folderRepository.save(dossier);
    }
}
