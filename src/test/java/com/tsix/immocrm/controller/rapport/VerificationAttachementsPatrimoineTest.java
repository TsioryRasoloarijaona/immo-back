package com.tsix.immocrm.controller.rapport;

import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
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
import java.util.UUID;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.descripteur;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.export;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignesOu;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.nombre;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.resultat;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.section;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.somme;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.totaux;
import static com.tsix.immocrm.controller.rapport.RapportsPatrimoineSupport.FORMAT;
import static com.tsix.immocrm.controller.rapport.RapportsPatrimoineSupport.PATRIMOINE;
import static com.tsix.immocrm.controller.rapport.RapportsPatrimoineSupport.TRANCHE_ID;
import static com.tsix.immocrm.controller.rapport.RapportsPatrimoineSupport.XLSX;
import static com.tsix.immocrm.controller.rapport.RapportsPatrimoineSupport.colonneDeLibelle;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureRapportsPatrimoine.VERIFICATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * VÉRIFICATION DES ATTACHEMENTS — articles (FEATURE-patrimoine-rapports.md §6, T7 ; Q6 : une ligne par article).
 * <pre>
 *   TV (en vente) : v1 VENTE  — 2 pièces PLAN, 1 pièce PLAN cochée non jointe, 1 pièce sans type
 *                   v2 VENTE  — aucune pièce                 v3 LOCATION — même libellé que v2, 1 pièce ETAGE
 *                   vs sous-article de v1 — 1 pièce PLAN     vp PROJET — 1 pièce PLAN
 *   TN (PAS en vente) : n1 VENTE — 1 pièce PLAN
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VerificationAttachementsPatrimoineTest {

    private static final String PIECES = "pieces";
    private static final String CODE = "code";
    private static final String TOTAL = "total";
    private static final String SANS_TYPE = "sansType";
    private static final String COCHES = "coches";
    private static final String AVEC_SOUS_ARTICLES = "avecSousArticles";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports fixture;
    @Autowired private FixtureRapportsPatrimoine rp;
    @Autowired private FixtureEtatVentes ev;

    private AppelsRapports appels;
    private RequestPostProcessor libre;
    private String typePlan;
    private String typeEtage;
    private CfgTranche tv;
    private StkItem v1;
    private StkItem v2;
    private StkItem v3;
    private StkItem vs;
    private StkItem vp;
    private StkItem n1;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        String prefixe = FixtureEtatVentes.prefixe("VAT");
        rp.rapportsP10a();
        libre = connecte(fixture.compte("vat." + prefixe, fixture.role("Libre " + prefixe, false)));
        CfgProject projet = ev.projet("P-" + prefixe, true);
        tv = ev.tranche("TV-" + prefixe, projet, true);
        CfgTranche tn = ev.tranche("TN-" + prefixe, projet, false);
        StkItemCategory categorie = ev.categorie("CAT-" + prefixe);
        StkItemType type = ev.type("TYP-" + prefixe);
        typePlan = "PLAN-" + prefixe;
        typeEtage = "ETAGE-" + prefixe;
        UUID plan = rp.typePiece(typePlan);
        UUID etage = rp.typePiece(typeEtage);

        v1 = ev.article(prefixe + "-V1", tv, categorie, type);
        v2 = ev.article(prefixe + "-V2", tv, categorie, type);
        v3 = ev.article(prefixe + "-V3", tv, categorie, type, rp.location());
        v3.setDescription(v2.getDescription());
        v3 = rp.enregistrer(v3);
        vs = ev.sousArticle(prefixe + "-VS", v1, categorie, type);
        vp = ev.article(prefixe + "-VP", tv, categorie, type, ev.projetVocation());
        n1 = ev.article(prefixe + "-N1", tn, categorie, type);

        rp.piece(v1, plan, false);
        rp.piece(v1, plan, false);
        rp.piece(v1, plan, true);
        rp.piece(v1, null, false);
        rp.piece(v3, etage, false);
        rp.piece(vs, plan, false);
        rp.piece(vp, plan, false);
        rp.piece(n1, plan, false);
        fixture.relire();
    }

    private MockHttpServletRequestBuilder surTv() {
        return get(resultat(PATRIMOINE, VERIFICATION)).param(TRANCHE_ID, tv.getId().toString()).with(libre);
    }

    private Map<String, Object> pieces(MockHttpServletRequestBuilder requete) throws Exception {
        return section(appels.sections(requete), PIECES);
    }

    private static Map<String, Object> ligneDe(Map<String, Object> pieces, StkItem article) {
        List<Map<String, Object>> trouvees = lignesOu(pieces, CODE, article.getCode());
        assertThat(trouvees).as("ligne de %s", article.getCode()).hasSize(1);
        return trouvees.get(0);
    }

    private static List<Object> codes(Map<String, Object> pieces) {
        return lignes(pieces).stream().map(l -> l.get(CODE)).toList();
    }

    // ------------------------------------------------------------------ comptage par type

    @Test
    void t7_parTypeDePiece_sansType_total_cochees() throws Exception {
        Map<String, Object> pieces = pieces(surTv());
        String colonnePlan = colonneDeLibelle(pieces, typePlan);
        Map<String, Object> ligne = ligneDe(pieces, v1);

        assertThat(colonnePlan).as("colonne dynamique « %s »", typePlan).isNotNull();
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(nombre(ligne.get(colonnePlan))).isEqualByComparingTo("3");
        soft.assertThat(nombre(ligne.get(SANS_TYPE))).isEqualByComparingTo("1");
        soft.assertThat(nombre(ligne.get(TOTAL))).isEqualByComparingTo("4");
        soft.assertThat(nombre(ligne.get(COCHES))).isEqualByComparingTo("1");
        soft.assertAll();
    }

    @Test
    void t7_articleSansPiece_ligneAZero() throws Exception {
        Map<String, Object> ligne = ligneDe(pieces(surTv()), v2);

        assertThat(nombre(ligne.get(TOTAL))).isEqualByComparingTo("0");
    }

    @Test
    void t7_deuxArticlesDeMemeLibelle_deuxLignes() throws Exception {
        Map<String, Object> pieces = pieces(surTv());

        assertThat(lignesOu(pieces, "article", v2.getDescription())).hasSize(2);
        assertThat(nombre(ligneDe(pieces, v3).get(TOTAL))).isEqualByComparingTo("1");
        assertThat(nombre(ligneDe(pieces, v2).get(TOTAL))).isEqualByComparingTo("0");
    }

    @Test
    void t7_colonnesDynamiques_typesPresentsSeulement() throws Exception {
        Map<String, Object> pieces = pieces(surTv());

        assertThat(colonneDeLibelle(pieces, typeEtage)).isNotNull();
        assertThat(colonneDeLibelle(pieces, "Sans type")).isEqualTo(SANS_TYPE);
        assertThat(colonneDeLibelle(pieces, "Fichiers attachés")).isEqualTo(TOTAL);
    }

    @Test
    void t7_vocationsVenteEtLocation_seulement() throws Exception {
        assertThat(codes(pieces(surTv()))).contains(v1.getCode(), v2.getCode(), v3.getCode())
                .doesNotContain(vp.getCode());
    }

    @Test
    void t7_totaux_sommeDesLignes() throws Exception {
        Map<String, Object> pieces = pieces(surTv());

        assertThat(nombre(totaux(pieces).get(TOTAL))).isEqualByComparingTo(somme(lignes(pieces), TOTAL))
                .isEqualByComparingTo("5");
    }

    @Test
    void t7_groupeParTranche() throws Exception {
        assertThat(pieces(surTv())).containsEntry("groupePar", "tranche");
    }

    // ------------------------------------------------------------------ sous-articles

    @Test
    void t7_avecSousArticlesFaux_parDefaut_sousArticleExclu() throws Exception {
        assertThat(codes(pieces(surTv()))).doesNotContain(vs.getCode());
        assertThat(codes(pieces(surTv().param(AVEC_SOUS_ARTICLES, "false")))).doesNotContain(vs.getCode());
    }

    @Test
    void t7_avecSousArticlesVrai_sousArticleCompte() throws Exception {
        Map<String, Object> pieces = pieces(surTv().param(AVEC_SOUS_ARTICLES, "true"));

        assertThat(nombre(ligneDe(pieces, vs).get(TOTAL))).isEqualByComparingTo("1");
    }

    // ------------------------------------------------------------------ tranches en vente

    @Test
    void t7_sansTrancheId_trancheHorsVenteExclue() throws Exception {
        List<Object> codes = codes(pieces(get(resultat(PATRIMOINE, VERIFICATION)).with(libre)));

        assertThat(codes).contains(v1.getCode()).doesNotContain(n1.getCode());
    }

    // ------------------------------------------------------------------ paramètres

    @Test
    void parametres_tranches_avecSousArticlesBooleenFauxParDefaut() throws Exception {
        mockMvc.perform(get(descripteur(PATRIMOINE, VERIFICATION)).with(libre))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parametres[?(@.nom == 'trancheId')].type").value("TRANCHES"))
                .andExpect(jsonPath("$.parametres[?(@.nom == 'avecSousArticles')].type").value("BOOLEEN"))
                .andExpect(jsonPath("$.parametres[?(@.nom == 'avecSousArticles')].valeurParDefaut").value("false"));
    }

    @Test
    void parametres_avecSousArticlesNonBooleen_400() throws Exception {
        assertThat(appels.statut(surTv().param(AVEC_SOUS_ARTICLES, "oui"))).isEqualTo(400);
    }

    // ------------------------------------------------------------------ T9 : export

    @Test
    void t9_export_uneFeuilleParSection_ligneTotal() throws Exception {
        MockHttpServletRequestBuilder export = get(export(PATRIMOINE, VERIFICATION)).param(FORMAT, XLSX)
                .param(TRANCHE_ID, tv.getId().toString()).with(libre);

        RapportsPatrimoineSupport.verifierExport(appels, surTv(), export);
    }
}
