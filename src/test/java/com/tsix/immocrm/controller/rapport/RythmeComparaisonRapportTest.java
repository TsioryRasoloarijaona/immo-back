package com.tsix.immocrm.controller.rapport;

import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.fixtures.FixtureFluxVente;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Jeu;
import com.tsix.immocrm.fixtures.FixtureRapports;
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

import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.nombre;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.section;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.totaux;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.colonne;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.noms;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.ENSEMBLE;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.RYTHME_COMPARAISON;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.colonneDe;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.colonnesTranches;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.demande;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * N° 10 {@code ComTdbSalesRythmeLivraison} « RYTHME … (Comparaison) » (FEATURE-tdb-vente-rapports-rythme.md §4.3, Q7, §7
 * « N° 10 ») : mêmes mesures, colonnes et totaux que le n° 9, axe = r-ième mois actif de chaque tranche. Projet P actif,
 * prix des dossiers 1 200 000 :
 * <pre>
 *   A : a1 CONC avancé 10/01/2071, encaissé 48 000 le 10/01/2071 ; a2 avancé 05/03/2071, encaissé 96 000 le 15/06/2071 ;
 *       a3 avancé 20/03/2071 ; 1 libre 1 200 000                 ⇒ 4 articles, CA potentiel 4 800 000
 *   B : b1 CONC avancé 15/02/2071, encaissé 24 000 le 01/12/2071 ; 1 libre 1 200 000 ⇒ 2 articles, 2 400 000
 *   aucune livraison
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RythmeComparaisonRapportTest {

    private static final String CONCRETISATIONS = "concretisations";
    private static final String LIVRAISONS = "livraisons";
    private static final String ENCAISSEMENTS = "encaissements";
    private static final String MOIS = "mois";
    private static final String PRIX_LIBRE = "1200000";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports rapports;
    @Autowired private FixtureFluxVente fixture;

    private AppelsRapports appels;
    private RequestPostProcessor libre;
    private CfgTranche a;
    private CfgTranche b;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        RapportsRythmeSupport.installerR2b(rapports);
        Jeu jeu = fixture.jeu("RC10");
        libre = connecte(rapports.compte("rc10." + jeu.prefixe(), rapports.role("Libre " + jeu.prefixe(), false)));
        CfgProject projet = jeu.projet("P", true);
        a = jeu.tranche("TA", projet);
        b = jeu.tranche("TB", projet);

        jeu.encaisser(concretise(jeu, a, j(2071, 1, 10)), "48000", j(2071, 1, 10));
        jeu.encaisser(concretise(jeu, a, j(2071, 3, 5)), "96000", j(2071, 6, 15));
        concretise(jeu, a, j(2071, 3, 20));
        jeu.articleLibre(a, PRIX_LIBRE);
        jeu.encaisser(concretise(jeu, b, j(2071, 2, 15)), "24000", j(2071, 12, 1));
        jeu.articleLibre(b, PRIX_LIBRE);
        fixture.relire();
    }

    private static ComFolder concretise(Jeu jeu, CfgTranche tranche, LocalDate avance) {
        return jeu.dater(jeu.dossier(tranche, jeu.statuts().concretisation()), avance, null, null, null);
    }

    private static LocalDate j(int annee, int mois, int jour) {
        return LocalDate.of(annee, mois, jour);
    }

    private List<Map<String, Object>> sections() throws Exception {
        return appels.sections(demande(RYTHME_COMPARAISON, libre, List.of(a, b)));
    }

    private static Map<String, Object> rang(Map<String, Object> section, int rang) {
        List<Map<String, Object>> trouvees = lignes(section).stream()
                .filter(l -> ("Mois " + rang).equals(l.get(MOIS))).toList();
        assertThat(trouvees).as("%s Mois %d", section.get("id"), rang).hasSize(1);
        return trouvees.get(0);
    }

    // ------------------------------------------------------------------ structure

    @Test
    void r10_troisSections_colonnesMoisTexteSansPeriode_unePourcentageParTranche_ensemble() throws Exception {
        List<Map<String, Object>> sections = sections();
        Map<String, Object> s = section(sections, CONCRETISATIONS);

        assertThat(sections).extracting(x -> x.get("id")).containsExactly(CONCRETISATIONS, LIVRAISONS, ENCAISSEMENTS);
        assertThat(noms(s)).containsExactly(MOIS, "tranche01", "tranche02", ENSEMBLE);
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(colonne(s, MOIS).get("type")).isEqualTo("TEXTE");
        soft.assertThat(colonne(s, "tranche01").get("type")).isEqualTo("POURCENTAGE");
        soft.assertThat(colonne(s, ENSEMBLE).get("type")).isEqualTo("POURCENTAGE");
        soft.assertThat(colonneDe(s, a)).isEqualTo("tranche01");
        soft.assertAll();
    }

    // ------------------------------------------------------------------ axe : rang du mois actif (§7 « N° 10 »)

    @Test
    void r10_trancheActiveEnJanvierEtMars_BEnFevrier_mois2EstMarsPourA_rienPourB() throws Exception {
        Map<String, Object> s = section(sections(), CONCRETISATIONS);

        assertThat(lignes(s)).extracting(l -> l.get(MOIS)).containsExactly("Mois 1", "Mois 2");
        Map<String, Object> mois2 = rang(s, 2);
        BigDecimal deB = nombre(mois2.get(colonneDe(s, b)));
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(nombre(rang(s, 1).get(colonneDe(s, a)))).as("A janvier : 1 ÷ 4").isEqualByComparingTo("25");
        soft.assertThat(nombre(rang(s, 1).get(colonneDe(s, b)))).as("B février : 1 ÷ 2").isEqualByComparingTo("50");
        soft.assertThat(nombre(mois2.get(colonneDe(s, a)))).as("A mars : 2 ÷ 4").isEqualByComparingTo("50");
        soft.assertThat(deB == null || deB.signum() == 0).as("B n'a pas de 2ᵉ mois actif : %s", deB).isTrue();
        soft.assertAll();
    }

    @Test
    void r10_rangDense_moisSansMouvementSautes() throws Exception {
        Map<String, Object> s = section(sections(), ENCAISSEMENTS);

        assertThat(lignes(s)).extracting(l -> l.get(MOIS)).containsExactly("Mois 1", "Mois 2");
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(nombre(rang(s, 2).get(colonneDe(s, a)))).as("A juin = Mois 2 : 96 000 ÷ 4 800 000")
                .isEqualByComparingTo("2");
        soft.assertThat(nombre(rang(s, 1).get(colonneDe(s, b)))).as("B décembre = Mois 1 : 24 000 ÷ 2 400 000")
                .isEqualByComparingTo("1");
        soft.assertThat(nombre(rang(s, 1).get(ENSEMBLE))).as("72 000 ÷ 7 200 000").isEqualByComparingTo("1");
        soft.assertAll();
    }

    @Test
    void r10_ensembleDuRang_ponderePasLaSommeDesPourcentages() throws Exception {
        Map<String, Object> s = section(sections(), CONCRETISATIONS);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(nombre(rang(s, 1).get(ENSEMBLE))).as("2 ÷ 6, pas 25 + 50").isEqualByComparingTo("33.33");
        soft.assertThat(nombre(rang(s, 2).get(ENSEMBLE))).as("2 ÷ 6").isEqualByComparingTo("33.33");
        soft.assertAll();
    }

    @Test
    void r10_totauxImposes_commeLeN9() throws Exception {
        Map<String, Object> s = section(sections(), CONCRETISATIONS);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(nombre(totaux(s).get(colonneDe(s, a)))).as("3 ÷ 4").isEqualByComparingTo("75");
        soft.assertThat(nombre(totaux(s).get(colonneDe(s, b)))).as("1 ÷ 2").isEqualByComparingTo("50");
        soft.assertThat(nombre(totaux(s).get(ENSEMBLE))).as("4 ÷ 6").isEqualByComparingTo("66.67");
        soft.assertAll();
    }

    /** §5 : une section sans aucune tranche avec mouvement reste valide, colonnes fixes seules. */
    @Test
    void r10_sectionSansMouvement_aucuneColonneDynamique_aucuneLigne_colonnesFixesGardees() throws Exception {
        Map<String, Object> s = section(sections(), LIVRAISONS);

        assertThat(colonnesTranches(s)).isEmpty();
        assertThat(lignes(s)).isEmpty();
        assertThat(noms(s)).contains(MOIS, ENSEMBLE);
    }

    // ------------------------------------------------------------------ graphiques (§4.3)

    @Test
    void r10_unGraphiqueLignesParSection_categorieMois_uneSerieParTranche() throws Exception {
        List<Map<String, Object>> sections = sections();

        SoftAssertions soft = new SoftAssertions();
        for (String id : List.of(CONCRETISATIONS, ENCAISSEMENTS)) {
            Map<String, Object> s = section(sections, id);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> graphiques = (List<Map<String, Object>>) s.get("graphiques");
            soft.assertThat(graphiques).as(id).hasSize(1);
            if (graphiques != null && graphiques.size() == 1) {
                soft.assertThat(graphiques.get(0)).as(id).containsEntry("type", "LIGNES")
                        .containsEntry("colonneCategorie", MOIS)
                        .containsEntry("colonnesValeurs", colonnesTranches(s));
            }
        }
        soft.assertThat(colonnesTranches(section(sections, CONCRETISATIONS))).containsExactly("tranche01", "tranche02");
        soft.assertAll();
    }
}
