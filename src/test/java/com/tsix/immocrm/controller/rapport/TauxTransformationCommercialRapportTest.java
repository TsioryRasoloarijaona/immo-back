package com.tsix.immocrm.controller.rapport;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.fixtures.FixtureTransformationVente;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.VENTE;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.colonnesAvecTotal;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.descripteur;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.nombre;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.resultat;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.section;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.somme;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.totaux;
import static com.tsix.immocrm.controller.rapport.JeuTransformationCommerciale.AU;
import static com.tsix.immocrm.controller.rapport.JeuTransformationCommerciale.COMMERCIAL;
import static com.tsix.immocrm.controller.rapport.JeuTransformationCommerciale.DU;
import static com.tsix.immocrm.controller.rapport.JeuTransformationCommerciale.ligneDe;
import static com.tsix.immocrm.controller.rapport.JeuTransformationCommerciale.lignesDe;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * N° 20 TAUX DE TRANSFORMATION PAR COMMERCIAL (FEATURE-tdb-vente-rapports-commercial.md §6.1, §2, §10 ; décisions du
 * 22/10 : filtre par {@code commerciauxVisibles()} / {@code tranchesVisibles()}, taux = (E + G) / visites × 100).
 * Jeu : {@link JeuTransformationCommerciale}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TauxTransformationCommercialRapportTest {

    static final String CODE = "ComTdbCommercialTransformation";
    private static final String DETAIL = "detail";
    private static final String PAR_COMMERCIAL = "par-commercial";
    private static final String TRANCHE_ID = "trancheId";
    private static final String PROSPECTIONS = "prospections";
    private static final String VISITES = "visites";
    private static final String RESERVATIONS = "reservations";
    private static final String CONCRETISATIONS = "concretisations";
    private static final String CONCRETISATIONS_TOTAL = "concretisationsTotal";
    private static final String ANNULATIONS_TOTAL = "annulationsTotal";
    private static final String SIGNES_OU_LIVRES = "signesOuLivres";
    private static final String TAUX = "taux";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports fixture;
    @Autowired private FixtureEtatVentes ev;
    @Autowired private FixtureTransformationVente fx;
    @Autowired private Clock clock;

    private AppelsRapports appels;
    private JeuTransformationCommerciale jeu;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        fixture.tables();
        fixture.rapportVente(CODE, "TAUX DE TRANSFORMATION PAR COMMERCIAL*", "", 2000);
        jeu = new JeuTransformationCommerciale("TTC", fixture, ev, fx);
    }

    private MockHttpServletRequestBuilder requete(AdmUser qui, CfgTranche... tranches) {
        MockHttpServletRequestBuilder r = get(resultat(VENTE, CODE)).param("du", DU.toString())
                .param("au", AU.toString()).with(connecte(qui));
        for (CfgTranche tr : tranches) {
            r.param(TRANCHE_ID, tr.getId().toString());
        }
        return r;
    }

    private Map<String, Object> parCommercialDirection() throws Exception {
        return section(appels.sections(requete(jeu.direction, jeu.t)), PAR_COMMERCIAL);
    }

    private Map<String, Object> ligneParCommercialDeA() throws Exception {
        return ligneDe(parCommercialDirection(), jeu.a);
    }

    // ------------------------------------------------------------------ définitions des colonnes (§6.1)

    @Test
    void r20_visites_prospectionsDeTypeVisiteDansLaPeriode_seulement() throws Exception {
        assertThat(nombre(ligneParCommercialDeA().get(VISITES))).isEqualByComparingTo("5");
    }

    @Test
    void r20_reservations_dossierSansStatutCompte_annuleExclu_saisieDansLaPeriode() throws Exception {
        assertThat(nombre(ligneParCommercialDeA().get(RESERVATIONS))).isEqualByComparingTo("4");
    }

    @Test
    void r20_concretisations_E_avanceDansLaPeriode_statutEntreConcretisationEtFinalisation() throws Exception {
        assertThat(nombre(ligneParCommercialDeA().get(CONCRETISATIONS))).isEqualByComparingTo("2");
    }

    @Test
    void r20_signesOuLivres_G_statutFinalisationOuPlus_annuleExclu() throws Exception {
        assertThat(nombre(ligneParCommercialDeA().get(SIGNES_OU_LIVRES))).isEqualByComparingTo("2");
    }

    @Test
    void r20_annulationsTotal_avecAvance_sansPeriode_annuleOuDesiste() throws Exception {
        assertThat(nombre(ligneParCommercialDeA().get(ANNULATIONS_TOTAL))).isEqualByComparingTo("2");
    }

    @Test
    void r20_detail_prospectionsEtConcretisationsTotal_horsPeriodeCompte() throws Exception {
        Map<String, Object> detail = section(appels.sections(requete(jeu.direction, jeu.t)), DETAIL);

        Map<String, Object> ligneA = ligneDe(detail, jeu.a);
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(nombre(ligneA.get(PROSPECTIONS))).as(PROSPECTIONS).isEqualByComparingTo("6");
        soft.assertThat(nombre(ligneA.get(CONCRETISATIONS_TOTAL))).as(CONCRETISATIONS_TOTAL).isEqualByComparingTo("3");
        soft.assertThat(nombre(ligneA.get(VISITES))).as(VISITES).isEqualByComparingTo("5");
        soft.assertThat(nombre(ligneA.get(RESERVATIONS))).as(RESERVATIONS).isEqualByComparingTo("4");
        soft.assertThat(nombre(ligneA.get(CONCRETISATIONS))).as(CONCRETISATIONS).isEqualByComparingTo("2");
        soft.assertThat(nombre(ligneA.get(ANNULATIONS_TOTAL))).as(ANNULATIONS_TOTAL).isEqualByComparingTo("2");
        soft.assertThat(nombre(ligneA.get(SIGNES_OU_LIVRES))).as(SIGNES_OU_LIVRES).isEqualByComparingTo("2");
        soft.assertAll();
    }

    // ------------------------------------------------------------------ taux (RF-R3, B1)

    @Test
    void rfR3_taux_ePlusGSurVisites_pourcentage0a100() throws Exception {
        Map<String, Object> parCommercial = parCommercialDirection();

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(nombre(ligneDe(parCommercial, jeu.a).get(TAUX)).doubleValue()).as("A (2 + 2) / 5")
                .isCloseTo(80.0, within(0.01));
        soft.assertThat(nombre(ligneDe(parCommercial, jeu.b).get(TAUX)).doubleValue()).as("B 1 / 1")
                .isCloseTo(100.0, within(0.01));
        soft.assertThat(nombre(ligneDe(parCommercial, jeu.c).get(TAUX)).doubleValue()).as("C 0 / 3")
                .isCloseTo(0.0, within(0.01));
        soft.assertAll();
    }

    @Test
    void rfR3_taux_visitesNulles_zero() throws Exception {
        Map<String, Object> ligneD = ligneDe(parCommercialDirection(), jeu.d);

        assertThat(nombre(ligneD.get(VISITES))).isEqualByComparingTo("0");
        assertThat(nombre(ligneD.get(TAUX))).isEqualByComparingTo("0");
    }

    @Test
    void rfR3_taux_detail_memeFormule_pourcentage() throws Exception {
        Map<String, Object> detail = section(appels.sections(requete(jeu.direction, jeu.t)), DETAIL);

        assertThat(nombre(ligneDe(detail, jeu.a).get(TAUX)).doubleValue()).isCloseTo(80.0, within(0.01));
    }

    /** Totaux imposés : 6 / 9 = 66,67 % ; ni la somme (180) ni la moyenne (45) des taux des lignes. */
    @Test
    void rfR3_totaux_tauxImpose_sommeNumerateurSurSommeVisites() throws Exception {
        Map<String, Object> parCommercial = parCommercialDirection();

        assertThat(nombre(totaux(parCommercial).get(VISITES))).isEqualByComparingTo("9");
        assertThat(nombre(totaux(parCommercial).get(TAUX)).doubleValue()).isCloseTo(66.67, within(0.01));
    }

    @Test
    void rfR3_totaux_detail_tauxImpose() throws Exception {
        Map<String, Object> detail = section(appels.sections(requete(jeu.direction, jeu.t)), DETAIL);

        assertThat(nombre(totaux(detail).get(TAUX)).doubleValue()).isCloseTo(66.67, within(0.01));
    }

    @Test
    void totaux_effectifsSomme_egalSommeDesLignes_deuxSections() throws Exception {
        List<Map<String, Object>> sections = appels.sections(requete(jeu.direction, jeu.t));

        SoftAssertions soft = new SoftAssertions();
        for (String id : List.of(DETAIL, PAR_COMMERCIAL)) {
            Map<String, Object> s = section(sections, id);
            List<String> sommes = colonnesAvecTotal(s, "SOMME");
            soft.assertThat(sommes).as("colonnes SOMME de %s", id).contains(VISITES, RESERVATIONS, CONCRETISATIONS,
                    ANNULATIONS_TOTAL, SIGNES_OU_LIVRES);
            for (String nom : sommes) {
                soft.assertThat(nombre(totaux(s).get(nom))).as("%s.%s", id, nom)
                        .isEqualByComparingTo(somme(lignes(s), nom));
            }
        }
        soft.assertAll();
    }

    @Test
    void detail_uneLigneParCommercialEtProjet_parCommercialAdditionne() throws Exception {
        jeu.prospections(jeu.t2, jeu.a, jeu.visite, 1);
        fixture.relire();

        List<Map<String, Object>> sections = appels.sections(requete(jeu.direction, jeu.t, jeu.t2));

        assertThat(lignesDe(section(sections, DETAIL), jeu.a)).hasSize(2);
        assertThat(nombre(ligneDe(section(sections, PAR_COMMERCIAL), jeu.a).get(VISITES))).isEqualByComparingTo("6");
    }

    // ------------------------------------------------------------------ graphiques (§6.1)

    @Test
    void graphiques_deuxColonnes_tauxSeul_etCinqEffectifs_parCommercial() throws Exception {
        String corps = appels.corps(requete(jeu.direction, jeu.t));

        List<String> types = JsonPath.read(corps, "$.sections[?(@.id == 'par-commercial')].graphiques[*].type");
        List<String> categories = JsonPath.read(corps,
                "$.sections[?(@.id == 'par-commercial')].graphiques[*].colonneCategorie");
        List<List<String>> series = JsonPath.read(corps,
                "$.sections[?(@.id == 'par-commercial')].graphiques[*].colonnesValeurs");
        assertThat(types).containsExactly("COLONNES", "COLONNES");
        assertThat(categories).containsOnly(COMMERCIAL);
        assertThat(series.stream().map(Set::copyOf).toList()).containsExactlyInAnyOrder(Set.of(TAUX),
                Set.of(VISITES, RESERVATIONS, CONCRETISATIONS, ANNULATIONS_TOTAL, SIGNES_OU_LIVRES));
    }

    // ------------------------------------------------------------------ paramètres (§6.1)

    @Test
    void descripteur_duPremierDuMois_auAujourdhui_trancheIdMultiple() throws Exception {
        LocalDate aujourdhui = LocalDate.now(clock);

        mockMvc.perform(get(descripteur(VENTE, CODE)).with(connecte(jeu.direction)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parametres[?(@.nom == 'du')].type").value("DATE"))
                .andExpect(jsonPath("$.parametres[?(@.nom == 'du')].valeurParDefaut")
                        .value(aujourdhui.withDayOfMonth(1).toString()))
                .andExpect(jsonPath("$.parametres[?(@.nom == 'au')].valeurParDefaut").value(aujourdhui.toString()))
                .andExpect(jsonPath("$.parametres[?(@.nom == 'trancheId')].type").value("TRANCHES"))
                .andExpect(jsonPath("$.parametres[?(@.nom == 'trancheId')].multiple").value(true));
    }

    // ------------------------------------------------------------------ visibilité (RV-R3a, RV-R3b, règle 6)

    @Test
    void rvR3b_commercial_neVoitQueSesLignes() throws Exception {
        List<Map<String, Object>> sections = appels.sections(requete(jeu.a));

        List<Map<String, Object>> toutes = new ArrayList<>(lignes(section(sections, PAR_COMMERCIAL)));
        toutes.addAll(lignes(section(sections, DETAIL)));
        assertThat(toutes).isNotEmpty().allSatisfy(l -> assertThat(String.valueOf(l.get(COMMERCIAL)))
                .contains(jeu.jeton("alpha")));
    }

    @Test
    void rvR3b_commercial_chiffresDeSaLigneInchanges() throws Exception {
        Map<String, Object> ligneA = ligneDe(section(appels.sections(requete(jeu.a)), PAR_COMMERCIAL), jeu.a);

        assertThat(nombre(ligneA.get(VISITES))).isEqualByComparingTo("5");
        assertThat(nombre(ligneA.get(TAUX)).doubleValue()).isCloseTo(80.0, within(0.01));
    }

    @Test
    void rvR3b_chef_voitLuiEtSonEquipe_pasLesAutres() throws Exception {
        Map<String, Object> parCommercial = section(appels.sections(requete(jeu.c, jeu.t)), PAR_COMMERCIAL);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(lignesDe(parCommercial, jeu.c)).as("chef").hasSize(1);
        soft.assertThat(lignesDe(parCommercial, jeu.a)).as("membre A").hasSize(1);
        soft.assertThat(lignesDe(parCommercial, jeu.d)).as("membre D").hasSize(1);
        soft.assertThat(lignesDe(parCommercial, jeu.b)).as("B hors équipe").isEmpty();
        soft.assertAll();
    }

    @Test
    void rvR3b_exempte_voitTousLesCommerciaux() throws Exception {
        Map<String, Object> parCommercial = parCommercialDirection();

        SoftAssertions soft = new SoftAssertions();
        for (AdmUser compte : List.of(jeu.a, jeu.b, jeu.c, jeu.d)) {
            soft.assertThat(lignesDe(parCommercial, compte)).as(compte.getCode()).hasSize(1);
        }
        soft.assertAll();
    }

    @Test
    void rvR3a_trancheDUneEntiteInactive_aucunChiffre() throws Exception {
        Map<String, Object> parCommercial = section(appels.sections(requete(jeu.direction)), PAR_COMMERCIAL);

        Map<String, Object> ligneA = ligneDe(parCommercial, jeu.a);
        assertThat(nombre(ligneA.get(VISITES))).isEqualByComparingTo("5");
        assertThat(nombre(ligneA.get(CONCRETISATIONS))).isEqualByComparingTo("2");
    }

    @Test
    void rvR3a_trancheIdInactive_resultatVide_jamaisToutes() throws Exception {
        List<Map<String, Object>> sections = appels.sections(requete(jeu.direction, jeu.ti));

        assertThat(sections).isNotEmpty();
        assertThat(lignes(section(sections, PAR_COMMERCIAL))).isEmpty();
        assertThat(lignes(section(sections, DETAIL))).isEmpty();
    }

    @Test
    void rvR3a_trancheIdHorsPerimetreDuCommercial_resultatVide() throws Exception {
        List<Map<String, Object>> sections = appels.sections(requete(jeu.a, jeu.t2));

        assertThat(lignes(section(sections, PAR_COMMERCIAL))).isEmpty();
        assertThat(lignes(section(sections, DETAIL))).isEmpty();
    }

    // ------------------------------------------------------------------ droits (transversal §10)

    @Test
    void droits_roleRestreintNonLie_403() throws Exception {
        AdmUser sansLien = fixture.compte(jeu.jeton("sanslien"), fixture.role("Sans lien " + jeu.prefixe, true));
        fixture.relire();

        mockMvc.perform(requete(sansLien, jeu.t)).andExpect(status().isForbidden());
    }
}
