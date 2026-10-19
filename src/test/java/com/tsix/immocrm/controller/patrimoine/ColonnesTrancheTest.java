package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Liste des tranches à colonnes choisies — FEATURE-patrimoine-tranche-fiche.md §4.4, recalé par §8.2 (catalogue sous
 * {@code /api/referentiels/colonnes/tranches}) et §8.4 (400 : statut + noms valides dans la réponse).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ColonnesTrancheTest {

    private static final String CATALOGUE = "/api/referentiels/colonnes/tranches";
    private static final String TRANCHES = "/api/tranches";
    private static final String COLONNES = "colonnes";
    private static final String SEARCH = "search";
    private static final String SIZE = "size";
    private static final String TOUS = "1000";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private CfgTrancheRepository trancheRepository;

    private AdmUser admin;
    private CfgProject projet;
    private String jeton;

    @BeforeEach
    void setUp() {
        admin = fixture.administrateur();
        jeton = "P1AC" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        projet = fixture.projet("PRJ-" + jeton, fixture.entite("ENT-" + jeton, true));
    }

    private CfgTranche tranche(String suffixe) {
        CfgTranche tranche = fixture.tranche("TR-" + jeton + "-" + suffixe, projet);
        tranche.setCity("Oujda");
        tranche.setSavAddress("SAV " + suffixe);
        tranche.setIsForSale(true);
        return trancheRepository.save(tranche);
    }

    private static String ligneDe(CfgTranche tranche) {
        return "$.content[?(@.id=='" + tranche.getId() + "')]";
    }

    // ------------------------------------------------------------------ catalogue

    @Test
    void r4_4_catalogue_publieLesColonnesDeLAide() throws Exception {
        mockMvc.perform(get(CATALOGUE).with(connecte(admin)))
                .andExpect(status().isOk())
                // Tableau Liste de la page TRANCHES de l'aide : 78 libellés.
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(78)))
                .andExpect(jsonPath("$[?(@.nom == 'code')].parDefaut").value(true))
                .andExpect(jsonPath("$[?(@.nom == 'description')].parDefaut").value(true))
                .andExpect(jsonPath("$[?(@.nom == 'estEnVente')].parDefaut").value(true))
                .andExpect(jsonPath("$[?(@.nom == 'adresseSav')].parDefaut").value(false))
                .andExpect(jsonPath("$[?(@.nom == 'visibleSav')]").isNotEmpty());
    }

    // ------------------------------------------------------------------ projection

    @Test
    void r4_4_projection_seulesLesColonnesDemandeesPlusId() throws Exception {
        CfgTranche tranche = tranche("PROJ");
        String ligne = ligneDe(tranche);

        mockMvc.perform(get(TRANCHES).param(COLONNES, "code,adresseSav").param(SEARCH, tranche.getCode())
                        .param(SIZE, TOUS).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne).isNotEmpty())
                .andExpect(jsonPath(ligne + ".code").value(tranche.getCode()))
                .andExpect(jsonPath(ligne + ".adresseSav").value("SAV PROJ"))
                .andExpect(jsonPath("$.content[0].ville").doesNotExist())
                .andExpect(jsonPath("$.content[0].description").doesNotExist());
    }

    @Test
    void r4_4_sansColonnes_lesColonnesParDefautSeules() throws Exception {
        CfgTranche tranche = tranche("DEFAUT");

        mockMvc.perform(get(TRANCHES).param(SEARCH, tranche.getCode()).param(SIZE, TOUS).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(tranche.getId().toString()))
                .andExpect(jsonPath("$.content[0].code").value(tranche.getCode()))
                .andExpect(jsonPath("$.content[0].estEnVente").value(true))
                .andExpect(jsonPath("$.content[0].adresseSav").doesNotExist());
    }

    @Test
    void r4_4_colonneInconnue_400_nommeLaColonneEtLesNomsValides() throws Exception {
        mockMvc.perform(get(TRANCHES).param(COLONNES, "pasUneColonne").with(connecte(admin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("pasUneColonne")))
                .andExpect(content().string(containsString("adresseSav")));
    }

    // ------------------------------------------------------------------ recherche

    @Test
    void r4_4_search_surLeCode_insensibleALaCasse() throws Exception {
        CfgTranche cible = tranche("CIBLE");
        CfgTranche autre = fixture.tranche("TR-AUTRE-" + UUID.randomUUID().toString().substring(0, 8), projet);

        mockMvc.perform(get(TRANCHES).param(SEARCH, cible.getCode().toLowerCase(Locale.ROOT)).param(SIZE, TOUS)
                        .with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligneDe(cible)).isNotEmpty())
                .andExpect(jsonPath(ligneDe(autre)).isEmpty());
    }

    @Test
    void r4_4_search_etFiltreProjet_secombinent() throws Exception {
        CfgTranche cible = tranche("COMBI");
        CfgProject autreProjet = fixture.projet("PRJ-AUTRE-" + jeton, fixture.entite("ENT-AUTRE-" + jeton, true));

        mockMvc.perform(get(TRANCHES).param(SEARCH, cible.getCode()).param("projetId", autreProjet.getId().toString())
                        .with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligneDe(cible)).isEmpty());
    }
}
