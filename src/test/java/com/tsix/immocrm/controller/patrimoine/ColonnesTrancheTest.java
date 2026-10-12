package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.FixtureFichePatrimoine;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FEATURE-patrimoine-tranche-fiche.md §4.4 et §6. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ColonnesTrancheTest {

    private static final String TRANCHES = "/api/tranches";
    private static final String RECHERCHE = "search";
    private static final String MOT = "kwzq";
    private static final String PREMIERE = "$.content[0]";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureFichePatrimoine fiche;
    @Autowired private CfgTrancheRepository trancheRepository;

    @BeforeEach
    void setUp() {
        CfgCompany entite = fixture.entite("ENT-COL-TRA", true);
        CfgProject projet = fixture.projet("PRJ-COL-TRA", entite);
        CfgTranche tranche = fixture.tranche("TRA-COL-KWZQ", projet);
        tranche.setCity("Oujda-Qxv");
        tranche.setSyndicAmount(java.math.BigDecimal.TEN);
        tranche.setCfgTrancheStanding(fiche.standing("STD-COL-TRA"));
        trancheRepository.save(tranche);
        fixture.tranche("TRA-COL-AUTRE", projet);
    }

    @Test
    void catalogue_colonnesOmranAvecDefautsEtTypes() throws Exception {
        mockMvc.perform(get(TRANCHES + "/colonnes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nom", hasItem("standingLibelle")))
                .andExpect(jsonPath("$[?(@.nom=='standingLibelle')].parDefaut").value(true))
                .andExpect(jsonPath("$[?(@.nom=='advancePercent')].type").value("TAUX"))
                .andExpect(jsonPath("$[?(@.nom=='dateMainLeveeAfh')].groupe").value("FONCIER"))
                .andExpect(jsonPath("$[?(@.nom=='montantSyndic')].parDefaut").value(false));
    }

    @Test
    void sansColonnes_lesColonnesParDefautSeulement() throws Exception {
        mockMvc.perform(get(TRANCHES).param(RECHERCHE, MOT))
                .andExpect(status().isOk())
                .andExpect(jsonPath(PREMIERE + ".code").value("TRA-COL-KWZQ"))
                .andExpect(jsonPath(PREMIERE + ".ville").value("Oujda-Qxv"))
                .andExpect(jsonPath(PREMIERE + ".standingLibelle").value("Standing STD-COL-TRA"))
                .andExpect(jsonPath(PREMIERE + ".entiteLibelle").value("Entité ENT-COL-TRA"))
                .andExpect(jsonPath(PREMIERE + ".estEnVente").value(true))
                .andExpect(jsonPath(PREMIERE + ".montantSyndic").doesNotHaveJsonPath());
    }

    @Test
    void projection_colonnesDemandeesPlusId() throws Exception {
        mockMvc.perform(get(TRANCHES).param(RECHERCHE, MOT).param("colonnes", "code,montantSyndic"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(PREMIERE + ".id").isNotEmpty())
                .andExpect(jsonPath(PREMIERE + ".code").value("TRA-COL-KWZQ"))
                .andExpect(jsonPath(PREMIERE + ".montantSyndic").value(10))
                .andExpect(jsonPath(PREMIERE + ".description").doesNotHaveJsonPath())
                .andExpect(jsonPath(PREMIERE + ".standingLibelle").doesNotHaveJsonPath());
    }

    @Test
    void colonneInconnue_400_avecColonnesValides() throws Exception {
        mockMvc.perform(get(TRANCHES).param("colonnes", "code,pasUneColonne"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.colonne").value("pasUneColonne"))
                .andExpect(jsonPath("$.colonnesValides", hasItem("standingLibelle")));
    }

    @Test
    void recherche_parCodeVilleOuProgramme_casseIgnoree() throws Exception {
        mockMvc.perform(get(TRANCHES).param(RECHERCHE, "OUJDA-QXV"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath(PREMIERE + ".code").value("TRA-COL-KWZQ"));
        mockMvc.perform(get(TRANCHES).param(RECHERCHE, "prj-col-tra"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }
}
