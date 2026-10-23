package com.tsix.immocrm.controller.rapport;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.fixtures.FixtureRapportsPatrimoine;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
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

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.descripteur;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.export;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignesOu;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.nombre;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.resultat;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.section;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.somme;
import static com.tsix.immocrm.controller.rapport.RapportsPatrimoineSupport.FORMAT;
import static com.tsix.immocrm.controller.rapport.RapportsPatrimoineSupport.PATRIMOINE;
import static com.tsix.immocrm.controller.rapport.RapportsPatrimoineSupport.TRANCHE_ID;
import static com.tsix.immocrm.controller.rapport.RapportsPatrimoineSupport.XLSX;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureRapportsPatrimoine.MOUVEMENTS;
import static com.tsix.immocrm.fixtures.FixtureRapportsPatrimoine.SEUIL_ANNULATION;
import static com.tsix.immocrm.fixtures.FixtureRapportsPatrimoine.SEUIL_CONCRETISATION;
import static com.tsix.immocrm.fixtures.FixtureRapportsPatrimoine.SEUIL_RESILIATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MOUVEMENTS DU STOCK (FEATURE-patrimoine-rapports.md §3, T5 ; décision 3 : transferts inter-tranches comptés). Année 2075
 * ({@code AdvanceDate} est un {@code smalldatetime}, borne 2079) :
 * <pre>
 *   TM (en vente, lancée le 10/01) : m1..m4 VENTE articles, ms sous-article de m1, mp article PROJET   ⇒ lancement +4
 *     m1 avancé 05/02 · ms avancé 10/02 · mp avancé 11/02                                          (statut concrétisation)
 *     m2 avancé 01/03 00:00, désisté 31/03 18:30 · m3 avancé 01/04 10:30, annulé 15/04
 *   TX, TY (en vente, non lancées) : dossier de y1 (TY) transféré depuis x1 (TX) le 01/06       ⇒ +1 x1, −1 y1
 *     dossier de x2 transféré depuis x3, même tranche TX                                          ⇒ rien
 *     dossier de y2 transféré depuis x4, historique au statut RESA (Sorting sous le seuil)         ⇒ rien
 *   TN (PAS en vente) : n1 avancé 01/02
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MouvementsStockPatrimoineTest {

    private static final String DU = "du";
    private static final String AU = "au";
    private static final String SYNTHESE = "synthese";
    private static final String MOUVEMENTS_SECTION = "mouvements";
    private static final String ARTICLE = "article";
    private static final String LIBELLE = "libelle";
    private static final String MOUVEMENT = "mouvement";
    private static final String STOCK_INITIAL = "stockInitial";
    private static final String STOCK_FINAL = "stockFinal";
    private static final String LANCEMENT = "LANCEMENT DE LA COMMERCIALISATION";
    private static final String CONCRETISATION = "CONCRETISATION";
    private static final String DESISTEMENT = "DÉSISTEMENT";
    private static final String ANNULATION = "ANNULATION";
    private static final String TRANSFERT_SORTIE = "TRANSFERT SORTIE";
    private static final String TRANSFERT_ENTREE = "TRANSFERT ENTREE";
    private static final String DEBUT_ANNEE = "2075-01-01";
    private static final String FIN_ANNEE = "2075-12-31";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports fixture;
    @Autowired private FixtureRapportsPatrimoine rp;
    @Autowired private FixtureEtatVentes ev;
    @Autowired private CfgTrancheRepository tranches;
    @Autowired private Clock clock;

    private AppelsRapports appels;
    private RequestPostProcessor libre;
    private String prefixe;
    private StkItemCategory categorie;
    private StkItemType type;
    private StrStatus concret;
    private CfgTranche tm;
    private CfgTranche tx;
    private CfgTranche ty;
    private CfgTranche tn;
    private StkItem m1;
    private StkItem m2;
    private StkItem m3;
    private StkItem n1;
    private StkItem x1;
    private StkItem x2;
    private StkItem x3;
    private StkItem x4;
    private StkItem y1;
    private StkItem y2;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        prefixe = FixtureEtatVentes.prefixe("MVT");
        rp.rapportsP10a();
        libre = connecte(fixture.compte("mvt." + prefixe, fixture.role("Libre " + prefixe, false)));
        categorie = ev.categorie("CAT-" + prefixe);
        type = ev.type("TYP-" + prefixe);

        concret = rp.statut("CONC-" + prefixe, 2, false, false);
        StrStatus annule = rp.statut("ANN-" + prefixe, 30, true, false);
        StrStatus resilie = rp.statut("RES-" + prefixe, 31, false, true);
        rp.seuil(SEUIL_CONCRETISATION, concret);
        rp.seuil(SEUIL_ANNULATION, annule);
        rp.seuil(SEUIL_RESILIATION, resilie);

        CfgProject projet = ev.projet("P-" + prefixe, true);
        tm = ev.tranche("TM-" + prefixe, projet, true);
        tm.setMarketingStartDate(LocalDateTime.of(2075, 1, 10, 0, 0));
        tm = tranches.save(tm);
        m1 = article("M1", tm);
        m2 = article("M2", tm);
        m3 = article("M3", tm);
        article("M4", tm);
        StkItem ms = ev.sousArticle(prefixe + "-MS", m1, categorie, type);
        StkItem mp = ev.article(prefixe + "-MP", tm, categorie, type, ev.projetVocation());
        avancer(m1, concret, LocalDateTime.of(2075, 2, 5, 0, 0), null);
        avancer(ms, concret, LocalDateTime.of(2075, 2, 10, 0, 0), null);
        avancer(mp, concret, LocalDateTime.of(2075, 2, 11, 0, 0), null);
        avancer(m2, resilie, LocalDateTime.of(2075, 3, 1, 0, 0), LocalDateTime.of(2075, 3, 31, 18, 30));
        avancer(m3, annule, LocalDateTime.of(2075, 4, 1, 10, 30), LocalDateTime.of(2075, 4, 15, 0, 0));

        tn = ev.tranche("TN-" + prefixe, projet, false);
        n1 = article("N1", tn);
        avancer(n1, concret, LocalDateTime.of(2075, 2, 1, 0, 0), null);
        fixture.relire();
    }

    private StkItem article(String suffixe, CfgTranche tranche) {
        return ev.article(prefixe + "-" + suffixe, tranche, categorie, type);
    }

    private ComFolder avancer(StkItem article, StrStatus statut, LocalDateTime avance, LocalDateTime sortie) {
        ComFolder dossier = ev.vendre(article, "DOS-" + article.getCode(), statut, null, null);
        dossier.setAdvanceDate(avance);
        dossier.setCancelDate(sortie);
        return ev.enregistrer(dossier);
    }

    /** Jeu des transferts (TX, TY) de la Javadoc de classe. */
    private void transferts() {
        CfgProject projet = ev.projet("PT-" + prefixe, true);
        tx = ev.tranche("TX-" + prefixe, projet, true);
        ty = ev.tranche("TY-" + prefixe, projet, true);
        x1 = article("X1", tx);
        x2 = article("X2", tx);
        x3 = article("X3", tx);
        x4 = article("X4", tx);
        y1 = article("Y1", ty);
        y2 = article("Y2", ty);
        StrStatus reservation = rp.statut("RESA-" + prefixe, 1, false, false);
        LocalDateTime avance = LocalDateTime.of(2075, 5, 1, 0, 0);
        rp.transfert(avancer(y1, concret, avance, null), x1, y1, LocalDateTime.of(2075, 6, 1, 0, 0), concret);
        rp.transfert(avancer(x2, concret, avance, null), x3, x2, LocalDateTime.of(2075, 6, 2, 0, 0), concret);
        rp.transfert(avancer(y2, concret, avance, null), x4, y2, LocalDateTime.of(2075, 6, 3, 0, 0), reservation);
        fixture.relire();
    }

    private MockHttpServletRequestBuilder periode(String du, String au, CfgTranche... tranchesDemandees) {
        MockHttpServletRequestBuilder requete = get(resultat(PATRIMOINE, MOUVEMENTS)).param(DU, du).param(AU, au)
                .with(libre);
        for (CfgTranche t : tranchesDemandees) {
            requete.param(TRANCHE_ID, t.getId().toString());
        }
        return requete;
    }

    private MockHttpServletRequestBuilder annee(CfgTranche... tranchesDemandees) {
        return periode(DEBUT_ANNEE, FIN_ANNEE, tranchesDemandees);
    }

    private static Map<String, Object> synthese(List<Map<String, Object>> sections) {
        List<Map<String, Object>> lignes = lignes(section(sections, SYNTHESE));
        assertThat(lignes).hasSize(1);
        return lignes.get(0);
    }

    private static List<Map<String, Object>> mouvementsDe(List<Map<String, Object>> sections, StkItem article) {
        return lignesOu(section(sections, MOUVEMENTS_SECTION), ARTICLE, article.getCode());
    }

    private static List<Map<String, Object>> libelles(List<Map<String, Object>> sections, String libelle) {
        return lignesOu(section(sections, MOUVEMENTS_SECTION), LIBELLE, libelle);
    }

    // ------------------------------------------------------------------ synthèse et cumul

    @Test
    void t5_anneeComplete_synthese_initialEntreesSortiesFinal() throws Exception {
        Map<String, Object> synthese = synthese(appels.sections(annee(tm)));

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(nombre(synthese.get(STOCK_INITIAL))).isEqualByComparingTo("0");
        soft.assertThat(nombre(synthese.get("entrees"))).isEqualByComparingTo("6");
        soft.assertThat(nombre(synthese.get("sorties"))).isEqualByComparingTo("-5");
        soft.assertThat(nombre(synthese.get(STOCK_FINAL))).isEqualByComparingTo("1");
        soft.assertAll();
    }

    @Test
    void t5_stockFinal_egal_stockInitialPlusSommeDesMouvements() throws Exception {
        List<Map<String, Object>> sections = appels.sections(periode("2075-03-01", "2075-03-31", tm));
        Map<String, Object> synthese = synthese(sections);

        assertThat(nombre(synthese.get(STOCK_INITIAL))).isEqualByComparingTo("1");
        assertThat(nombre(synthese.get(STOCK_FINAL)))
                .isEqualByComparingTo(nombre(synthese.get(STOCK_INITIAL))
                        .add(somme(lignes(section(sections, MOUVEMENTS_SECTION)), MOUVEMENT)))
                .isEqualByComparingTo("1");
    }

    @Test
    void t5_cumulDeLaDerniereLigne_egalStockFinal() throws Exception {
        List<Map<String, Object>> sections = appels.sections(periode("2075-02-01", "2075-12-31", tm));
        List<Map<String, Object>> lignes = lignes(section(sections, MOUVEMENTS_SECTION));

        assertThat(lignes).isNotEmpty();
        assertThat(nombre(lignes.get(lignes.size() - 1).get("cumul")))
                .isEqualByComparingTo(nombre(synthese(sections).get(STOCK_FINAL)));
    }

    @Test
    void t5_mouvementsTriesParDate() throws Exception {
        List<String> dates = lignes(section(appels.sections(annee(tm)), MOUVEMENTS_SECTION)).stream()
                .map(l -> String.valueOf(l.get("date"))).toList();

        assertThat(dates).isNotEmpty().isSortedAccordingTo(Comparator.naturalOrder());
    }

    // ------------------------------------------------------------------ règles des mouvements

    @Test
    void t5_lancement_articlesVenteHorsSousArticles() throws Exception {
        List<Map<String, Object>> lancement = libelles(appels.sections(annee(tm)), LANCEMENT);

        assertThat(lancement).hasSize(1);
        assertThat(nombre(lancement.get(0).get(MOUVEMENT))).isEqualByComparingTo("4");
    }

    @Test
    void t5_concretisation_toutesVocations_articlesEtSousArticles_moinsUn() throws Exception {
        List<Map<String, Object>> concretisations = libelles(appels.sections(annee(tm)), CONCRETISATION);

        assertThat(concretisations).hasSize(5)
                .allSatisfy(l -> assertThat(nombre(l.get(MOUVEMENT))).isEqualByComparingTo("-1"));
    }

    @Test
    void t5_dossierAvancePuisDesiste_moinsUnPuisPlusUn() throws Exception {
        List<Map<String, Object>> lignes = mouvementsDe(appels.sections(annee(tm)), m2);

        assertThat(lignes).extracting(l -> l.get(LIBELLE), l -> nombre(l.get(MOUVEMENT)).intValueExact())
                .containsExactly(tuple(CONCRETISATION, -1), tuple(DESISTEMENT, 1));
    }

    @Test
    void t5_dossierAvancePuisAnnule_moinsUnPuisPlusUn() throws Exception {
        List<Map<String, Object>> lignes = mouvementsDe(appels.sections(annee(tm)), m3);

        assertThat(lignes).extracting(l -> l.get(LIBELLE), l -> nombre(l.get(MOUVEMENT)).intValueExact())
                .containsExactly(tuple(CONCRETISATION, -1), tuple(ANNULATION, 1));
    }

    /** {@code du} = 01/03 00:00 et {@code au} = 31/03 avec une sortie à 18:30 : les deux bornes sont incluses. */
    @Test
    void t5_bornesDuAuInclusives_smalldatetime() throws Exception {
        List<Map<String, Object>> sections = appels.sections(periode("2075-03-01", "2075-03-31", tm));

        assertThat(mouvementsDe(sections, m2)).extracting(l -> l.get(LIBELLE))
                .containsExactly(CONCRETISATION, DESISTEMENT);
        assertThat(mouvementsDe(sections, m1)).isEmpty();
        assertThat(lignes(section(sections, MOUVEMENTS_SECTION))).hasSize(2);
    }

    @Test
    void t5_seuilResiliationAbsent_avertissement_pasDErreur_blocVide() throws Exception {
        rp.sansSeuil(SEUIL_RESILIATION);
        fixture.relire();

        String corps = appels.corps(annee(tm));

        List<String> avertissements = JsonPath.read(corps, "$.avertissements");
        List<Map<String, Object>> sections = JsonPath.read(corps, "$.sections");
        assertThat(avertissements).isNotEmpty();
        assertThat(libelles(sections, DESISTEMENT)).isEmpty();
        assertThat(libelles(sections, CONCRETISATION)).hasSize(5);
    }

    // ------------------------------------------------------------------ transferts (décision 3)

    @Test
    void t5_transfertInterTranches_plusUnAncienArticle_moinsUnNouveau() throws Exception {
        transferts();

        List<Map<String, Object>> sections = appels.sections(annee(tx, ty));

        List<Map<String, Object>> sortie = lignesOu(section(sections, MOUVEMENTS_SECTION), LIBELLE, TRANSFERT_SORTIE)
                .stream().filter(l -> Objects.equals(x1.getCode(), l.get(ARTICLE))).toList();
        List<Map<String, Object>> entree = lignesOu(section(sections, MOUVEMENTS_SECTION), LIBELLE, TRANSFERT_ENTREE)
                .stream().filter(l -> Objects.equals(y1.getCode(), l.get(ARTICLE))).toList();
        assertThat(sortie).hasSize(1);
        assertThat(entree).hasSize(1);
        assertThat(nombre(sortie.get(0).get(MOUVEMENT))).isEqualByComparingTo("1");
        assertThat(nombre(entree.get(0).get(MOUVEMENT))).isEqualByComparingTo("-1");
    }

    @Test
    void t5_transfertDansLaMemeTranche_aucunMouvementDeTransfert() throws Exception {
        transferts();

        List<Map<String, Object>> sections = appels.sections(annee(tx, ty));

        assertThat(mouvementsDe(sections, x3)).isEmpty();
        assertThat(mouvementsDe(sections, x2)).extracting(l -> l.get(LIBELLE)).containsExactly(CONCRETISATION);
    }

    @Test
    void t5_transfertAvecHistoriqueSousLeSeuil_aucunMouvementDeTransfert() throws Exception {
        transferts();

        List<Map<String, Object>> sections = appels.sections(annee(tx, ty));

        assertThat(mouvementsDe(sections, x4)).isEmpty();
        assertThat(mouvementsDe(sections, y2)).extracting(l -> l.get(LIBELLE)).containsExactly(CONCRETISATION);
    }

    @Test
    void t5_transferts_seulementLesDeuxLignesInterTranches() throws Exception {
        transferts();

        List<Map<String, Object>> sections = appels.sections(annee(tx, ty));

        assertThat(libelles(sections, TRANSFERT_SORTIE)).hasSize(1);
        assertThat(libelles(sections, TRANSFERT_ENTREE)).hasSize(1);
    }

    // ------------------------------------------------------------------ tranches en vente seulement

    @Test
    void t5_trancheHorsVenteDemandee_aucunMouvement() throws Exception {
        assertThat(lignes(section(appels.sections(annee(tn)), MOUVEMENTS_SECTION))).isEmpty();
    }

    @Test
    void t5_sansTrancheId_trancheHorsVenteExclue_trancheEnVenteRetenue() throws Exception {
        List<Object> articles = lignes(section(appels.sections(annee()), MOUVEMENTS_SECTION)).stream()
                .map(l -> l.get(ARTICLE)).toList();

        assertThat(articles).contains(m1.getCode()).doesNotContain(n1.getCode());
    }

    // ------------------------------------------------------------------ paramètres

    @Test
    void parametres_duDefautMoinsUnMois_auDefautAujourdhui_tranches() throws Exception {
        LocalDate aujourdhui = LocalDate.now(clock);

        mockMvc.perform(get(descripteur(PATRIMOINE, MOUVEMENTS)).with(libre))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parametres[?(@.nom == 'du')].type").value("DATE"))
                .andExpect(jsonPath("$.parametres[?(@.nom == 'du')].valeurParDefaut")
                        .value(aujourdhui.minusMonths(1).toString()))
                .andExpect(jsonPath("$.parametres[?(@.nom == 'au')].valeurParDefaut").value(aujourdhui.toString()))
                .andExpect(jsonPath("$.parametres[?(@.nom == 'trancheId')].type").value("TRANCHES"));
    }

    // ------------------------------------------------------------------ T9 : export

    @Test
    void t9_export_uneFeuilleParSection_ligneTotal() throws Exception {
        MockHttpServletRequestBuilder export = get(export(PATRIMOINE, MOUVEMENTS)).param(FORMAT, XLSX)
                .param(DU, DEBUT_ANNEE).param(AU, FIN_ANNEE).param(TRANCHE_ID, tm.getId().toString()).with(libre);

        RapportsPatrimoineSupport.verifierExport(appels, annee(tm), export);
    }
}
