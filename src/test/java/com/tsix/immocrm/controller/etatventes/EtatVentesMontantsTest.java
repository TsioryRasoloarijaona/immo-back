package com.tsix.immocrm.controller.etatventes;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureEtatVentes.Nature;
import com.tsix.immocrm.web.export.ExportTableur;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Colonnes d'argent de l'état des ventes (FEATURE-etat-des-ventes-parite.md §3, §5.2-5.6, §5.8) : une nature par test,
 * prix du dossier ou du catalogue, Σ, réconciliation avec la liste des dossiers, visibilité, export, catalogue.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class EtatVentesMontantsTest {

    private static final String URL = "/api/etat-des-ventes";
    private static final String URL_TOTAUX = URL + "/totaux-colonnes";
    private static final String URL_DOSSIERS = "/api/dossiers";
    private static final String TRANCHE_ID = "trancheId";
    private static final String COLONNES = "colonnes";
    private static final String AGREGATS = "agregats";
    private static final String SEARCH = "search";
    private static final String POINTE = "POINTED";
    private static final String ROLE_COMMERCIAL = "Commercial";
    private static final String UN_MILLION = "1000000";
    private static final String CENT_MILLE = "100000";

    private static final String MONTANT_REGLE = "montantRegle";
    private static final String RESTE_A_REGLER = "resteARegler";
    private static final String NET_A_PAYER = "netAPayer";
    private static final String NET_A_PAYER_REGLE = "netAPayerRegle";
    private static final String NET_A_PAYER_RESTE = "netAPayerReste";
    private static final String AVANCE = "avance";
    private static final String AVANCE_REGLEE = "avanceReglee";
    private static final String AVANCE_RESTE = "avanceReste";
    private static final String AUTOFINANCEMENT = "autofinancement";
    private static final String AUTOFINANCEMENT_REGLE = "autofinancementRegle";
    private static final String AUTOFINANCEMENT_RESTE = "autofinancementReste";
    private static final String CREDIT = "credit";
    private static final String CREDIT_REGLE = "creditRegle";
    private static final String CREDIT_RESTE = "creditReste";
    private static final String FRAIS = "frais";
    private static final String FRAIS_REGLES = "fraisRegles";
    private static final String FRAIS_RESTE = "fraisReste";
    private static final String MODIFICATIONS = "modifications";
    private static final String MODIFICATIONS_REGLEES = "modificationsReglees";
    private static final String MODIFICATIONS_RESTE = "modificationsReste";
    private static final String TROP_PERCU = "tropPercu";
    private static final String TROP_PERCU_REGLE = "tropPercuRegle";
    private static final String TROP_PERCU_RESTE = "tropPercuReste";
    private static final String PENALITES = "penalites";
    private static final String PENALITES_REGLES = "penalitesRegles";
    private static final String PENALITES_RESTE = "penalitesReste";
    private static final String PRIX_CONTRACTUEL_REGLE = "prixContractuelRegle";
    private static final String PRIX_CONTRACTUEL_RESTE = "prixContractuelReste";
    private static final String TAUX_AVANCE = "tauxAvance";
    private static final String TAUX_AVANCE_REEL = "tauxAvanceReel";
    private static final String PRIX_VENTE_M2_TTC = "prixVenteM2Ttc";
    private static final String PRIX_VENTE_M2_HT = "prixVenteM2Ht";
    private static final String PRIX_ARTICLE_HT = "prixArticleHt";
    private static final String PRIX_ARTICLE_TTC = "prixArticleTtc";
    private static final String PRIX_SOUS_ARTICLES_HT = "prixSousArticlesHt";
    private static final String PRIX_SOUS_ARTICLES_TTC = "prixSousArticlesTtc";
    private static final String PRIX_HT = "prixHt";
    private static final String PRIX_VENTE_TTC = "prixVenteTTC";
    private static final String REMISE_MONTANT = "remiseMontant";
    private static final String PRIX_CONTRACTUEL = "prixContractuel";

    /** Colonnes (a) additionnables : Σ calculée en Java (§3.1). */
    private static final List<String> NATURES_SOMMABLES = List.of(MONTANT_REGLE, RESTE_A_REGLER, NET_A_PAYER,
            NET_A_PAYER_REGLE, NET_A_PAYER_RESTE, AVANCE, AVANCE_REGLEE, AVANCE_RESTE, AUTOFINANCEMENT,
            AUTOFINANCEMENT_REGLE, AUTOFINANCEMENT_RESTE, CREDIT, CREDIT_REGLE, CREDIT_RESTE, FRAIS, FRAIS_REGLES,
            FRAIS_RESTE, MODIFICATIONS, MODIFICATIONS_REGLEES, MODIFICATIONS_RESTE, TROP_PERCU, TROP_PERCU_REGLE,
            TROP_PERCU_RESTE, PENALITES, PENALITES_REGLES, PENALITES_RESTE, PRIX_CONTRACTUEL_REGLE,
            PRIX_CONTRACTUEL_RESTE);
    /** Colonnes (b), prix du dossier ou du catalogue : Σ SQL (§3.2). */
    private static final List<String> PRIX_SOMMABLES = List.of(PRIX_ARTICLE_HT, PRIX_ARTICLE_TTC,
            PRIX_SOUS_ARTICLES_HT, PRIX_SOUS_ARTICLES_TTC, PRIX_HT, PRIX_VENTE_TTC, REMISE_MONTANT, PRIX_CONTRACTUEL);
    /** Rapports et prix au m² : jamais additionnés (§3.1, réponse (3) du manager). */
    private static final List<String> NON_SOMMABLES = List.of(TAUX_AVANCE, TAUX_AVANCE_REEL, PRIX_VENTE_M2_TTC,
            PRIX_VENTE_M2_HT);
    /** Nom de la même grandeur dans la liste des dossiers quand il diffère (§3.2). */
    private static final Map<String, String> NOM_DOSSIER = Map.of(MONTANT_REGLE, "prixRegle", RESTE_A_REGLER,
            "prixReste");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private FixtureEtatVentes fixture;
    @Autowired
    private FixtureDossierVente fixtureDossier;

    private String prefixe;
    private CfgProject projet;
    private CfgTranche tranche;
    private StkItemCategory categorie;
    private StkItemType type;

    @BeforeEach
    void setUp() {
        prefixe = FixtureEtatVentes.prefixe("EDVM");
        projet = fixture.projet("PRJ-" + prefixe, true);
        tranche = fixture.tranche("TR-" + prefixe, projet, true);
        categorie = fixture.categorie("APP-" + prefixe);
        type = fixture.type("T3-" + prefixe);
    }

    // ------------------------------------------------------------------ jeu

    private StkItem article(String suffixe) {
        return article(suffixe, tranche);
    }

    private StkItem article(String suffixe, CfgTranche t) {
        return fixture.article(prefixe + "-" + suffixe, t, categorie, type);
    }

    private ComFolder vendu(StkItem a, String prix) {
        ComFolder d = fixture.vendre(a, "DOS-" + a.getCode(), fixture.statut(StatutDossier.CONCRETISATION), null,
                LocalDateTime.of(2025, 3, 15, 10, 0));
        return fixture.prix(d, new BigDecimal(prix));
    }

    private ComDocument echeance(ComFolder d, Nature nature, boolean auCredit, String montant) {
        return fixture.echeance(d, nature, auCredit, new BigDecimal(montant));
    }

    private void regler(ComDocument e, String montant) {
        fixture.regler(e, new BigDecimal(montant), true, POINTE);
    }

    /**
     * Jeu commun de la tranche : D1 (toutes les natures, prix du dossier posés), D2 (trop-perçu), un dossier annulé
     * avec une avance réglée (ligne libre, exclu des deux listes) et un article libre au prix catalogue.
     */
    private ComFolder jeuCommun() {
        ComFolder d1 = vendu(article("D1"), UN_MILLION);
        regler(echeance(d1, Nature.AVANCE, true, CENT_MILLE), "60000");
        echeance(d1, Nature.AVANCE, false, "10000");
        regler(echeance(d1, Nature.FRAIS, true, "15000"), "15000");
        regler(echeance(d1, Nature.AUTOFINANCEMENT, true, "400000"), "50000");
        regler(echeance(d1, Nature.CREDIT, true, "300000"), CENT_MILLE);
        d1.setModificationsAmount(new BigDecimal("40000"));
        regler(echeance(d1, Nature.MODIFICATION, true, "40000"), "10000");
        regler(echeance(d1, Nature.PENALITE, true, "20000"), "5000");
        prixDuDossier(d1);

        ComFolder d2 = vendu(article("D2"), CENT_MILLE);
        regler(echeance(d2, Nature.AVANCE, true, CENT_MILLE), "120000");

        StkItem annule = article("AN");
        ComFolder d3 = fixture.vendre(annule, "DOS-" + annule.getCode(), fixture.statutSortie("ANN-" + prefixe, true),
                null, null);
        regler(echeance(d3, Nature.AVANCE, true, "80000"), "80000");

        fixture.catalogue(article("L1"), new BigDecimal("840000"), new BigDecimal("960000"), new BigDecimal("0.2"),
                new BigDecimal("800000"));
        fixture.relire();
        return d1;
    }

    private void prixDuDossier(ComFolder d) {
        d.setItemPriceHT(new BigDecimal("500001"));
        d.setItemPriceAfter(new BigDecimal("600002"));
        d.setUnderItemsPriceHT(new BigDecimal("70003"));
        d.setUnderItemsPriceAfter(new BigDecimal("84004"));
        d.setPriceHT(new BigDecimal("570005"));
        d.setPrice(new BigDecimal("684006"));
        d.setDiscountAmount(new BigDecimal("16007"));
        d.setAgreementPrice(new BigDecimal("700008"));
        fixture.enregistrer(d);
    }

    // ------------------------------------------------------------------ lecture

    private String corps(MockHttpServletRequestBuilder requete) throws Exception {
        return mockMvc.perform(requete).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private List<Map<String, Object>> lignes(CfgTranche t, List<String> colonnes) throws Exception {
        return JsonPath.read(corps(get(URL).param(TRANCHE_ID, t.getId().toString()).param("size", "100")
                .param(COLONNES, "articleId," + String.join(",", colonnes))), "$.content");
    }

    /** Ligne de l'article, nombres en {@code Double} ({@code null} conservé). */
    private Map<String, Double> montants(StkItem a, String... colonnes) throws Exception {
        List<Map<String, Object>> trouvees = lignes(tranche, List.of(colonnes)).stream()
                .filter(l -> a.getId().toString().equals(l.get("articleId")))
                .toList();
        assertThat(trouvees).hasSize(1);
        return enNombres(trouvees.getFirst(), List.of(colonnes));
    }

    private static Map<String, Double> enNombres(Map<String, Object> ligne, List<String> colonnes) {
        Map<String, Double> nombres = new HashMap<>();
        for (String c : colonnes) {
            assertThat(ligne).containsKey(c);
            nombres.put(c, ligne.get(c) instanceof Number n ? n.doubleValue() : null);
        }
        return nombres;
    }

    private Map<String, Object> totaux(CfgTranche t, List<String> agregats) throws Exception {
        return JsonPath.read(corps(get(URL_TOTAUX).param(TRANCHE_ID, t.getId().toString())
                .param(AGREGATS, String.join(",", agregats))), "$.totaux");
    }

    private static double valeur(Object nombre) {
        return nombre instanceof Number n ? n.doubleValue() : 0.0;
    }

    private static double somme(List<Map<String, Object>> lignes, String colonne) {
        return lignes.stream().map(l -> l.get(colonne)).filter(Objects::nonNull).mapToDouble(EtatVentesMontantsTest::valeur).sum();
    }

    private static String nomDossier(String colonne) {
        return NOM_DOSSIER.getOrDefault(colonne, colonne);
    }

    private static List<String> naturesEtRapports() {
        return Stream.concat(NATURES_SOMMABLES.stream(), NON_SOMMABLES.stream()).toList();
    }

    // ------------------------------------------------------------------ §5.2 une nature par test

    @Test
    void n2_avance_echeancesAuCreditSeules_regleDeToutes() throws Exception {
        StkItem a = article("AV");
        ComFolder d = vendu(a, UN_MILLION);
        regler(echeance(d, Nature.AVANCE, true, CENT_MILLE), "60000");
        echeance(d, Nature.AVANCE, false, "10000");
        fixture.relire();

        assertThat(montants(a, AVANCE, AVANCE_REGLEE, AVANCE_RESTE))
                .containsEntry(AVANCE, 100000.0)
                .containsEntry(AVANCE_REGLEE, 60000.0)
                .containsEntry(AVANCE_RESTE, 40000.0);
    }

    @Test
    void n2_taux_avanceSurPrixEtAvanceReelleSurAvance() throws Exception {
        StkItem a = article("TX");
        ComFolder d = vendu(a, UN_MILLION);
        regler(echeance(d, Nature.AVANCE, true, CENT_MILLE), "60000");
        fixture.relire();

        assertThat(montants(a, TAUX_AVANCE, TAUX_AVANCE_REEL))
                .containsEntry(TAUX_AVANCE, 10.0)
                .containsEntry(TAUX_AVANCE_REEL, 60.0);
    }

    @Test
    void n2_taux_baseNulle_zeroSansDivision() throws Exception {
        StkItem a = article("T0");
        echeance(vendu(a, "0"), Nature.AVANCE, true, CENT_MILLE);
        fixture.relire();

        assertThat(montants(a, TAUX_AVANCE)).containsEntry(TAUX_AVANCE, 0.0);
    }

    @Test
    void n2_autofinancement_reglementAnnuleExclu() throws Exception {
        StkItem a = article("AF");
        ComDocument e = echeance(vendu(a, UN_MILLION), Nature.AUTOFINANCEMENT, true, "200000");
        fixture.regler(e, new BigDecimal("50000"), true, "IN_WORK");
        fixture.regler(e, new BigDecimal("30000"), true, "CANCELED");
        fixture.relire();

        assertThat(montants(a, AUTOFINANCEMENT, AUTOFINANCEMENT_REGLE, AUTOFINANCEMENT_RESTE))
                .containsEntry(AUTOFINANCEMENT, 200000.0)
                .containsEntry(AUTOFINANCEMENT_REGLE, 50000.0)
                .containsEntry(AUTOFINANCEMENT_RESTE, 150000.0);
    }

    @Test
    void n2_credit_decaissementSigneNegatif() throws Exception {
        StkItem a = article("CR");
        ComDocument e = echeance(vendu(a, UN_MILLION), Nature.CREDIT, true, "500000");
        fixture.regler(e, new BigDecimal("300000"), true, null);
        fixture.regler(e, new BigDecimal("20000"), false, null);
        fixture.relire();

        assertThat(montants(a, CREDIT, CREDIT_REGLE, CREDIT_RESTE))
                .containsEntry(CREDIT, 500000.0)
                .containsEntry(CREDIT_REGLE, 280000.0)
                .containsEntry(CREDIT_RESTE, 220000.0);
    }

    /** Frais hors prix ; prix réglé/reste = {@code montantRegle}/{@code resteARegler} ; prix contractuel réglé/reste. */
    @Test
    void n2_frais_horsPrix_prixRegleResteEtPrixContractuel() throws Exception {
        StkItem a = article("FR");
        ComFolder d = vendu(a, UN_MILLION);
        regler(echeance(d, Nature.FRAIS, true, "15000"), "15000");
        regler(echeance(d, Nature.AVANCE, true, CENT_MILLE), CENT_MILLE);
        fixture.relire();

        assertThat(montants(a, FRAIS, FRAIS_REGLES, FRAIS_RESTE, MONTANT_REGLE, RESTE_A_REGLER,
                PRIX_CONTRACTUEL_REGLE, PRIX_CONTRACTUEL_RESTE))
                .containsEntry(FRAIS, 15000.0)
                .containsEntry(FRAIS_REGLES, 15000.0)
                .containsEntry(FRAIS_RESTE, 0.0)
                .containsEntry(MONTANT_REGLE, 100000.0)
                .containsEntry(RESTE_A_REGLER, 900000.0)
                .containsEntry(PRIX_CONTRACTUEL_REGLE, 100000.0)
                .containsEntry(PRIX_CONTRACTUEL_RESTE, 900000.0);
    }

    /** Net à payer = prix + frais ; réglé = avance + autofinancement + crédit + frais réglés. */
    @Test
    void n2_netAPayer_prixPlusFrais_regleEtReste() throws Exception {
        StkItem a = article("NP");
        ComFolder d = vendu(a, UN_MILLION);
        regler(echeance(d, Nature.FRAIS, true, "15000"), "15000");
        regler(echeance(d, Nature.AVANCE, true, CENT_MILLE), "60000");
        regler(echeance(d, Nature.AUTOFINANCEMENT, true, "400000"), "50000");
        fixture.relire();

        assertThat(montants(a, NET_A_PAYER, NET_A_PAYER_REGLE, NET_A_PAYER_RESTE))
                .containsEntry(NET_A_PAYER, 1015000.0)
                .containsEntry(NET_A_PAYER_REGLE, 125000.0)
                .containsEntry(NET_A_PAYER_RESTE, 890000.0);
    }

    @Test
    void n2_tropPercu_dossierCrediteur_leSurplus() throws Exception {
        StkItem a = article("TP");
        regler(echeance(vendu(a, CENT_MILLE), Nature.AVANCE, true, CENT_MILLE), "120000");
        fixture.relire();

        assertThat(montants(a, TROP_PERCU, TROP_PERCU_REGLE, TROP_PERCU_RESTE))
                .containsEntry(TROP_PERCU, 20000.0)
                .containsEntry(TROP_PERCU_REGLE, 0.0)
                .containsEntry(TROP_PERCU_RESTE, 20000.0);
    }

    @Test
    void n2_tropPercu_dossierDebiteur_zero() throws Exception {
        StkItem a = article("TD");
        regler(echeance(vendu(a, UN_MILLION), Nature.AVANCE, true, CENT_MILLE), "60000");
        fixture.relire();

        assertThat(montants(a, TROP_PERCU, TROP_PERCU_REGLE, TROP_PERCU_RESTE))
                .containsEntry(TROP_PERCU, 0.0)
                .containsEntry(TROP_PERCU_REGLE, 0.0)
                .containsEntry(TROP_PERCU_RESTE, 0.0);
    }

    /** §3.3 : montant = ComFolder.ModificationsAmount ; réglé = réglé des échéances IsSaleModification. */
    @Test
    void n2_modifications_montantDuDossier_regleDesEcheancesModification() throws Exception {
        StkItem a = article("MO");
        ComFolder d = vendu(a, UN_MILLION);
        d.setModificationsAmount(new BigDecimal("50000"));
        fixture.enregistrer(d);
        regler(echeance(d, Nature.MODIFICATION, true, "50000"), "20000");
        fixture.relire();

        assertThat(montants(a, MODIFICATIONS, MODIFICATIONS_REGLEES, MODIFICATIONS_RESTE))
                .containsEntry(MODIFICATIONS, 50000.0)
                .containsEntry(MODIFICATIONS_REGLEES, 20000.0)
                .containsEntry(MODIFICATIONS_RESTE, 30000.0);
    }

    /** §3.3 : montant = Σ signé des échéances IsSalePenality (crédit +, débit −) ; reste = montant − réglé. */
    @Test
    void n2_penalites_sommeSigneeDesEcheances_regleEtReste() throws Exception {
        StkItem a = article("PE");
        ComFolder d = vendu(a, UN_MILLION);
        regler(echeance(d, Nature.PENALITE, true, "30000"), "10000");
        echeance(d, Nature.PENALITE, false, "5000");
        fixture.relire();

        assertThat(montants(a, PENALITES, PENALITES_REGLES, PENALITES_RESTE))
                .containsEntry(PENALITES, 25000.0)
                .containsEntry(PENALITES_REGLES, 10000.0)
                .containsEntry(PENALITES_RESTE, 15000.0);
    }

    /** §3.1 : ligne libre, colonnes (a) nulles, y compris rapports et prix au m² (réponse (3) du manager). */
    @Test
    void n2_ligneLibre_colonnesDeNatureNulles() throws Exception {
        StkItem libre = article("LB");
        fixture.relire();

        Map<String, Double> ligne = montants(libre, naturesEtRapports().toArray(String[]::new));

        assertThat(ligne).hasSize(naturesEtRapports().size()).containsOnlyKeys(naturesEtRapports())
                .allSatisfy((colonne, v) -> assertThat(v).as(colonne).isNull());
    }

    // ------------------------------------------------------------------ §5.3 prix (b)

    @Test
    void n3_prixVendu_colonnesDuDossier() throws Exception {
        StkItem a = article("PV");
        prixDuDossier(vendu(a, UN_MILLION));
        fixture.relire();

        assertThat(montants(a, PRIX_SOMMABLES.toArray(String[]::new)))
                .containsEntry(PRIX_ARTICLE_HT, 500001.0)
                .containsEntry(PRIX_ARTICLE_TTC, 600002.0)
                .containsEntry(PRIX_SOUS_ARTICLES_HT, 70003.0)
                .containsEntry(PRIX_SOUS_ARTICLES_TTC, 84004.0)
                .containsEntry(PRIX_HT, 570005.0)
                .containsEntry(PRIX_VENTE_TTC, 684006.0)
                .containsEntry(REMISE_MONTANT, 16007.0)
                .containsEntry(PRIX_CONTRACTUEL, 700008.0);
    }

    /** Branche libre de ComSaleMonitoringView : SalePrice 840 000, ComSalePrice 960 000, Vat 0,2, ComSalePriceHT 800 000. */
    @Test
    void n3_prixLibre_formuleDuCatalogue() throws Exception {
        StkItem libre = fixture.catalogue(article("PL"), new BigDecimal("840000"), new BigDecimal("960000"),
                new BigDecimal("0.2"), new BigDecimal("800000"));
        fixture.relire();

        assertCatalogue(montants(libre, PRIX_SOMMABLES.toArray(String[]::new)));
    }

    @Test
    void n3_dossierAnnule_prixDuCatalogue() throws Exception {
        StkItem annule = fixture.catalogue(article("PA"), new BigDecimal("840000"), new BigDecimal("960000"),
                new BigDecimal("0.2"), new BigDecimal("800000"));
        ComFolder d = fixture.vendre(annule, "DOS-" + annule.getCode(), fixture.statutSortie("ANN-" + prefixe, true),
                null, null);
        prixDuDossier(d);
        fixture.relire();

        assertCatalogue(montants(annule, PRIX_SOMMABLES.toArray(String[]::new)));
    }

    private static void assertCatalogue(Map<String, Double> ligne) {
        assertThat(ligne)
                .containsEntry(PRIX_ARTICLE_HT, 700000.0)
                .containsEntry(PRIX_ARTICLE_TTC, 840000.0)
                .containsEntry(PRIX_SOUS_ARTICLES_HT, 100000.0)
                .containsEntry(PRIX_SOUS_ARTICLES_TTC, 120000.0)
                .containsEntry(PRIX_HT, 800000.0)
                .containsEntry(PRIX_VENTE_TTC, 960000.0)
                .containsEntry(REMISE_MONTANT, 0.0)
                .containsEntry(PRIX_CONTRACTUEL, 960000.0);
    }

    @Test
    void n3_dossierAnnule_colonnesDeNatureNulles() throws Exception {
        StkItem annule = article("NA");
        ComFolder d = fixture.vendre(annule, "DOS-" + annule.getCode(), fixture.statutSortie("ANN-" + prefixe, true),
                null, null);
        regler(echeance(d, Nature.AVANCE, true, CENT_MILLE), CENT_MILLE);
        fixture.relire();

        assertThat(montants(annule, AVANCE, AVANCE_REGLEE, NET_A_PAYER, MONTANT_REGLE))
                .containsEntry(AVANCE, null)
                .containsEntry(AVANCE_REGLEE, null)
                .containsEntry(NET_A_PAYER, null)
                .containsEntry(MONTANT_REGLE, null);
    }

    // ------------------------------------------------------------------ §5.4 Σ

    /** R5 : Σ de chaque colonne additionnable = somme des lignes de la liste filtrée (valeur répétée comprise). */
    @Test
    void n4_sommeDeChaqueColonneSommable_egaleSommeDesLignes() throws Exception {
        jeuCommun();
        List<String> sommables = Stream.concat(PRIX_SOMMABLES.stream(), NATURES_SOMMABLES.stream()).toList();

        List<Map<String, Object>> lignes = lignes(tranche, sommables);
        Map<String, Object> totaux = totaux(tranche, sommables);

        SoftAssertions verif = new SoftAssertions();
        verif.assertThat(lignes).hasSize(4);
        verif.assertThat(totaux).containsKeys(sommables.toArray(String[]::new));
        for (String c : sommables) {
            verif.assertThat(valeur(totaux.get(c))).as(c).isCloseTo(somme(lignes, c), within(0.01));
        }
        verif.assertThat(valeur(totaux.get(AVANCE))).as("Σ avance non vide").isEqualTo(200000.0);
        verif.assertAll();
    }

    @ParameterizedTest
    @ValueSource(strings = {TAUX_AVANCE, TAUX_AVANCE_REEL, PRIX_VENTE_M2_TTC, PRIX_VENTE_M2_HT})
    void n4_colonneNonSommable_400(String colonne) throws Exception {
        mockMvc.perform(get(URL_TOTAUX).param(TRANCHE_ID, tranche.getId().toString()).param(AGREGATS, colonne))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ §5.5 réconciliation avec la liste des dossiers

    @Test
    void n5_sommeParNature_egaleTotauxDeLaListeDesDossiers() throws Exception {
        jeuCommun();

        Map<String, Object> etat = totaux(tranche, NATURES_SOMMABLES);
        Map<String, Object> dossiers = JsonPath.read(corps(get(URL_DOSSIERS + "/totaux")
                .param(TRANCHE_ID, tranche.getId().toString())), "$");

        SoftAssertions verif = new SoftAssertions();
        verif.assertThat(dossiers).containsEntry("nombreDossiers", 2);
        Map<String, Object> sommes = JsonPath.read(dossiers, "$.sommes");
        for (String c : NATURES_SOMMABLES) {
            verif.assertThat(etat).as(c).containsKey(c);
            verif.assertThat(sommes).as(nomDossier(c)).containsKey(nomDossier(c));
            verif.assertThat(valeur(etat.get(c))).as(c)
                    .isCloseTo(valeur(sommes.get(nomDossier(c))), within(0.01));
        }
        verif.assertAll();
    }

    @Test
    void n5_chaqueLigne_egaleLaLigneDuDossier() throws Exception {
        ComFolder d1 = jeuCommun();
        List<String> colonnes = naturesEtRapports();
        List<String> colonnesDossier = colonnes.stream().map(EtatVentesMontantsTest::nomDossier).toList();

        List<Map<String, Object>> etat = lignes(tranche, Stream.concat(Stream.of("dossierId"), colonnes.stream())
                .toList());
        List<Map<String, Object>> dossiers = JsonPath.read(corps(get(URL_DOSSIERS)
                .param(TRANCHE_ID, tranche.getId().toString()).param("size", "50")
                .param(COLONNES, "id," + String.join(",", colonnesDossier))), "$.content");

        List<Map<String, Object>> vendues = etat.stream().filter(l -> l.get("dossierId") != null).toList();
        assertThat(vendues).hasSize(2).anySatisfy(l -> assertThat(l).containsEntry("dossierId", d1.getId().toString()));
        SoftAssertions verif = new SoftAssertions();
        for (Map<String, Object> ligne : vendues) {
            Map<String, Object> dossier = dossiers.stream().filter(d -> ligne.get("dossierId").equals(d.get("id")))
                    .findFirst().orElseThrow(() -> new AssertionError("dossier absent de /api/dossiers"));
            for (String c : colonnes) {
                verif.assertThat(nombre(ligne.get(c))).as("%s %s", ligne.get("dossierId"), c)
                        .isEqualTo(nombre(dossier.get(nomDossier(c))));
            }
        }
        verif.assertAll();
    }

    private static Double nombre(Object o) {
        return o instanceof Number n ? n.doubleValue() : null;
    }

    /** Réponse (2) du manager : un dossier à deux articles racines compte deux fois ici, une fois dans la liste. */
    @Test
    void n5_dossierADeuxRacines_compteDeuxFois_uneFoisDansLaListeDesDossiers() throws Exception {
        CfgTranche t2 = fixture.tranche("TR2-" + prefixe, projet, true);
        ComFolder d = vendu(article("R1", t2), UN_MILLION);
        fixture.rattacher(article("R2", t2), d);
        regler(echeance(d, Nature.AVANCE, true, CENT_MILLE), "60000");
        fixture.relire();

        List<Map<String, Object>> lignes = lignes(t2, List.of(AVANCE));
        Map<String, Object> etat = totaux(t2, List.of(AVANCE));
        Map<String, Object> dossiers = JsonPath.read(corps(get(URL_DOSSIERS + "/totaux")
                .param(TRANCHE_ID, t2.getId().toString())), "$");

        assertAll(
                () -> assertThat(lignes).hasSize(2)
                        .allSatisfy(l -> assertThat(valeur(l.get(AVANCE))).isEqualTo(100000.0)),
                () -> assertThat(valeur(etat.get(AVANCE))).isEqualTo(200000.0),
                () -> assertThat(dossiers).containsEntry("nombreDossiers", 1),
                () -> assertThat(valeur(JsonPath.read(dossiers, "$.sommes.avance"))).isEqualTo(100000.0));
    }

    // ------------------------------------------------------------------ §5.6 visibilité inchangée

    private record Perimetre(StkItem visible, StkItem horsPerimetre, StkItem inactif, AdmUser commercial) {
    }

    /** A (projet du commercial) avance 100 000, B (autre projet) 70 000, entité inactive 50 000. */
    private Perimetre perimetre() {
        StkItem a = article("VA");
        echeance(vendu(a, UN_MILLION), Nature.AVANCE, true, CENT_MILLE);
        CfgTranche trancheB = fixture.tranche("TRB-" + prefixe, fixture.projet("PRJB-" + prefixe, true), true);
        StkItem b = article("VB", trancheB);
        echeance(vendu(b, UN_MILLION), Nature.AVANCE, true, "70000");
        CfgTranche trancheI = fixture.tranche("TRI-" + prefixe, fixture.projet("PRJI-" + prefixe, false), true);
        StkItem i = article("VI", trancheI);
        echeance(vendu(i, UN_MILLION), Nature.AVANCE, true, "50000");
        AdmUser commercial = fixtureDossier.compte("edvm.com." + prefixe);
        fixture.affecter(commercial, projet);
        fixture.relire();
        return new Perimetre(a, b, i, commercial);
    }

    @Test
    void n6_commercialRestreint_liste_sansLigneHorsPerimetre() throws Exception {
        Perimetre p = perimetre();

        List<Map<String, Object>> lignes = JsonPath.read(corps(get(URL).param(SEARCH, prefixe)
                .param(COLONNES, "articleId,avance").with(connecte(p.commercial(), ROLE_COMMERCIAL))), "$.content");

        assertThat(lignes).extracting(l -> l.get("articleId"))
                .containsExactly(p.visible().getId().toString());
    }

    @Test
    void n6_commercialRestreint_totaux_sommeDuSeulPerimetre() throws Exception {
        Perimetre p = perimetre();

        Map<String, Object> totaux = JsonPath.read(corps(get(URL_TOTAUX).param(SEARCH, prefixe)
                .param(AGREGATS, AVANCE).with(connecte(p.commercial(), ROLE_COMMERCIAL))), "$");

        assertAll(
                () -> assertThat(totaux).containsEntry("nombreTotal", 1),
                () -> assertThat(valeur(JsonPath.read(totaux, "$.totaux.avance"))).isEqualTo(100000.0));
    }

    @Test
    void n6_commercialRestreint_groupes_sansArticleHorsPerimetre() throws Exception {
        Perimetre p = perimetre();

        List<Integer> effectifs = JsonPath.read(corps(get(URL + "/groupes").param(SEARCH, prefixe)
                .param("groupBy", "trancheCode").with(connecte(p.commercial(), ROLE_COMMERCIAL))), "$.content[*].nombre");

        assertThat(effectifs).containsExactly(1);
    }

    @Test
    void n6_commercialRestreint_export_niLigneNiMontantHorsPerimetre() throws Exception {
        Perimetre p = perimetre();

        List<String> valeurs = cellules(get(URL + "/export").param(SEARCH, prefixe)
                .param(COLONNES, "articleCode,avance").with(connecte(p.commercial(), ROLE_COMMERCIAL)));

        assertThat(valeurs).contains(p.visible().getCode(), "100000.0")
                .doesNotContain(p.horsPerimetre().getCode(), p.inactif().getCode(), "70000.0", "50000.0");
    }

    @Test
    void n6_compteNonRestreint_voitTout_saufEntiteInactive() throws Exception {
        perimetre();

        Map<String, Object> totaux = JsonPath.read(corps(get(URL_TOTAUX).param(SEARCH, prefixe)
                .param(AGREGATS, AVANCE)), "$");

        assertAll(
                () -> assertThat(totaux).containsEntry("nombreTotal", 2),
                () -> assertThat(valeur(JsonPath.read(totaux, "$.totaux.avance"))).isEqualTo(170000.0));
    }

    // ------------------------------------------------------------------ §5.8 export

    private List<String> cellules(MockHttpServletRequestBuilder requete) throws Exception {
        List<String> valeurs = new ArrayList<>();
        try (XSSFWorkbook classeur = classeur(requete)) {
            for (Row row : classeur.getSheet(ExportTableur.FEUILLE)) {
                for (Cell cell : row) {
                    valeurs.add(cell.toString());
                }
            }
        }
        return valeurs;
    }

    private XSSFWorkbook classeur(MockHttpServletRequestBuilder requete) throws Exception {
        MvcResult asynchrone = mockMvc.perform(requete).andExpect(request().asyncStarted()).andReturn();
        byte[] contenu = mockMvc.perform(asyncDispatch(asynchrone)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        return new XSSFWorkbook(new ByteArrayInputStream(contenu));
    }

    @Test
    void n8_export_avanceTropPercuPrixHt_enTetesDansLOrdre_cellulesMontant() throws Exception {
        StkItem a = article("EX");
        ComFolder d = vendu(a, CENT_MILLE);
        d.setPriceHT(new BigDecimal("90000"));
        fixture.enregistrer(d);
        regler(echeance(d, Nature.AVANCE, true, CENT_MILLE), "120000");
        fixture.relire();

        try (XSSFWorkbook classeur = classeur(get(URL + "/export").param(TRANCHE_ID, tranche.getId().toString())
                .param(SEARCH, a.getCode()).param(COLONNES, "avance,tropPercu,prixHt"))) {
            Sheet feuille = classeur.getSheet(ExportTableur.FEUILLE);
            Row entete = feuille.getRow(0);
            Row ligne = feuille.getRow(1);
            assertAll(
                    () -> assertThat(feuille.getLastRowNum()).isEqualTo(1),
                    () -> assertThat(entete.getLastCellNum()).isEqualTo((short) 3),
                    () -> assertThat(entete.getCell(0).getStringCellValue()).isNotBlank(),
                    () -> assertThat(ligne.getCell(0).getCellType()).isEqualTo(CellType.NUMERIC),
                    () -> assertThat(ligne.getCell(1).getCellType()).isEqualTo(CellType.NUMERIC),
                    () -> assertThat(ligne.getCell(2).getCellType()).isEqualTo(CellType.NUMERIC),
                    () -> assertThat(ligne.getCell(0).getNumericCellValue()).isEqualTo(100000.0),
                    () -> assertThat(ligne.getCell(1).getNumericCellValue()).isEqualTo(20000.0),
                    () -> assertThat(ligne.getCell(2).getNumericCellValue()).isEqualTo(90000.0));
        }
    }

    // ------------------------------------------------------------------ §3.1 / §3.4 catalogue et contrat

    private Map<String, Map<String, Object>> catalogue() throws Exception {
        List<Map<String, Object>> colonnes = JsonPath.read(corps(get("/api/referentiels/colonnes/etat-des-ventes")),
                "$");
        Map<String, Map<String, Object>> parNom = new HashMap<>();
        colonnes.forEach(c -> parNom.put((String) c.get("nom"), c));
        return parNom;
    }

    @Test
    void c_catalogue_nouvellesColonnes_presentesEtHorsDefaut() throws Exception {
        Map<String, Map<String, Object>> catalogue = catalogue();
        List<String> nouvelles = Stream.of(PRIX_SOMMABLES, NATURES_SOMMABLES, NON_SOMMABLES).flatMap(List::stream)
                .filter(c -> !List.of(PRIX_VENTE_TTC, MONTANT_REGLE, RESTE_A_REGLER).contains(c)).toList();

        SoftAssertions verif = new SoftAssertions();
        for (String c : nouvelles) {
            verif.assertThat(catalogue).as(c).containsKey(c);
            verif.assertThat(catalogue.getOrDefault(c, Map.of())).as(c).containsEntry("parDefaut", false);
        }
        verif.assertThat(catalogue.get(MONTANT_REGLE)).containsEntry("parDefaut", true);
        verif.assertThat(catalogue.get("articleCode")).containsEntry("parDefaut", true);
        verif.assertAll();
    }

    /** Impact front §3.4 : montantRegle et resteARegler annoncent SUM. */
    @Test
    void c_catalogue_montantRegleEtResteARegler_fonctionsSum() throws Exception {
        Map<String, Map<String, Object>> catalogue = catalogue();

        assertAll(
                () -> assertThat(catalogue.get(MONTANT_REGLE)).containsEntry("fonctions", List.of("SUM")),
                () -> assertThat(catalogue.get(RESTE_A_REGLER)).containsEntry("fonctions", List.of("SUM")));
    }

    @Test
    void c_catalogue_naturesSommables_sumSeul_niGroupablesNiAgregeables() throws Exception {
        Map<String, Map<String, Object>> catalogue = catalogue();

        SoftAssertions verif = new SoftAssertions();
        for (String c : NATURES_SOMMABLES) {
            verif.assertThat(catalogue.getOrDefault(c, Map.of())).as(c)
                    .containsEntry("fonctions", List.of("SUM"))
                    .containsEntry("groupable", false)
                    .containsEntry("agregeable", false);
        }
        verif.assertAll();
    }

    @Test
    void c_catalogue_rapportsEtPrixAuM2_sansFonction() throws Exception {
        Map<String, Map<String, Object>> catalogue = catalogue();

        SoftAssertions verif = new SoftAssertions();
        for (String c : NON_SOMMABLES) {
            verif.assertThat(catalogue.getOrDefault(c, Map.of())).as(c).containsEntry("fonctions", List.of());
        }
        verif.assertAll();
    }

    @Test
    void c_catalogue_prixDuDossier_sommeSql() throws Exception {
        Map<String, Map<String, Object>> catalogue = catalogue();

        SoftAssertions verif = new SoftAssertions();
        for (String c : PRIX_SOMMABLES) {
            Object fonctions = catalogue.getOrDefault(c, Map.of()).get("fonctions");
            verif.assertThat(fonctions instanceof List<?> liste && liste.contains("SUM")).as(c).isTrue();
        }
        verif.assertAll();
    }

    /** §3.1 : sans ?colonnes=, la réponse ne change pas (colonnes nouvelles parDefaut = false). */
    @Test
    void c_sansColonnes_reponseInchangee() throws Exception {
        StkItem a = article("SC");
        vendu(a, UN_MILLION);
        fixture.relire();

        List<Map<String, Object>> lignes = JsonPath.read(corps(get(URL).param(TRANCHE_ID, tranche.getId().toString())),
                "$.content");

        assertThat(lignes).singleElement().satisfies(l -> assertThat(l)
                .containsKeys(MONTANT_REGLE, RESTE_A_REGLER, PRIX_VENTE_TTC)
                .doesNotContainKeys(AVANCE, NET_A_PAYER, PRIX_HT, PRIX_ARTICLE_HT, PENALITES, TAUX_AVANCE));
    }

    /** §3.1 : (a) ni triables ni groupables — 400, comme aujourd'hui pour montantRegle. */
    @Test
    void c_natureNonTriable_400() throws Exception {
        mockMvc.perform(get(URL).param(TRANCHE_ID, tranche.getId().toString()).param("sort", "avance,asc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void c_natureNonGroupable_400() throws Exception {
        mockMvc.perform(get(URL + "/groupes").param("groupBy", AVANCE))
                .andExpect(status().isBadRequest());
    }
}
