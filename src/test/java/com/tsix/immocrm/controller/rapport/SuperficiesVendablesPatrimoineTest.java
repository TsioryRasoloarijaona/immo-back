package com.tsix.immocrm.controller.rapport;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.fixtures.FixtureRapportsPatrimoine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
import static com.tsix.immocrm.fixtures.FixtureRapportsPatrimoine.SUPERFICIES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * ÉTAT DES SUPERFICIES VENDABLES (FEATURE-patrimoine-rapports.md §4, T6). Tranche TS (en vente) :
 * <pre>
 *   a1 VENTE libre (50 m²)        s2 sous-article de a1, NON toujours affecté (13 m²)
 *                                 s3 sous-article de a1, toujours affecté, VENDU (17 m²)
 *   a2 VENTE vendue (70 m², dossier SANS statut)   s1 sous-article de a2, toujours affecté, libre (11 m²)
 *   a3 article PROJET (90 m²)
 * </pre>
 * Les sous-articles n'ont pas de colonne code : ils se reconnaissent au libellé du parent et à la surface.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SuperficiesVendablesPatrimoineTest {

    private static final String ETAT = "etat";
    private static final String LIBRE = "LIBRE";
    private static final String VENDU = "VENDU";
    private static final String ARTICLES = "articles";
    private static final String SOUS_ARTICLES = "sous-articles";
    private static final String CODE = "code";
    private static final String SURFACE = "surface";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports fixture;
    @Autowired private FixtureRapportsPatrimoine rp;
    @Autowired private FixtureEtatVentes ev;

    private AppelsRapports appels;
    private RequestPostProcessor libre;
    private CfgTranche ts;
    private StkItem a1;
    private StkItem a2;
    private StkItem a3;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        String prefixe = FixtureEtatVentes.prefixe("SUP");
        rp.rapportsP10a();
        libre = connecte(fixture.compte("sup." + prefixe, fixture.role("Libre " + prefixe, false)));
        ts = ev.tranche("TS-" + prefixe, ev.projet("P-" + prefixe, true), true);
        StkItemCategory categorie = ev.categorie("CAT-" + prefixe);
        StkItemType type = ev.type("TYP-" + prefixe);

        a1 = rp.mesures(ev.article(prefixe + "-A1", ts, categorie, type), null, null, "50", null);
        a2 = rp.mesures(ev.article(prefixe + "-A2", ts, categorie, type), null, null, "70", null);
        ev.vendre(a2, "DOS-" + prefixe + "-A2", null, null, null);
        a3 = rp.mesures(ev.article(prefixe + "-A3", ts, categorie, type, ev.projetVocation()), null, null, "90", null);
        sousArticle(ev.sousArticle(prefixe + "-S1", a2, categorie, type), "11", true);
        sousArticle(ev.sousArticle(prefixe + "-S2", a1, categorie, type), "13", false);
        StkItem s3 = sousArticle(ev.sousArticle(prefixe + "-S3", a1, categorie, type), "17", true);
        ev.vendre(s3, "DOS-" + prefixe + "-S3", null, null, null);
        fixture.relire();
    }

    private StkItem sousArticle(StkItem s, String surface, boolean toujoursAffecte) {
        s.setArea(new BigDecimal(surface));
        s.setAlwaysAffected(toujoursAffecte);
        return rp.enregistrer(s);
    }

    private MockHttpServletRequestBuilder requete(String... etats) {
        MockHttpServletRequestBuilder requete = get(resultat(PATRIMOINE, SUPERFICIES))
                .param(TRANCHE_ID, ts.getId().toString()).with(libre);
        if (etats.length > 0) {
            requete.param(ETAT, etats);
        }
        return requete;
    }

    private List<Object> codesArticles(String... etats) throws Exception {
        return lignes(section(appels.sections(requete(etats)), ARTICLES)).stream().map(l -> l.get(CODE)).toList();
    }

    /** Surfaces des sous-articles dont le parent est {@code parent}. */
    private static List<BigDecimal> surfacesSousArticles(List<Map<String, Object>> sections, StkItem parent) {
        return lignes(section(sections, SOUS_ARTICLES)).stream()
                .filter(l -> Objects.equals(parent.getDescription(), l.get("article")))
                .map(l -> nombre(l.get(SURFACE)).stripTrailingZeros()).toList();
    }

    // ------------------------------------------------------------------ articles

    @Test
    void t6_parDefaut_articlesVenteLibresEtVendus_horsAutreVocation() throws Exception {
        assertThat(codesArticles()).containsExactlyInAnyOrder(a1.getCode(), a2.getCode()).doesNotContain(a3.getCode());
    }

    @Test
    void t6_etatLibre_exclutLArticleAvecDossier() throws Exception {
        assertThat(codesArticles(LIBRE)).containsExactly(a1.getCode());
    }

    @Test
    void t6_etatVendu_dossierSansStatutCompteVendu() throws Exception {
        assertThat(codesArticles(VENDU)).containsExactly(a2.getCode());
    }

    @Test
    void t6_colonneEtat_libreOuVendu() throws Exception {
        List<Map<String, Object>> lignes = lignes(section(appels.sections(requete()), ARTICLES));

        assertThat(lignes).extracting(l -> l.get(CODE) + "=" + l.get(ETAT))
                .containsExactlyInAnyOrder(a1.getCode() + "=Libre", a2.getCode() + "=Vendu");
    }

    @Test
    void t6_totalSurface_sommeDesLignes() throws Exception {
        Map<String, Object> articles = section(appels.sections(requete()), ARTICLES);

        assertThat(nombre(totaux(articles).get(SURFACE))).isEqualByComparingTo(somme(lignes(articles), SURFACE))
                .isEqualByComparingTo("120");
    }

    @Test
    void t6_articles_groupeParTranche() throws Exception {
        assertThat(section(appels.sections(requete()), ARTICLES)).containsEntry("groupePar", "tranche");
    }

    // ------------------------------------------------------------------ sous-articles

    @Test
    void t6_sousArticleNonToujoursAffecte_exclu() throws Exception {
        List<Map<String, Object>> sections = appels.sections(requete());

        assertThat(surfacesSousArticles(sections, a1)).containsExactly(new BigDecimal("17"));
        assertThat(surfacesSousArticles(sections, a2)).containsExactly(new BigDecimal("11"));
    }

    @Test
    void t6_sousArticleFiltreSurSonPropreEtat_libreSousParentVendu() throws Exception {
        List<Map<String, Object>> sections = appels.sections(requete(LIBRE));

        assertThat(surfacesSousArticles(sections, a2)).containsExactly(new BigDecimal("11"));
        assertThat(surfacesSousArticles(sections, a1)).isEmpty();
    }

    @Test
    void t6_sousArticleFiltreSurSonPropreEtat_venduSousParentLibre() throws Exception {
        List<Map<String, Object>> sections = appels.sections(requete(VENDU));

        assertThat(surfacesSousArticles(sections, a1)).containsExactly(new BigDecimal("17"));
        assertThat(surfacesSousArticles(sections, a2)).isEmpty();
    }

    @Test
    void t6_sousArticles_totalSurface() throws Exception {
        Map<String, Object> sousArticles = section(appels.sections(requete()), SOUS_ARTICLES);

        assertThat(nombre(totaux(sousArticles).get(SURFACE))).isEqualByComparingTo("28");
    }

    // ------------------------------------------------------------------ paramètres

    @Test
    void parametres_etat_libreEtVendu() throws Exception {
        List<String> options = JsonPath.read(appels.corps(get(descripteur(PATRIMOINE, SUPERFICIES)).with(libre)),
                "$.parametres[?(@.nom == 'etat')].options[*].valeur");

        assertThat(options).containsExactlyInAnyOrder(LIBRE, VENDU);
    }

    @Test
    void parametres_etatInconnu_400() throws Exception {
        assertThat(appels.statut(requete("RESERVE"))).isEqualTo(400);
    }

    // ------------------------------------------------------------------ T9 : export

    @Test
    void t9_export_uneFeuilleParSection_ligneTotal() throws Exception {
        MockHttpServletRequestBuilder export = get(export(PATRIMOINE, SUPERFICIES)).param(FORMAT, XLSX)
                .param(TRANCHE_ID, ts.getId().toString()).with(libre);

        RapportsPatrimoineSupport.verifierExport(appels, requete(), export);
    }
}
