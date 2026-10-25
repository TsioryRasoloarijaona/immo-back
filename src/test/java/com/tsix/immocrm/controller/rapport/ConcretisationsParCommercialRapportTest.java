package com.tsix.immocrm.controller.rapport;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureFluxVente;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Jeu;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Statuts;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.services.equipe.RolesEquipe;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
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
import static com.tsix.immocrm.controller.rapport.AppelsRapports.totaux;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.colonne;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.ligne;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.noms;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.ANNEE;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.NON_AFFECTE;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.PAR_COMMERCIAL;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.TOTAL;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.colonneLibellee;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.colonnesCommerciales;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.demande;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * N° 21 {@code ComTdbConcretPerCommercial} « CONCRÉTISATIONS PAR PÉRIODES » (FEATURE-tdb-vente-rapports-rythme.md §4.4, §7
 * « N° 21 », Q8, Q9). Concrétisations nettes ({@code AdvanceDate}, statut ≥ CONC, ni annulé ni désisté), {@code annee} = 2023 :
 * <pre>
 *   projet P (actif) : tranche T — A (Commercial) et C (Commercial Chef) affectés ; projet P2 : tranche T2 — B affecté
 *   projet PI (entité INACTIVE) : tranche TI
 *   A sur T : CONC 01/01/2023 (dimanche ⇒ S01), CONC 02/01/2023 (lundi ⇒ S02), MIN 10/05/2023,
 *             ANN concrétisé 11/05/2023 (exclu), CONC 03/03/2021, CONC 04/04/2018 (hors 2019–2023), CONC 05/01/2024
 *   B : CONC 15/05/2023 sur T2, CONC 07/07/2023 sur T ;  C sur T : CONC 09/09/2023
 *   sans commercial sur T : CONC 08/08/2023 ;  A sur TI : CONC 20/05/2023 (entité inactive)
 *   ⇒ exempté, par mois 2023 : A 3, B 2, C 1, non affecté 1, total 7 ; par année : 2021 = 1, 2023 = 7
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ConcretisationsParCommercialRapportTest {

    private static final int ANNEE_RAPPORT = 2023;
    private static final String PAR_SEMAINE = "par-semaine";
    private static final String PAR_MOIS = "par-mois";
    private static final String PAR_ANNEE = "par-annee";
    private static final String SEMAINE = "semaine";
    private static final String MOIS = "mois";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports rapports;
    @Autowired private FixtureFluxVente fixture;
    @Autowired private FixtureEtatVentes ev;
    @Autowired private Clock clock;

    private AppelsRapports appels;
    private String prefixe;
    private Statuts st;
    private CfgTranche t;
    private CfgTranche t2;
    private CfgTranche ti;
    private AdmUser a;
    private AdmUser direction;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        RapportsRythmeSupport.installer(rapports);
        Jeu jeu = fixture.jeu("C21");
        prefixe = jeu.prefixe();
        st = jeu.statuts();
        CfgProject p = jeu.projet("P", true);
        CfgProject p2 = jeu.projet("P2", true);
        t = jeu.tranche("T", p);
        t2 = jeu.tranche("T2", p2);
        ti = jeu.tranche("TI", jeu.projet("PI", false));

        AdmRole libre = rapports.role("Libre " + prefixe, false);
        a = rapports.compte(jeton("alpha"), libre, rapports.role(RolesEquipe.COMMERCIAL, true));
        AdmUser b = rapports.compte(jeton("beta"), libre, rapports.role(RolesEquipe.COMMERCIAL, true));
        AdmUser c = rapports.compte(jeton("gamma"), libre, rapports.role(RolesEquipe.CHEF, true));
        direction = rapports.compte(jeton("dg"), rapports.role("Direction Générale", false));
        ev.affecter(a, p);
        ev.affecter(c, p);
        ev.affecter(b, p2);

        concretise(jeu, t, a, st.concretisation(), j(2023, 1, 1));
        concretise(jeu, t, a, st.concretisation(), j(2023, 1, 2));
        concretise(jeu, t, a, st.minute(), j(2023, 5, 10));
        jeu.dater(concretise(jeu, t, a, st.annulation(), j(2023, 5, 11)), j(2023, 5, 11), null, null, j(2023, 6, 1));
        concretise(jeu, t, a, st.concretisation(), j(2021, 3, 3));
        concretise(jeu, t, a, st.concretisation(), j(2018, 4, 4));
        concretise(jeu, t, a, st.concretisation(), j(2024, 1, 5));
        concretise(jeu, t2, b, st.concretisation(), j(2023, 5, 15));
        concretise(jeu, t, b, st.concretisation(), j(2023, 7, 7));
        concretise(jeu, t, c, st.concretisation(), j(2023, 9, 9));
        concretise(jeu, t, null, st.concretisation(), j(2023, 8, 8));
        concretise(jeu, ti, a, st.concretisation(), j(2023, 5, 20));
        rapports.relire();
    }

    private String jeton(String nom) {
        return nom + "." + prefixe;
    }

    private static LocalDate j(int annee, int mois, int jour) {
        return LocalDate.of(annee, mois, jour);
    }

    private ComFolder concretise(Jeu jeu, CfgTranche tranche, AdmUser commercial, StrStatus statut, LocalDate avance) {
        ComFolder d = jeu.dossier(tranche, statut);
        d.setCfgCommercial(commercial == null ? null : commercial.getCfgEmployee());
        return jeu.dater(d, avance, null, null, null);
    }

    private List<Map<String, Object>> sections(AdmUser qui, int annee) throws Exception {
        return appels.sections(demande(PAR_COMMERCIAL, connecte(qui), List.of(t, t2, ti), ANNEE,
                String.valueOf(annee)));
    }

    private List<Map<String, Object>> sectionsDirection() throws Exception {
        return sections(direction, ANNEE_RAPPORT);
    }

    private static BigDecimal totalDe(Map<String, Object> section, String colonne) {
        return nombre(totaux(section).get(colonne));
    }

    // ------------------------------------------------------------------ structure

    @Test
    void r21_troisSections_parSemaineParMoisParAnnee_titreAvecLAnnee() throws Exception {
        List<Map<String, Object>> sections = sectionsDirection();

        assertThat(sections).extracting(s -> s.get("id")).containsExactly(PAR_SEMAINE, PAR_MOIS, PAR_ANNEE);
        assertThat(String.valueOf(section(sections, PAR_SEMAINE).get("titre"))).contains("2023");
        assertThat(String.valueOf(section(sections, PAR_MOIS).get("titre"))).contains("2023");
    }

    @Test
    void r21_colonnes_periodePuisUneParCommercialNonAffecteTotal_typesNombre() throws Exception {
        Map<String, Object> mois = section(sectionsDirection(), PAR_MOIS);

        assertThat(noms(mois)).containsExactly(MOIS, "commercial01", "commercial02", "commercial03", NON_AFFECTE,
                TOTAL);
        SoftAssertions soft = new SoftAssertions();
        for (String nom : List.of("commercial01", "commercial02", "commercial03", NON_AFFECTE, TOTAL)) {
            soft.assertThat(colonne(mois, nom).get("type")).as(nom).isEqualTo("NOMBRE");
        }
        soft.assertThat(colonne(mois, NON_AFFECTE)).containsEntry("libelle", "Non affecté");
        soft.assertAll();
    }

    @Test
    void r21_colonnesCommerciales_nommeesParRang_ordreDuNom_libelleNomDuCommercial() throws Exception {
        Map<String, Object> mois = section(sectionsDirection(), PAR_MOIS);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(colonneLibellee(mois, jeton("alpha"))).isEqualTo("commercial01");
        soft.assertThat(colonneLibellee(mois, jeton("beta"))).isEqualTo("commercial02");
        soft.assertThat(colonneLibellee(mois, jeton("gamma"))).isEqualTo("commercial03");
        soft.assertAll();
    }

    // ------------------------------------------------------------------ valeurs

    @Test
    void r21_parMois_concretisationsNettesParCommercial_nonAffecte_total() throws Exception {
        Map<String, Object> mois = section(sectionsDirection(), PAR_MOIS);

        assertThat(lignes(mois)).hasSize(12);
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(totalDe(mois, colonneLibellee(mois, jeton("alpha")))).isEqualByComparingTo("3");
        soft.assertThat(totalDe(mois, colonneLibellee(mois, jeton("beta")))).as("T2 + T").isEqualByComparingTo("2");
        soft.assertThat(totalDe(mois, colonneLibellee(mois, jeton("gamma")))).isEqualByComparingTo("1");
        soft.assertThat(totalDe(mois, NON_AFFECTE)).isEqualByComparingTo("1");
        soft.assertThat(totalDe(mois, TOTAL)).as("entité inactive exclue").isEqualByComparingTo("7");
        soft.assertThat(nombre(lignes(mois).get(0).get(colonneLibellee(mois, jeton("alpha"))))).as("janvier")
                .isEqualByComparingTo("2");
        soft.assertAll();
    }

    @Test
    void r21_dossierAnnule_exclu() throws Exception {
        Map<String, Object> mois = section(sectionsDirection(), PAR_MOIS);

        assertThat(nombre(lignes(mois).get(4).get(colonneLibellee(mois, jeton("alpha"))))).as("mai : MIN, pas ANN")
                .isEqualByComparingTo("1");
    }

    @Test
    void r21_parAnnee_fenetreAnneeMoins4AAnnee_horsFenetreIgnores() throws Exception {
        Map<String, Object> annees = section(sectionsDirection(), PAR_ANNEE);

        assertThat(lignes(annees)).extracting(l -> String.valueOf(l.get(ANNEE)))
                .containsExactly("2019", "2020", "2021", "2022", "2023");
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(nombre(ligne(annees, ANNEE, 2021).get(TOTAL))).isEqualByComparingTo("1");
        soft.assertThat(nombre(ligne(annees, ANNEE, 2023).get(TOTAL))).isEqualByComparingTo("7");
        soft.assertThat(totalDe(annees, TOTAL)).as("ni 2018 ni 2024").isEqualByComparingTo("8");
        soft.assertAll();
    }

    // Q8 : semaine SQL Server, DATEFIRST 1 = WeekFields.of(MONDAY, 1)
    @Test
    void r21_semaine_dimanche01012023_S01_lundi02012023_S02() throws Exception {
        Map<String, Object> semaines = section(sectionsDirection(), PAR_SEMAINE);
        String colonneA = colonneLibellee(semaines, jeton("alpha"));

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(String.valueOf(lignes(semaines).get(0).get(SEMAINE))).isEqualTo("S01");
        soft.assertThat(nombre(ligne(semaines, SEMAINE, "S01").get(colonneA))).isEqualByComparingTo("1");
        soft.assertThat(nombre(ligne(semaines, SEMAINE, "S02").get(colonneA))).isEqualByComparingTo("1");
        soft.assertAll();
    }

    @Test
    void r21_parSemaine_uneLigneParSemaineDeLAnnee() throws Exception {
        Map<String, Object> semaines = section(sectionsDirection(), PAR_SEMAINE);
        int derniere = j(ANNEE_RAPPORT, 12, 31).get(WeekFields.of(DayOfWeek.MONDAY, 1).weekOfYear());

        assertThat(lignes(semaines)).hasSize(derniere);
        assertThat(String.valueOf(lignes(semaines).get(derniere - 1).get(SEMAINE)))
                .isEqualTo("S%02d".formatted(derniere));
    }

    @Test
    void r21_totalDeLigne_egalSommeDesColonnesAffichees() throws Exception {
        List<Map<String, Object>> sections = sectionsDirection();

        SoftAssertions soft = new SoftAssertions();
        for (Map<String, Object> s : sections) {
            List<String> affichees = new ArrayList<>(colonnesCommerciales(s));
            if (noms(s).contains(NON_AFFECTE)) {
                affichees.add(NON_AFFECTE);
            }
            soft.assertThat(affichees).as("%s colonnes", s.get("id")).isNotEmpty();
            for (Map<String, Object> l : lignes(s)) {
                BigDecimal somme = affichees.stream().map(n -> nombre(l.get(n))).reduce(BigDecimal.ZERO,
                        BigDecimal::add);
                soft.assertThat(nombre(l.get(TOTAL))).as("%s %s", s.get("id"), l).isEqualByComparingTo(somme);
            }
        }
        soft.assertAll();
    }

    @Test
    void r21_totauxSomme_egalSommeDesLignes() throws Exception {
        List<Map<String, Object>> sections = sectionsDirection();

        SoftAssertions soft = new SoftAssertions();
        for (Map<String, Object> s : sections) {
            List<String> nombres = new ArrayList<>(colonnesCommerciales(s));
            nombres.add(TOTAL);
            for (String nom : nombres) {
                soft.assertThat(colonne(s, nom).get("fonctionTotal")).as("%s.%s", s.get("id"), nom).isEqualTo("SOMME");
                soft.assertThat(totalDe(s, nom)).as("%s.%s", s.get("id"), nom)
                        .isEqualByComparingTo(somme(lignes(s), nom));
            }
        }
        soft.assertAll();
    }

    // ------------------------------------------------------------------ colonnes présentes selon la fenêtre (§4.4)

    @Test
    void r21_commercialSansConcretisationDansLaFenetreDeLaSection_pasDeColonne_nonAffecteAbsentSiNul()
            throws Exception {
        List<Map<String, Object>> sections = sections(direction, 2022);

        Map<String, Object> mois = section(sections, PAR_MOIS);
        Map<String, Object> annees = section(sections, PAR_ANNEE);
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(colonnesCommerciales(mois)).as("aucune concrétisation en 2022").isEmpty();
        soft.assertThat(colonnesCommerciales(annees)).as("A seul, 2018 et 2021").hasSize(1);
        soft.assertThat(totalDe(annees, colonneLibellee(annees, jeton("alpha")))).isEqualByComparingTo("2");
        for (Map<String, Object> s : sections) {
            soft.assertThat(noms(s)).as("%s sans non affecté", s.get("id")).doesNotContain(NON_AFFECTE);
        }
        soft.assertAll();
    }

    // ------------------------------------------------------------------ portée des commerciaux (§7 « N° 21 », Q9)

    @Test
    void r21_commercialRestreint_seuleSaColonne_pasDeNonAffecte_totalEgalSesConcretisations() throws Exception {
        List<Map<String, Object>> sections = sections(a, ANNEE_RAPPORT);

        SoftAssertions soft = new SoftAssertions();
        for (Map<String, Object> s : sections) {
            soft.assertThat(colonnesCommerciales(s)).as("%s", s.get("id")).hasSize(1);
            soft.assertThat(noms(s)).as("%s", s.get("id")).doesNotContain(NON_AFFECTE);
            soft.assertThat(colonnes(s)).as("%s libellés", s.get("id")).isNotEmpty()
                    .noneMatch(c -> String.valueOf(c.get("libelle")).contains(jeton("beta")))
                    .noneMatch(c -> String.valueOf(c.get("libelle")).contains(jeton("gamma")));
        }
        Map<String, Object> mois = section(sections, PAR_MOIS);
        soft.assertThat(colonneLibellee(mois, jeton("alpha"))).isEqualTo("commercial01");
        soft.assertThat(totalDe(mois, TOTAL)).isEqualByComparingTo("3");
        soft.assertThat(totalDe(section(sections, PAR_ANNEE), TOTAL)).isEqualByComparingTo("4");
        soft.assertAll();
    }

    // ------------------------------------------------------------------ graphiques et paramètres

    @Test
    void r21_graphiques_unColonnesParSection_categoriePremiereColonne_serieTotal() throws Exception {
        List<Map<String, Object>> sections = sectionsDirection();

        SoftAssertions soft = new SoftAssertions();
        for (Map<String, Object> s : sections) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> graphiques = (List<Map<String, Object>>) s.get("graphiques");
            soft.assertThat(graphiques).as(String.valueOf(s.get("id"))).hasSize(1);
            if (graphiques != null && graphiques.size() == 1) {
                soft.assertThat(graphiques.get(0)).as(String.valueOf(s.get("id"))).containsEntry("type", "COLONNES")
                        .containsEntry("colonneCategorie", noms(s).get(0))
                        .containsEntry("colonnesValeurs", List.of(TOTAL));
            }
        }
        soft.assertAll();
    }

    @Test
    void r21_descripteur_anneeDefautAnneeCourante_trancheIdMultiple() throws Exception {
        String corps = appels.corps(get(descripteur(VENTE, PAR_COMMERCIAL)).with(connecte(direction)));
        List<Map<String, Object>> annee = JsonPath.read(corps, "$.parametres[?(@.nom == 'annee')]");
        List<Map<String, Object>> tranches = JsonPath.read(corps, "$.parametres[?(@.nom == 'trancheId')]");

        assertThat(annee).singleElement().satisfies(p -> assertThat(p).containsEntry("type", "ANNEE")
                .containsEntry("valeurParDefaut", String.valueOf(LocalDate.now(clock).getYear())));
        assertThat(tranches).singleElement().satisfies(p -> assertThat(p).containsEntry("type", "TRANCHES"));
    }
}
