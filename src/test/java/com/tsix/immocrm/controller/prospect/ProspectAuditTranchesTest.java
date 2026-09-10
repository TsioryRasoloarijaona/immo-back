package com.tsix.immocrm.controller.prospect;

import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTierType;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.com.ComProspectionProject;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierTypeRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComProspectionProjectRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Piste d'audit et tranches d'intérêt de la fiche prospect — voir
 * {@code instructions/FEATURE-audit-tranches-prospect.md}.
 *
 * <p><b>Le point sensible est le dédoublonnage.</b> La règle vient de la définition de
 * {@code ComSaleProspectView} : union <em>distincte</em> de la tranche principale de chaque
 * prospection et de ses « autres tranches ». Une même tranche présente des deux côtés ne doit
 * apparaître qu'une fois — c'est ce que {@link #memeTrancheDesDeuxCotes_neCompteQuUneFois()}
 * tient, et c'est ce qu'un {@code UNION ALL} sans {@code DISTINCT} casserait en silence.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ProspectAuditTranchesTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CfgTierRepository tierRepository;

    @Autowired
    private CfgTierTypeRepository tierTypeRepository;

    @Autowired
    private ComProspectionRepository prospectionRepository;

    @Autowired
    private ComProspectionProjectRepository prospectionProjectRepository;

    @Autowired
    private CfgTrancheRepository trancheRepository;

    @Autowired
    private CfgProjectRepository projectRepository;

    /**
     * Type {@code PRO}, sans lequel le tiers est <b>absent de {@code GET /api/prospects}</b> :
     * la liste restreint à {@code CfgTierType.Code} ∈ {@code (pro, prornt)}, la classification
     * qu'Omran utilise lui-même. La table est vide dans la base de test, d'où sa création ici.
     */
    private CfgTierType typeProspect() {
        CfgTierType type = new CfgTierType();
        type.setId(UUID.randomUUID());
        type.setCode("PRO");
        type.setDescription("PROSPECT");
        return tierTypeRepository.saveAndFlush(type);
    }

    private CfgTier prospect() {
        CfgTier t = new CfgTier();
        t.setId(UUID.randomUUID());
        t.setCode("AUD-TIER-" + UUID.randomUUID());
        t.setFirstName("Audit");
        t.setLastName("TEST");
        t.setCfgTierType(typeProspect());
        // Renseigné explicitement : en base réelle ces colonnes le sont sur ~100 % des lignes,
        // mais rien ne les pose côté test. Le test doit vérifier qu'elles sont servies.
        t.setUserCreate("createur-test");
        t.setUserCreateDateTime(LocalDateTime.now());
        t.setUserUpdate("modificateur-test");
        t.setUserUpdateDateTime(LocalDateTime.now());
        return tierRepository.saveAndFlush(t);
    }

    private CfgTranche tranche(String libelle) {
        CfgProject projet = new CfgProject();
        projet.setId(UUID.randomUUID());
        projet.setCode("AUD-PRJ-" + UUID.randomUUID().toString().substring(0, 8));
        projet.setDescription("Projet " + libelle);
        projectRepository.saveAndFlush(projet);

        CfgTranche t = new CfgTranche();
        t.setId(UUID.randomUUID());
        t.setCode("AUD-TR-" + UUID.randomUUID().toString().substring(0, 8));
        t.setDescription(libelle);
        t.setCfgProject(projet);
        return trancheRepository.saveAndFlush(t);
    }

    /** Prospection du prospect, avec une tranche principale facultative. */
    private ComProspection prospection(CfgTier prospect, CfgTranche principale) {
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode("PSN-AUD-" + UUID.randomUUID());
        p.setDocDate(LocalDateTime.now());
        p.setCfgProspect(prospect);
        p.setStkHierarchy(principale);
        return prospectionRepository.saveAndFlush(p);
    }

    /** « Autre tranche » (intérêt secondaire) rattachée à une prospection. */
    private void autreTranche(ComProspection prospection, CfgTranche tranche) {
        ComProspectionProject pp = new ComProspectionProject();
        pp.setId(UUID.randomUUID());
        pp.setComProspection(prospection);
        pp.setStkHierarchy(tranche);
        prospectionProjectRepository.saveAndFlush(pp);
    }

    // ------------------------------------------------------------------
    // Piste d'audit
    // ------------------------------------------------------------------

    @Test
    void fiche_exposeLaPisteDaudit() throws Exception {
        CfgTier t = prospect();

        mockMvc.perform(get("/api/prospects/{id}", t.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creePar").value("createur-test"))
                .andExpect(jsonPath("$.creeLe").exists())
                .andExpect(jsonPath("$.modifiePar").value("modificateur-test"))
                .andExpect(jsonPath("$.modifieLe").exists());
    }

    // ------------------------------------------------------------------
    // Tranches d'intérêt
    // ------------------------------------------------------------------

    @Test
    void fiche_exposeLaTranchePrincipaleDeSesProspections() throws Exception {
        CfgTier t = prospect();
        CfgTranche tr = tranche("BEL AIR");
        prospection(t, tr);

        mockMvc.perform(get("/api/prospects/{id}", t.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tranchesInteret.length()").value(1))
                .andExpect(jsonPath("$.tranchesInteret[0].id").value(tr.getId().toString()))
                .andExpect(jsonPath("$.tranchesInteret[0].description").value("BEL AIR"));
    }

    @Test
    void fiche_exposeAussiLesInteretsSecondaires() throws Exception {
        CfgTier t = prospect();
        CfgTranche principale = tranche("BEL AIR");
        CfgTranche secondaire = tranche("TRIBECA");
        ComProspection p = prospection(t, principale);
        autreTranche(p, secondaire);

        mockMvc.perform(get("/api/prospects/{id}", t.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tranchesInteret.length()").value(2))
                // Tri par libellé : BEL AIR avant TRIBECA.
                .andExpect(jsonPath("$.tranchesInteret[0].description").value("BEL AIR"))
                .andExpect(jsonPath("$.tranchesInteret[1].description").value("TRIBECA"));
    }

    /**
     * Le {@code DISTINCT} de la vue légataire. Sans lui, la tranche remonterait deux fois — une
     * par branche de l'union — et la fiche annoncerait deux intérêts là où il n'y en a qu'un.
     */
    @Test
    void memeTrancheDesDeuxCotes_neCompteQuUneFois() throws Exception {
        CfgTier t = prospect();
        CfgTranche tr = tranche("BEL AIR");
        ComProspection p = prospection(t, tr);
        autreTranche(p, tr);

        mockMvc.perform(get("/api/prospects/{id}", t.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tranchesInteret.length()").value(1))
                .andExpect(jsonPath("$.tranchesInteret[0].id").value(tr.getId().toString()));
    }

    /** Deux prospections sur la même tranche : un seul intérêt, pas deux. */
    @Test
    void deuxProspectionsSurLaMemeTranche_neComptentQuUneFois() throws Exception {
        CfgTier t = prospect();
        CfgTranche tr = tranche("BEL AIR");
        prospection(t, tr);
        prospection(t, tr);

        mockMvc.perform(get("/api/prospects/{id}", t.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tranchesInteret.length()").value(1));
    }

    /**
     * Liste vide et non absente : « ce prospect n'a aucune prospection » est une information, et
     * un tableau vide se parcourt sans test de nullité côté front.
     */
    @Test
    void prospectSansProspection_rendUnTableauVide() throws Exception {
        CfgTier t = prospect();

        mockMvc.perform(get("/api/prospects/{id}", t.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tranchesInteret").isArray())
                .andExpect(jsonPath("$.tranchesInteret.length()").value(0));
    }

    /** Une prospection sans tranche principale ne doit rien ajouter, ni faire échouer la fiche. */
    @Test
    void prospectionSansTranche_neCasseRien() throws Exception {
        CfgTier t = prospect();
        prospection(t, null);

        mockMvc.perform(get("/api/prospects/{id}", t.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tranchesInteret.length()").value(0));
    }

    /** La liste des prospects n'est pas touchée par ce lot. */
    @Test
    void listeDesProspects_resteInchangee() throws Exception {
        CfgTier t = prospect();

        mockMvc.perform(get("/api/prospects").param("search", t.getCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value(t.getCode()))
                .andExpect(jsonPath("$.content[0].tranchesInteret").doesNotExist())
                .andExpect(jsonPath("$.content[0].creePar").doesNotExist());
    }
}
