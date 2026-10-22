package com.tsix.immocrm.controller.rapport;

import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureEtatVentes.Nature;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.repositories.str.StrStatusRepository;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.VENTE;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.colonnes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.colonnesAvecTotal;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignesOu;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.nombre;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.resultat;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.section;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.somme;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.sousTotaux;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.totaux;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureRapports.SUIVI_GLOBAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * N° 12 SUIVI GLOBAL DE VENTE (FEATURE-tdb-vente-rapports-socle.md §6) et totaux serveur (§4.3) :
 * <pre>
 *   TA : a1 vendu, a2 libre, a3 libre (IsUnderItem = 0, VENTE) + s1 sous-article (IsUnderItem = 1) + h1 hors VENTE
 *   TB : b1 vendu, b2 libre
 *   TC : c1 retenu par un dossier SANS statut (StrStatusId NULL)
 *   TD : d1 retenu par un dossier ANNULÉ, échéance d'avance 777 777
 * </pre>
 * « Vendu » = statut {@code BoolField001 = 1}. Chaque appel filtre sur les tranches du test ({@code trancheId}).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SuiviGlobalVenteRapportTest {

    private static final String TRANCHE_ID = "trancheId";
    private static final String NB_ARTICLES = "nbArticles";
    private static final String NB_VENDUS = "nbVendus";
    private static final String NB_LIBRES = "nbLibres";
    private static final String TAUX_VENTE = "tauxVente";
    private static final String TRANCHE_CODE = "trancheCode";
    private static final String TRANCHE_LIBELLE = "trancheLibelle";
    private static final String STOCK = "stock";
    private static final String STATUTS = "statuts";
    private static final String SOMME = "SOMME";
    private static final BigDecimal MONTANT_ANNULE = new BigDecimal("777777");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports fixture;
    @Autowired private FixtureEtatVentes ev;
    @Autowired private StrStatusRepository statuts;

    private AppelsRapports appels;
    private RequestPostProcessor libre;
    private CfgTranche ta;
    private CfgTranche tb;
    private CfgTranche tc;
    private CfgTranche td;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        String prefixe = FixtureEtatVentes.prefixe("SGV");
        fixture.tables();
        fixture.rapportVente(SUIVI_GLOBAL, "SUIVI GLOBAL DE VENTE*", "", 810);
        libre = connecte(fixture.compte("sgv." + prefixe, fixture.role("Libre " + prefixe, false)));

        CfgProject projet = ev.projet("P-" + prefixe, true);
        ta = ev.tranche("TA-" + prefixe, projet, true);
        tb = ev.tranche("TB-" + prefixe, projet, true);
        tc = ev.tranche("TC-" + prefixe, projet, true);
        td = ev.tranche("TD-" + prefixe, projet, true);
        StkItemCategory categorie = ev.categorie("CAT-" + prefixe);
        StkItemType type = ev.type("TYP-" + prefixe);
        StrStatus vendu = ev.statutEnCours("VENDU-" + prefixe);
        vendu.setVenteEffective(true);
        vendu.setSorting(50);
        vendu = statuts.save(vendu);

        StkItem a1 = ev.article(prefixe + "-A1", ta, categorie, type);
        ev.vendre(a1, "DOS-" + prefixe + "-A1", vendu, null, null);
        ev.article(prefixe + "-A2", ta, categorie, type);
        ev.article(prefixe + "-A3", ta, categorie, type);
        ev.sousArticle(prefixe + "-S1", a1, categorie, type);
        ev.article(prefixe + "-H1", ta, categorie, type, ev.projetVocation());

        ev.vendre(ev.article(prefixe + "-B1", tb, categorie, type), "DOS-" + prefixe + "-B1", vendu, null, null);
        ev.article(prefixe + "-B2", tb, categorie, type);

        ev.vendre(ev.article(prefixe + "-C1", tc, categorie, type), "DOS-" + prefixe + "-C1", null, null, null);

        ComFolder annule = ev.vendre(ev.article(prefixe + "-D1", td, categorie, type), "DOS-" + prefixe + "-D1",
                ev.statutSortie("ANN-" + prefixe, true), null, null);
        ev.prix(annule, MONTANT_ANNULE);
        ev.echeance(annule, Nature.AVANCE, true, MONTANT_ANNULE);
        fixture.relire();
    }

    private MockHttpServletRequestBuilder sur(CfgTranche... tranches) {
        MockHttpServletRequestBuilder requete = get(resultat(VENTE, SUIVI_GLOBAL)).with(libre);
        for (CfgTranche t : tranches) {
            requete.param(TRANCHE_ID, t.getId().toString());
        }
        return requete;
    }

    private static Map<String, Object> ligneDe(Map<String, Object> section, CfgTranche t) {
        List<Map<String, Object>> trouvees = lignesOu(section, TRANCHE_CODE, t.getCode());
        assertThat(trouvees).as("ligne de %s", t.getCode()).hasSize(1);
        return trouvees.get(0);
    }

    // ------------------------------------------------------------------ section stock

    @Test
    void stock_articlesIsUnderItem0VenteSeulement_vendusEtLibres() throws Exception {
        Map<String, Object> stock = section(appels.sections(sur(ta, tb)), STOCK);

        Map<String, Object> ligneA = ligneDe(stock, ta);
        Map<String, Object> ligneB = ligneDe(stock, tb);
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(nombre(ligneA.get(NB_ARTICLES))).isEqualByComparingTo("3");
        soft.assertThat(nombre(ligneA.get(NB_VENDUS))).isEqualByComparingTo("1");
        soft.assertThat(nombre(ligneA.get(NB_LIBRES))).isEqualByComparingTo("2");
        soft.assertThat(nombre(ligneB.get(NB_ARTICLES))).isEqualByComparingTo("2");
        soft.assertThat(nombre(ligneB.get(NB_VENDUS))).isEqualByComparingTo("1");
        soft.assertAll();
    }

    @Test
    void stock_nbLibres_egalArticlesMoinsVendus_surChaqueLigne() throws Exception {
        List<Map<String, Object>> lignes = lignes(section(appels.sections(sur(ta, tb)), STOCK));

        assertThat(lignes).isNotEmpty().allSatisfy(l -> assertThat(nombre(l.get(NB_LIBRES)))
                .isEqualByComparingTo(nombre(l.get(NB_ARTICLES)).subtract(nombre(l.get(NB_VENDUS)))));
    }

    @Test
    void stock_totalNbArticles_egalSommeDesLignes_egalArticlesDuTest() throws Exception {
        Map<String, Object> stock = section(appels.sections(sur(ta, tb)), STOCK);

        assertThat(nombre(totaux(stock).get(NB_ARTICLES)))
                .isEqualByComparingTo(somme(lignes(stock), NB_ARTICLES))
                .isEqualByComparingTo("5");
    }

    /** Taux imposé dans {@code totaux} : somme vendus / somme articles (2 / 5), pas la somme ni la moyenne des taux. */
    @Test
    void stock_tauxVenteTotal_valeurImposee_pasUneSomme() throws Exception {
        Map<String, Object> stock = section(appels.sections(sur(ta, tb)), STOCK);
        Map<String, Object> ligneB = ligneDe(stock, tb);
        // Unité lue sur la ligne B (1 vendu / 2 articles) : 0,5 ou 50 selon l'échelle choisie par le lot.
        BigDecimal echelle = nombre(ligneB.get(TAUX_VENTE)).multiply(BigDecimal.valueOf(2));

        BigDecimal attendu = echelle.multiply(new BigDecimal("0.4"));
        assertThat(nombre(totaux(stock).get(TAUX_VENTE)).doubleValue())
                .isCloseTo(attendu.doubleValue(), within(echelle.doubleValue() * 0.005));
    }

    /** B1 (« Ajouts au socle ») : POURCENTAGE = valeur 0–100 ; la ligne B vaut 50, le total imposé 40. */
    @Test
    void b1_tauxVente_echelle0a100() throws Exception {
        Map<String, Object> stock = section(appels.sections(sur(ta, tb)), STOCK);

        assertThat(nombre(ligneDe(stock, tb).get(TAUX_VENTE)).doubleValue()).isCloseTo(50.0, within(0.05));
        assertThat(nombre(totaux(stock).get(TAUX_VENTE)).doubleValue()).isCloseTo(40.0, within(0.05));
    }

    @Test
    void stock_trancheSansArticleVendu_tauxZero() throws Exception {
        Map<String, Object> stock = section(appels.sections(sur(tc)), STOCK);

        assertThat(nombre(ligneDe(stock, tc).get(TAUX_VENTE))).isEqualByComparingTo("0");
    }

    // ------------------------------------------------------------------ section statuts

    @Test
    void statuts_sommeParTranche_egalNbArticlesDuStock() throws Exception {
        List<Map<String, Object>> sections = appels.sections(sur(ta, tb, tc));
        Map<String, Object> stock = section(sections, STOCK);
        Map<String, Object> statutsSection = section(sections, STATUTS);

        SoftAssertions soft = new SoftAssertions();
        for (CfgTranche t : List.of(ta, tb, tc)) {
            Map<String, Object> ligneStock = ligneDe(stock, t);
            List<Map<String, Object>> lignesStatuts = lignesOu(statutsSection, TRANCHE_LIBELLE,
                    ligneStock.get(TRANCHE_LIBELLE));
            soft.assertThat(somme(lignesStatuts, NB_ARTICLES)).as(t.getCode())
                    .isEqualByComparingTo(nombre(ligneStock.get(NB_ARTICLES)));
        }
        soft.assertAll();
    }

    @Test
    void statuts_dossierSansStatut_ligneSansStatut() throws Exception {
        Map<String, Object> statutsSection = section(appels.sections(sur(tc)), STATUTS);

        List<Map<String, Object>> sansStatut = lignesOu(statutsSection, "statutLibelle", "Sans statut");
        assertThat(sansStatut).hasSize(1);
        assertThat(nombre(sansStatut.get(0).get(NB_ARTICLES))).isEqualByComparingTo("1");
    }

    @Test
    void statuts_groupeParTranche_sousTotauxEgauxAuxLignesDuGroupe() throws Exception {
        Map<String, Object> statutsSection = section(appels.sections(sur(ta, tb)), STATUTS);

        assertThat(statutsSection).containsEntry("groupePar", TRANCHE_LIBELLE);
        Map<String, Map<String, Object>> sousTotaux = sousTotaux(statutsSection);
        assertThat(sousTotaux).isNotNull().isNotEmpty();
        SoftAssertions soft = new SoftAssertions();
        sousTotaux.forEach((groupe, valeurs) -> soft.assertThat(nombre(valeurs.get(NB_ARTICLES))).as(groupe)
                .isEqualByComparingTo(somme(lignesOu(statutsSection, TRANCHE_LIBELLE, groupe), NB_ARTICLES)));
        soft.assertAll();
    }

    // ------------------------------------------------------------------ ventes : dossier annulé exclu (RV3)

    @Test
    void ventes_dossierAnnuleExclu_aucunMontant() throws Exception {
        Map<String, Object> ventes = section(appels.sections(sur(td)), "ventes");

        List<String> nonNuls = new ArrayList<>();
        for (Map<String, Object> ligne : lignes(ventes)) {
            for (Map<String, Object> colonne : colonnes(ventes)) {
                Object valeur = ligne.get(colonne.get("nom"));
                if ("MONTANT".equals(colonne.get("type")) && valeur != null
                        && nombre(valeur).signum() != 0) {
                    nonNuls.add(colonne.get("nom") + "=" + valeur);
                }
            }
        }
        assertThat(nonNuls).isEmpty();
    }

    // ------------------------------------------------------------------ totaux serveur (§4.3)

    /** Toute colonne SOMME : total = somme des lignes ; section sans ligne ⇒ {@code totaux} null. */
    @Test
    void totaux_sommeServeur_egalSommeDesLignes_sectionVideTotauxNull() throws Exception {
        List<Map<String, Object>> sections = appels.sections(sur(ta, tb, tc, td));

        assertThat(sections).isNotEmpty();
        SoftAssertions soft = new SoftAssertions();
        for (Map<String, Object> s : sections) {
            if (lignes(s).isEmpty()) {
                soft.assertThat(totaux(s)).as("section vide %s", s.get("id")).isNull();
                continue;
            }
            for (String nom : colonnesAvecTotal(s, SOMME)) {
                soft.assertThat(nombre(totaux(s).get(nom))).as("%s.%s", s.get("id"), nom)
                        .isEqualByComparingTo(somme(lignes(s), nom));
            }
        }
        soft.assertAll();
    }

    @Test
    void repartitions_lignesANbArticlesZeroOmises() throws Exception {
        List<Map<String, Object>> sections = appels.sections(sur(ta, tb));

        List<Map<String, Object>> lignesRepartition = new ArrayList<>();
        for (String id : List.of("categories", "types", "etages", "immeubles", "presentations", "orientations")) {
            lignesRepartition.addAll(lignes(section(sections, id)));
        }
        assertThat(lignesRepartition).isNotEmpty()
                .allSatisfy(l -> assertThat(nombre(l.get(NB_ARTICLES))).isPositive());
    }
}
