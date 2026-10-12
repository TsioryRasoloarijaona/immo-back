package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.config.audit.AuditDeTest;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmWebHistorics;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.cfg.CfgTrancheLevel;
import com.tsix.immocrm.entities.lnd.LndLevel;
import com.tsix.immocrm.fixtures.FixtureFichePatrimoine;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.adm.AdmWebHistoricsRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheLevelRepository;
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

/** FEATURE-patrimoine-tranche-onglets.md §2.2 et §7. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TrancheEtagesTest {

    private static final String URL = "/api/tranches/{id}/etages";
    private static final String NIVEAU = "niveauTerrainId";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureFichePatrimoine fiche;
    @Autowired private CfgTrancheLevelRepository etageRepository;
    @Autowired private AdmWebHistoricsRepository historiques;
    @Autowired private EntityManager em;

    private AdmUser admin;
    private CfgTranche tranche;
    private LndLevel r1;
    private LndLevel r2;

    @BeforeEach
    void setUp() {
        admin = fixture.utilisateur("ADM-TRA-ETG");
        tranche = fixture.tranche("TRA-ETG", fixture.projet("PRJ-TRA-ETG", fixture.entite("ENT-TRA-ETG", true)));
        r1 = fiche.niveau("TST-R+1");
        r2 = fiche.niveau("TST-R+2");
    }

    private ResultActions enregistrer(UUID trancheId, String role, List<Map<String, Object>> lignes) throws Exception {
        return mockMvc.perform(put(URL, trancheId).with(connecte(admin, role))
                .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("lignes", lignes))));
    }

    private static Map<String, Object> ligne(UUID id, UUID niveauId) {
        Map<String, Object> ligne = new HashMap<>();
        ligne.put("id", id);
        ligne.put(NIVEAU, niveauId);
        return ligne;
    }

    private CfgTrancheLevel etage(LndLevel niveau) {
        CfgTrancheLevel etage = new CfgTrancheLevel();
        etage.setId(UUID.randomUUID());
        etage.setCfgTranche(tranche);
        etage.setLndLevel(niveau);
        return etageRepository.save(etage);
    }

    @Test
    void reconciliation_modifieSupprimeEtCree_doublonIgnore() throws Exception {
        CfgTrancheLevel modifie = etage(r1);
        CfgTrancheLevel retire = etage(r2);
        LndLevel r3 = fiche.niveau("TST-R+3");

        enregistrer(tranche.getId(), ROLE_ADMIN, List.of(ligne(modifie.getId(), r3.getId()), ligne(null, r1.getId()),
                ligne(null, r3.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].niveauCode").value("TST-R+1"))
                .andExpect(jsonPath("$[1].id").value(modifie.getId().toString()))
                .andExpect(jsonPath("$[1].niveauCode").value("TST-R+3"));
        em.flush();
        em.clear();

        assertThat(etageRepository.findById(retire.getId())).isEmpty();
    }

    @Test
    void lecture_ouverteAuxAuthentifies() throws Exception {
        etage(r1);
        mockMvc.perform(get(URL, tranche.getId()).with(connecte(fixture.utilisateur("COM-TRA-ETG"), "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].niveauTerrainId").value(r1.getId().toString()));
    }

    @Test
    void niveauManquantOuInconnu_400SurLaLigne() throws Exception {
        enregistrer(tranche.getId(), ROLE_ADMIN, List.of(ligne(null, r1.getId()), ligne(null, null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.champ").value("lignes[1].niveauTerrainId"));
        enregistrer(tranche.getId(), ROLE_ADMIN, List.of(ligne(null, tranche.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.champ").value("lignes[0].niveauTerrainId"));
    }

    @Test
    void nonAdministrateur_403() throws Exception {
        enregistrer(tranche.getId(), "Commercial", List.of()).andExpect(status().isForbidden());
    }

    @Test
    void trancheInconnue_404() throws Exception {
        enregistrer(UUID.randomUUID(), ROLE_ADMIN, List.of()).andExpect(status().isNotFound());
    }

    @Test
    void audit_ligneEnfantDeLaTranche_libelleEtTypeOmran() throws Exception {
        AuditDeTest.oublierPreparation(em);
        String reponse = enregistrer(tranche.getId(), ROLE_ADMIN, List.of(ligne(null, r1.getId())))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        AuditDeTest.simulerCommit(em, admin);
        UUID etageId = UUID.fromString(JsonPath.read(reponse, "$[0].id"));

        assertThat(historiques.findByObjectId(etageId)).singleElement().satisfies(h -> assertThat(h)
                .extracting(AdmWebHistorics::getObjectType, AdmWebHistorics::getObjectTypeLabel,
                        AdmWebHistorics::getObjectLabel, AdmWebHistorics::getParentObjectId)
                .containsExactly("Yes.Cfg.CfgTrancheLevel", "Prospection de projet", etageId.toString(),
                        tranche.getId()));
    }
}
