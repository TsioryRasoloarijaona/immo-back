package com.tsix.immocrm.controller.rapport;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.fixtures.FixtureRapportsPatrimoine;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.descripteur;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.export;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.nombre;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.resultat;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.section;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.somme;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.totaux;
import static com.tsix.immocrm.controller.rapport.RapportsPatrimoineSupport.FORMAT;
import static com.tsix.immocrm.controller.rapport.RapportsPatrimoineSupport.PATRIMOINE;
import static com.tsix.immocrm.controller.rapport.RapportsPatrimoineSupport.TRANCHE_ID;
import static com.tsix.immocrm.controller.rapport.RapportsPatrimoineSupport.XLSX;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureRapportsPatrimoine.VALORISATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * VALORISATION DU STOCK (FEATURE-patrimoine-rapports.md §2, T4) :
 * <pre>
 *   E1 (active) / P1  : T1 = a1 VENTE article (50 | 500 000 | 60 | 600 000), a2 LOCATION article (40 | 300 000 | 45 | 350 000),
 *                            s1 VENTE sous-article de a1 (10 | 20 000 | 12 | 25 000)
 *   E1 (active) / P1B : T2 = b1 VENTE article (70 | 700 000 | 80 | 800 000), b2 VENTE article (NULL | NULL | 30 | 100 000)
 *   E2 (active) / P2 : T3 = c1 VENTE article (100 | 1 000 000 | 110 | 1 100 000)
 *   EI (INACTIVE)    : TI = i1 VENTE article (999 | 9 999 999 | 999 | 9 999 999)
 * </pre>
 * Mesures = (AreaFloor | SalePriceFloor | Area | SalePrice). Chaque appel filtre sur les tranches du test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ValorisationStockPatrimoineTest {

    private static final String NB_ARTICLES = "nbArticles";
    private static final String SURFACE_BASE = "surfaceBase";
    private static final String VALO_BASE = "valorisationBase";
    private static final String SURFACE_FINALE = "surfaceFinale";
    private static final String VALO_FINALE = "valorisationFinale";
    private static final List<String> MESURES = List.of(NB_ARTICLES, SURFACE_BASE, VALO_BASE, SURFACE_FINALE,
            VALO_FINALE);
    private static final List<String> SECTIONS = List.of("detail", "programmes", "tranches");
    private static final String DETAIL = "detail";
    private static final String ENTITE_ID = "entiteId";
    private static final String VOCATION = "vocation";
    private static final String NATURE = "nature";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports fixture;
    @Autowired private FixtureRapportsPatrimoine rp;
    @Autowired private FixtureEtatVentes ev;
    @Autowired private FixturePatrimoine patrimoine;

    private AppelsRapports appels;
    private RequestPostProcessor libre;
    private CfgCompany e1;
    private CfgCompany e2;
    private CfgCompany ei;
    private CfgTranche t1;
    private CfgTranche t2;
    private CfgTranche t3;
    private CfgTranche ti;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        String prefixe = FixtureEtatVentes.prefixe("VAL");
        rp.rapportsP10a();
        libre = connecte(fixture.compte("val." + prefixe, fixture.role("Libre " + prefixe, false)));

        e1 = patrimoine.entite("E1-" + prefixe, true);
        e2 = patrimoine.entite("E2-" + prefixe, true);
        ei = patrimoine.entite("EI-" + prefixe, false);
        t1 = ev.tranche("T1-" + prefixe, patrimoine.projet("P1-" + prefixe, e1), true);
        t2 = ev.tranche("T2-" + prefixe, patrimoine.projet("P1B-" + prefixe, e1), true);
        t3 = ev.tranche("T3-" + prefixe, patrimoine.projet("P2-" + prefixe, e2), true);
        ti = ev.tranche("TI-" + prefixe, patrimoine.projet("PI-" + prefixe, ei), true);
        StkItemCategory categorie = ev.categorie("CAT-" + prefixe);
        StkItemType type = ev.type("TYP-" + prefixe);

        StkItem a1 = rp.mesures(ev.article(prefixe + "-A1", t1, categorie, type), "50", "500000", "60", "600000");
        rp.mesures(ev.article(prefixe + "-A2", t1, categorie, type, rp.location()), "40", "300000", "45", "350000");
        rp.mesures(ev.sousArticle(prefixe + "-S1", a1, categorie, type), "10", "20000", "12", "25000");
        rp.mesures(ev.article(prefixe + "-B1", t2, categorie, type), "70", "700000", "80", "800000");
        rp.mesures(ev.article(prefixe + "-B2", t2, categorie, type), null, null, "30", "100000");
        rp.mesures(ev.article(prefixe + "-C1", t3, categorie, type), "100", "1000000", "110", "1100000");
        rp.mesures(ev.article(prefixe + "-I1", ti, categorie, type), "999", "9999999", "999", "9999999");
        fixture.relire();
    }

    private MockHttpServletRequestBuilder sur(CfgTranche... tranches) {
        MockHttpServletRequestBuilder requete = get(resultat(PATRIMOINE, VALORISATION)).with(libre);
        for (CfgTranche t : tranches) {
            requete.param(TRANCHE_ID, t.getId().toString());
        }
        return requete;
    }

    private MockHttpServletRequestBuilder tout() {
        return sur(t1, t2, t3);
    }

    /** Les 5 mesures attendues, dans l'ordre de {@link #MESURES}. */
    private static void verifierTotaux(SoftAssertions soft, Map<String, Object> section, String... attendus) {
        for (int i = 0; i < MESURES.size(); i++) {
            String mesure = MESURES.get(i);
            soft.assertThat(somme(lignes(section), mesure)).as("Σ lignes %s.%s", section.get("id"), mesure)
                    .isEqualByComparingTo(attendus[i]);
            soft.assertThat(nombre(totaux(section).get(mesure))).as("total %s.%s", section.get("id"), mesure)
                    .isEqualByComparingTo(attendus[i]);
        }
    }

    // ------------------------------------------------------------------ T4 : un seul calcul, trois sections

    @Test
    void t4_troisSections_memesTotaux_egauxALaSommeDesArticles() throws Exception {
        List<Map<String, Object>> sections = appels.sections(tout());

        SoftAssertions soft = new SoftAssertions();
        for (String id : SECTIONS) {
            verifierTotaux(soft, section(sections, id), "6", "270", "2520000", "337", "2975000");
        }
        soft.assertAll();
    }

    @Test
    void t4_programmesUneLigneParProgramme_tranchesUneLigneParTranche() throws Exception {
        List<Map<String, Object>> sections = appels.sections(tout());

        assertThat(lignes(section(sections, "programmes"))).hasSize(3);
        assertThat(lignes(section(sections, "tranches"))).hasSize(3);
    }

    @Test
    void t4_detail_groupeParTranche() throws Exception {
        assertThat(section(appels.sections(tout()), DETAIL)).containsEntry("groupePar", "tranche");
    }

    @Test
    void t4_mesuresNulles_compteesAZero_articleCompte() throws Exception {
        Map<String, Object> detail = section(appels.sections(sur(t2)), DETAIL);

        SoftAssertions soft = new SoftAssertions();
        verifierTotaux(soft, detail, "2", "70", "700000", "110", "900000");
        soft.assertAll();
    }

    @Test
    void t4_filtreEntite_seulesLesTranchesDeLEntite() throws Exception {
        Map<String, Object> detail = section(appels.sections(tout().param(ENTITE_ID, e2.getId().toString())), DETAIL);

        SoftAssertions soft = new SoftAssertions();
        verifierTotaux(soft, detail, "1", "100", "1000000", "110", "1100000");
        soft.assertAll();
    }

    @Test
    void t4_filtreVocation_location() throws Exception {
        Map<String, Object> detail = section(appels.sections(tout().param(VOCATION, "LOCATION")), DETAIL);

        SoftAssertions soft = new SoftAssertions();
        verifierTotaux(soft, detail, "1", "40", "300000", "45", "350000");
        soft.assertAll();
    }

    @Test
    void t4_filtreNature_sousArticleSeul() throws Exception {
        Map<String, Object> detail = section(appels.sections(tout().param(NATURE, "SOUS_ARTICLE")), DETAIL);

        SoftAssertions soft = new SoftAssertions();
        verifierTotaux(soft, detail, "1", "10", "20000", "12", "25000");
        soft.assertAll();
    }

    /** E1 ∩ VENTE ∩ ARTICLE = a1, b1, b2. */
    @Test
    void t4_filtresCombines_entiteVocationNature() throws Exception {
        List<Map<String, Object>> sections = appels.sections(tout().param(ENTITE_ID, e1.getId().toString())
                .param(VOCATION, "VENTE").param(NATURE, "ARTICLE"));

        SoftAssertions soft = new SoftAssertions();
        for (String id : SECTIONS) {
            verifierTotaux(soft, section(sections, id), "3", "120", "1200000", "170", "1500000");
        }
        soft.assertAll();
    }

    @Test
    void t4_filtresMultiples_deuxNatures_egalSansFiltre() throws Exception {
        Map<String, Object> detail = section(appels.sections(tout().param(NATURE, "ARTICLE", "SOUS_ARTICLE")),
                DETAIL);

        SoftAssertions soft = new SoftAssertions();
        verifierTotaux(soft, detail, "6", "270", "2520000", "337", "2975000");
        soft.assertAll();
    }

    // ------------------------------------------------------------------ T3 : entité inactive

    @Test
    void t3_trancheDEntiteInactive_absenteDesTotaux() throws Exception {
        Map<String, Object> detail = section(appels.sections(sur(t3, ti)), DETAIL);

        SoftAssertions soft = new SoftAssertions();
        verifierTotaux(soft, detail, "1", "100", "1000000", "110", "1100000");
        soft.assertAll();
    }

    @Test
    void t3_trancheDEntiteInactiveSeule_sectionsVides() throws Exception {
        List<Map<String, Object>> sections = appels.sections(sur(ti));

        assertThat(sections).isNotEmpty().hasSize(SECTIONS.size()).allSatisfy(s -> assertThat(lignes(s)).isEmpty());
    }

    // ------------------------------------------------------------------ paramètres (§2)

    @Test
    void parametres_entiteVocationNatureTranches() throws Exception {
        mockMvc.perform(get(descripteur(PATRIMOINE, VALORISATION)).with(libre))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parametres[?(@.nom == 'entiteId')].type").value("CHOIX"))
                .andExpect(jsonPath("$.parametres[?(@.nom == 'entiteId')].multiple").value(true))
                .andExpect(jsonPath("$.parametres[?(@.nom == 'vocation')].multiple").value(true))
                .andExpect(jsonPath("$.parametres[?(@.nom == 'nature')].multiple").value(true))
                .andExpect(jsonPath("$.parametres[?(@.nom == 'trancheId')].type").value("TRANCHES"));
    }

    @Test
    void parametres_optionsEntite_activesSeulement() throws Exception {
        List<String> options = JsonPath.read(appels.corps(get(descripteur(PATRIMOINE, VALORISATION)).with(libre)),
                "$.parametres[?(@.nom == 'entiteId')].options[*].valeur");

        assertThat(options).contains(e1.getId().toString(), e2.getId().toString())
                .doesNotContain(ei.getId().toString());
    }

    @Test
    void parametres_optionsVocation_parCode_optionsNature() throws Exception {
        String corps = appels.corps(get(descripteur(PATRIMOINE, VALORISATION)).with(libre));

        List<String> vocations = JsonPath.read(corps, "$.parametres[?(@.nom == 'vocation')].options[*].valeur");
        List<String> natures = JsonPath.read(corps, "$.parametres[?(@.nom == 'nature')].options[*].valeur");
        assertThat(vocations).contains("VENTE", "LOCATION");
        assertThat(natures).containsExactlyInAnyOrder("ARTICLE", "SOUS_ARTICLE");
    }

    @Test
    void parametres_entiteInactive_refusee400() throws Exception {
        assertThat(appels.statut(tout().param(ENTITE_ID, ei.getId().toString()))).isEqualTo(400);
    }

    @Test
    void parametres_natureInconnue_400() throws Exception {
        assertThat(appels.statut(tout().param(NATURE, "TERRAIN"))).isEqualTo(400);
    }

    // ------------------------------------------------------------------ T9 : export

    @Test
    void t9_export_uneFeuilleParSection_ligneTotal() throws Exception {
        MockHttpServletRequestBuilder export = get(export(PATRIMOINE, VALORISATION)).param(FORMAT, XLSX)
                .with(libre);
        for (CfgTranche t : List.of(t1, t2, t3)) {
            export.param(TRANCHE_ID, t.getId().toString());
        }

        RapportsPatrimoineSupport.verifierExport(appels, tout(), export);
    }
}
