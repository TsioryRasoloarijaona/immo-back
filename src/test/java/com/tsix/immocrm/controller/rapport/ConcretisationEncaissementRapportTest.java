package com.tsix.immocrm.controller.rapport;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.InteractionKpi;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessageType;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.fixtures.FixtureFluxVente;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Genre;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Jeu;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Statuts;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.fixtures.FixtureTransformationVente;
import com.tsix.immocrm.services.rapport.vente.flux.AnomaliesFlux;
import com.tsix.immocrm.services.rapport.vente.flux.CleFlux;
import com.tsix.immocrm.services.rapport.vente.flux.DemandeFlux;
import com.tsix.immocrm.services.rapport.vente.flux.FluxMensuel;
import com.tsix.immocrm.services.rapport.vente.flux.FluxVenteMensuel;
import com.tsix.immocrm.services.rapport.vente.flux.MesureFlux;
import com.tsix.immocrm.services.rapport.vente.flux.SourcePrevu;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.VENTE;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.descripteur;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.nombre;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.section;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.somme;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.totaux;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.PREVU_NON_SAISI;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.colonne;
import static com.tsix.immocrm.controller.rapport.RapportsFluxSupport.noms;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.COEFFICIENT;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.CONCRET_ENCAISSEMENT;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.DATE;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.demande;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.ligneContenant;
import static com.tsix.immocrm.controller.rapport.RapportsRythmeSupport.lignesContenant;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureFluxVente.CANCELED;
import static com.tsix.immocrm.fixtures.FixtureFluxVente.IN_WORK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * N° 23 {@code ComTdbConcretAndSetlement} « CONCRÉTISATION ET ENCAISSEMENT » (FEATURE-tdb-vente-rapports-rythme.md §4.6, §7
 * « N° 23 », §8 M2, M5, M6, Q10–Q12). Une ligne par projet ayant une tranche visible, cumuls jusqu'à {@code date} = 15/06/2025 :
 * <pre>
 *   PA (actif) : TA1, TA2 ; PB (actif) : TB ; PC (actif) : TC sans rien ; PI (entité INACTIVE) : TI
 *   visites (type Visite) : TA1 15/06/2025, 10/01/2025, 03/03/2019, 16/06/2025 (exclue) + 1 Appel ; TA2 05/05/2025 ;
 *                           TB 03/03/2025, 04/04/2025 ; TI 03/03/2025            ⇒ PA 4, PB 2
 *   concrétisations nettes : TA1 CONC 15/06/2025 (D), CONC 16/06/2025 (exclue), CONC 03/03/2024, ANN 01/01/2025 (exclue) ;
 *                            TA2 CONC 02/02/2025 ; TB CONC 05/05/2025 ; TI CONC 01/01/2025   ⇒ PA 3, PB 1
 *   encaissements : D 1 000 le 15/06, 5 000 le 16/06 (exclu), 999 CANCELED ; ANN 300 le 02/02 ; dossier sans statut 777 ;
 *                   TB 190,75 + 0,25 ; TI 3 000                                            ⇒ PA 1 300, PB 191,00
 * </pre>
 * {@link SourcePrevu} est simulé : vide et « non saisi » par défaut ; prévu de PA dans certains tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ConcretisationEncaissementRapportTest {

    private static final LocalDate AU = LocalDate.of(2025, 6, 15);
    private static final String PROJETS = "projets";
    private static final String PROJET = "projet";
    private static final String VISITES = "visites";
    private static final String CONCRETISATIONS = "concretisations";
    private static final String TAUX_VISITES = "tauxVisites";
    private static final String CONCRETISATIONS_PREVUES = "concretisationsPrevues";
    private static final String TAUX_CONCRETISATION = "tauxConcretisation";
    private static final String ENCAISSEMENTS = "encaissements";
    private static final String ENCAISSEMENTS_PREVUS = "encaissementsPrevus";
    private static final String TAUX_ENCAISSEMENT = "tauxEncaissement";
    private static final List<String> SOMMEES = List.of(VISITES, CONCRETISATIONS, CONCRETISATIONS_PREVUES,
            ENCAISSEMENTS, ENCAISSEMENTS_PREVUS);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports rapports;
    @Autowired private FixtureFluxVente fixture;
    @Autowired private FixtureTransformationVente fx;
    @Autowired private FluxVenteMensuel flux;
    @Autowired private Clock clock;
    @MockitoBean private SourcePrevu sourcePrevu;

    private AppelsRapports appels;
    private RequestPostProcessor libre;
    private CfgProject pa;
    private CfgProject pb;
    private CfgProject pc;
    private CfgProject pi;
    private CfgTranche ta1;
    private CfgTranche ta2;
    private CfgTranche tb;
    private CfgTranche tc;
    private CfgTranche ti;

    @BeforeEach
    void setUp() {
        when(sourcePrevu.prevu(any())).thenReturn(FluxMensuel.vide(new AnomaliesFlux(0, null, List.of(), true)));
        appels = new AppelsRapports(mockMvc);
        RapportsRythmeSupport.installer(rapports);
        Jeu jeu = fixture.jeu("C23");
        libre = connecte(rapports.compte("c23." + jeu.prefixe(), rapports.role("Libre " + jeu.prefixe(), false)));
        Statuts st = jeu.statuts();
        pa = jeu.projet("PA", true);
        pb = jeu.projet("PB", true);
        pc = jeu.projet("PC", true);
        pi = jeu.projet("PI", false);
        ta1 = jeu.tranche("TA1", pa);
        ta2 = jeu.tranche("TA2", pa);
        tb = jeu.tranche("TB", pb);
        tc = jeu.tranche("TC", pc);
        ti = jeu.tranche("TI", pi);

        ComActionMessageType visite = fx.type(InteractionKpi.TYPE_VISITE);
        ComActionMessageType appel = fx.type("APPEL-" + jeu.prefixe());
        for (LocalDate jour : List.of(AU, j(2025, 1, 10), j(2019, 3, 3), AU.plusDays(1))) {
            fx.prospection(ta1, null, visite, jour, null);
        }
        fx.prospection(ta1, null, appel, j(2025, 2, 2), null);
        fx.prospection(ta2, null, visite, j(2025, 5, 5), null);
        fx.prospection(tb, null, visite, j(2025, 3, 3), null);
        fx.prospection(tb, null, visite, j(2025, 4, 4), null);
        fx.prospection(ti, null, visite, j(2025, 3, 3), null);

        ComFolder d = jeu.dater(jeu.dossier(ta1, st.concretisation()), AU, null, null, null);
        jeu.dater(jeu.dossier(ta1, st.concretisation()), AU.plusDays(1), null, null, null);
        jeu.dater(jeu.dossier(ta1, st.concretisation()), j(2024, 3, 3), null, null, null);
        ComFolder annule = jeu.dater(jeu.dossier(ta1, st.annulation()), j(2025, 1, 1), null, null, j(2025, 2, 1));
        ComFolder sansStatut = jeu.dossier(ta1, null);
        jeu.dater(jeu.dossier(ta2, st.concretisation()), j(2025, 2, 2), null, null, null);
        ComFolder dossierB = jeu.dater(jeu.dossier(tb, st.concretisation()), j(2025, 5, 5), null, null, null);
        ComFolder dossierI = jeu.dater(jeu.dossier(ti, st.concretisation()), j(2025, 1, 1), null, null, null);

        jeu.encaisser(d, "1000", AU);
        jeu.encaisser(d, "5000", AU.plusDays(1));
        BigDecimal annuleMontant = new BigDecimal("999");
        jeu.regler(jeu.echeance(d, jeu.categorie(Genre.AUTOFINANCEMENT, true), annuleMontant, j(2025, 1, 1)),
                annuleMontant, true, CANCELED, null, j(2025, 1, 1));
        jeu.regler(jeu.echeance(annule, jeu.categorie(Genre.AUTOFINANCEMENT, true), new BigDecimal("300"),
                j(2025, 2, 2)), new BigDecimal("300"), true, IN_WORK, null, j(2025, 2, 2));
        jeu.encaisser(sansStatut, "777", j(2025, 3, 3));
        jeu.encaisser(dossierB, "190.75", j(2025, 6, 1));
        jeu.encaisser(dossierB, "0.25", j(2025, 6, 2));
        jeu.encaisser(dossierI, "3000", j(2025, 3, 3));
        fixture.relire();
    }

    private static LocalDate j(int annee, int mois, int jour) {
        return LocalDate.of(annee, mois, jour);
    }

    private MockHttpServletRequestBuilder requete(LocalDate au, String... autres) {
        String[] parametres = new String[autres.length + 2];
        parametres[0] = DATE;
        parametres[1] = au.toString();
        System.arraycopy(autres, 0, parametres, 2, autres.length);
        return demande(CONCRET_ENCAISSEMENT, libre, List.of(ta1, ta2, tb, tc, ti), parametres);
    }

    private Map<String, Object> projets(MockHttpServletRequestBuilder requete) throws Exception {
        return section(appels.sections(requete), PROJETS);
    }

    private Map<String, Object> projets() throws Exception {
        return projets(requete(AU));
    }

    private static Map<String, Object> ligneDe(Map<String, Object> s, CfgProject p) {
        return ligneContenant(s, PROJET, p.getCode());
    }

    private static MesureFlux baseConcretisationQuantite() {
        return Enum.valueOf(MesureFlux.class, "BASE_CONCRETISATION_QUANTITE");
    }

    /** Prévu de PA : concrétisations 6 (03/2025) + 4 (06/2025), 100 en 07/2025 ; encaissements 2 000 (06), 9 999 (07). */
    private void prevuDePa(int concretisationsDeMars) {
        FluxMensuel.Cumul cumul = new FluxMensuel.Cumul();
        cumul.ajouter(baseConcretisationQuantite(), new CleFlux(ta1.getId(), 2025, 3),
                BigDecimal.valueOf(concretisationsDeMars));
        cumul.ajouter(baseConcretisationQuantite(), new CleFlux(ta2.getId(), 2025, 6), new BigDecimal("4"));
        cumul.ajouter(baseConcretisationQuantite(), new CleFlux(ta1.getId(), 2025, 7), new BigDecimal("100"));
        cumul.ajouter(MesureFlux.BASE_ENCAISSEMENT, new CleFlux(ta1.getId(), 2025, 6), new BigDecimal("2000"));
        cumul.ajouter(MesureFlux.BASE_ENCAISSEMENT, new CleFlux(ta1.getId(), 2025, 7), new BigDecimal("9999"));
        when(sourcePrevu.prevu(any())).thenReturn(cumul.flux(AnomaliesFlux.AUCUNE));
    }

    // ------------------------------------------------------------------ structure (§4.6)

    @Test
    void r23_uneLigneParProjetAyantUneTrancheVisible_trieeParDescription_entiteInactiveAbsente() throws Exception {
        Map<String, Object> s = projets();

        assertThat(lignes(s)).extracting(l -> String.valueOf(l.get(PROJET)))
                .containsExactly(pa.getDescription(), pb.getDescription(), pc.getDescription());
        assertThat(lignesContenant(s, PROJET, pi.getCode())).isEmpty();
    }

    @Test
    void r23_colonnes_ordreEtTypes() throws Exception {
        Map<String, Object> s = projets();

        assertThat(noms(s)).containsExactly(PROJET, VISITES, CONCRETISATIONS, TAUX_VISITES, CONCRETISATIONS_PREVUES,
                TAUX_CONCRETISATION, ENCAISSEMENTS, ENCAISSEMENTS_PREVUS, TAUX_ENCAISSEMENT);
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(colonne(s, PROJET).get("type")).isEqualTo("TEXTE");
        for (String nom : List.of(VISITES, CONCRETISATIONS, CONCRETISATIONS_PREVUES)) {
            soft.assertThat(colonne(s, nom).get("type")).as(nom).isEqualTo("NOMBRE");
        }
        for (String nom : List.of(TAUX_VISITES, TAUX_CONCRETISATION, TAUX_ENCAISSEMENT)) {
            soft.assertThat(colonne(s, nom).get("type")).as(nom).isEqualTo("POURCENTAGE");
        }
        for (String nom : List.of(ENCAISSEMENTS, ENCAISSEMENTS_PREVUS)) {
            soft.assertThat(colonne(s, nom).get("type")).as(nom).isEqualTo("MONTANT");
        }
        soft.assertAll();
    }

    // ------------------------------------------------------------------ visites, concrétisations (§4.6)

    @Test
    void r23_visites_typeVisite_cumuleesJusquALaDate_toutesAnnees_horsAutresTypes() throws Exception {
        Map<String, Object> s = projets();

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(nombre(ligneDe(s, pa).get(VISITES))).isEqualByComparingTo("4");
        soft.assertThat(nombre(ligneDe(s, pb).get(VISITES))).isEqualByComparingTo("2");
        soft.assertThat(nombre(ligneDe(s, pc).get(VISITES))).isEqualByComparingTo("0");
        soft.assertAll();
    }

    @Test
    void r23_concretisationDuJourComptee_duLendemainNon_annuleeExclue() throws Exception {
        Map<String, Object> s = projets();

        assertThat(nombre(ligneDe(s, pa).get(CONCRETISATIONS))).isEqualByComparingTo("3");
        assertThat(nombre(ligneDe(s, pb).get(CONCRETISATIONS))).isEqualByComparingTo("1");
    }

    @Test
    void r23_concretisationDuLendemain_compteeQuandLaDateAvance() throws Exception {
        Map<String, Object> s = projets(requete(AU.plusDays(1)));

        assertThat(nombre(ligneDe(s, pa).get(CONCRETISATIONS))).isEqualByComparingTo("4");
    }

    @Test
    void r23_tauxVisites_concretisationsSurVisites_totalImpose() throws Exception {
        Map<String, Object> s = projets();

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(nombre(ligneDe(s, pa).get(TAUX_VISITES)).doubleValue()).isCloseTo(75.0, within(0.01));
        soft.assertThat(nombre(ligneDe(s, pb).get(TAUX_VISITES)).doubleValue()).isCloseTo(50.0, within(0.01));
        soft.assertThat(nombre(ligneDe(s, pc).get(TAUX_VISITES))).as("0 visite ⇒ 0").isEqualByComparingTo("0");
        soft.assertThat(nombre(totaux(s).get(TAUX_VISITES)).doubleValue()).as("4 / 6, pas 125 ni 41,67")
                .isCloseTo(66.67, within(0.01));
        soft.assertAll();
    }

    // ------------------------------------------------------------------ encaissements (Q12, M2, M5)

    @Test
    void r23_encaissements_regleDuFlux_annulesCompte_canceledEtSansStatutExclus_jusquALaDate() throws Exception {
        Map<String, Object> s = projets();

        assertThat(nombre(ligneDe(s, pa).get(ENCAISSEMENTS))).as("1 000 + 300").isEqualByComparingTo("1300");
    }

    @Test
    void m5_encaissements_centimesConserves_190virgule75Plus0virgule25() throws Exception {
        Map<String, Object> s = projets();

        assertThat(nombre(ligneDe(s, pb).get(ENCAISSEMENTS))).isEqualByComparingTo("191.00");
        assertThat(nombre(totaux(s).get(ENCAISSEMENTS))).isEqualByComparingTo("1491.00");
    }

    @Test
    void m2_dateAu31Decembre_encaissementsEgalSommeDesAnneesDuFlux() throws Exception {
        Map<String, Object> s = projets(requete(j(2025, 12, 31)));
        FluxMensuel reference = flux.calculer(new DemandeFlux(List.of(ta1.getId(), ta2.getId()), null, null,
                Set.of(MesureFlux.ENCAISSEMENTS)));
        BigDecimal attendu = BigDecimal.ZERO;
        for (int a = 2000; a <= 2025; a++) {
            attendu = attendu.add(reference.annee(MesureFlux.ENCAISSEMENTS, a));
        }

        assertThat(nombre(ligneDe(s, pa).get(ENCAISSEMENTS))).isEqualByComparingTo(attendu)
                .isEqualByComparingTo("6300");
    }

    @Test
    void m2_dateAu15Juin_seulsLesMouvementsJusquAu15() throws Exception {
        Map<String, Object> au14 = projets(requete(AU.minusDays(1)));

        assertThat(nombre(ligneDe(au14, pa).get(ENCAISSEMENTS))).as("le 1 000 du 15/06 sort")
                .isEqualByComparingTo("300");
    }

    // ------------------------------------------------------------------ prévu, coefficient (Q11, M6, B7)

    @Test
    void r23_prevuNonSaisi_prevusAZero_tauxAZero_avertissement() throws Exception {
        String corps = appels.corps(requete(AU));
        Map<String, Object> l = ligneDe(section(JsonPath.read(corps, "$.sections"), PROJETS), pa);
        List<String> avertissements = JsonPath.read(corps, "$.avertissements");

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(nombre(l.get(CONCRETISATIONS_PREVUES))).isEqualByComparingTo("0");
        soft.assertThat(nombre(l.get(TAUX_CONCRETISATION))).isEqualByComparingTo("0");
        soft.assertThat(nombre(l.get(ENCAISSEMENTS_PREVUS))).isEqualByComparingTo("0");
        soft.assertThat(nombre(l.get(TAUX_ENCAISSEMENT))).isEqualByComparingTo("0");
        soft.assertThat(avertissements).anyMatch(a -> a.contains(PREVU_NON_SAISI));
        soft.assertAll();
    }

    @Test
    void r23_prevu_coefficientParDefaut100_moisJusquAuMoisDeLaDate_taux() throws Exception {
        prevuDePa(6);

        Map<String, Object> l = ligneDe(projets(), pa);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(nombre(l.get(CONCRETISATIONS_PREVUES))).as("6 + 4, pas juillet").isEqualByComparingTo("10");
        soft.assertThat(nombre(l.get(TAUX_CONCRETISATION)).doubleValue()).isCloseTo(30.0, within(0.01));
        soft.assertThat(nombre(l.get(ENCAISSEMENTS_PREVUS))).isEqualByComparingTo("2000");
        soft.assertThat(nombre(l.get(TAUX_ENCAISSEMENT)).doubleValue()).isCloseTo(65.0, within(0.01));
        soft.assertAll();
    }

    @Test
    void r23_coefficient50_prevu10_donne5_encaissementsPrevusDeMoitie() throws Exception {
        prevuDePa(6);

        Map<String, Object> l = ligneDe(projets(requete(AU, COEFFICIENT, "50")), pa);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(nombre(l.get(CONCRETISATIONS_PREVUES))).isEqualByComparingTo("5");
        soft.assertThat(nombre(l.get(TAUX_CONCRETISATION)).doubleValue()).isCloseTo(60.0, within(0.01));
        soft.assertThat(nombre(l.get(ENCAISSEMENTS_PREVUS))).isEqualByComparingTo("1000");
        soft.assertThat(nombre(l.get(TAUX_ENCAISSEMENT)).doubleValue()).isCloseTo(130.0, within(0.01));
        soft.assertAll();
    }

    @Test
    void r23_concretisationsPrevues_arrondiEntierHalfUp() throws Exception {
        prevuDePa(1);

        Map<String, Object> l = ligneDe(projets(requete(AU, COEFFICIENT, "50")), pa);

        assertThat(nombre(l.get(CONCRETISATIONS_PREVUES))).as("(1 + 4) × 50 % = 2,5 ⇒ 3").isEqualByComparingTo("3");
    }

    @Test
    void m6_coefficient_neToucheQueLesColonnesPrevues() throws Exception {
        prevuDePa(6);

        Map<String, Object> cent = projets(requete(AU));
        Map<String, Object> cinquante = projets(requete(AU, COEFFICIENT, "50"));

        SoftAssertions soft = new SoftAssertions();
        for (CfgProject p : List.of(pa, pb, pc)) {
            for (String nom : List.of(VISITES, CONCRETISATIONS, TAUX_VISITES, ENCAISSEMENTS)) {
                soft.assertThat(nombre(ligneDe(cinquante, p).get(nom))).as("%s %s", p.getCode(), nom)
                        .isEqualByComparingTo(nombre(ligneDe(cent, p).get(nom)));
            }
        }
        soft.assertAll();
    }

    @Test
    void r23_coefficient1001_400() throws Exception {
        assertThat(appels.statut(requete(AU, COEFFICIENT, "1001"))).isEqualTo(400);
    }

    // ------------------------------------------------------------------ totaux, graphiques, paramètres

    @Test
    void r23_totaux_sommeDesNombresEtMontants_tauxImposes() throws Exception {
        prevuDePa(6);

        Map<String, Object> s = projets();

        SoftAssertions soft = new SoftAssertions();
        for (String nom : SOMMEES) {
            soft.assertThat(colonne(s, nom).get("fonctionTotal")).as(nom).isEqualTo("SOMME");
            soft.assertThat(nombre(totaux(s).get(nom))).as(nom).isEqualByComparingTo(somme(lignes(s), nom));
        }
        soft.assertThat(nombre(totaux(s).get(TAUX_CONCRETISATION)).doubleValue()).as("4 / 10")
                .isCloseTo(40.0, within(0.01));
        soft.assertThat(nombre(totaux(s).get(TAUX_ENCAISSEMENT)).doubleValue()).as("1 491 / 2 000")
                .isCloseTo(74.55, within(0.01));
        soft.assertAll();
    }

    @Test
    void r23_graphiques_deuxColonnes_categorieProjet_concretisationsEtEncaissements() throws Exception {
        String corps = appels.corps(requete(AU));

        List<String> types = JsonPath.read(corps, "$.sections[?(@.id == 'projets')].graphiques[*].type");
        List<String> categories = JsonPath.read(corps,
                "$.sections[?(@.id == 'projets')].graphiques[*].colonneCategorie");
        List<List<String>> series = JsonPath.read(corps,
                "$.sections[?(@.id == 'projets')].graphiques[*].colonnesValeurs");
        assertThat(types).containsExactly("COLONNES", "COLONNES");
        assertThat(categories).containsOnly(PROJET);
        assertThat(series).containsExactlyInAnyOrder(List.of(CONCRETISATIONS, CONCRETISATIONS_PREVUES),
                List.of(ENCAISSEMENTS, ENCAISSEMENTS_PREVUS));
    }

    @Test
    void r23_descripteur_dateAuDefautAujourdhui_coefficientEntier0A1000Defaut100_trancheId() throws Exception {
        String corps = appels.corps(get(descripteur(VENTE, CONCRET_ENCAISSEMENT)).with(libre));
        List<Map<String, Object>> date = JsonPath.read(corps, "$.parametres[?(@.nom == 'date')]");
        List<Map<String, Object>> coefficient = JsonPath.read(corps, "$.parametres[?(@.nom == 'coefficient')]");
        List<Map<String, Object>> tranches = JsonPath.read(corps, "$.parametres[?(@.nom == 'trancheId')]");

        assertThat(date).singleElement().satisfies(p -> assertThat(p).containsEntry("type", "DATE")
                .containsEntry("libelle", "Au").containsEntry("valeurParDefaut", LocalDate.now(clock).toString()));
        assertThat(coefficient).singleElement().satisfies(p -> assertThat(p).containsEntry("type", "ENTIER")
                .containsEntry("libelle", "Coefficient de prévision (%)").containsEntry("valeurParDefaut", "100")
                .containsEntry("min", 0).containsEntry("max", 1000));
        assertThat(tranches).singleElement().satisfies(p -> assertThat(p).containsEntry("type", "TRANCHES"));
    }
}
