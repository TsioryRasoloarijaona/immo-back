package com.tsix.immocrm.controller.suiviprevisions;

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
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.FRAIS;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.MOIS;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.MONTANTS;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.RACINE;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.REEL;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.SCHEMA;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.lignes;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.requete;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.section;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.somme;
import static com.tsix.immocrm.controller.suiviprevisions.SuiviPrevisionsSupport.texteDuClasseur;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureFluxVente.IN_WORK;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Seconde revue Pa (FEATURE-tdb-vente-previsions.md §4.1, R-V1/R-V3) : export et détail du commercial du projet A quand
 * la requête mêle une tranche visible et une tranche hors portée, ou ne porte qu'une tranche hors portée, avec frais.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SuiviPrevisionsSecondeRevueTest {

    private static final String AN = "2025";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureFluxVente fixture;
    @Autowired private FixtureSuiviPrevisions sp;
    @Autowired private FixtureRapports rapports;
    @Autowired private FixtureEtatVentes ev;

    private SuiviPrevisionsSupport appels;
    private String prefixe;
    private CfgTranche ta;
    private CfgTranche tb;
    private RequestPostProcessor a;

    @BeforeEach
    void setUp() {
        appels = new SuiviPrevisionsSupport(mockMvc);
        Jeu jeu = fixture.jeu("SPS");
        prefixe = jeu.prefixe();
        CfgProject projetA = jeu.projet("PA", true);
        ta = jeu.tranche("TA", projetA);
        tb = jeu.tranche("TB", jeu.projet("PB", true));

        AdmRole libre = rapports.role("Libre " + prefixe, false);
        AdmUser compteA = rapports.compte("alpha." + prefixe, libre, rapports.role(RolesEquipe.COMMERCIAL, true));
        ev.affecter(compteA, projetA);
        a = connecte(compteA);

        vente(jeu, ta, 5, "ALPHA", "2222", "1111", "61");
        vente(jeu, tb, 6, "BETA", "8888", "7777", "93");
        fixture.relire();
    }

    private void vente(Jeu jeu, CfgTranche t, int jour, String client, String du, String encaisse, String frais) {
        ComFolder d = sp.vente(jeu, t, LocalDate.of(2025, 3, jour), client + prefixe);
        sp.regler(jeu, sp.echeance(jeu, d, Genre.AUTOFINANCEMENT, du, LocalDate.of(2025, 3, 10)), encaisse, IN_WORK,
                LocalDate.of(2025, 3, 10));
        sp.regler(jeu, sp.echeance(jeu, d, Genre.FRAIS, frais, LocalDate.of(2025, 3, 11)), frais, IN_WORK,
                LocalDate.of(2025, 3, 11));
    }

    @Test
    void ecran_trancheVisibleEtTrancheHorsPortee_avecFrais_seulementTA() throws Exception {
        Map<String, Object> montants = section(appels.sections(requete(RACINE, a, List.of(ta, tb), ANNEE, AN,
                FRAIS, "AVEC")), MONTANTS);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(somme(lignes(montants), REEL)).as("1 111 + 61").isEqualByComparingTo("1172");
        soft.assertThat(somme(lignes(montants), SCHEMA)).as("2 222 + 61").isEqualByComparingTo("2283");
        soft.assertAll();
    }

    @Test
    void detail_trancheVisibleEtTrancheHorsPortee_seulsFrais_aucunClientNiMontantDeB() throws Exception {
        List<Map<String, Object>> detail = appels.sections(requete(DETAIL, a, List.of(ta, tb), ANNEE, AN,
                FRAIS, "SEULS", MOIS, "3"));

        assertThat(detail.toString()).contains("ALPHA" + prefixe).doesNotContain("BETA" + prefixe);
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(somme(lignes(section(detail, "mouvements")), "montant")).isEqualByComparingTo("61");
        soft.assertThat(somme(lignes(section(detail, "echeances")), "montant")).isEqualByComparingTo("61");
        soft.assertThat(lignes(section(detail, "dossiers"))).hasSize(1);
        soft.assertAll();
    }

    @Test
    void export_seuleTrancheHorsPortee_200_aucunMontantDeB() throws Exception {
        MockHttpServletResponse reponse = appels.exporter(requete(EXPORT, a, List.of(tb), ANNEE, AN, FRAIS, "AVEC",
                "format", "xlsx")).getResponse();

        assertThat(reponse.getStatus()).isEqualTo(200);
        assertThat(texteDuClasseur(reponse.getContentAsByteArray())).doesNotContain("7777", "8888", "7870", "8981",
                "1111", "2222");
    }
}
