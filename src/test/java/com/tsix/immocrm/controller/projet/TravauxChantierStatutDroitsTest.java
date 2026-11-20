package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsTravauxChantier.Reponse;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import com.tsix.immocrm.fixtures.FixtureTravauxChantier;
import com.tsix.immocrm.fixtures.FixtureTravauxChantier.Chantier;
import com.tsix.immocrm.fixtures.FixtureTravauxChantier.Compte;
import com.tsix.immocrm.fixtures.FixtureTravauxChantier.Jour;
import com.tsix.immocrm.fixtures.FixtureTravauxChantier.ValeursType;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.DEMANDE_VALIDATION;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.DROIT_LIRE;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.DROIT_MODIFIER;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.DROIT_SUPPRIMER;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.EN_COURS;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.TOUS_LES_STATUTS;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.TOUTES_LES_TRANSITIONS;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.T_ANNULATION;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.T_DEMANDE;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.T_REJET;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.T_VALIDATION;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.VALIDEE;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.majuscules;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * N2b R-N2b-1 et R-N2b-2 (FEATURE-n2-travaux-chantier.md) : seul droit = lecture {@code Yes.Prj.Front.Bul.GrhWorkedDayView}
 * (404 d'abord, puis 403) ; transitions proposées = {@code MoteurWorkflowStr.permises} (libre : toute l'entité sauf vers le
 * statut courant ; restreint : intervenants {@code WithPassword = 0} seulement).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TravauxChantierStatutDroitsTest {

    private static final LocalDate DATE = LocalDate.of(2077, 3, 10);
    private static final String IDS = "$[*].id";

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper json;
    @Autowired private FixtureTravauxChantier fixture;

    private AppelsTravauxChantier appels;
    private String jeton;
    private Chantier chantier;
    private UUID zone;
    private UUID type;

    @BeforeEach
    void setUp() {
        fixture.preparer();
        appels = new AppelsTravauxChantier(mockMvc, json, fixture);
        jeton = FixtureProjetLots.jeton();
        chantier = fixture.chantier("GSD-" + jeton);
        zone = fixture.zone(chantier.cfgProjet(), "Z-" + jeton);
        type = fixture.type("TD-" + jeton, new ValeursType("J", "150", "0.2", "H", "20", "0.1"));
    }

    private UUID jour(UUID projet, UUID statut, String code) {
        return fixture.jour(new Jour(chantier.societe(), projet, type, zone, statut, DATE.atStartOfDay(), code + "-" + jeton,
                null));
    }

    private UUID jour(UUID statut, String code) {
        return jour(chantier.projet(), statut, code);
    }

    /** Restreint qui voit les 5 statuts et intervient sur les transitions données. */
    private RequestPostProcessor restreint(String suffixe, List<UUID> transitions, String... droits) {
        return fixture.restreint(jeton + suffixe, chantier.projet(), TOUS_LES_STATUTS, transitions, droits).connexion();
    }

    private RequestPostProcessor libre(String suffixe) {
        return fixture.compte(jeton + suffixe, false, List.of(chantier.projet()), DROIT_LIRE).connexion();
    }

    // ------------------------------------------------------------------ R-N2b-1 : droit de lecture, 403 avec témoin

    @Test
    void r1_sansDroitLecture_403_surLesTroisRoutes_rienNEstEcrit() throws Exception {
        RequestPostProcessor lecteur = restreint("a", TOUTES_LES_TRANSITIONS, DROIT_LIRE);
        RequestPostProcessor sansLecture = restreint("b", TOUTES_LES_TRANSITIONS, DROIT_MODIFIER, DROIT_SUPPRIMER);
        UUID jour = jour(EN_COURS, "R1A");

        Reponse temoinListe = appels.transitions(jour, lecteur);
        Reponse temoinHistorique = appels.historique(jour, lecteur);
        Reponse liste = appels.transitions(jour, sansLecture);
        Reponse historique = appels.historique(jour, sansLecture);
        Reponse execution = appels.executer(jour, T_DEMANDE, sansLecture);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(temoinListe.statut()).as("témoin : " + temoinListe.corps()).isEqualTo(200);
            s.assertThat(temoinHistorique.statut()).as("témoin : " + temoinHistorique.corps()).isEqualTo(200);
            s.assertThat(liste.statut()).as(liste.corps()).isEqualTo(403);
            s.assertThat(historique.statut()).as(historique.corps()).isEqualTo(403);
            s.assertThat(execution.statut()).as(execution.corps()).isEqualTo(403);
            s.assertThat(fixture.statutDe(jour)).isEqualTo(majuscules(EN_COURS));
            s.assertThat(fixture.historique(jour)).isEmpty();
        });
    }

    @Test
    void r1_lectureSeule_suffitPourExecuter_aucunDroitPropreAuxTransitions() throws Exception {
        RequestPostProcessor lecteur = restreint("c", TOUTES_LES_TRANSITIONS, DROIT_LIRE);
        UUID jour = jour(EN_COURS, "R1B");

        Reponse r = appels.executer(jour, T_DEMANDE, lecteur);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(fixture.statutDe(jour)).isEqualTo(majuscules(DEMANDE_VALIDATION));
    }

    @Test
    void r1_jourInvisible_404_avantLeDroit() throws Exception {
        RequestPostProcessor sansLecture = restreint("d", TOUTES_LES_TRANSITIONS, DROIT_MODIFIER);
        UUID visible = jour(EN_COURS, "R1C");
        UUID horsPerimetre = jour(fixture.projet(chantier.societe(), "GDN-" + jeton), EN_COURS, "R1D");

        Reponse visibleListe = appels.transitions(visible, sansLecture);
        Reponse invisibleListe = appels.transitions(horsPerimetre, sansLecture);
        Reponse invisibleExecution = appels.executer(horsPerimetre, T_DEMANDE, sansLecture);
        Reponse invisibleHistorique = appels.historique(horsPerimetre, sansLecture);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(visibleListe.statut()).as("témoin : " + visibleListe.corps()).isEqualTo(403);
            s.assertThat(invisibleListe.statut()).as(invisibleListe.corps()).isEqualTo(404);
            s.assertThat(invisibleExecution.statut()).as(invisibleExecution.corps()).isEqualTo(404);
            s.assertThat(invisibleHistorique.statut()).as(invisibleHistorique.corps()).isEqualTo(404);
        });
    }

    // ------------------------------------------------------------------ R-N2b-2 : restreint

    @Test
    void r2_restreintSansIntervenant_listeVide_et409_rienNEstEcrit() throws Exception {
        RequestPostProcessor sansIntervenant = restreint("e", List.of(), DROIT_LIRE);
        UUID jour = jour(EN_COURS, "R2A");

        Reponse liste = appels.transitions(jour, sansIntervenant);
        Reponse execution = appels.executer(jour, T_DEMANDE, sansIntervenant);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(liste.statut()).as(liste.corps()).isEqualTo(200);
            s.assertThat(liste.<List<String>>lire(IDS)).isEmpty();
            s.assertThat(execution.statut()).as(execution.corps()).isEqualTo(409);
            s.assertThat(fixture.statutDe(jour)).isEqualTo(majuscules(EN_COURS));
            s.assertThat(fixture.historique(jour)).isEmpty();
        });
    }

    @Test
    void r2_restreint_intervenantSurUneSeuleTransition_seuleCelleLa() throws Exception {
        RequestPostProcessor annuleur = restreint("f", List.of(T_ANNULATION), DROIT_LIRE);
        UUID jour = jour(EN_COURS, "R2B");

        Reponse liste = appels.transitions(jour, annuleur);
        Reponse refus = appels.executer(jour, T_DEMANDE, annuleur);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(liste.statut()).as(liste.corps()).isEqualTo(200);
            s.assertThat(majuscules(liste.<List<String>>lire(IDS))).containsExactly(majuscules(T_ANNULATION));
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(409);
            s.assertThat(refus.<List<String>>lire("$.transitionsAutorisees")).containsExactly("Annulée");
            s.assertThat(fixture.statutDe(jour)).isEqualTo(majuscules(EN_COURS));
        });
    }

    @Test
    void r2_restreint_intervenantAMotDePasseSeulement_nOuvrePasLaTransition() throws Exception {
        Compte c = fixture.restreint(jeton + "g", chantier.projet(), TOUS_LES_STATUTS, List.of(), DROIT_LIRE);
        fixture.intervenantTransition(T_DEMANDE, c.role(), true);
        UUID jour = jour(EN_COURS, "R2C");

        Reponse liste = appels.transitions(jour, c.connexion());

        assertThat(liste.statut()).as(liste.corps()).isEqualTo(200);
        assertThat(liste.<List<String>>lire(IDS)).isEmpty();
    }

    // ------------------------------------------------------------------ R-N2b-2 : libre

    @Test
    void r2_libre_enCours_lesQuatreTransitionsDeLEntite() throws Exception {
        UUID jour = jour(EN_COURS, "R2D");

        Reponse liste = appels.transitions(jour, libre("h"));

        assertThat(liste.statut()).as(liste.corps()).isEqualTo(200);
        assertThat(majuscules(liste.<List<String>>lire(IDS))).containsExactlyInAnyOrder(majuscules(T_DEMANDE),
                majuscules(T_VALIDATION), majuscules(T_ANNULATION), majuscules(T_REJET));
    }

    @Test
    void r2_libre_validee_toutesSaufVersLeStatutCourant() throws Exception {
        UUID jour = jour(VALIDEE, "R2E");

        Reponse liste = appels.transitions(jour, libre("i"));

        assertThat(liste.statut()).as(liste.corps()).isEqualTo(200);
        assertThat(majuscules(liste.<List<String>>lire(IDS))).containsExactlyInAnyOrder(majuscules(T_DEMANDE),
                majuscules(T_ANNULATION), majuscules(T_REJET));
    }

    @Test
    void r2_libre_postRelitLeMemeCalcul_transitionDUnAutreDepart_200() throws Exception {
        UUID jour = jour(EN_COURS, "R2F");

        Reponse r = appels.executer(jour, T_VALIDATION, libre("j"));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(fixture.statutDe(jour)).isEqualTo(majuscules(VALIDEE));
    }
}
