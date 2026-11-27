package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.FixtureAgences;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixturePlans;
import com.tsix.immocrm.fixtures.FixtureVisibiliteOmran;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * R-S3 par l'onglet Dossiers de la fiche lot : les statuts du rôle Commercial ne rouvrent pas le dossier d'un vendeur
 * d'une autre agence (FEATURE-agences-correctifs-passe.md).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AgencesStatutsVisiblesArticleTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePlans plans;
    @Autowired private FixtureAgences agences;
    @Autowired private FixtureVisibiliteOmran visibilite;

    private String jeton;
    private CfgTranche tranche;
    private UUID roleCommercial;
    private UUID vendeurA;
    private RequestPostProcessor agence;

    @BeforeEach
    void setUp() {
        jeton = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        tranche = plans.tranche("ASA-" + jeton, true);
        roleCommercial = visibilite.role("Commercial");
        UUID agenceA = agences.agence("AGA-" + jeton);
        vendeurA = agences.vendeur("VA-" + jeton);
        agences.lier(agenceA, vendeurA);
        AdmUser compte = agences.compteAgence("CA-" + jeton, vendeurA);
        plans.habiliter(compte, tranche.getCfgProject());
        agence = FixtureDossierVente.connecte(compte, FixtureAgences.ROLE_AGENCE);
    }

    private UUID[] dossier(String prefixe, UUID vendeur) {
        StkItem article = plans.article(tranche, prefixe + "-" + jeton, false, null);
        ComFolder cree = plans.dossier(article, StatutDossier.RESERVATION, "Magenta", "RESERVATION");
        visibilite.intervenantRole(StatutDossier.CONFIG_ENTITY_ID, cree.getStrStatus().getId(), roleCommercial);
        plans.comFolderId(article.getId(), cree.getId());
        agences.commercialDuDossier(cree.getId(), vendeur);
        return new UUID[]{article.getId(), cree.getId()};
    }

    private String dossiersDe(UUID article) throws Exception {
        return mockMvc.perform(get("/api/articles/" + article + "/dossiers").with(agence))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
    }

    @Test
    void ongletDossiersDuLot_agence_voitSonVendeurPasLAutreAgence() throws Exception {
        UUID agenceB = agences.agence("AGB-" + jeton);
        UUID vendeurB = agences.vendeur("VB-" + jeton);
        agences.lier(agenceB, vendeurB);
        UUID[] sien = dossier("L1", vendeurA);
        UUID[] autre = dossier("L3", vendeurB);

        assertThat(dossiersDe(sien[0])).contains(sien[1].toString().toLowerCase(Locale.ROOT));
        assertThat(dossiersDe(autre[0])).doesNotContain(autre[1].toString().toLowerCase(Locale.ROOT));
    }
}
