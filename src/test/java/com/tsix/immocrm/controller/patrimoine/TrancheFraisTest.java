package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.config.audit.AuditDeTest;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmWebHistorics;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.cfg.CfgTrancheSettlementCategory;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.fixtures.FixtureOngletsTranche;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.adm.AdmWebHistoricsRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheSettlementCategoryRepository;
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
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FEATURE-patrimoine-tranche-onglets.md §2.4 et §7. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TrancheFraisTest {

    private static final String URL = "/api/tranches/{id}/frais";
    private static final String CATEGORIES = "/api/tranches/categories-frais";
    private static final String CHAMP = "$.champ";
    private static final String CATEGORIE = "categorieReglementId";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureOngletsTranche onglets;
    @Autowired private CfgTrancheSettlementCategoryRepository fraisRepository;
    @Autowired private AdmWebHistoricsRepository historiques;
    @Autowired private EntityManager em;

    private AdmUser admin;
    private CfgTranche tranche;
    private ComSettlementCategory notaire;
    private ComSettlementCategory enregistrement;

    @BeforeEach
    void setUp() {
        admin = fixture.utilisateur("ADM-TRA-FRS");
        tranche = fixture.tranche("TRA-FRS", fixture.projet("PRJ-TRA-FRS", fixture.entite("ENT-TRA-FRS", true)));
        notaire = onglets.categorieReglement("TST-NOTAIRE", true, true);
        enregistrement = onglets.categorieReglement("TST-ENREG", true, true);
    }

    private ResultActions enregistrer(UUID trancheId, String role, List<Map<String, Object>> lignes) throws Exception {
        return mockMvc.perform(put(URL, trancheId).with(connecte(admin, role))
                .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("lignes", lignes))));
    }

    private static Map<String, Object> ligne(UUID id, UUID categorieId, String ht, String tva) {
        Map<String, Object> ligne = new HashMap<>();
        ligne.put("id", id);
        ligne.put(CATEGORIE, categorieId);
        ligne.put("montantHT", ht == null ? null : new BigDecimal(ht));
        ligne.put("tauxTva", tva == null ? null : new BigDecimal(tva));
        return ligne;
    }

    private static Map<String, Object> ligneTtc(UUID categorieId, String ttc, String tva) {
        Map<String, Object> ligne = ligne(null, categorieId, null, tva);
        ligne.put("montantTTC", new BigDecimal(ttc));
        ligne.put("saisiEnTTC", true);
        return ligne;
    }

    @Test
    void saisieHT_ttcCalcule_ordreParDefautLeRang() throws Exception {
        enregistrer(tranche.getId(), ROLE_ADMIN, List.of(ligne(null, notaire.getId(), "1000", "0.2")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].montantHT").value(1000))
                .andExpect(jsonPath("$[0].montantTTC").value(1200))
                .andExpect(jsonPath("$[0].ordre").value(0))
                .andExpect(jsonPath("$[0].categorieCode").value("TST-NOTAIRE"));
    }

    @Test
    void saisieTTC_htCalcule_arrondiASixDecimales() throws Exception {
        enregistrer(tranche.getId(), ROLE_ADMIN, List.of(ligneTtc(notaire.getId(), "100", "0.07")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].montantTTC").value(100))
                .andExpect(jsonPath("$[0].montantHT").value(93.457944));
    }

    @Test
    void reconciliation_modifieSupprimeEtCree_doublonIgnore() throws Exception {
        CfgTrancheSettlementCategory modifie = frais(notaire, 1);
        CfgTrancheSettlementCategory retire = frais(enregistrement, 2);
        ComSettlementCategory dossier = onglets.categorieReglement("TST-DOSSIER", true, true);

        enregistrer(tranche.getId(), ROLE_ADMIN, List.of(ligne(modifie.getId(), notaire.getId(), "50", null),
                ligne(null, dossier.getId(), "10", null), ligne(null, notaire.getId(), "999", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(modifie.getId().toString()))
                .andExpect(jsonPath("$[0].montantTTC").value(50));
        em.flush();
        em.clear();

        assertThat(fraisRepository.findById(retire.getId())).isEmpty();
    }

    @Test
    void categories_fraisDeVenteSansLesTroisCategoriesAutomatiques() throws Exception {
        ComSettlementCategory transfert = onglets.categorieReglement("TST-TRANSFERT", true, true);
        ComSettlementCategory debit = onglets.categorieReglement("TST-DEBIT", false, true);
        onglets.parametreGuid("DefaultCustomerTransfertDocumentType", transfert.getId());

        mockMvc.perform(get(CATEGORIES).with(connecte(admin, ROLE_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(hasItem(notaire.getId().toString())))
                .andExpect(jsonPath("$[*].id").value(not(hasItem(transfert.getId().toString()))))
                .andExpect(jsonPath("$[*].id").value(not(hasItem(debit.getId().toString()))));
    }

    @Test
    void categorieExclue_400SurLaLigne() throws Exception {
        ComSettlementCategory desistement = onglets.categorieReglement("TST-DESIST", true, true);
        onglets.parametreGuid("DefaultCancellationDocumentType", desistement.getId());

        enregistrer(tranche.getId(), ROLE_ADMIN, List.of(ligne(null, notaire.getId(), "1", null),
                ligne(null, desistement.getId(), "1", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CHAMP).value("lignes[1].categorieReglementId"));
    }

    @Test
    void tvaHorsBornesOuMontantNegatif_400() throws Exception {
        enregistrer(tranche.getId(), ROLE_ADMIN, List.of(ligne(null, notaire.getId(), "1", "1.5")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CHAMP).value("lignes[0].tauxTva"));
        enregistrer(tranche.getId(), ROLE_ADMIN, List.of(ligne(null, notaire.getId(), "-1", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CHAMP).value("lignes[0].montantHT"));
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
    void audit_ligneEnfantFraisDeVente() throws Exception {
        AuditDeTest.oublierPreparation(em);
        String reponse = enregistrer(tranche.getId(), ROLE_ADMIN, List.of(ligne(null, notaire.getId(), "1", null)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        AuditDeTest.simulerCommit(em, admin);
        UUID fraisId = UUID.fromString(JsonPath.read(reponse, "$[0].id"));

        assertThat(historiques.findByObjectId(fraisId)).singleElement().satisfies(h -> assertThat(h)
                .extracting(AdmWebHistorics::getObjectType, AdmWebHistorics::getObjectTypeLabel,
                        AdmWebHistorics::getParentObjectId)
                .containsExactly("Yes.Cfg.CfgTrancheSettlementCategory", "Frais de vente", tranche.getId()));
    }

    private CfgTrancheSettlementCategory frais(ComSettlementCategory categorie, int ordre) {
        CfgTrancheSettlementCategory frais = new CfgTrancheSettlementCategory();
        frais.setId(UUID.randomUUID());
        frais.setCfgTranche(tranche);
        frais.setComSettlementCategory(categorie);
        frais.setSorting(ordre);
        return fraisRepository.save(frais);
    }
}
