package com.tsix.immocrm.controller.rapport;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComAction;
import com.tsix.immocrm.entities.com.ComActionMessageType;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.fixtures.FixtureTransformationVente;
import com.tsix.immocrm.fixtures.FixtureTransformationVente.StatutsVente;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.VENTE;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.colonnes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.descripteur;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignesOu;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.nombre;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.resultat;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.section;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.totaux;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * N° 30 SUIVI DES ACTIONS COMMERCIALES (FEATURE-tdb-vente-rapports-commercial.md §9.1, §10 ; décision (6) du 22/10 :
 * budget seul + avertissement). Période mars 2025 ; codes d'action préfixés par le test (paramètre {@code prefixe}).
 * <pre>
 *   X : 10/03 → 30/04, budget 50 000 ; 4 prospections sur T (2 Visite) + 1 sur TI (entité inactive, ignorée) ;
 *       dossiers : CONC 1 000 000 (concrétisé), ANN 900 000 (exclu), RES 800 000 (sous le seuil, exclu)
 *       ⇒ visites 2, concrétisations 1, CA 1 000 000, coût/prospection 12 500, coût/concrétisation 50 000,
 *         % coût/CA concrétisé 5, taux de conversion 25
 *   Y : 10/01 → 20/03 (seule la fin dans la période), budget 20 000 ; 2 prospections sur T2, aucun dossier
 *       ⇒ ratios à dénominateur nul = 0, coût/prospection 10 000
 *   Z : dans la période, aucune prospection ⇒ absente ;  W : 2024 ⇒ absente ;  I : prospections sur TI seulement ⇒ absente
 *   Totaux imposés : prospections 6, budget 70 000 ⇒ coût/prospection 11 666,67, taux 16,67, % coût/CA concrétisé 7
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SuiviActionsCommercialesRapportTest {

    private static final String CODE = "ComSaleDashboardSuiviActionCommerciale";
    private static final LocalDate DU = LocalDate.of(2025, 3, 1);
    private static final LocalDate AU = LocalDate.of(2025, 3, 31);
    private static final String ACTION = "action";
    private static final String PREFIXE = "prefixe";
    private static final String VISITES = "visites";
    private static final String CONCRETISATIONS = "concretisations";
    private static final String CA_CONCRETISE = "caConcretise";
    private static final String PROSPECTIONS = "prospections";
    private static final String BUDGET = "budgetInitial";
    private static final String COUT_PROSPECTION = "coutParProspection";
    private static final String COUT_CONCRETISATION = "coutParConcretisation";
    private static final String PCT_CA_CONCRETISE = "pctCoutCaConcretise";
    private static final String PCT_CA_GLOBAL = "pctCoutCaGlobal";
    private static final String TAUX_CONVERSION = "tauxConversion";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports fixture;
    @Autowired private FixtureEtatVentes ev;
    @Autowired private FixtureTransformationVente fx;
    @Autowired private Clock clock;

    private AppelsRapports appels;
    private String prefixe;
    private CfgTranche t2;
    private AdmUser direction;
    private int numero;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        prefixe = FixtureEtatVentes.prefixe("SAC");
        fixture.tables();
        fixture.rapportVente(CODE, "SUIVI DES ACTIONS COMMERCIALES*", "VENTE", 2090);
        direction = fixture.compte("dg." + prefixe, fixture.role("Direction Générale", false));
        CfgTranche t = ev.tranche("T-" + prefixe, ev.projet("P-" + prefixe, true), true);
        t2 = ev.tranche("T2-" + prefixe, ev.projet("P2-" + prefixe, true), true);
        CfgTranche ti = ev.tranche("TI-" + prefixe, ev.projet("PI-" + prefixe, false), true);
        StatutsVente statuts = fx.statutsVente(prefixe);
        ComActionMessageType visite = fx.type("Visite");

        ComAction x = fx.action(code("X"), LocalDate.of(2025, 3, 10), LocalDate.of(2025, 4, 30),
                new BigDecimal("50000"));
        dossier(prospection(t, visite, x), statuts.concretisation(), "1000000");
        dossier(prospection(t, visite, x), statuts.annule(), "900000");
        dossier(prospection(t, null, x), statuts.reservation(), "800000");
        prospection(t, null, x);
        prospection(ti, visite, x);

        ComAction y = fx.action(code("Y"), LocalDate.of(2025, 1, 10), LocalDate.of(2025, 3, 20),
                new BigDecimal("20000"));
        prospection(t2, null, y);
        prospection(t2, null, y);

        fx.action(code("Z"), LocalDate.of(2025, 3, 5), LocalDate.of(2025, 3, 6), new BigDecimal("1000"));
        prospection(t, visite, fx.action(code("W"), LocalDate.of(2024, 3, 5), LocalDate.of(2024, 3, 6),
                new BigDecimal("1000")));
        prospection(ti, visite, fx.action(code("I"), LocalDate.of(2025, 3, 5), LocalDate.of(2025, 3, 6),
                new BigDecimal("1000")));
        fixture.relire();
    }

    private String code(String lettre) {
        return prefixe + "-" + lettre;
    }

    private ComProspection prospection(CfgTranche tranche, ComActionMessageType type, ComAction action) {
        return fx.prospection(tranche, null, type, LocalDate.of(2025, 3, 15), action);
    }

    private void dossier(ComProspection prospection, StrStatus statut, String prix) {
        numero++;
        ev.prix(fx.dossier("DSA-" + prefixe + "-" + numero, prospection.getStkHierarchy(), null, statut, prospection),
                new BigDecimal(prix));
    }

    private MockHttpServletRequestBuilder requete(String prefixeAction, CfgTranche... tranches) {
        MockHttpServletRequestBuilder r = get(resultat(VENTE, CODE)).param("du", DU.toString())
                .param("au", AU.toString()).param(PREFIXE, prefixeAction).with(connecte(direction));
        for (CfgTranche tr : tranches) {
            r.param("trancheId", tr.getId().toString());
        }
        return r;
    }

    private Map<String, Object> actions(MockHttpServletRequestBuilder requete) throws Exception {
        List<Map<String, Object>> sections = appels.sections(requete);
        assertThat(sections).as("une section").hasSize(1);
        return sections.get(0);
    }

    private static List<Object> codes(Map<String, Object> section) {
        return lignes(section).stream().map(l -> l.get(ACTION)).toList();
    }

    private Map<String, Object> ligne(Map<String, Object> section, String lettre) {
        List<Map<String, Object>> trouvees = lignesOu(section, ACTION, code(lettre));
        assertThat(trouvees).as("ligne %s", lettre).hasSize(1);
        return trouvees.get(0);
    }

    // ------------------------------------------------------------------ population (§9.1)

    @Test
    void r30_actionsDeLaPeriode_debutOuFin_avecProspectionVisible() throws Exception {
        assertThat(codes(actions(requete(prefixe)))).containsExactlyInAnyOrder(code("X"), code("Y"));
    }

    @Test
    void r30_seuleLaFinDansLaPeriode_incluse() throws Exception {
        assertThat(codes(actions(requete(prefixe)))).contains(code("Y"));
    }

    @Test
    void r30_actionSansProspection_absente() throws Exception {
        assertThat(codes(actions(requete(prefixe)))).isNotEmpty().doesNotContain(code("Z"));
    }

    @Test
    void r30_prefixe_codeCommencantPar() throws Exception {
        assertThat(codes(actions(requete(code("X"))))).containsExactly(code("X"));
    }

    // ------------------------------------------------------------------ colonnes et ratios (§9.1, RF-R3)

    @Test
    void r30_nominal_effectifsEtMontants() throws Exception {
        Map<String, Object> x = ligne(actions(requete(prefixe)), "X");

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(nombre(x.get(PROSPECTIONS))).as("prospections hors TI").isEqualByComparingTo("4");
        soft.assertThat(nombre(x.get(VISITES))).as(VISITES).isEqualByComparingTo("2");
        soft.assertThat(nombre(x.get(CONCRETISATIONS))).as("CONC seul : annulé et réservation exclus")
                .isEqualByComparingTo("1");
        soft.assertThat(nombre(x.get(CA_CONCRETISE))).as(CA_CONCRETISE).isEqualByComparingTo("1000000");
        soft.assertThat(nombre(x.get(BUDGET))).as("ComAction.Budget").isEqualByComparingTo("50000");
        soft.assertAll();
    }

    @Test
    void r30_ratios_coutsEtPourcentages0a100() throws Exception {
        Map<String, Object> x = ligne(actions(requete(prefixe)), "X");

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(nombre(x.get(COUT_PROSPECTION)).doubleValue()).as("50 000 / 4")
                .isCloseTo(12500.0, within(0.01));
        soft.assertThat(nombre(x.get(COUT_CONCRETISATION)).doubleValue()).as("50 000 / 1")
                .isCloseTo(50000.0, within(0.01));
        soft.assertThat(nombre(x.get(PCT_CA_CONCRETISE)).doubleValue()).as("50 000 / 1 000 000 × 100")
                .isCloseTo(5.0, within(0.01));
        soft.assertThat(nombre(x.get(TAUX_CONVERSION)).doubleValue()).as("1 / 4 × 100")
                .isCloseTo(25.0, within(0.01));
        soft.assertAll();
    }

    @Test
    void rfR3_denominateurNul_ratiosAZero() throws Exception {
        Map<String, Object> y = ligne(actions(requete(prefixe)), "Y");

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(nombre(y.get(CONCRETISATIONS))).isEqualByComparingTo("0");
        soft.assertThat(nombre(y.get(COUT_CONCRETISATION))).as(COUT_CONCRETISATION).isEqualByComparingTo("0");
        soft.assertThat(nombre(y.get(PCT_CA_CONCRETISE))).as(PCT_CA_CONCRETISE).isEqualByComparingTo("0");
        soft.assertThat(nombre(y.get(TAUX_CONVERSION))).as(TAUX_CONVERSION).isEqualByComparingTo("0");
        soft.assertThat(nombre(y.get(COUT_PROSPECTION)).doubleValue()).as("20 000 / 2")
                .isCloseTo(10000.0, within(0.01));
        soft.assertAll();
    }

    /** Même dénominateur pour toutes les lignes : pctCoutCaGlobal / budget constant. */
    @Test
    void r30_caGlobal_communATouteLesLignes() throws Exception {
        Map<String, Object> s = actions(requete(prefixe));
        Map<String, Object> x = ligne(s, "X");
        Map<String, Object> y = ligne(s, "Y");

        BigDecimal rapportX = nombre(x.get(PCT_CA_GLOBAL)).divide(nombre(x.get(BUDGET)), 12, RoundingMode.HALF_UP);
        BigDecimal rapportY = nombre(y.get(PCT_CA_GLOBAL)).divide(nombre(y.get(BUDGET)), 12, RoundingMode.HALF_UP);
        assertThat(nombre(x.get(PCT_CA_GLOBAL))).isPositive();
        assertThat(rapportY.doubleValue()).isCloseTo(rapportX.doubleValue(), within(rapportX.doubleValue() * 0.001));
    }

    @Test
    void rfR3_totaux_sommesEtRatiosImposes() throws Exception {
        Map<String, Object> totaux = totaux(actions(requete(prefixe)));

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(nombre(totaux.get(PROSPECTIONS))).as(PROSPECTIONS).isEqualByComparingTo("6");
        soft.assertThat(nombre(totaux.get(BUDGET))).as(BUDGET).isEqualByComparingTo("70000");
        soft.assertThat(nombre(totaux.get(CA_CONCRETISE))).as(CA_CONCRETISE).isEqualByComparingTo("1000000");
        soft.assertThat(nombre(totaux.get(COUT_PROSPECTION)).doubleValue()).as("70 000 / 6")
                .isCloseTo(11666.67, within(0.01));
        soft.assertThat(nombre(totaux.get(TAUX_CONVERSION)).doubleValue()).as("1 / 6 × 100")
                .isCloseTo(16.67, within(0.01));
        soft.assertThat(nombre(totaux.get(PCT_CA_CONCRETISE)).doubleValue()).as("70 000 / 1 000 000 × 100")
                .isCloseTo(7.0, within(0.01));
        soft.assertAll();
    }

    // ------------------------------------------------------------------ décision (6) : budget seul + avertissement

    @Test
    void decision6_montantsDAchatAbsents_avertissement() throws Exception {
        String corps = appels.corps(requete(prefixe));

        List<Map<String, Object>> sections = JsonPath.read(corps, "$.sections");
        List<String> avertissements = JsonPath.read(corps, "$.avertissements");
        assertThat(colonnes(sections.get(0))).extracting(c -> c.get("nom"))
                .isNotEmpty().doesNotContain("montantFacture", "montantRegle");
        assertThat(avertissements).anySatisfy(a -> assertThat(a).containsIgnoringCase("commandes d'achat"));
    }

    // ------------------------------------------------------------------ paramètres (§9.1)

    @Test
    void descripteur_duPremierJanvier_au31Decembre_prefixeTexte() throws Exception {
        LocalDate aujourdhui = LocalDate.now(clock);

        mockMvc.perform(get(descripteur(VENTE, CODE)).with(connecte(direction)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parametres[?(@.nom == 'du')].valeurParDefaut")
                        .value(aujourdhui.withDayOfYear(1).toString()))
                .andExpect(jsonPath("$.parametres[?(@.nom == 'au')].valeurParDefaut")
                        .value(LocalDate.of(aujourdhui.getYear(), 12, 31).toString()))
                .andExpect(jsonPath("$.parametres[?(@.nom == 'trancheId')].type").value("TRANCHES"))
                .andExpect(jsonPath("$.parametres[?(@.nom == 'prefixe')].type").value("TEXTE"));
    }

    // ------------------------------------------------------------------ visibilité (RV-R3a)

    @Test
    void rvR3a_prospectionsDUneTrancheInactive_ignorees() throws Exception {
        Map<String, Object> s = actions(requete(prefixe));

        assertThat(codes(s)).isNotEmpty().doesNotContain(code("I"));
        assertThat(nombre(ligne(s, "X").get(VISITES))).isEqualByComparingTo("2");
    }

    @Test
    void rvR3a_trancheId_seulesLesProspectionsDeCetteTranche() throws Exception {
        assertThat(codes(actions(requete(prefixe, t2)))).containsExactly(code("Y"));
    }

    @Test
    void rvR3a_trancheIdInactive_resultatVide() throws Exception {
        CfgTranche ti = ev.tranche("TJ-" + prefixe, ev.projet("PJ-" + prefixe, false), true);
        fixture.relire();

        assertThat(lignes(actions(requete(prefixe, ti)))).isEmpty();
    }

    // ------------------------------------------------------------------ droits

    @Test
    void droits_roleRestreintNonLie_403() throws Exception {
        AdmUser sansLien = fixture.compte("sanslien." + prefixe, fixture.role("Sans lien " + prefixe, true));
        fixture.relire();

        mockMvc.perform(get(resultat(VENTE, CODE)).with(connecte(sansLien))).andExpect(status().isForbidden());
    }
}
