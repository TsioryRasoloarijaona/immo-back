package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.cfg.CfgTrancheCoveredFloor;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.cfg.CfgTrancheCoveredFloorRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixturePatrimoine.ROLE_ADMIN;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.connecte;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Revue P1b : visibilité de tous les onglets imbriqués et réconciliation avec doublon id / sans id. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TrancheOngletsVisibiliteTest {

    private static final String LIGNES = "lignes";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private CfgTrancheCoveredFloorRepository plancherRepository;
    @Autowired private CfgTrancheRepository trancheRepository;
    @Autowired private EntityManager em;

    private AdmUser admin;

    @BeforeEach
    void setUp() {
        admin = fixture.utilisateur("ADM-REV-ONG");
    }

    @ParameterizedTest
    @ValueSource(strings = {"terrains", "etages", "plancher-couvert", "frais"})
    void ongletDUneTrancheDEntiteInactive_404EnLectureEtEnEcriture(String onglet) throws Exception {
        CfgTranche masquee = fixture.tranche("TRA-REV-INACT",
                fixture.projet("PRJ-REV-INACT", fixture.entite("ENT-REV-INACT", false)));
        String url = "/api/tranches/{id}/" + onglet;
        mockMvc.perform(get(url, masquee.getId()).with(connecte(admin, ROLE_ADMIN)))
                .andExpect(status().isNotFound());
        mockMvc.perform(put(url, masquee.getId()).with(connecte(admin, ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of(LIGNES, List.of()))))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"etages", "plancher-couvert", "frais"})
    void ecritureNonAdministrateur_403(String onglet) throws Exception {
        CfgTranche tranche = fixture.tranche("TRA-REV-403",
                fixture.projet("PRJ-REV-403", fixture.entite("ENT-REV-403", true)));
        mockMvc.perform(put("/api/tranches/{id}/" + onglet, tranche.getId()).with(connecte(admin, "Commercial"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of(LIGNES, List.of()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void plancher_nouvelleLigneAvantLigneExistanteDeMemeCategorie_uneSeuleLigneEtTotalCoherent() throws Exception {
        CfgTranche tranche = fixture.tranche("TRA-REV-PLA",
                fixture.projet("PRJ-REV-PLA", fixture.entite("ENT-REV-PLA", true)));
        StkItemCategory categorie = fixture.categorie("TST-REV-PLA", fixture.natureBatiment(), false);
        CfgTrancheCoveredFloor existante = new CfgTrancheCoveredFloor();
        existante.setId(UUID.randomUUID());
        existante.setCfgTranche(tranche);
        existante.setStkItemCategory(categorie);
        existante.setArea(BigDecimal.TEN);
        plancherRepository.save(existante);
        em.flush();

        Map<String, Object> nouvelle = new HashMap<>();
        nouvelle.put("categorieId", categorie.getId());
        nouvelle.put("surface", new BigDecimal("5"));
        Map<String, Object> ancienne = new HashMap<>();
        ancienne.put("id", existante.getId());
        ancienne.put("categorieId", categorie.getId());
        ancienne.put("surface", new BigDecimal("7"));

        mockMvc.perform(put("/api/tranches/{id}/plancher-couvert", tranche.getId()).with(connecte(admin, ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(LIGNES, List.of(nouvelle, ancienne)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].surface").value(5));
        em.flush();
        em.clear();

        assertThat(plancherRepository.findByCfgTrancheId(tranche.getId())).hasSize(1);
        assertThat(trancheRepository.findById(tranche.getId()).orElseThrow().getCoveredFloorArea())
                .isEqualByComparingTo("5");
    }
}
