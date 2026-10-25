package com.tsix.immocrm.controller.rapport;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
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

import static com.tsix.immocrm.controller.rapport.AppelsRapports.VENTE;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.colonnes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.descripteur;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.nombre;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.section;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.somme;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.sousTotaux;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.totaux;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.colonne;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.noms;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.ANNEE;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.RYTHME_VENTES;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.TOTAL;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.demande;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.lignesContenant;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * N° 8 {@code ComTdbSalesRythme} « RYTHME DES VENTES » (FEATURE-tdb-vente-rapports-rythme.md §4.1, §7 « N° 8 »). Six sections,
 * une ligne par (tranche, année) de {@code annee} à l'année de l'horloge. Jeu, projet P (entité active) :
 * <pre>
 *   T1 : M1 MIN   saisi 05/05/2022, concrétisé 06/06/2022, acte 07/07/2022
 *        L1 CLEFS saisi 01/08/2022, livré 09/09/2023
 *        D1 DES   saisi 01/09/2022, concrétisé 01/10/2022, désisté 04/04/2024
 *        C1 CONC  saisi 15/01/2023, concrétisé 05/03/2023
 *        A1 ANN   saisi 20/01/2023, concrétisé 10/03/2023, annulé 02/02/2024
 *        R1 RES   saisi 10/02/2023 ;  S1 SANS statut saisi 11/02/2023
 *   T2 : C2 CONC  saisi 01/06/2024, concrétisé 01/07/2024
 *   T0 : aucun dossier
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RythmeVentesRapportTest {

    private static final int DEPART = 2022;
    private static final String TRANCHE = "tranche";
    private static final String RESERVATIONS = "reservations";
    private static final String CONCRETISATIONS = "concretisations";
    private static final String CONTRATS = "contrats";
    private static final String LIVRAISONS = "livraisons";
    private static final String DESISTEMENTS = "desistements";
    private static final String ANNULATIONS = "annulations";
    private static final List<String> SECTIONS = List.of(RESERVATIONS, CONCRETISATIONS, CONTRATS, LIVRAISONS,
            DESISTEMENTS, ANNULATIONS);
    private static final List<String> MOIS = List.of("m01", "m02", "m03", "m04", "m05", "m06", "m07", "m08", "m09",
            "m10", "m11", "m12");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports rapports;
    @Autowired private FixtureFluxVente fixture;
    @Autowired private FixtureEtatVentes ev;
    @Autowired private Clock clock;

    private AppelsRapports appels;
    private RequestPostProcessor libre;
    private CfgTranche t0;
    private CfgTranche t1;
    private CfgTranche t2;
    private int courante;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        RapportsRythmeSupport.installer(rapports);
        Jeu jeu = fixture.jeu("RV8");
        libre = connecte(rapports.compte("rv8." + jeu.prefixe(), rapports.role("Libre " + jeu.prefixe(), false)));
        courante = LocalDate.now(clock).getYear();
        Statuts st = jeu.statuts();
        CfgProject projet = jeu.projet("P", true);
        t0 = jeu.tranche("T0", projet);
        t1 = jeu.tranche("T1", projet);
        t2 = jeu.tranche("T2", projet);

        jeu.dater(saisi(jeu.dossier(t1, st.minute()), j(2022, 5, 5)), j(2022, 6, 6), j(2022, 7, 7), null, null);
        jeu.dater(saisi(jeu.dossier(t1, st.remiseClefs()), j(2022, 8, 1)), null, null, j(2023, 9, 9), null);
        jeu.dater(saisi(jeu.dossier(t1, st.desistement()), j(2022, 9, 1)), j(2022, 10, 1), null, null, j(2024, 4, 4));
        jeu.dater(saisi(jeu.dossier(t1, st.concretisation()), j(2023, 1, 15)), j(2023, 3, 5), null, null, null);
        jeu.dater(saisi(jeu.dossier(t1, st.annulation()), j(2023, 1, 20)), j(2023, 3, 10), null, null, j(2024, 2, 2));
        saisi(jeu.dossier(t1, st.reservation()), j(2023, 2, 10));
        saisi(jeu.dossier(t1, null), j(2023, 2, 11));
        jeu.dater(saisi(jeu.dossier(t2, st.concretisation()), j(2024, 6, 1)), j(2024, 7, 1), null, null, null);
        fixture.relire();
    }

    private static LocalDate j(int annee, int mois, int jour) {
        return LocalDate.of(annee, mois, jour);
    }

    private ComFolder saisi(ComFolder dossier, LocalDate saisie) {
        dossier.setDocDate(saisie.atStartOfDay());
        return ev.enregistrer(dossier);
    }

    private List<Map<String, Object>> sections() throws Exception {
        return appels.sections(demande(RYTHME_VENTES, libre, List.of(t0, t1, t2), ANNEE, String.valueOf(DEPART)));
    }

    /** Cellule de la ligne (tranche, année). */
    private static BigDecimal cellule(Map<String, Object> section, CfgTranche t, int annee, String colonne) {
        List<Map<String, Object>> trouvees = lignesContenant(section, TRANCHE, t.getCode()).stream()
                .filter(l -> String.valueOf(annee).equals(String.valueOf(l.get(ANNEE)))).toList();
        assertThat(trouvees).as("%s %s %d", section.get("id"), t.getCode(), annee).hasSize(1);
        return nombre(trouvees.get(0).get(colonne));
    }

    private String rang(Object libelleTranche) {
        String libelle = String.valueOf(libelleTranche);
        for (CfgTranche t : List.of(t0, t1, t2)) {
            if (libelle.contains(t.getCode())) {
                return t.getCode();
            }
        }
        return "?" + libelle;
    }

    // ------------------------------------------------------------------ structure (§4.1)

    @Test
    void r8_sixSectionsDansLOrdre_colonnesTrancheAnneeDouzeMoisTotal_types() throws Exception {
        List<Map<String, Object>> sections = sections();

        assertThat(sections).extracting(s -> s.get("id")).containsExactlyElementsOf(SECTIONS);
        List<String> attendues = new ArrayList<>(List.of(TRANCHE, ANNEE));
        attendues.addAll(MOIS);
        attendues.add(TOTAL);
        SoftAssertions soft = new SoftAssertions();
        for (Map<String, Object> s : sections) {
            soft.assertThat(noms(s)).as("%s colonnes", s.get("id")).containsExactlyElementsOf(attendues);
            soft.assertThat(colonne(s, TRANCHE).get("type")).as("%s tranche", s.get("id")).isEqualTo("TEXTE");
            soft.assertThat(colonne(s, ANNEE).get("type")).as("%s annee", s.get("id")).isEqualTo("TEXTE");
            for (String nom : attendues.subList(2, attendues.size())) {
                soft.assertThat(colonne(s, nom).get("type")).as("%s.%s", s.get("id"), nom).isEqualTo("NOMBRE");
            }
        }
        soft.assertAll();
    }

    @Test
    void r8_libellesDesMois_janvierADecembre() throws Exception {
        Map<String, Object> s = section(sections(), RESERVATIONS);

        assertThat(colonne(s, "m01")).containsEntry("libelle", "Janvier");
        assertThat(colonne(s, "m12")).containsEntry("libelle", "Décembre");
    }

    @Test
    void r8_uneLigneParTrancheEtAnneeDeLaFenetre_zerosCompris_trieesParTranchePuisAnnee() throws Exception {
        List<Map<String, Object>> sections = sections();

        List<String> attendu = new ArrayList<>();
        for (CfgTranche t : List.of(t0, t1, t2)) {
            for (int a = DEPART; a <= courante; a++) {
                attendu.add(t.getCode() + "/" + a);
            }
        }
        SoftAssertions soft = new SoftAssertions();
        for (Map<String, Object> s : sections) {
            soft.assertThat(lignes(s)).as(String.valueOf(s.get("id")))
                    .extracting(l -> rang(l.get(TRANCHE)) + "/" + l.get(ANNEE)).containsExactlyElementsOf(attendu);
        }
        soft.assertAll();
    }

    @Test
    void r8_trancheSansMouvement_lignesAZero_aucuneValeurNulle() throws Exception {
        List<Map<String, Object>> sections = sections();

        SoftAssertions soft = new SoftAssertions();
        for (Map<String, Object> s : sections) {
            List<Map<String, Object>> deT0 = lignesContenant(s, TRANCHE, t0.getCode());
            soft.assertThat(deT0).as("%s T0", s.get("id")).hasSize(courante - DEPART + 1);
            for (Map<String, Object> l : lignes(s)) {
                for (Map<String, Object> c : colonnes(s)) {
                    soft.assertThat(l.get(c.get("nom"))).as("%s.%s", s.get("id"), c.get("nom")).isNotNull();
                }
            }
            for (Map<String, Object> l : deT0) {
                soft.assertThat(nombre(l.get(TOTAL))).as("%s T0 %s", s.get("id"), l.get(ANNEE))
                        .isEqualByComparingTo("0");
            }
        }
        soft.assertAll();
    }

    // ------------------------------------------------------------------ mesures (§3.2, §4.1)

    @Test
    void r8_reservations_aLaDateDeSaisie_annuleEtDesisteCompte_sansStatutExclu() throws Exception {
        Map<String, Object> s = section(sections(), RESERVATIONS);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(cellule(s, t1, 2023, "m01")).as("C1 + A1 annulé").isEqualByComparingTo("2");
        soft.assertThat(cellule(s, t1, 2023, "m02")).as("R1, pas S1 sans statut").isEqualByComparingTo("1");
        soft.assertThat(cellule(s, t1, 2022, TOTAL)).as("M1 L1 D1 désisté").isEqualByComparingTo("3");
        soft.assertThat(cellule(s, t1, 2024, TOTAL)).as("ni la date d'annulation ni celle de désistement")
                .isEqualByComparingTo("0");
        soft.assertThat(cellule(s, t2, 2024, "m06")).isEqualByComparingTo("1");
        soft.assertAll();
    }

    @Test
    void r8_concretisations_dontAnnuleesEtDesistees_aLaDateDAvance() throws Exception {
        Map<String, Object> s = section(sections(), CONCRETISATIONS);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(cellule(s, t1, 2023, "m03")).as("C1 + A1").isEqualByComparingTo("2");
        soft.assertThat(cellule(s, t1, 2022, "m06")).isEqualByComparingTo("1");
        soft.assertThat(cellule(s, t1, 2022, "m10")).as("D1 désisté").isEqualByComparingTo("1");
        soft.assertThat(nombre(totaux(s).get(TOTAL))).isEqualByComparingTo("5");
        soft.assertAll();
    }

    @Test
    void r8_contratsLivraisonsDesistementsAnnulations_mesuresDuFlux() throws Exception {
        List<Map<String, Object>> sections = sections();

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(cellule(section(sections, CONTRATS), t1, 2022, "m07")).isEqualByComparingTo("1");
        soft.assertThat(nombre(totaux(section(sections, CONTRATS)).get(TOTAL))).isEqualByComparingTo("1");
        soft.assertThat(cellule(section(sections, LIVRAISONS), t1, 2023, "m09")).isEqualByComparingTo("1");
        soft.assertThat(nombre(totaux(section(sections, LIVRAISONS)).get(TOTAL))).isEqualByComparingTo("1");
        soft.assertThat(cellule(section(sections, DESISTEMENTS), t1, 2024, "m04")).isEqualByComparingTo("1");
        soft.assertThat(nombre(totaux(section(sections, DESISTEMENTS)).get(TOTAL))).isEqualByComparingTo("1");
        soft.assertThat(cellule(section(sections, ANNULATIONS), t1, 2024, "m02")).isEqualByComparingTo("1");
        soft.assertThat(nombre(totaux(section(sections, ANNULATIONS)).get(TOTAL))).isEqualByComparingTo("1");
        soft.assertAll();
    }

    // ------------------------------------------------------------------ totaux (§7 « N° 8 »)

    @Test
    void r8_totalDeLigne_egalSommeDesDouzeMois() throws Exception {
        List<Map<String, Object>> sections = sections();

        SoftAssertions soft = new SoftAssertions();
        for (Map<String, Object> s : sections) {
            for (Map<String, Object> l : lignes(s)) {
                BigDecimal mois = MOIS.stream().map(m -> nombre(l.get(m))).reduce(BigDecimal.ZERO, BigDecimal::add);
                soft.assertThat(nombre(l.get(TOTAL))).as("%s %s %s", s.get("id"), l.get(TRANCHE), l.get(ANNEE))
                        .isEqualByComparingTo(mois);
            }
        }
        soft.assertAll();
    }

    @Test
    void r8_groupeParTranche_sousTotalDeTranche_egalSommeDeSesAnnees() throws Exception {
        Map<String, Object> s = section(sections(), RESERVATIONS);
        Map<String, Map<String, Object>> parTranche = sousTotaux(s);

        assertThat(s).containsEntry("groupePar", TRANCHE);
        assertThat(parTranche).isNotNull();
        List<String> cles = parTranche.keySet().stream().filter(k -> k.contains(t1.getCode())).toList();
        assertThat(cles).hasSize(1);
        List<Map<String, Object>> deT1 = lignesContenant(s, TRANCHE, t1.getCode());
        SoftAssertions soft = new SoftAssertions();
        for (String nom : List.of("m01", "m02", "m05", TOTAL)) {
            soft.assertThat(nombre(parTranche.get(cles.get(0)).get(nom))).as(nom)
                    .isEqualByComparingTo(somme(deT1, nom));
        }
        soft.assertThat(nombre(parTranche.get(cles.get(0)).get(TOTAL))).isEqualByComparingTo("6");
        soft.assertAll();
    }

    @Test
    void r8_totauxSomme_surLesMoisEtLeTotal_egalSommeDesLignes() throws Exception {
        List<Map<String, Object>> sections = sections();

        SoftAssertions soft = new SoftAssertions();
        for (Map<String, Object> s : sections) {
            List<String> sommees = new ArrayList<>(MOIS);
            sommees.add(TOTAL);
            for (String nom : sommees) {
                soft.assertThat(colonne(s, nom).get("fonctionTotal")).as("%s.%s", s.get("id"), nom).isEqualTo("SOMME");
                soft.assertThat(nombre(totaux(s).get(nom))).as("%s total %s", s.get("id"), nom)
                        .isEqualByComparingTo(somme(lignes(s), nom));
            }
        }
        soft.assertThat(nombre(totaux(section(sections, RESERVATIONS)).get(TOTAL))).isEqualByComparingTo("7");
        soft.assertAll();
    }

    @Test
    void r8_pasDeGraphique() throws Exception {
        List<Map<String, Object>> sections = sections();

        SoftAssertions soft = new SoftAssertions();
        for (Map<String, Object> s : sections) {
            Object graphiques = s.get("graphiques");
            soft.assertThat(graphiques == null || ((List<?>) graphiques).isEmpty()).as(String.valueOf(s.get("id")))
                    .isTrue();
        }
        soft.assertAll();
    }

    // ------------------------------------------------------------------ paramètres (§4.1, Q15)

    @Test
    void r8_anneeApresLAnneeDeLHorloge_sectionsVides_200() throws Exception {
        List<Map<String, Object>> sections = appels.sections(demande(RYTHME_VENTES, libre, List.of(t0, t1, t2), ANNEE,
                String.valueOf(courante + 1)));

        assertThat(sections).extracting(s -> s.get("id")).containsExactlyElementsOf(SECTIONS);
        assertThat(sections).isNotEmpty().allSatisfy(s -> assertThat(lignes(s)).isEmpty());
    }

    @Test
    void r8_sansParametreAnnee_fenetreAnneeCouranteMoins5_aAnneeCourante() throws Exception {
        Map<String, Object> s = section(appels.sections(demande(RYTHME_VENTES, libre, List.of(t1))), RESERVATIONS);

        List<String> annees = new ArrayList<>();
        for (int a = courante - 5; a <= courante; a++) {
            annees.add(String.valueOf(a));
        }
        assertThat(lignes(s)).extracting(l -> String.valueOf(l.get(ANNEE))).containsExactlyElementsOf(annees);
    }

    @Test
    void r8_descripteur_annee_aPartirDeLAnnee_defautAnneeCouranteMoins5_trancheIdMultiple() throws Exception {
        String corps = appels.corps(get(descripteur(VENTE, RYTHME_VENTES)).with(libre));
        List<Map<String, Object>> annee = JsonPath.read(corps, "$.parametres[?(@.nom == 'annee')]");
        List<Map<String, Object>> tranches = JsonPath.read(corps, "$.parametres[?(@.nom == 'trancheId')]");

        assertThat(annee).singleElement().satisfies(p -> assertThat(p).containsEntry("type", "ANNEE")
                .containsEntry("libelle", "À partir de l'année")
                .containsEntry("valeurParDefaut", String.valueOf(courante - 5)));
        assertThat(tranches).singleElement().satisfies(p -> assertThat(p).containsEntry("type", "TRANCHES")
                .containsEntry("multiple", true));
    }

    @Test
    void r8_avertissement_seuilsDeStatutAppliques() throws Exception {
        String corps = appels.corps(demande(RYTHME_VENTES, libre, List.of(t1), ANNEE, String.valueOf(DEPART)));
        List<String> avertissements = JsonPath.read(corps, "$.avertissements");

        assertThat(avertissements).anyMatch(a -> a.contains("seuils de statut"));
    }
}
