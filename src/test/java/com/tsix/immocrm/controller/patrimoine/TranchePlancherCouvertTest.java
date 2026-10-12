package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.config.audit.AuditDeTest;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmWebHistorics;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.cfg.CfgTrancheCoveredFloor;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.adm.AdmWebHistoricsRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheCoveredFloorRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
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

/** FEATURE-patrimoine-tranche-onglets.md §2.3 et §7. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TranchePlancherCouvertTest {

    private static final String URL = "/api/tranches/{id}/plancher-couvert";
    private static final String CHAMP = "$.champ";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private CfgTrancheCoveredFloorRepository plancherRepository;
    @Autowired private CfgTrancheRepository trancheRepository;
    @Autowired private AdmWebHistoricsRepository historiques;
    @Autowired private EntityManager em;

    private AdmUser admin;
    private CfgTranche tranche;
    private StkItemCategory appartement;
    private StkItemCategory parking;

    @BeforeEach
    void setUp() {
        admin = fixture.utilisateur("ADM-TRA-PLC");
        tranche = fixture.tranche("TRA-PLC", fixture.projet("PRJ-TRA-PLC", fixture.entite("ENT-TRA-PLC", true)));
        appartement = fixture.categorie("TST-APP-PLC", fixture.natureBatiment(), false);
        parking = fixture.categorie("TST-PAR-PLC", fixture.natureBatiment(), true);
    }

    private ResultActions enregistrer(UUID trancheId, String role, List<Map<String, Object>> lignes) throws Exception {
        return mockMvc.perform(put(URL, trancheId).with(connecte(admin, role))
                .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("lignes", lignes))));
    }

    private static Map<String, Object> ligne(UUID id, UUID categorieId, String surface) {
        Map<String, Object> ligne = new HashMap<>();
        ligne.put("id", id);
        ligne.put("categorieId", categorieId);
        ligne.put("surface", surface == null ? null : new BigDecimal(surface));
        return ligne;
    }

    private CfgTrancheCoveredFloor plancher(StkItemCategory categorie, String surface) {
        CfgTrancheCoveredFloor plancher = new CfgTrancheCoveredFloor();
        plancher.setId(UUID.randomUUID());
        plancher.setCfgTranche(tranche);
        plancher.setStkItemCategory(categorie);
        plancher.setArea(new BigDecimal(surface));
        return plancherRepository.save(plancher);
    }

    @Test
    void enregistrement_reecritLePlancherCouvertDeLaTranche() throws Exception {
        enregistrer(tranche.getId(), ROLE_ADMIN, List.of(ligne(null, appartement.getId(), "1200.5"),
                ligne(null, parking.getId(), "300")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        em.flush();
        em.clear();

        assertThat(trancheRepository.findById(tranche.getId()).orElseThrow().getCoveredFloorArea())
                .isEqualByComparingTo("1500.5");
        mockMvc.perform(get("/api/tranches/{id}", tranche.getId()).with(connecte(admin, ROLE_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plancherCouvert").value(1500.5));
    }

    @Test
    void reconciliation_modifieSupprimeEtCree_doublonIgnore_totalRecalcule() throws Exception {
        CfgTrancheCoveredFloor modifie = plancher(appartement, "100");
        CfgTrancheCoveredFloor retire = plancher(parking, "50");
        StkItemCategory villa = fixture.categorie("TST-VIL-PLC", fixture.natureBatiment(), false);

        enregistrer(tranche.getId(), ROLE_ADMIN, List.of(ligne(modifie.getId(), appartement.getId(), "120"),
                ligne(null, villa.getId(), "80"), ligne(null, appartement.getId(), "999")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        em.flush();
        em.clear();

        assertThat(plancherRepository.findById(retire.getId())).isEmpty();
        assertThat(trancheRepository.findById(tranche.getId()).orElseThrow().getCoveredFloorArea())
                .isEqualByComparingTo("200");
    }

    @Test
    void listeVide_plancherCouvertAZero() throws Exception {
        plancher(appartement, "100");
        enregistrer(tranche.getId(), ROLE_ADMIN, List.of()).andExpect(status().isOk());
        em.flush();
        em.clear();

        assertThat(trancheRepository.findById(tranche.getId()).orElseThrow().getCoveredFloorArea()).isZero();
    }

    @Test
    void categorieNonBatie_400() throws Exception {
        StkItemCategory terrain = fixture.categorie("TST-TER-PLC",
                fixture.nature(UUID.randomUUID(), "TST-NAT-PLC"), false);
        enregistrer(tranche.getId(), ROLE_ADMIN, List.of(ligne(null, terrain.getId(), "10")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CHAMP).value("lignes[0].categorieId"));
    }

    @Test
    void surfaceNegative_400() throws Exception {
        enregistrer(tranche.getId(), ROLE_ADMIN, List.of(ligne(null, appartement.getId(), "-1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CHAMP).value("lignes[0].surface"));
    }

    @Test
    void nonAdministrateur_403() throws Exception {
        enregistrer(tranche.getId(), "Commercial", List.of()).andExpect(status().isForbidden());
    }

    @Test
    void trancheInconnue_404() throws Exception {
        mockMvc.perform(get(URL, UUID.randomUUID()).with(connecte(admin, ROLE_ADMIN)))
                .andExpect(status().isNotFound());
    }

    @Test
    void audit_ligneEnfantDeLaTranche() throws Exception {
        AuditDeTest.oublierPreparation(em);
        String reponse = enregistrer(tranche.getId(), ROLE_ADMIN, List.of(ligne(null, appartement.getId(), "10")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        AuditDeTest.simulerCommit(em, admin);
        UUID plancherId = UUID.fromString(JsonPath.read(reponse, "$[0].id"));

        assertThat(historiques.findByObjectId(plancherId)).singleElement().satisfies(h -> assertThat(h)
                .extracting(AdmWebHistorics::getObjectType, AdmWebHistorics::getParentObjectType,
                        AdmWebHistorics::getParentObjectId)
                .containsExactly("Yes.Cfg.CfgTrancheCoveredFloor", "Yes.Cfg.CfgTranche", tranche.getId()));
    }
}
