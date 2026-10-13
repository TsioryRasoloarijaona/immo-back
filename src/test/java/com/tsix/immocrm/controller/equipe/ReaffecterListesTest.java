package com.tsix.immocrm.controller.equipe;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.services.equipe.RolesEquipe;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Listes lues par la fenêtre « Réaffecter » : commercial, période, projet, plus récentes d'abord (FEATURE-reaffecter-filtres.md §2). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReaffecterListesTest {

    private static final String CHEF = RolesEquipe.CHEF;

    @Autowired private MockMvc mockMvc;
    @Autowired private EquipeOutils outils;
    @Autowired private EntityManager em;

    private CfgProject projetA;
    private CfgProject projetC;
    private AdmUser chef;
    private AdmUser commercial;

    @BeforeEach
    void setUp() {
        projetA = outils.projet("EQ-RLI-A");
        projetC = outils.projet("EQ-RLI-C");
        CfgTranche trancheA = outils.tranche("EQ-RLI-TA", projetA);
        CfgTranche trancheC = outils.tranche("EQ-RLI-TC", projetC);
        chef = outils.compte("eq.rli.chef", CHEF);
        outils.affecter(chef, projetA, projetC);
        commercial = outils.compte("eq.rli.com", RolesEquipe.COMMERCIAL);
        outils.affecter(commercial, projetA, projetC);

        LocalDateTime maintenant = LocalDateTime.now();
        affectee("EQ-RLI-ANCIENNE", trancheA, maintenant.minusDays(5));
        affectee("EQ-RLI-RECENTE", trancheA, maintenant.minusDays(1));
        affectee("EQ-RLI-AUTRE-PROJET", trancheC, maintenant.minusDays(2));
        affectee("EQ-RLI-HORS-PERIODE", trancheA, maintenant.minusDays(60));
        em.flush();
    }

    @Test
    void preProspections_filtreesParProjetEtPeriode_plusRecentesDAbord() throws Exception {
        lister(projetA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].code", contains("EQ-RLI-RECENTE", "EQ-RLI-ANCIENNE")));
    }

    @Test
    void preProspections_sansProjet_tousLesProjetsDeLaPeriode() throws Exception {
        lister(null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].code",
                        contains("EQ-RLI-RECENTE", "EQ-RLI-AUTRE-PROJET", "EQ-RLI-ANCIENNE")));
    }

    private void affectee(String code, CfgTranche tranche, LocalDateTime docDate) {
        ComProspection pre = outils.preProspection(code, tranche, docDate);
        pre.setCfgCommercial(commercial.getCfgEmployee());
    }

    private ResultActions lister(CfgProject projet) throws Exception {
        var requete = get("/api/pre-prospections")
                .with(EquipeOutils.connecte(chef, CHEF))
                .param("commercial", commercial.getCfgEmployee().getId().toString())
                .param("ouvertes", "true")
                .param("dateDebut", LocalDateTime.now().minusDays(30).toString())
                .param("sort", "docDate,desc");
        if (projet != null) {
            requete.param("projetId", projet.getId().toString());
        }
        return mockMvc.perform(requete);
    }
}
