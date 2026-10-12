package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.config.audit.AuditDeTest;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmWebHistorics;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.cfg.CfgTrancheStkItem;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.FixtureOngletsTranche;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.adm.AdmWebHistoricsRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheStkItemRepository;
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
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FEATURE-patrimoine-tranche-onglets.md §2.1 et §7. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TrancheTerrainsTest {

    private static final String URL = "/api/tranches/{id}/terrains";
    private static final String CHAMP = "$.champ";
    private static final String ID = "id";
    private static final String TERRAIN_ID = "terrainId";
    private static final String RATIO = "ratio";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureOngletsTranche onglets;
    @Autowired private CfgTrancheStkItemRepository lienRepository;
    @Autowired private AdmWebHistoricsRepository historiques;
    @Autowired private EntityManager em;

    private AdmUser admin;
    private CfgTranche tranche;
    private StkItem terrainA;
    private StkItem terrainB;

    @BeforeEach
    void setUp() {
        admin = fixture.utilisateur("ADM-TRA-TER");
        tranche = fixture.tranche("TRA-TER", fixture.projet("PRJ-TRA-TER", fixture.entite("ENT-TRA-TER", true)));
        terrainA = onglets.terrain("TST-TER-A");
        terrainB = onglets.terrain("TST-TER-B");
    }

    private ResultActions enregistrer(UUID trancheId, List<Map<String, Object>> lignes) throws Exception {
        return enregistrerEn(ROLE_ADMIN, trancheId, lignes);
    }

    private ResultActions enregistrerEn(String role, UUID trancheId, List<Map<String, Object>> lignes)
            throws Exception {
        return mockMvc.perform(put(URL, trancheId).with(connecte(admin, role))
                .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("lignes", lignes))));
    }

    private static Map<String, Object> ligne(UUID id, UUID terrainId, String ratio) {
        Map<String, Object> ligne = new HashMap<>();
        ligne.put(ID, id);
        ligne.put(TERRAIN_ID, terrainId);
        ligne.put(RATIO, ratio == null ? null : new BigDecimal(ratio));
        return ligne;
    }

    private CfgTrancheStkItem lien(StkItem terrain, BigDecimal ratio) {
        CfgTrancheStkItem lien = new CfgTrancheStkItem();
        lien.setId(UUID.randomUUID());
        lien.setCfgTranche(tranche);
        lien.setStkItem(terrain);
        lien.setRatio(ratio);
        lien.setSorting(0);
        return lienRepository.save(lien);
    }

    @Test
    void creation_ratioParDefautUn_ordreEgalAuRang_terrainLu() throws Exception {
        enregistrer(tranche.getId(), List.of(ligne(null, terrainA.getId(), null), ligne(null, terrainB.getId(), "0.4")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].terrainCode").value("TST-TER-A"))
                .andExpect(jsonPath("$[0].ratio").value(1))
                .andExpect(jsonPath("$[0].ordre").value(0))
                .andExpect(jsonPath("$[0].surface").value(1250.5))
                .andExpect(jsonPath("$[1].ratio").value(0.4))
                .andExpect(jsonPath("$[1].ordre").value(1));
    }

    @Test
    void reconciliation_modifieSupprimeEtCreeEnUnSeulPut() throws Exception {
        CfgTrancheStkItem garde = lien(terrainA, BigDecimal.ONE);
        CfgTrancheStkItem retire = lien(terrainB, BigDecimal.ONE);
        StkItem terrainC = onglets.terrain("TST-TER-C");

        enregistrer(tranche.getId(), List.of(ligne(garde.getId(), terrainA.getId(), "0.5"),
                ligne(null, terrainC.getId(), null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        em.flush();
        em.clear();

        assertThat(lienRepository.findByCfgTrancheIdOrderBySortingAsc(tranche.getId()))
                .extracting(l -> l.getStkItem().getId(), CfgTrancheStkItem::getRatio)
                .containsExactly(tuple(terrainA.getId(), new BigDecimal("0.500000")),
                        tuple(terrainC.getId(), new BigDecimal("1.000000")));
        assertThat(lienRepository.findById(retire.getId())).isEmpty();
    }

    @Test
    void doublon_ignore_uneSeuleLigneGardee() throws Exception {
        enregistrer(tranche.getId(), List.of(ligne(null, terrainA.getId(), "0.3"), ligne(null, terrainA.getId(), "0.9")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].ratio").value(0.3));
    }

    @Test
    void listeVide_supprimeTout() throws Exception {
        lien(terrainA, BigDecimal.ONE);
        enregistrer(tranche.getId(), List.of()).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void lecture_ouverteAuxAuthentifies() throws Exception {
        lien(terrainA, BigDecimal.ONE);
        AdmUser commercial = fixture.utilisateur("COM-TRA-TER");
        mockMvc.perform(get(URL, tranche.getId()).with(connecte(commercial, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].terrainId").value(terrainA.getId().toString()));
    }

    @Test
    void terrainsDisponibles_seulementNatureTerEtVocationProjet() throws Exception {
        StkItem horsProjet = onglets.terrain("TST-TER-VENTE", onglets.natureTerrain(), "VENTE");
        mockMvc.perform(get("/api/tranches/terrains-disponibles").with(connecte(admin, ROLE_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(hasItem(terrainA.getId().toString())))
                .andExpect(jsonPath("$[*].id").value(not(hasItem(horsProjet.getId().toString()))));
    }

    @Test
    void terrainNonProjet_400SurLaLigne() throws Exception {
        StkItem horsProjet = onglets.terrain("TST-TER-VENTE2", onglets.natureTerrain(), "VENTE");
        enregistrer(tranche.getId(), List.of(ligne(null, terrainA.getId(), null), ligne(null, horsProjet.getId(), null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CHAMP).value("lignes[1].terrainId"));
    }

    @Test
    void terrainManquant_400() throws Exception {
        enregistrer(tranche.getId(), List.of(ligne(null, null, null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CHAMP).value("lignes[0].terrainId"));
    }

    @Test
    void ratioNegatif_400() throws Exception {
        enregistrer(tranche.getId(), List.of(ligne(null, terrainA.getId(), "-1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CHAMP).value("lignes[0].ratio"));
    }

    @Test
    void idDUneAutreTranche_400() throws Exception {
        CfgTranche autre = fixture.tranche("TRA-TER-AUTRE", tranche.getCfgProject());
        CfgTrancheStkItem lienAutre = lien(terrainA, BigDecimal.ONE);
        lienAutre.setCfgTranche(autre);
        lienRepository.save(lienAutre);
        enregistrer(tranche.getId(), List.of(ligne(lienAutre.getId(), terrainA.getId(), null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CHAMP).value("lignes[0].id"));
    }

    @Test
    void nonAdministrateur_403() throws Exception {
        enregistrerEn("Commercial", tranche.getId(), List.of()).andExpect(status().isForbidden());
    }

    @Test
    void trancheInconnue_404() throws Exception {
        enregistrer(UUID.randomUUID(), List.of()).andExpect(status().isNotFound());
        mockMvc.perform(get(URL, UUID.randomUUID()).with(connecte(admin, ROLE_ADMIN))).andExpect(status().isNotFound());
    }

    @Test
    void entiteInactive_404MemePourAdministrateur() throws Exception {
        CfgTranche masquee = fixture.tranche("TRA-TER-INACT",
                fixture.projet("PRJ-TER-INACT", fixture.entite("ENT-TER-INACT", false)));
        mockMvc.perform(get(URL, masquee.getId()).with(connecte(admin, ROLE_ADMIN))).andExpect(status().isNotFound());
        enregistrer(masquee.getId(), List.of()).andExpect(status().isNotFound());
    }

    @Test
    void audit_ligneEnfantDeLaTranche() throws Exception {
        AuditDeTest.oublierPreparation(em);
        String reponse = enregistrer(tranche.getId(), List.of(ligne(null, terrainA.getId(), null)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        AuditDeTest.simulerCommit(em, admin);
        UUID lienId = UUID.fromString(JsonPath.read(reponse, "$[0].id"));

        assertThat(historiques.findByObjectId(lienId)).singleElement().satisfies(h -> assertThat(h)
                .extracting(AdmWebHistorics::getObjectType, AdmWebHistorics::getAction,
                        AdmWebHistorics::getParentObjectType, AdmWebHistorics::getParentObjectId)
                .containsExactly("Yes.Cfg.CfgTrancheStkItem", "Added", "Yes.Cfg.CfgTranche", tranche.getId()));
    }
}
