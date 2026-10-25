package com.tsix.immocrm.controller.rapport;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.fixtures.FixtureFluxVente;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Genre;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Jeu;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Statuts;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.services.rapport.vente.flux.DemandeFlux;
import com.tsix.immocrm.services.rapport.vente.flux.FluxMensuel;
import com.tsix.immocrm.services.rapport.vente.flux.FluxVenteMensuel;
import com.tsix.immocrm.services.rapport.vente.flux.LignesMensuelles;
import com.tsix.immocrm.services.rapport.vente.flux.MesureFlux;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.VENTE;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.descripteur;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.nombre;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.section;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.totaux;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.colonne;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.noms;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.ENSEMBLE;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.RYTHME_CONCRETISATIONS;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.colonneDe;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.colonnesTranches;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.demande;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * N° 9 {@code ComTdbSalesRythmeConcretisation} « RYTHME … (Par mois) » (FEATURE-tdb-vente-rapports-rythme.md §4.2, §3.4,
 * §7 « N° 9 », M1). Prix des dossiers 1 200 000 ; projet P (entité active) :
 * <pre>
 *   T1 : d1 CONC avancé 15/01/2071, encaissé 120 000 le 20/01/2071 (+ 120 CANCELED le même jour)
 *        d4 CLEFS avancé 01/02/2071, livré 15/04/2071
 *        d2 CONC avancé 10/03/2071, encaissé 360 000 le 05/03/2071
 *        d3 ANN  avancé 12/03/2071, annulé 01/06/2071, encaissé 60 000 le 10/03/2071
 *        1 article libre SalePrice 1 200 000          ⇒ nbArticles 5, CA potentiel 3 × 1,2 M + 1,2 M = 4 800 000
 *   T2 : d5 CONC avancé 01/05/2071, encaissé 240 000 le 01/05/2071 ; 1 libre 1 200 000 ⇒ 2 articles, 2 400 000
 *   T3 : 3 articles libres, aucun mouvement (hors colonnes, hors dénominateurs)
 *   T4 : d6 CONC prix 0, avancé et encaissé 10 000 le 01/01/2072 ⇒ CA potentiel 0 (demandée seule)
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RythmeConcretisationsRapportTest {

    private static final String CONCRETISATIONS = "concretisations";
    private static final String LIVRAISONS = "livraisons";
    private static final String ENCAISSEMENTS = "encaissements";
    private static final String MOIS = "mois";
    private static final String PERIODE = "periode";
    private static final String PRIX_LIBRE = "1200000";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports rapports;
    @Autowired private FixtureFluxVente fixture;
    @Autowired private FluxVenteMensuel flux;

    private AppelsRapports appels;
    private RequestPostProcessor libre;
    private Jeu jeu;
    private CfgProject projet;
    private CfgTranche t1;
    private CfgTranche t2;
    private CfgTranche t3;
    private CfgTranche t4;
    private ComFolder d1;
    private ComFolder d5;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        RapportsRythmeSupport.installerR2b(rapports);
        jeu = fixture.jeu("RC9");
        libre = connecte(rapports.compte("rc9." + jeu.prefixe(), rapports.role("Libre " + jeu.prefixe(), false)));
        Statuts st = jeu.statuts();
        projet = jeu.projet("P", true);
        t1 = jeu.tranche("T1", projet);
        t2 = jeu.tranche("T2", projet);
        t3 = jeu.tranche("T3", projet);
        t4 = jeu.tranche("T4", projet);

        d1 = jeu.dater(jeu.dossier(t1, st.concretisation()), j(2071, 1, 15), null, null, null);
        jeu.encaisser(d1, "120000", j(2071, 1, 20));
        ComDocument annulee = jeu.echeance(d1, jeu.categorie(Genre.AUTOFINANCEMENT, true), new BigDecimal("120"),
                j(2071, 1, 20));
        jeu.regler(annulee, new BigDecimal("120"), true, FixtureFluxVente.CANCELED, null, j(2071, 1, 20));
        jeu.dater(jeu.dossier(t1, st.remiseClefs()), j(2071, 2, 1), null, j(2071, 4, 15), null);
        jeu.encaisser(jeu.dater(jeu.dossier(t1, st.concretisation()), j(2071, 3, 10), null, null, null), "360000",
                j(2071, 3, 5));
        jeu.encaisser(jeu.dater(jeu.dossier(t1, st.annulation()), j(2071, 3, 12), null, null, j(2071, 6, 1)), "60000",
                j(2071, 3, 10));
        jeu.articleLibre(t1, PRIX_LIBRE);

        d5 = jeu.dater(jeu.dossier(t2, st.concretisation()), j(2071, 5, 1), null, null, null);
        jeu.encaisser(d5, "240000", j(2071, 5, 1));
        jeu.articleLibre(t2, PRIX_LIBRE);

        for (int i = 0; i < 3; i++) {
            jeu.articleLibre(t3, PRIX_LIBRE);
        }

        ComFolder d6 = jeu.prix(jeu.dossier(t4, st.concretisation()), BigDecimal.ZERO, BigDecimal.ZERO);
        jeu.encaisser(jeu.dater(d6, j(2072, 1, 1), null, null, null), "10000", j(2072, 1, 1));
        fixture.relire();
    }

    private static LocalDate j(int annee, int mois, int jour) {
        return LocalDate.of(annee, mois, jour);
    }

    private List<Map<String, Object>> sections(CfgTranche... tranches) throws Exception {
        return appels.sections(demande(RYTHME_CONCRETISATIONS, libre, List.of(tranches)));
    }

    private List<Map<String, Object>> sections() throws Exception {
        return sections(t1, t2, t3);
    }

    private static BigDecimal cellule(Map<String, Object> section, int rang, String colonne) {
        return nombre(lignes(section).get(rang - 1).get(colonne));
    }

    private static BigDecimal total(Map<String, Object> section, String colonne) {
        return nombre(totaux(section).get(colonne));
    }

    // ------------------------------------------------------------------ structure (§4.2)

    @Test
    void r9_troisSections_concretisationsLivraisonsEncaissements_sansDecaissements() throws Exception {
        List<Map<String, Object>> sections = sections();

        assertThat(sections).extracting(s -> s.get("id")).containsExactly(CONCRETISATIONS, LIVRAISONS, ENCAISSEMENTS);
        assertThat(String.valueOf(section(sections, ENCAISSEMENTS).get("titre"))).contains("Rythme des encaissements");
    }

    @Test
    void r9_colonnes_moisPeriodePuisUneParTrancheAvecMouvementPuisEnsemble_types() throws Exception {
        Map<String, Object> s = section(sections(), CONCRETISATIONS);

        assertThat(noms(s)).containsExactly(MOIS, PERIODE, "tranche01", "tranche02", ENSEMBLE);
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(colonne(s, MOIS).get("type")).isEqualTo("NOMBRE");
        soft.assertThat(colonne(s, PERIODE).get("type")).isEqualTo("TEXTE");
        soft.assertThat(colonne(s, "tranche01").get("type")).isEqualTo("POURCENTAGE");
        soft.assertThat(colonne(s, ENSEMBLE).get("type")).isEqualTo("POURCENTAGE");
        soft.assertThat(colonneDe(s, t1)).as("ordre programme puis code de tranche").isEqualTo("tranche01");
        soft.assertThat(colonneDe(s, t2)).isEqualTo("tranche02");
        soft.assertThat(colonne(s, "tranche01")).containsEntry("groupe", projet.getCode());
        soft.assertAll();
    }

    @Test
    void r9_trancheSansMouvementDeLaMesure_niColonne_niDenominateur() throws Exception {
        List<Map<String, Object>> sections = sections();

        Map<String, Object> livraisons = section(sections, LIVRAISONS);
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(colonnesTranches(livraisons)).as("seule T1 livre").containsExactly(colonneDe(livraisons, t1));
        for (Map<String, Object> s : sections) {
            soft.assertThat(colonnes(s)).as("%s : T3", s.get("id")).isNotEmpty()
                    .noneMatch(c -> String.valueOf(c.get("libelle")).contains(t3.getCode()));
        }
        soft.assertThat(cellule(section(sections, CONCRETISATIONS), 1, ENSEMBLE)).as("1 ÷ (5 + 2), pas ÷ (5 + 2 + 3)")
                .isEqualByComparingTo("14.29");
        soft.assertAll();
    }

    private static List<Map<String, Object>> colonnes(Map<String, Object> s) {
        return AppelsRapports.colonnes(s);
    }

    // ------------------------------------------------------------------ lignes : mois calendaires (§7 « N° 9 »)

    @Test
    void r9_mois1_premierMoisDuPremierMouvement_puisMoisCalendairesJusquAuDernier() throws Exception {
        Map<String, Object> s = section(sections(), CONCRETISATIONS);

        assertThat(lignes(s)).extracting(l -> nombre(l.get(MOIS)).intValueExact()).containsExactly(1, 2, 3, 4, 5);
        assertThat(lignes(s).get(0)).containsEntry(PERIODE, "janv. 2071");
    }

    @Test
    void r9_moisIntermediaireSansMouvement_ligneAZero() throws Exception {
        Map<String, Object> s = section(sections(), CONCRETISATIONS);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(cellule(s, 4, "tranche01")).as("avril, T1").isEqualByComparingTo("0");
        soft.assertThat(cellule(s, 4, "tranche02")).as("avril, T2").isEqualByComparingTo("0");
        soft.assertThat(cellule(s, 4, ENSEMBLE)).as("avril").isEqualByComparingTo("0");
        soft.assertAll();
    }

    @Test
    void r9_cellule_numerateurDuMoisSurNbArticlesFoisCent() throws Exception {
        Map<String, Object> s = section(sections(), CONCRETISATIONS);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(cellule(s, 1, "tranche01")).as("d1 ÷ 5").isEqualByComparingTo("20");
        soft.assertThat(cellule(s, 2, "tranche01")).as("d4 ÷ 5").isEqualByComparingTo("20");
        soft.assertThat(cellule(s, 3, "tranche01")).as("d2 + d3 annulé ÷ 5").isEqualByComparingTo("40");
        soft.assertThat(cellule(s, 5, "tranche02")).as("d5 ÷ 2").isEqualByComparingTo("50");
        soft.assertThat(cellule(s, 1, "tranche02")).isEqualByComparingTo("0");
        soft.assertAll();
    }

    @Test
    void r9_ensemble_ponderePasLaSommeDesPourcentages() throws Exception {
        Map<String, Object> s = section(sections(), CONCRETISATIONS);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(cellule(s, 1, ENSEMBLE)).as("1 ÷ 7, pas 20 + 0").isEqualByComparingTo("14.29");
        soft.assertThat(cellule(s, 3, ENSEMBLE)).as("2 ÷ 7").isEqualByComparingTo("28.57");
        soft.assertThat(cellule(s, 5, ENSEMBLE)).as("1 ÷ 7, pas 0 + 50").isEqualByComparingTo("14.29");
        soft.assertAll();
    }

    @Test
    void r9_totauxImposes_parTrancheSommeDesNumerateursSurDenominateur_ensemblePondere() throws Exception {
        Map<String, Object> s = section(sections(), CONCRETISATIONS);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(total(s, "tranche01")).as("4 ÷ 5").isEqualByComparingTo("80");
        soft.assertThat(total(s, "tranche02")).as("1 ÷ 2").isEqualByComparingTo("50");
        soft.assertThat(total(s, ENSEMBLE)).as("5 ÷ 7, pas Σ des lignes 71,44 ni 80 + 50")
                .isEqualByComparingTo("71.43");
        soft.assertAll();
    }

    @Test
    void r9_livraisons_mesureLivraisons_uneLigneAuMoisDeLaLivraison() throws Exception {
        Map<String, Object> s = section(sections(), LIVRAISONS);

        assertThat(lignes(s)).hasSize(1);
        assertThat(lignes(s).get(0)).containsEntry(PERIODE, "avr. 2071");
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(cellule(s, 1, "tranche01")).as("d4 ÷ 5").isEqualByComparingTo("20");
        soft.assertThat(total(s, ENSEMBLE)).isEqualByComparingTo("20");
        soft.assertAll();
    }

    // ------------------------------------------------------------------ encaissements (§3.2 ENCAISSEMENTS_VIVANTS, §3.4)

    @Test
    void r9_encaissements_surCaPotentiel_prixDesDossiersVivantsPlusPrixDesLibres() throws Exception {
        Map<String, Object> s = section(sections(), ENCAISSEMENTS);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(cellule(s, 1, "tranche01")).as("120 000 ÷ 4 800 000").isEqualByComparingTo("2.50");
        soft.assertThat(cellule(s, 5, "tranche02")).as("240 000 ÷ 2 400 000").isEqualByComparingTo("10");
        soft.assertThat(total(s, "tranche01")).as("480 000 ÷ 4 800 000").isEqualByComparingTo("10");
        soft.assertThat(total(s, ENSEMBLE)).as("720 000 ÷ 7 200 000").isEqualByComparingTo("10");
        soft.assertThat(cellule(s, 1, ENSEMBLE)).as("120 000 ÷ 7 200 000").isEqualByComparingTo("1.67");
        soft.assertAll();
    }

    @Test
    void r9_encaissements_excluentLeDossierAnnuleEtLeReglementCanceled() throws Exception {
        Map<String, Object> s = section(sections(), ENCAISSEMENTS);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(cellule(s, 1, "tranche01")).as("sans les 120 CANCELED").isEqualByComparingTo("2.50");
        soft.assertThat(cellule(s, 3, "tranche01")).as("360 000 seul, pas + 60 000 de d3 annulé")
                .isEqualByComparingTo("7.50");
        soft.assertThat(cellule(s, 3, ENSEMBLE)).isEqualByComparingTo("5");
        soft.assertAll();
    }

    @Test
    void r9_denominateurNul_tauxAZero_pasDErreur() throws Exception {
        Map<String, Object> s = section(sections(t4), ENCAISSEMENTS);

        assertThat(lignes(s)).hasSize(1);
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(cellule(s, 1, colonneDe(s, t4))).isEqualByComparingTo("0");
        soft.assertThat(cellule(s, 1, ENSEMBLE)).isEqualByComparingTo("0");
        soft.assertThat(total(s, colonneDe(s, t4))).isEqualByComparingTo("0");
        soft.assertThat(total(s, ENSEMBLE)).isEqualByComparingTo("0");
        soft.assertAll();
    }

    // ------------------------------------------------------------------ M1 (règle 6)

    /** M1 : flux de 2 tranches sur 3 ans ; Σ des numérateurs « encaissements » = {@code calculer(ENCAISSEMENTS_VIVANTS)}. */
    @Test
    void m1_encaissements_totauxDuN9_egalCalculerEncaissementsVivantsSurCaPotentiel() throws Exception {
        jeu.encaisser(d1, "480000", j(2072, 6, 1));
        jeu.encaisser(d5, "240000", j(2073, 2, 1));
        fixture.relire();
        FluxMensuel calcul = flux.calculer(new DemandeFlux(List.of(t1.getId(), t2.getId()), null, null,
                Set.of(MesureFlux.ENCAISSEMENTS_VIVANTS)));

        Map<String, Object> s = section(sections(t1, t2), ENCAISSEMENTS);

        BigDecimal caT1 = new BigDecimal("4800000");
        BigDecimal caT2 = new BigDecimal("2400000");
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(lignes(s)).as("janv. 2071 … févr. 2073").hasSize(26);
        soft.assertThat(total(s, colonneDe(s, t1))).isEqualByComparingTo(
                LignesMensuelles.taux(calcul.tranche(MesureFlux.ENCAISSEMENTS_VIVANTS, t1.getId()), caT1));
        soft.assertThat(total(s, colonneDe(s, t2))).isEqualByComparingTo(
                LignesMensuelles.taux(calcul.tranche(MesureFlux.ENCAISSEMENTS_VIVANTS, t2.getId()), caT2));
        soft.assertThat(total(s, ENSEMBLE)).isEqualByComparingTo(
                LignesMensuelles.taux(calcul.total(MesureFlux.ENCAISSEMENTS_VIVANTS), caT1.add(caT2)));
        soft.assertThat(calcul.total(MesureFlux.ENCAISSEMENTS_VIVANTS)).isEqualByComparingTo("1440000");
        soft.assertAll();
    }

    // ------------------------------------------------------------------ paramètres et graphiques

    @Test
    void r9_descripteur_seulementTrancheIdMultiple() throws Exception {
        String corps = appels.corps(get(descripteur(VENTE, RYTHME_CONCRETISATIONS)).with(libre));
        List<String> nomsParametres = JsonPath.read(corps, "$.parametres[*].nom");

        assertThat(nomsParametres).containsExactly("trancheId");
    }

    @Test
    void r9_pasDeGraphique() throws Exception {
        List<Map<String, Object>> sections = sections();

        assertThat(sections).isNotEmpty().allSatisfy(s -> assertThat((List<?>) s.get("graphiques"))
                .as(String.valueOf(s.get("id"))).isNullOrEmpty());
    }
}
