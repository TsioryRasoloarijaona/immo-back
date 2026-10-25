package com.tsix.immocrm.controller.suiviprevisions;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureFluxVente;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Genre;
import com.tsix.immocrm.fixtures.FixtureFluxVente.Jeu;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.fixtures.FixtureSuiviPrevisions;
import com.tsix.immocrm.services.equipe.RolesEquipe;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.ANNEE;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.DETAIL;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.EXPORT;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.MOIS;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.MONTANTS;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.N18;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.PARAMETRES;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.QUANTITES;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.RACINE;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.REEL;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.SCHEMA;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.lignes;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.requete;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.section;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.somme;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.sommeDesChiffres;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.texteDuClasseur;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureFluxVente.IN_WORK;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Périmètre de l'écran et du n° 18 (FEATURE-tdb-vente-previsions.md §4.1, §9.4 R-V1…R-V3). Année 2025, mars :
 * <pre>
 *   TA : projet A (actif)          — 1 vente le 05/03 (client ALPHA), échéance 2 222 au 10/03, encaissé 1 111
 *   TB : projet B (actif)          — 1 vente le 06/03 (client BETA),  échéance 8 888 au 10/03, encaissé 7 777
 *   TI : projet I (entité INACTIVE) — 1 vente le 07/03 (client GAMMA), échéance 4 444 au 10/03, encaissé 3 333
 * </pre>
 * « a » : Commercial affecté au projet A, avec un rôle libre pour les rapports ; « direction » : Direction Générale.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SuiviPrevisionsVisibiliteTest {

    private static final String AN = "2025";
    private static final String N18_CODE = "ComTdbPrevisionState";
    private static final String CONCRETISATIONS_QUANTITE = "concretisations-quantite";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureFluxVente fixture;
    @Autowired private FixtureSuiviPrevisions sp;
    @Autowired private FixtureRapports rapports;
    @Autowired private FixtureEtatVentes ev;

    private SuiviPrevisionsSupport appels;
    private String prefixe;
    private CfgTranche ta;
    private CfgTranche tb;
    private CfgTranche ti;
    private RequestPostProcessor a;
    private RequestPostProcessor direction;

    @BeforeEach
    void setUp() {
        appels = new SuiviPrevisionsSupport(mockMvc);
        rapports.tables();
        rapports.rapportVente(N18_CODE, "SUIVI DES PREVISIONS DE VENTE", "", 1600);
        Jeu jeu = fixture.jeu("SPV");
        prefixe = jeu.prefixe();
        CfgProject projetA = jeu.projet("PA", true);
        ta = jeu.tranche("TA", projetA);
        tb = jeu.tranche("TB", jeu.projet("PB", true));
        ti = jeu.tranche("TI", jeu.projet("PI", false));

        AdmRole libre = rapports.role("Libre " + prefixe, false);
        AdmUser compteA = rapports.compte("alpha." + prefixe, libre, rapports.role(RolesEquipe.COMMERCIAL, true));
        ev.affecter(compteA, projetA);
        AdmUser dg = rapports.compte("dg." + prefixe, rapports.role("Direction Générale", false));
        a = connecte(compteA);
        direction = connecte(dg);

        vente(jeu, ta, 5, "ALPHA", "2222", "1111");
        vente(jeu, tb, 6, "BETA", "8888", "7777");
        vente(jeu, ti, 7, "GAMMA", "4444", "3333");
        rapports.relire();
    }

    private void vente(Jeu jeu, CfgTranche t, int jour, String client, String du, String encaisse) {
        ComFolder d = sp.vente(jeu, t, LocalDate.of(2025, 3, jour), client + prefixe);
        sp.regler(jeu, sp.echeance(jeu, d, Genre.AUTOFINANCEMENT, du, LocalDate.of(2025, 3, 10)), encaisse, IN_WORK,
                LocalDate.of(2025, 3, 10));
    }

    private List<Map<String, Object>> ecran(RequestPostProcessor qui, CfgTranche... tranches) throws Exception {
        return appels.sections(requete(RACINE, qui, List.of(tranches), ANNEE, AN));
    }

    private List<Map<String, Object>> detail(RequestPostProcessor qui, CfgTranche... tranches) throws Exception {
        return appels.sections(requete(DETAIL, qui, List.of(tranches), ANNEE, AN, MOIS, "3"));
    }

    private List<Map<String, Object>> n18(RequestPostProcessor qui, CfgTranche... tranches) throws Exception {
        return appels.sections(requete(N18 + "/resultat", qui, List.of(tranches), ANNEE, AN));
    }

    private static String texte(List<Map<String, Object>> sections) {
        return sections.toString();
    }

    // ------------------------------------------------------------------ R-V1 périmètre (projet A)

    @Test
    void rV1_ecran_commercialDuProjetA_sansTrancheId_seulementTA() throws Exception {
        List<Map<String, Object>> sections = ecran(a);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(somme(lignes(section(sections, QUANTITES)), REEL)).isEqualByComparingTo("1");
        soft.assertThat(somme(lignes(section(sections, MONTANTS)), REEL)).isEqualByComparingTo("1111");
        soft.assertThat(somme(lignes(section(sections, MONTANTS)), SCHEMA)).isEqualByComparingTo("2222");
        soft.assertAll();
    }

    @Test
    void rV1_detail_commercialDuProjetA_aucuneLigneNiClientDeB() throws Exception {
        List<Map<String, Object>> sections = detail(a);

        assertThat(texte(sections)).contains("ALPHA" + prefixe).doesNotContain("BETA" + prefixe)
                .doesNotContain("GAMMA" + prefixe);
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(lignes(section(sections, "dossiers"))).hasSize(1);
        soft.assertThat(somme(lignes(section(sections, "mouvements")), "montant")).isEqualByComparingTo("1111");
        soft.assertThat(somme(lignes(section(sections, "echeances")), "montant")).isEqualByComparingTo("2222");
        soft.assertAll();
    }

    @Test
    void rV1_export_commercialDuProjetA_aucunMontantDeB() throws Exception {
        MockHttpServletResponse reponse = appels.exporter(requete(EXPORT, a, List.of(), ANNEE, AN, "format", "xlsx"))
                .getResponse();

        assertThat(reponse.getStatus()).isEqualTo(200);
        assertThat(texteDuClasseur(reponse.getContentAsByteArray())).contains("1111").doesNotContain("7777")
                .doesNotContain("3333");
    }

    @Test
    void rV1_parametres_commercialDuProjetA_optionsDeTranches_seulementTA() throws Exception {
        String corps = appels.corps(requete(PARAMETRES, a, List.of()));

        List<String> options = JsonPath.read(corps, "$[?(@.nom == 'trancheId')].options[*].valeur");
        assertThat(options).contains(ta.getId().toString()).doesNotContain(tb.getId().toString(),
                ti.getId().toString());
    }

    @Test
    void rV1_n18_commercialDuProjetA_sansTrancheId_seulementTA() throws Exception {
        List<Map<String, Object>> sections = n18(a);

        assertThat(somme(lignes(section(sections, CONCRETISATIONS_QUANTITE)), REEL)).isEqualByComparingTo("1");
    }

    // ------------------------------------------------------------------ R-V2 société inactive

    @Test
    void rV2_ecran_trancheDUneSocieteInactiveDemandee_absente() throws Exception {
        List<Map<String, Object>> sections = ecran(direction, ta, tb, ti);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(somme(lignes(section(sections, QUANTITES)), REEL)).isEqualByComparingTo("2");
        soft.assertThat(somme(lignes(section(sections, MONTANTS)), REEL)).as("1 111 + 7 777")
                .isEqualByComparingTo("8888");
        soft.assertThat(somme(lignes(section(sections, MONTANTS)), SCHEMA)).as("2 222 + 8 888")
                .isEqualByComparingTo("11110");
        soft.assertAll();
    }

    @Test
    void rV2_detail_trancheDUneSocieteInactive_aucuneLigne() throws Exception {
        List<Map<String, Object>> sections = detail(direction, ta, tb, ti);

        assertThat(texte(sections)).contains("ALPHA" + prefixe, "BETA" + prefixe).doesNotContain("GAMMA" + prefixe);
        assertThat(lignes(section(sections, "dossiers"))).hasSize(2);
    }

    @Test
    void rV2_n18_trancheDUneSocieteInactive_absente() throws Exception {
        List<Map<String, Object>> sections = n18(direction, ta, tb, ti);

        assertThat(somme(lignes(section(sections, CONCRETISATIONS_QUANTITE)), REEL)).isEqualByComparingTo("2");
    }

    @Test
    void rV2_seuleTrancheDemandeeInactive_ecranA0_detailVide() throws Exception {
        assertAucunChiffre(direction, ti);
    }

    // ------------------------------------------------------------------ R-V3 trancheId hors portée

    @Test
    void rV3_trancheIdDuProjetB_pourLeCommercialDeA_200_ecranA0_detailVide_jamaisToutes() throws Exception {
        assertAucunChiffre(a, tb);
    }

    @Test
    void rV3_trancheIdDuProjetB_pourLeCommercialDeA_n18A0() throws Exception {
        List<Map<String, Object>> sections = n18(a, tb);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(sections).isNotEmpty();
        for (Map<String, Object> s : sections) {
            soft.assertThat(lignes(s)).as("%s", s.get("id")).hasSize(12);
            soft.assertThat(sommeDesChiffres(s)).as("%s", s.get("id")).isEqualByComparingTo("0");
        }
        soft.assertAll();
    }

    private void assertAucunChiffre(RequestPostProcessor qui, CfgTranche tranche) throws Exception {
        List<Map<String, Object>> ecran = ecran(qui, tranche);
        List<Map<String, Object>> detail = detail(qui, tranche);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(ecran).as("écran").hasSize(2);
        for (Map<String, Object> s : ecran) {
            soft.assertThat(lignes(s)).as("écran %s : 12 lignes de 0", s.get("id")).hasSize(12);
            soft.assertThat(sommeDesChiffres(s)).as("écran %s", s.get("id")).isEqualByComparingTo("0");
        }
        soft.assertThat(detail).as("détail").hasSize(3);
        for (Map<String, Object> s : detail) {
            soft.assertThat(lignes(s)).as("détail %s", s.get("id")).isEmpty();
        }
        soft.assertAll();
    }
}
