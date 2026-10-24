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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.VENTE;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.colonnes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.descripteur;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignesOu;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.nombre;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.resultat;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.section;
import static com.tsix.immocrm.controller.rapport.JeuTransformationCommerciale.AU;
import static com.tsix.immocrm.controller.rapport.JeuTransformationCommerciale.COMMERCIAL;
import static com.tsix.immocrm.controller.rapport.JeuTransformationCommerciale.DU;
import static com.tsix.immocrm.controller.rapport.JeuTransformationCommerciale.jour;
import static com.tsix.immocrm.controller.rapport.JeuTransformationCommerciale.ligneDe;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * N° 26 TAUX DE TRANSFORMATION PAR NATURE D'ACTION (FEATURE-tdb-vente-rapports-commercial.md §6.2, §10) : même calcul
 * que le n° 20, paramètre {@code typeAction} (défaut Visite) à la place du type figé, colonne {@code actions},
 * commerciaux sans affectation sous « Non affecté ». Jeu : {@link JeuTransformationCommerciale}, plus sur T une
 * prospection Visite et un dossier CONC (avance 10/03) <b>sans commercial</b>.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TauxTransformationParNatureRapportTest {

    private static final String CODE = "ComTdbCommercialTransformationByComActionMessageType";
    private static final String PAR_COMMERCIAL = "par-commercial";
    private static final String TYPE_ACTION = "typeAction";
    private static final String ACTIONS = "actions";
    private static final String VISITES = "visites";
    private static final String TAUX = "taux";
    private static final String CONCRETISATIONS = "concretisations";
    private static final String NON_AFFECTE = "Non affecté";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports fixture;
    @Autowired private FixtureEtatVentes ev;
    @Autowired private FixtureTransformationVente fx;

    private AppelsRapports appels;
    private JeuTransformationCommerciale jeu;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        fixture.tables();
        fixture.rapportVente(CODE, "TAUX DE TRANSFORMATION PAR NATURE D'ACTION", "Suivi GLOBAL", 2060);
        fixture.rapportVente(TauxTransformationCommercialRapportTest.CODE, "TAUX DE TRANSFORMATION PAR COMMERCIAL*", "",
                2000);
        jeu = new JeuTransformationCommerciale("TTN", fixture, ev, fx);
        fx.prospection(jeu.t, null, jeu.visite, jour(3, 4), null);
        jeu.dossier(jeu.t, null, jeu.statuts.concretisation(), jour(3, 10), jour(3, 3));
        fixture.relire();
    }

    private MockHttpServletRequestBuilder requete(String code, AdmUser qui, CfgTranche... tranches) {
        MockHttpServletRequestBuilder r = get(resultat(VENTE, code)).param("du", DU.toString())
                .param("au", AU.toString()).with(connecte(qui));
        for (CfgTranche tr : tranches) {
            r.param("trancheId", tr.getId().toString());
        }
        return r;
    }

    private Map<String, Object> parCommercial(MockHttpServletRequestBuilder requete) throws Exception {
        return section(appels.sections(requete), PAR_COMMERCIAL);
    }

    /** Valeur de l'option {@code typeAction} dont le libellé vaut {@code libelle} (options lues en base, B4). */
    private String optionTypeAction(String libelle) throws Exception {
        List<String> valeurs = JsonPath.read(appels.corps(get(descripteur(VENTE, CODE)).with(connecte(jeu.direction))),
                "$.parametres[?(@.nom == 'typeAction')].options[?(@.libelle == '" + libelle + "')].valeur");
        assertThat(valeurs).as("option %s", libelle).hasSize(1);
        return valeurs.get(0);
    }

    // ------------------------------------------------------------------ un seul calcul (§6.2)

    @Test
    void r26_typeActionParDefautVisite_egalN20_surLesMemesFiltres() throws Exception {
        Map<String, Object> n26 = parCommercial(requete(CODE, jeu.direction, jeu.t));
        Map<String, Object> n20 = parCommercial(requete(TauxTransformationCommercialRapportTest.CODE, jeu.direction,
                jeu.t));

        SoftAssertions soft = new SoftAssertions();
        for (AdmUser compte : List.of(jeu.a, jeu.b, jeu.c, jeu.d)) {
            Map<String, Object> l26 = ligneDe(n26, compte);
            Map<String, Object> l20 = ligneDe(n20, compte);
            soft.assertThat(nombre(l26.get(ACTIONS))).as("%s actions", compte.getCode())
                    .isEqualByComparingTo(nombre(l20.get(VISITES)));
            for (String nom : List.of("reservations", CONCRETISATIONS, "annulationsTotal", "signesOuLivres", TAUX)) {
                soft.assertThat(nombre(l26.get(nom))).as("%s %s", compte.getCode(), nom)
                        .isEqualByComparingTo(nombre(l20.get(nom)));
            }
        }
        soft.assertAll();
    }

    @Test
    void r26_typeActionAutre_compteCeType_tauxPeutDepasser100() throws Exception {
        String appel = optionTypeAction(jeu.appel.getDescription());

        Map<String, Object> ligneA = ligneDe(parCommercial(requete(CODE, jeu.direction, jeu.t)
                .param(TYPE_ACTION, appel)), jeu.a);

        assertThat(nombre(ligneA.get(ACTIONS))).isEqualByComparingTo("1");
        assertThat(nombre(ligneA.get(TAUX)).doubleValue()).as("(2 + 2) / 1").isCloseTo(400.0, within(0.01));
    }

    @Test
    void r26_typeActionInconnu_400() throws Exception {
        mockMvc.perform(requete(CODE, jeu.direction, jeu.t).param(TYPE_ACTION, UUID.randomUUID().toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void r26_descripteur_typeActionChoix_defautVisite() throws Exception {
        String visite = optionTypeAction("Visite");

        String corps = appels.corps(get(descripteur(VENTE, CODE)).with(connecte(jeu.direction)));
        List<String> types = JsonPath.read(corps, "$.parametres[?(@.nom == 'typeAction')].type");
        List<String> defauts = JsonPath.read(corps, "$.parametres[?(@.nom == 'typeAction')].valeurParDefaut");
        assertThat(types).containsExactly("CHOIX");
        assertThat(defauts).containsExactly(visite);
    }

    @Test
    void r26_colonneActions_libelleDuTypeChoisi() throws Exception {
        Map<String, Object> section = parCommercial(requete(CODE, jeu.direction, jeu.t));

        List<Map<String, Object>> actions = colonnes(section).stream().filter(c -> ACTIONS.equals(c.get("nom")))
                .toList();
        assertThat(actions).hasSize(1);
        assertThat(actions.get(0)).containsEntry("libelle", "Visite");
    }

    // ------------------------------------------------------------------ « Non affecté » (§6.2, RV-R3b)

    @Test
    void r26_sansCommercial_ligneNonAffecte_pourExempte() throws Exception {
        List<Map<String, Object>> nonAffecte = lignesOu(parCommercial(requete(CODE, jeu.direction, jeu.t)),
                COMMERCIAL, NON_AFFECTE);

        assertThat(nonAffecte).hasSize(1);
        assertThat(nombre(nonAffecte.get(0).get(ACTIONS))).isEqualByComparingTo("1");
        assertThat(nombre(nonAffecte.get(0).get(CONCRETISATIONS))).isEqualByComparingTo("1");
    }

    @Test
    void rvR3b_nonAffecte_absentPourUnCommercial() throws Exception {
        assertThat(lignesOu(parCommercial(requete(CODE, jeu.a, jeu.t)), COMMERCIAL, NON_AFFECTE)).isEmpty();
    }

    @Test
    void rvR3b_nonAffecte_absentPourUnChef() throws Exception {
        assertThat(lignesOu(parCommercial(requete(CODE, jeu.c, jeu.t)), COMMERCIAL, NON_AFFECTE)).isEmpty();
    }

    @Test
    void rvR3b_commercial_neVoitQueSesLignes() throws Exception {
        List<Map<String, Object>> lignesA = lignes(parCommercial(requete(CODE, jeu.a, jeu.t)));

        assertThat(lignesA).isNotEmpty().allSatisfy(l -> assertThat(String.valueOf(l.get(COMMERCIAL)))
                .contains(jeu.jeton("alpha")));
    }

    @Test
    void rvR3a_trancheIdInactive_resultatVide() throws Exception {
        assertThat(lignes(parCommercial(requete(CODE, jeu.direction, jeu.ti)))).isEmpty();
    }

    // ------------------------------------------------------------------ droits

    @Test
    void droits_roleRestreintNonLie_403() throws Exception {
        AdmUser sansLien = fixture.compte(jeu.jeton("sanslien"), fixture.role("Sans lien " + jeu.prefixe, true));
        fixture.relire();

        mockMvc.perform(requete(CODE, sansLien, jeu.t)).andExpect(status().isForbidden());
    }
}
