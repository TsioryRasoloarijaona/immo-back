package com.tsix.immocrm.controller.referentiel;

import com.tsix.immocrm.domain.TypeTiersPatrimoine;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.prj.PrjProject;
import com.tsix.immocrm.fixtures.FixtureFichePatrimoine;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FEATURE-patrimoine-tranche-fiche.md §2 et §6. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ReferentielPatrimoineControllerTest {

    private static final String BASE = "/api/referentiels-patrimoine/";
    private static final String CODES = "$[*].code";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureFichePatrimoine fiche;
    @Autowired private FixturePatrimoine patrimoine;

    @Test
    void standings_listes() throws Exception {
        fiche.standing("STD-REF-HAUT");
        mockMvc.perform(get(BASE + "standings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(CODES, hasItem("STD-REF-HAUT")));
    }

    @Test
    void niveauxTerrain_listes() throws Exception {
        fiche.niveau("R+4-REF");
        mockMvc.perform(get(BASE + "niveaux-terrain"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(CODES, hasItem("R+4-REF")));
    }

    @Test
    void rubriquesTranche_avecOrdre() throws Exception {
        fiche.rubrique("RUB-REF-1", 7);
        mockMvc.perform(get(BASE + "rubriques-tranche"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code=='RUB-REF-1')].sorting", hasItem(7)));
    }

    @Test
    void personnels_seulementLesEmployes() throws Exception {
        CfgTier employe = fiche.tiers(TypeTiersPatrimoine.PERSONNEL, "EMP-REF-PAT");
        CfgTier fournisseur = fiche.tiers(TypeTiersPatrimoine.FOURNISSEUR, "FOU-REF-PAT");
        CfgTier prospect = fiche.prospect("PRO-REF-PAT");

        mockMvc.perform(get(BASE + "personnels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(CODES, hasItem(employe.getCode())))
                .andExpect(jsonPath(CODES, not(hasItem(fournisseur.getCode()))))
                .andExpect(jsonPath(CODES, not(hasItem(prospect.getCode()))));
    }

    @Test
    void tiersAutrui_seulementLesFournisseurs() throws Exception {
        CfgTier employe = fiche.tiers(TypeTiersPatrimoine.PERSONNEL, "EMP-REF-AUT");
        CfgTier fournisseur = fiche.tiers(TypeTiersPatrimoine.FOURNISSEUR, "FOU-REF-AUT");

        mockMvc.perform(get(BASE + "tiers-autrui"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(CODES, hasItem(fournisseur.getCode())))
                .andExpect(jsonPath(CODES, not(hasItem(employe.getCode()))));
    }

    @Test
    void plansComptables_listes() throws Exception {
        fiche.compte("411REF");
        mockMvc.perform(get(BASE + "plans-comptables"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(CODES, hasItem("411REF")));
    }

    @Test
    void projetsFacturation_filtresParEntite() throws Exception {
        CfgCompany entite = patrimoine.entite("ENT-REF-FACT", true);
        CfgCompany autre = patrimoine.entite("ENT-REF-FACT2", true);
        PrjProject duMeme = fiche.projetChantier("PRJ-FACT-A", entite);
        PrjProject dUnAutre = fiche.projetChantier("PRJ-FACT-B", autre);

        mockMvc.perform(get(BASE + "projets-facturation").param("entiteId", entite.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath(CODES, hasItem(duMeme.getCode())))
                .andExpect(jsonPath(CODES, not(hasItem(dUnAutre.getCode()))));

        mockMvc.perform(get(BASE + "projets-facturation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(CODES, hasItems(duMeme.getCode(), dUnAutre.getCode())));
    }
}
