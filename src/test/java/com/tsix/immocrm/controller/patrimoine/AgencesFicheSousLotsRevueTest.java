package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.FixtureAgences;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixturePlans;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Revue A1a : la fiche d'un lot libre ne liste pas le sous-article vendu par une autre agence (FEATURE-agences-vente.md V1). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AgencesFicheSousLotsRevueTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixturePlans plans;
    @Autowired private FixtureAgences agences;

    private StkItem parent;
    private StkItem sousB;
    private RequestPostProcessor compteA;

    @BeforeEach
    void setUp() {
        String jeton = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        CfgTranche tranche = plans.tranche("AGF-" + jeton, true);
        UUID agenceA = agences.agence("AGA-" + jeton);
        UUID agenceB = agences.agence("AGB-" + jeton);
        UUID vendeurA = agences.vendeur("VA-" + jeton);
        UUID vendeurB = agences.vendeur("VB-" + jeton);
        agences.lier(agenceA, vendeurA);
        agences.lier(agenceB, vendeurB);

        parent = plans.article(tranche, "PL-" + jeton, false, null);
        sousB = plans.article(tranche, "SB-" + jeton, true, null);
        agences.parent(sousB.getId(), parent.getId());
        ComFolder dossierB = plans.dossier(sousB, StatutDossier.RESERVATION, "Magenta", "Réservation");
        agences.commercialDuDossier(dossierB.getId(), vendeurB);
        plans.comFolderId(sousB.getId(), dossierB.getId());

        AdmUser compte = agences.compteAgence("CA-" + jeton, vendeurA);
        plans.habiliter(compte, tranche.getCfgProject());
        compteA = FixtureDossierVente.connecte(compte, FixtureAgences.ROLE_AGENCE);
    }

    private List<String> sousLots(RequestPostProcessor qui) throws Exception {
        String json = mockMvc.perform(get("/api/articles/{id}", parent.getId()).with(qui))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(json, "$.sousLots[*].code");
    }

    @Test
    void ficheDuLotLibre_agenceA_sansLeSousArticleVenduParB() throws Exception {
        assertThat(sousLots(compteA)).doesNotContain(sousB.getCode());
    }

    @Test
    void ficheDuLotLibre_temoinSiege_listeLeSousArticleDeB() throws Exception {
        assertThat(sousLots(connecte(patrimoine.administrateur()))).contains(sousB.getCode());
    }
}
