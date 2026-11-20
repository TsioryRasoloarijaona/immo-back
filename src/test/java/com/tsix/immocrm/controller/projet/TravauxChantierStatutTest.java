package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsTravauxChantier.Reponse;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import com.tsix.immocrm.fixtures.FixtureTravauxChantier;
import com.tsix.immocrm.fixtures.FixtureTravauxChantier.Chantier;
import com.tsix.immocrm.fixtures.FixtureTravauxChantier.Jour;
import com.tsix.immocrm.fixtures.FixtureTravauxChantier.ValeursType;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.ANNULEE;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.DEMANDE_VALIDATION;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.DROIT_LIRE;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.DROIT_MODIFIER;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.EN_COURS;
import static com.tsix.immocrm.fixtures.FixtureTravauxChantier.REJETEE;
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
 * N2b (FEATURE-n2-travaux-chantier.md § N2b) : {@code GET/POST /api/projet/travaux-chantier/{id}/transitions}. Le compte
 * « restreint » voit les 5 statuts et intervient sur les 4 transitions de prod (intervenants posés par la fixture, 0 en prod).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TravauxChantierStatutTest {

    private static final LocalDate DATE = LocalDate.of(2077, 3, 10);
    private static final String MARS = "du=2077-03-01&au=2077-03-31";
    private static final String IDS = "$[*].id";

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper json;
    @Autowired private FixtureTravauxChantier fixture;

    private AppelsTravauxChantier appels;
    private String jeton;
    private Chantier chantier;
    private UUID zone;
    private UUID type;
    private RequestPostProcessor restreint;

    @BeforeEach
    void setUp() {
        fixture.preparer();
        appels = new AppelsTravauxChantier(mockMvc, json, fixture);
        jeton = FixtureProjetLots.jeton();
        chantier = fixture.chantier("GST-" + jeton);
        zone = fixture.zone(chantier.cfgProjet(), "Z-" + jeton);
        type = fixture.type("TS-" + jeton, new ValeursType("J", "150", "0.2", "H", "20", "0.1"));
        restreint = fixture.restreint(jeton + "r", chantier.projet(), TOUS_LES_STATUTS, TOUTES_LES_TRANSITIONS, DROIT_LIRE,
                DROIT_MODIFIER).connexion();
    }

    private UUID jour(UUID projet, UUID statut, String code) {
        return fixture.jour(new Jour(chantier.societe(), projet, type, zone, statut, DATE.atStartOfDay(), code, null));
    }

    private UUID jour(UUID statut, String code) {
        return jour(chantier.projet(), statut, code + "-" + jeton);
    }

    private void inchange(SoftAssertions s, UUID jour, UUID statut) {
        s.assertThat(fixture.statutDe(jour)).isEqualTo(majuscules(statut));
        s.assertThat(fixture.historique(jour)).isEmpty();
    }

    // ------------------------------------------------------------------ R-N2b-2 / R-N2b-4 : transitions proposées

    @Test
    void r2_r4_enCours_proposeDemandeDeValidationEtAnnulation_avecStatutCible() throws Exception {
        UUID jour = jour(EN_COURS, "LEC");

        Reponse r = appels.transitions(jour, restreint);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(majuscules(r.<List<String>>lire(IDS))).containsExactly(majuscules(T_DEMANDE), majuscules(T_ANNULATION));
            s.assertThat(r.<List<String>>lire("$[*].libelle")).containsExactly("Demande de validation", "Annulation");
            s.assertThat(r.<String>lire("$[0].statutCible.id")).isEqualToIgnoringCase(DEMANDE_VALIDATION.toString());
            s.assertThat(r.<String>lire("$[0].statutCible.code")).isEqualTo("Demande de validation");
            s.assertThat(r.<String>lire("$[0].statutCible.libelle")).isEqualTo("Demande de validation");
            s.assertThat(r.<String>lire("$[0].statutCible.couleur")).isEqualTo("Crimson");
            s.assertThat(r.<String>lire("$[1].statutCible.id")).isEqualToIgnoringCase(ANNULEE.toString());
            s.assertThat(r.<List<Boolean>>lire("$[*].avecCommentaire")).containsExactly(false, false);
            s.assertThat(r.<List<Boolean>>lire("$[*].avecDateUtilisateur")).containsExactly(false, false);
            s.assertThat(r.<List<Boolean>>lire("$[*].avecSecondValideur")).containsExactly(false, false);
        });
    }

    @Test
    void r4_demandeDeValidation_proposeValidationEtRejet() throws Exception {
        UUID jour = jour(DEMANDE_VALIDATION, "LDV");

        Reponse r = appels.transitions(jour, restreint);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(majuscules(r.<List<String>>lire(IDS))).containsExactly(majuscules(T_VALIDATION), majuscules(T_REJET));
            s.assertThat(r.<List<String>>lire("$[*].statutCible.libelle")).containsExactly("Validée", "Rejetée");
        });
    }

    static Stream<UUID> statutsFinaux() {
        return Stream.of(VALIDEE, ANNULEE, REJETEE);
    }

    @ParameterizedTest
    @MethodSource("statutsFinaux")
    void r4_statutFinal_restreint_listeVide(UUID statut) throws Exception {
        UUID jour = jour(statut, "LF" + statut.toString().substring(0, 4));

        Reponse r = appels.transitions(jour, restreint);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(r.<List<String>>lire(IDS)).isEmpty();
    }

    // ------------------------------------------------------------------ R-N2b-4 : les 4 chemins permis

    static Stream<Arguments> cheminsPermis() {
        return Stream.of(Arguments.of(EN_COURS, T_DEMANDE, DEMANDE_VALIDATION, "Demande de validation"),
                Arguments.of(EN_COURS, T_ANNULATION, ANNULEE, "Annulée"),
                Arguments.of(DEMANDE_VALIDATION, T_VALIDATION, VALIDEE, "Validée"),
                Arguments.of(DEMANDE_VALIDATION, T_REJET, REJETEE, "Rejetée"));
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("cheminsPermis")
    void r4_cheminPermis_200_statutEcrit_ficheRelue_uneLigneDHistorique(UUID depart, UUID transition, UUID cible,
                                                                          String libelle) throws Exception {
        UUID jour = jour(depart, "CP" + transition.toString().substring(0, 4));

        Reponse r = appels.executer(jour, transition, restreint);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        List<Map<String, Object>> historique = fixture.historique(jour);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.<String>lire("$.id")).isEqualToIgnoringCase(jour.toString());
            s.assertThat(r.<String>lire("$.statut.id")).isEqualToIgnoringCase(cible.toString());
            s.assertThat(r.<String>lire("$.statut.libelle")).isEqualTo(libelle);
            s.assertThat(fixture.statutDe(jour)).isEqualTo(majuscules(cible));
            s.assertThat(historique).hasSize(1);
            s.assertThat(historique).extracting(l -> String.valueOf(l.get("StrWorkflowId")).toUpperCase())
                    .containsExactly(majuscules(transition));
            s.assertThat(historique).extracting(l -> String.valueOf(l.get("StrStatusId")).toUpperCase())
                    .containsExactly(majuscules(cible));
            s.assertThat(historique).extracting(l -> String.valueOf(l.get("StrConfigEntityId")).toUpperCase())
                    .containsExactly(majuscules(fixture.entite()));
        });
    }

    static Stream<Arguments> statutFinalEtTransition() {
        return statutsFinaux().flatMap(statut -> TOUTES_LES_TRANSITIONS.stream().map(t -> Arguments.of(statut, t)));
    }

    @ParameterizedTest
    @MethodSource("statutFinalEtTransition")
    void r4_statutFinal_touteExecution_409_rienNEstEcrit(UUID statut, UUID transition) throws Exception {
        UUID jour = jour(statut, "F" + statut.toString().substring(0, 3) + transition.toString().substring(0, 3));

        Reponse r = appels.executer(jour, transition, restreint);

        assertThat(r.statut()).as(r.corps()).isEqualTo(409);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.<List<String>>lire("$.transitionsAutorisees")).isEmpty();
            inchange(s, jour, statut);
        });
    }

    // ------------------------------------------------------------------ R-N2b-3 : transition hors liste

    @Test
    void r3_transitionDUnAutreStatutDeDepart_409_corpsAvecLesStatutsPermis_rienNEstEcrit() throws Exception {
        UUID jour = jour(EN_COURS, "HDE");

        Reponse r = appels.executer(jour, T_VALIDATION, restreint);

        assertThat(r.statut()).as(r.corps()).isEqualTo(409);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.<List<String>>lire("$.transitionsAutorisees"))
                    .containsExactlyInAnyOrder("Demande de validation", "Annulée");
            inchange(s, jour, EN_COURS);
        });
    }

    @Test
    void r3_transitionDUneAutreEntite_409_memePourUnLibre() throws Exception {
        RequestPostProcessor libre = fixture.compte(jeton + "l", false, List.of(chantier.projet()), DROIT_LIRE).connexion();
        UUID etrangere = fixture.transitionEtrangere(jeton);
        UUID jour = jour(EN_COURS, "ETR");

        Reponse r = appels.executer(jour, etrangere, libre);

        assertThat(r.statut()).as(r.corps()).isEqualTo(409);
        SoftAssertions.assertSoftly(s -> inchange(s, jour, EN_COURS));
    }

    @Test
    void r3_transitionInconnue_409() throws Exception {
        UUID jour = jour(EN_COURS, "INC");

        Reponse r = appels.executer(jour, UUID.randomUUID(), restreint);

        assertThat(r.statut()).as(r.corps()).isEqualTo(409);
        SoftAssertions.assertSoftly(s -> inchange(s, jour, EN_COURS));
    }

    @Test
    void post_transitionIdAbsent_400_rienNEstEcrit() throws Exception {
        UUID jour = jour(EN_COURS, "SID");

        Reponse r = appels.executer(jour, new LinkedHashMap<>(), restreint);

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
        SoftAssertions.assertSoftly(s -> inchange(s, jour, EN_COURS));
    }

    // ------------------------------------------------------------------ R-N2b-1 : 404 hors périmètre (avec témoin 200)

    @Test
    void r1_jourInconnu_404() throws Exception {
        UUID temoin = jour(EN_COURS, "TIN");

        Reponse okListe = appels.transitions(temoin, restreint);
        Reponse liste = appels.transitions(UUID.randomUUID(), restreint);
        Reponse execution = appels.executer(UUID.randomUUID(), T_DEMANDE, restreint);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(okListe.statut()).as("témoin : " + okListe.corps()).isEqualTo(200);
            s.assertThat(liste.statut()).as(liste.corps()).isEqualTo(404);
            s.assertThat(execution.statut()).as(execution.corps()).isEqualTo(404);
        });
    }

    @Test
    void r1_projetNonHabilite_404_rienNEstEcrit() throws Exception {
        UUID horsPerimetre = jour(fixture.projet(chantier.societe(), "GSN-" + jeton), EN_COURS, "HP-" + jeton);
        UUID temoin = jour(EN_COURS, "THP");

        Reponse ok = appels.executer(temoin, T_DEMANDE, restreint);
        Reponse liste = appels.transitions(horsPerimetre, restreint);
        Reponse execution = appels.executer(horsPerimetre, T_DEMANDE, restreint);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(ok.statut()).as("témoin : " + ok.corps()).isEqualTo(200);
            s.assertThat(liste.statut()).as(liste.corps()).isEqualTo(404);
            s.assertThat(execution.statut()).as(execution.corps()).isEqualTo(404);
            inchange(s, horsPerimetre, EN_COURS);
        });
    }

    @Test
    void r1_statutInvisible_404_rienNEstEcrit() throws Exception {
        RequestPostProcessor voitEnCours = fixture.restreint(jeton + "v", chantier.projet(), List.of(EN_COURS),
                TOUTES_LES_TRANSITIONS, DROIT_LIRE).connexion();
        UUID temoin = jour(EN_COURS, "TSI");
        UUID invisible = jour(DEMANDE_VALIDATION, "SIN");

        Reponse ok = appels.transitions(temoin, voitEnCours);
        Reponse liste = appels.transitions(invisible, voitEnCours);
        Reponse execution = appels.executer(invisible, T_VALIDATION, voitEnCours);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(ok.statut()).as("témoin : " + ok.corps()).isEqualTo(200);
            s.assertThat(liste.statut()).as(liste.corps()).isEqualTo(404);
            s.assertThat(execution.statut()).as(execution.corps()).isEqualTo(404);
            inchange(s, invisible, DEMANDE_VALIDATION);
        });
    }

    // ------------------------------------------------------------------ R-N2b-10 : nouveau statut invisible de l'appelant

    @Test
    void r10_nouveauStatutInvisible_200_ficheRelue_puisJourHorsListeEtFiche404() throws Exception {
        RequestPostProcessor voitEnCours = fixture.restreint(jeton + "w", chantier.projet(), List.of(EN_COURS),
                List.of(T_ANNULATION), DROIT_LIRE).connexion();
        UUID jour = jour(EN_COURS, "R10");

        Reponse r = appels.executer(jour, T_ANNULATION, voitEnCours);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        Reponse fiche = appels.fiche(jour, voitEnCours);
        List<String> liste = appels.ids(MARS, voitEnCours);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.<String>lire("$.statut.id")).isEqualToIgnoringCase(ANNULEE.toString());
            s.assertThat(r.<String>lire("$.statut.libelle")).isEqualTo("Annulée");
            s.assertThat(fixture.statutDe(jour)).isEqualTo(majuscules(ANNULEE));
            s.assertThat(liste).doesNotContain(jour.toString());
            s.assertThat(fiche.statut()).as(fiche.corps()).isEqualTo(404);
        });
    }

    // ------------------------------------------------------------------ R-N2b-11 : auteur de la mise à jour

    @Test
    void r11_execution_metAJourUserUpdateEtUserUpdateDateTime() throws Exception {
        UUID jour = jour(EN_COURS, "R11");

        Reponse r = appels.executer(jour, T_DEMANDE, restreint);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(fixture.lire("SELECT UserUpdate FROM GrhWorkedDay WHERE Pkey = ?", jour)).isNotBlank();
            s.assertThat(fixture.lire("SELECT CONVERT(varchar(30), UserUpdateDateTime, 126) FROM GrhWorkedDay WHERE Pkey = ?",
                    jour)).isNotBlank();
        });
    }

    // ------------------------------------------------------------------ R-N2b-6 : aucun verrou d'édition après statut

    @ParameterizedTest
    @MethodSource("statutsFinaux")
    void r6_put_statutFinal_200_aucunVerrou(UUID statut) throws Exception {
        String code = "PUT" + statut.toString().substring(0, 4) + "-" + jeton;
        UUID jour = jour(chantier.projet(), statut, code);
        Map<String, Object> corps = AppelsTravauxChantier.ecriture(chantier.societe(), chantier.projet(), type, zone, DATE,
                code + "M");

        Reponse r = appels.modifier(jour, corps, restreint);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(fixture.lire("SELECT Code FROM GrhWorkedDay WHERE Pkey = ?", jour)).isEqualTo(code + "M");
    }
}
