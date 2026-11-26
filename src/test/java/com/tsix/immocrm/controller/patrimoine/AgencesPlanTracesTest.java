package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkFloor;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.FixtureAgences;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixturePlans;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Plan de tranche pour un compte agence — FEATURE-agences-vente.md §4 V2, R-V2b, §8 (« tracé Indisponible sans champ
 * client/dossier »). Projet P affecté à l'agence A : tracés d'un lot libre, d'un lot vendu par A, par B et par le siège.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AgencesPlanTracesTest {

    private static final String TRACES = "/api/plans/{id}/traces";
    private static final String D = "M 0,0 h 10 v 10 z";
    private static final String LIBRE = "libre";
    private static final String LOT_A = "lotA";
    private static final String LOT_B = "lotB";
    private static final String LOT_SIEGE = "lotSiege";
    private static final String INDISPONIBLE = "indisponible";
    private static final String DOSSIER_ID = "dossierId";
    private static final String CLIENT = "client";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixturePlans plans;
    @Autowired private FixtureAgences agences;

    private String jeton;
    private StkFloor etage;
    private UUID plan;
    private final Map<String, StkItem> lots = new HashMap<>();
    private final Map<String, UUID> dossiers = new HashMap<>();
    private RequestPostProcessor siege;
    private RequestPostProcessor compteA;

    @BeforeEach
    void setUp() {
        jeton = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        siege = connecte(patrimoine.administrateur());
        etage = plans.etage("AGE-" + jeton);
        CfgTranche tranche = plans.tranche("AGT-" + jeton, true);

        UUID agenceA = agences.agence("AGA-" + jeton);
        UUID agenceB = agences.agence("AGB-" + jeton);
        UUID vendeurA = agences.vendeur("VA-" + jeton);
        UUID vendeurB = agences.vendeur("VB-" + jeton);
        UUID vendeurSiege = agences.vendeur("VS-" + jeton);
        agences.lier(agenceA, vendeurA);
        agences.lier(agenceB, vendeurB);

        lots.put(LIBRE, article(tranche, "LIB"));
        vendu(LOT_A, article(tranche, "LA"), vendeurA);
        vendu(LOT_B, article(tranche, "LB"), vendeurB);
        vendu(LOT_SIEGE, article(tranche, "LS"), vendeurSiege);

        AdmUser compte = agences.compteAgence("CA-" + jeton, vendeurA);
        plans.habiliter(compte, tranche.getCfgProject());
        compteA = FixtureDossierVente.connecte(compte, FixtureAgences.ROLE_AGENCE);

        // Plan en dernier : parents, dossiers et tracés d'abord.
        lots.values().forEach(a -> plans.trace(a.getId(), etage.getId(), D));
        plan = plans.plan(tranche.getId(), etage.getId(), "Etage", FixturePlans.dataUriPng());
    }

    private StkItem article(CfgTranche tranche, String prefixe) {
        return plans.article(tranche, prefixe + "-" + jeton, false, etage);
    }

    private void vendu(String nom, StkItem article, UUID vendeur) {
        ComFolder dossier = plans.dossier(article, StatutDossier.RESERVATION, "Magenta", "Réservation");
        agences.commercialDuDossier(dossier.getId(), vendeur);
        plans.comFolderId(article.getId(), dossier.getId());
        lots.put(nom, article);
        dossiers.put(nom, dossier.getId());
    }

    private String traces(RequestPostProcessor qui) throws Exception {
        return mockMvc.perform(get(TRACES, plan).with(qui)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private static Map<String, Object> trace(String json, StkItem article) {
        List<Map<String, Object>> trouves = JsonPath.read(json, "$[?(@.articleId == '" + article.getId() + "')]");
        assertThat(trouves).hasSize(1);
        return trouves.getFirst();
    }

    @Test
    void v2_agenceA_voitTousLesTracesDuProjetAffecte() throws Exception {
        List<String> articles = JsonPath.read(traces(compteA), "$[*].articleId");

        assertThat(articles).contains(lots.values().stream().map(a -> a.getId().toString()).toArray(String[]::new));
    }

    @ParameterizedTest(name = "tracé {0}")
    @ValueSource(strings = {LOT_B, LOT_SIEGE})
    void v2_rV2b_lotNiLibreNiDeSonAgence_indisponible_sansDossierClientNiCouleur(String lot) throws Exception {
        Map<String, Object> trace = trace(traces(compteA), lots.get(lot));

        assertThat(trace)
                .containsEntry(INDISPONIBLE, true)
                .containsEntry("statutLibelle", "Indisponible")
                .containsEntry(DOSSIER_ID, null)
                .containsEntry(CLIENT, null)
                .containsEntry("couleur", null);
    }

    @Test
    void v2_lotDeSonAgence_gardeDossierEtClient_pasIndisponible() throws Exception {
        Map<String, Object> trace = trace(traces(compteA), lots.get(LOT_A));

        assertThat(trace)
                .containsEntry(INDISPONIBLE, false)
                .containsEntry(DOSSIER_ID, dossiers.get(LOT_A).toString())
                .extractingByKey(CLIENT).isNotNull();
    }

    @Test
    void v2_lotLibre_resteLibre_pasIndisponible() throws Exception {
        Map<String, Object> trace = trace(traces(compteA), lots.get(LIBRE));

        assertThat(trace).containsEntry(INDISPONIBLE, false).containsEntry("libre", true);
    }

    @Test
    void v2_temoinSiege_lotDeB_dossierEtClientExposes() throws Exception {
        Map<String, Object> trace = trace(traces(siege), lots.get(LOT_B));

        assertThat(trace)
                .containsEntry(DOSSIER_ID, dossiers.get(LOT_B).toString())
                .extractingByKey(CLIENT).isNotNull();
    }
}
