package com.tsix.immocrm.controller.rapport;

import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.fixtures.FixtureFluxVente;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Jeu;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Statuts;
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
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.colonnes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.nombre;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.section;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.totaux;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.ETAT_VENTES;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.colonne;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.demande;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.ligne;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.noms;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * N° 14 {@code ComTdbSalesState} « ÉTAT DES VENTE » (FEATURE-tdb-vente-rapports-flux.md §4.3, §9 « N° 14 »). Six sections années ×
 * mois, fenêtre {@code annee − 5 … annee}. Jeu (tranche T1, sauf mention) :
 * <pre>
 *   C1 CONC  concrétisé 05/03/2024 ; encaissement 1 000 le 05/05/2024
 *   A1 ANN   concrétisé 20/03/2024, annulé 02/08/2024
 *   M1 MIN   acte 07/07/2023, prix 1 200 000
 *   L1 CLEFS livré 10/10/2022
 *   D1 DES   désisté 02/02/2021
 *   V1 CONC  concrétisé 06/06/2018 (hors fenêtre 2019–2024)
 *   X1 CONC  concrétisé 01/03/2024 sur T2 (tranche non demandée)
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EtatVentesMensuelRapportTest {

    private static final int ANNEE = 2024;
    private static final String CONCRETISATIONS = "concretisations";
    private static final String CHIFFRE_AFFAIRES = "chiffre-affaires";
    private static final String LIVRAISONS = "livraisons";
    private static final String ENCAISSEMENTS = "encaissements";
    private static final String ANNULATIONS = "annulations";
    private static final String DESISTEMENTS = "desistements";
    private static final List<String> SECTIONS = List.of(CONCRETISATIONS, CHIFFRE_AFFAIRES, LIVRAISONS, ENCAISSEMENTS,
            ANNULATIONS, DESISTEMENTS);
    private static final String COL_ANNEE = "annee";
    private static final String TOTAL = "total";
    private static final List<String> MOIS = List.of("m01", "m02", "m03", "m04", "m05", "m06", "m07", "m08", "m09",
            "m10", "m11", "m12");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports rapports;
    @Autowired private FixtureFluxVente fixture;
    @Autowired private Clock clock;

    private AppelsRapports appels;
    private RequestPostProcessor libre;
    private CfgTranche t1;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        RapportsFluxSupport.installer(rapports);
        Jeu jeu = fixture.jeu("E14");
        libre = connecte(rapports.compte("e14." + jeu.prefixe(), rapports.role("Libre " + jeu.prefixe(), false)));
        Statuts st = jeu.statuts();
        CfgProject projet = jeu.projet("P", true);
        t1 = jeu.tranche("T1", projet);
        CfgTranche t2 = jeu.tranche("T2", projet);

        ComFolder c1 = jeu.dater(jeu.dossier(t1, st.concretisation()), j(2024, 3, 5), null, null, null);
        jeu.encaisser(c1, "1000", j(2024, 5, 5));
        jeu.dater(jeu.dossier(t1, st.annulation()), j(2024, 3, 20), null, null, j(2024, 8, 2));
        jeu.dater(jeu.dossier(t1, st.minute()), null, j(2023, 7, 7), null, null);
        jeu.dater(jeu.dossier(t1, st.remiseClefs()), null, null, j(2022, 10, 10), null);
        jeu.dater(jeu.dossier(t1, st.desistement()), null, null, null, j(2021, 2, 2));
        jeu.dater(jeu.dossier(t1, st.concretisation()), j(2018, 6, 6), null, null, null);
        jeu.dater(jeu.dossier(t2, st.concretisation()), j(2024, 3, 1), null, null, null);
        fixture.relire();
    }

    private static LocalDate j(int annee, int mois, int jour) {
        return LocalDate.of(annee, mois, jour);
    }

    private List<Map<String, Object>> sections2024() throws Exception {
        return appels.sections(demande(ETAT_VENTES, libre, ANNEE, t1));
    }

    private static BigDecimal cellule(Map<String, Object> section, int annee, String mois) {
        return nombre(ligne(section, COL_ANNEE, annee).get(mois));
    }

    @Test
    void n14_sixSections_dansLOrdre_colonnesAnneeDouzeMoisTotal_typesParMesure() throws Exception {
        List<Map<String, Object>> sections = sections2024();

        assertThat(sections).extracting(s -> s.get("id")).containsExactlyElementsOf(SECTIONS);
        List<String> attendues = new ArrayList<>(List.of(COL_ANNEE));
        attendues.addAll(MOIS);
        attendues.add(TOTAL);
        SoftAssertions soft = new SoftAssertions();
        for (Map<String, Object> s : sections) {
            String type = List.of(CHIFFRE_AFFAIRES, ENCAISSEMENTS).contains(s.get("id")) ? "MONTANT" : "NOMBRE";
            soft.assertThat(noms(s)).as("%s colonnes", s.get("id")).containsExactlyElementsOf(attendues);
            soft.assertThat(colonne(s, COL_ANNEE).get("type")).as("%s annee", s.get("id")).isEqualTo("TEXTE");
            for (String nom : attendues.subList(1, attendues.size())) {
                soft.assertThat(colonne(s, nom).get("type")).as("%s.%s", s.get("id"), nom).isEqualTo(type);
            }
        }
        soft.assertAll();
    }

    @Test
    void n14_fenetre_anneeMoins5AAnnee_sixLignes_plusAncienneDAbord() throws Exception {
        List<Map<String, Object>> sections = sections2024();

        SoftAssertions soft = new SoftAssertions();
        for (Map<String, Object> s : sections) {
            soft.assertThat(lignes(s)).as(String.valueOf(s.get("id")))
                    .extracting(l -> String.valueOf(l.get(COL_ANNEE)))
                    .containsExactly("2019", "2020", "2021", "2022", "2023", "2024");
        }
        soft.assertAll();
    }

    @Test
    void n14_valeurs_concretisationsAvecAnnulees_caLivraisonsEncaissementsAnnulationsDesistements() throws Exception {
        List<Map<String, Object>> sections = sections2024();

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(cellule(section(sections, CONCRETISATIONS), 2024, "m03")).isEqualByComparingTo("2");
        soft.assertThat(cellule(section(sections, CHIFFRE_AFFAIRES), 2023, "m07")).isEqualByComparingTo("1200000");
        soft.assertThat(cellule(section(sections, LIVRAISONS), 2022, "m10")).isEqualByComparingTo("1");
        soft.assertThat(cellule(section(sections, ENCAISSEMENTS), 2024, "m05")).isEqualByComparingTo("1000");
        soft.assertThat(cellule(section(sections, ANNULATIONS), 2024, "m08")).isEqualByComparingTo("1");
        soft.assertThat(cellule(section(sections, DESISTEMENTS), 2021, "m02")).isEqualByComparingTo("1");
        soft.assertAll();
    }

    @Test
    void n14_horsFenetreEtTrancheNonDemandee_ignorees_totalDesConcretisations2() throws Exception {
        Map<String, Object> concretisations = section(sections2024(), CONCRETISATIONS);

        assertThat(nombre(totaux(concretisations).get(TOTAL))).isEqualByComparingTo("2");
    }

    @Test
    void n14_totalDeLigne_egalSommeDesDouzeMois() throws Exception {
        List<Map<String, Object>> sections = sections2024();

        SoftAssertions soft = new SoftAssertions();
        for (Map<String, Object> s : sections) {
            for (Map<String, Object> l : lignes(s)) {
                BigDecimal somme = MOIS.stream().map(m -> nombre(l.get(m))).reduce(BigDecimal.ZERO, BigDecimal::add);
                soft.assertThat(nombre(l.get(TOTAL))).as("%s %s", s.get("id"), l.get(COL_ANNEE))
                        .isEqualByComparingTo(somme);
            }
        }
        soft.assertAll();
    }

    @Test
    void n14_totauxServeur_sommeDesAnnees_surChaqueMoisEtLeTotal() throws Exception {
        List<Map<String, Object>> sections = sections2024();

        SoftAssertions soft = new SoftAssertions();
        for (Map<String, Object> s : sections) {
            List<String> sommees = new ArrayList<>(MOIS);
            sommees.add(TOTAL);
            for (String nom : sommees) {
                soft.assertThat(colonne(s, nom).get("fonctionTotal")).as("%s.%s", s.get("id"), nom).isEqualTo("SOMME");
                soft.assertThat(nombre(totaux(s).get(nom))).as("%s total %s", s.get("id"), nom)
                        .isEqualByComparingTo(AppelsRapports.somme(lignes(s), nom));
            }
        }
        soft.assertAll();
    }

    @Test
    void n14_graphiques_unColonnesParSection_categorieAnnee_serieTotal() throws Exception {
        List<Map<String, Object>> sections = sections2024();

        SoftAssertions soft = new SoftAssertions();
        for (Map<String, Object> s : sections) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> graphiques = (List<Map<String, Object>>) s.get("graphiques");
            soft.assertThat(graphiques).as(String.valueOf(s.get("id"))).hasSize(1);
            if (graphiques != null && graphiques.size() == 1) {
                soft.assertThat(graphiques.get(0)).containsEntry("type", "COLONNES")
                        .containsEntry("colonneCategorie", COL_ANNEE)
                        .containsEntry("colonnesValeurs", List.of(TOTAL));
            }
        }
        soft.assertAll();
    }

    @Test
    void n14_libellesDesMois_francaisCapitalises() throws Exception {
        Map<String, Object> s = section(sections2024(), CONCRETISATIONS);

        assertThat(colonne(s, "m01")).containsEntry("libelle", "Janvier");
        assertThat(colonne(s, "m08")).containsEntry("libelle", "Août");
        assertThat(colonne(s, "m12")).containsEntry("libelle", "Décembre");
    }

    @Test
    void n14_sansParametreAnnee_fenetreFinitALAnneeDeLHorloge() throws Exception {
        int courante = LocalDate.now(clock).getYear();

        Map<String, Object> s = section(appels.sections(demande(ETAT_VENTES, libre, null, t1)), CONCRETISATIONS);

        assertThat(lignes(s)).hasSize(6).last().extracting(l -> String.valueOf(l.get(COL_ANNEE)))
                .isEqualTo(String.valueOf(courante));
    }

    @Test
    void n14_colonnesDeTypeAttendu_aucuneValeurNulle() throws Exception {
        List<Map<String, Object>> sections = sections2024();

        List<String> nulles = new ArrayList<>();
        for (Map<String, Object> s : sections) {
            for (Map<String, Object> l : lignes(s)) {
                for (Map<String, Object> c : colonnes(s)) {
                    if (l.get(c.get("nom")) == null) {
                        nulles.add(s.get("id") + "." + c.get("nom") + "@" + l.get(COL_ANNEE));
                    }
                }
            }
        }
        assertThat(nulles).as("zéros partout où il n'y a rien").isEmpty();
    }
}
