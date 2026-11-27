package com.tsix.immocrm.controller.perimetre;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.FixtureAgences;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixturePlans;
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

/** Revue A1b : rejets 400, totaux (même prédicat que la liste) et compte agence combiné à « siege ». */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AgencesA1bRevueTest {

    private static final String AGENCE = "agence";
    private static final String INVALIDE = "pas-un-uuid";
    private static final String SEARCH = "search";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixturePlans plans;
    @Autowired private FixtureAgences agences;

    private String jeton;
    private RequestPostProcessor siege;
    private RequestPostProcessor compteA;
    private UUID agenceA;
    private UUID agenceB;

    @BeforeEach
    void setUp() {
        jeton = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        siege = connecte(patrimoine.administrateur());
        CfgTranche tranche = plans.tranche("AGR-" + jeton, true);
        agenceA = agences.agence("RVA-" + jeton);
        agenceB = agences.agence("RVB-" + jeton);
        UUID vendeurA = agences.vendeur("RVA-" + jeton);
        UUID vendeurB = agences.vendeur("RVB-" + jeton);
        agences.lier(agenceA, vendeurA);
        agences.lier(agenceB, vendeurB);
        creer(tranche, "RA", vendeurA);
        creer(tranche, "RB", vendeurB);
        AdmUser utilisateurA = agences.compteAgence("RC-" + jeton, vendeurA);
        plans.habiliter(utilisateurA, tranche.getCfgProject());
        compteA = FixtureDossierVente.connecte(utilisateurA, FixtureAgences.ROLE_AGENCE);
    }

    private void creer(CfgTranche tranche, String prefixe, UUID vendeur) {
        StkItem article = plans.article(tranche, prefixe + "-" + jeton, false, null);
        ComFolder cree = plans.dossier(article, StatutDossier.RESERVATION, "Magenta", "Réservation");
        UUID id = cree.getId();
        agences.commercialDuDossier(id, vendeur);
        plans.comFolderId(article.getId(), id);
    }

    @Test
    void valeurInvalide_listeDossiers_400() throws Exception {
        mockMvc.perform(get("/api/dossiers").param(SEARCH, jeton).param(AGENCE, INVALIDE).with(siege))
                .andExpect(status().isBadRequest());
    }

    @Test
    void valeurInvalide_totauxDossiers_400() throws Exception {
        mockMvc.perform(get("/api/dossiers/totaux-colonnes").param(SEARCH, jeton).param(AGENCE, INVALIDE).with(siege))
                .andExpect(status().isBadRequest());
    }

    @Test
    void valeurInvalide_prospections_400() throws Exception {
        mockMvc.perform(get("/api/prospections").param(AGENCE, INVALIDE).with(siege))
                .andExpect(status().isBadRequest());
    }

    @Test
    void valeurInvalide_offres_400() throws Exception {
        mockMvc.perform(get("/api/devis").param(AGENCE, INVALIDE).with(siege))
                .andExpect(status().isBadRequest());
    }

    @Test
    void valeurInvalide_etatDesVentes_400() throws Exception {
        mockMvc.perform(get("/api/etat-des-ventes").param(AGENCE, INVALIDE).with(siege))
                .andExpect(status().isBadRequest());
    }

    @Test
    void totauxColonnes_suiventLeFiltreAgence() throws Exception {
        String corps = mockMvc.perform(get("/api/dossiers/totaux-colonnes").param(SEARCH, jeton)
                        .param(AGENCE, agenceA.toString()).with(siege))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(JsonPath.<Number>read(corps, "$.nombreTotal").intValue()).isEqualTo(1);
    }

    @Test
    void compteAgenceA_filtreSiege_neRendRien() throws Exception {
        String corps = mockMvc.perform(get("/api/dossiers").param(SEARCH, jeton).param("size", "200")
                        .param(AGENCE, "siege").with(compteA))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        List<Object> contenu = JsonPath.read(corps, "$.content");
        assertThat(contenu).isEmpty();
    }
}
