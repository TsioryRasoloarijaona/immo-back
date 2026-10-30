package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureReferentielsAdmin;
import com.tsix.immocrm.fixtures.FixtureTaches;
import com.tsix.immocrm.fixtures.FixtureTaches.LigneHistorique;
import com.tsix.immocrm.fixtures.FixtureTaches.Statut;
import com.tsix.immocrm.fixtures.FixtureTaches.Transition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.AUTEUR;
import static com.tsix.immocrm.fixtures.FixtureTaches.ANNULEE;
import static com.tsix.immocrm.fixtures.FixtureTaches.ATTENTE;
import static com.tsix.immocrm.fixtures.FixtureTaches.COMMENTAIRE;
import static com.tsix.immocrm.fixtures.FixtureTaches.DIFFEREE;
import static com.tsix.immocrm.fixtures.FixtureTaches.EN_COURS;
import static com.tsix.immocrm.fixtures.FixtureTaches.ENTITE_TACHE;
import static com.tsix.immocrm.fixtures.FixtureTaches.NON_COMMENCEE;
import static com.tsix.immocrm.fixtures.FixtureTaches.STATUT;
import static com.tsix.immocrm.fixtures.FixtureTaches.STATUTS_TACHE;
import static com.tsix.immocrm.fixtures.FixtureTaches.STATUT_ID;
import static com.tsix.immocrm.fixtures.FixtureTaches.TERMINEE;
import static com.tsix.immocrm.fixtures.FixtureTaches.TRANSITIONS;
import static com.tsix.immocrm.fixtures.FixtureTaches.permis;
import static com.tsix.immocrm.fixtures.FixtureTaches.transition;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Passage de statut — FEATURE-admin-taches.md, règle 5 et décision « le statut reçu se résout par {@code StatusToId} ».
 * Les 30 couples de statuts distincts sont joués : 9 permis, 21 refusés.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TachesStatutTest {

    private static final String AUTORISES = "$.transitionsAutorisees";
    private static final String STATUT_COLONNE = "StrStatusId";
    private static final String MOTIF = "Motif de l'annulation";

    static final List<Transition> NEUF = TRANSITIONS;

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureTaches fixture;

    private AdmUser admin;
    private String jeton;

    static Stream<Arguments> horsDesNeuf() {
        return STATUTS_TACHE.stream().flatMap(depuis -> STATUTS_TACHE.stream()
                .filter(vers -> !vers.equals(depuis) && !permis(depuis).contains(vers))
                .map(vers -> Arguments.of(depuis, vers)));
    }

    @BeforeEach
    void setUp() {
        fixture.assurer();
        admin = patrimoine.administrateur();
        jeton = FixtureReferentielsAdmin.jeton();
    }

    private ResultActions passer(UUID id, CorpsJson corps) throws Exception {
        return mockMvc.perform(post(STATUT, id).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    private static CorpsJson vers(Statut statut) {
        return corps().avec(STATUT_ID, statut.fil());
    }

    private UUID tache(Statut statut) {
        return fixture.tache("STA-" + jeton, statut);
    }

    private static String[] libelles(Statut depuis) {
        return permis(depuis).stream().map(Statut::libelle).toArray(String[]::new);
    }

    @ParameterizedTest(name = "{0}")
    @FieldSource("NEUF")
    void r5_transitionAutorisee_200_statutEcrit(Transition t) throws Exception {
        UUID id = tache(t.depuis());

        passer(id, vers(t.vers()).avec(COMMENTAIRE, MOTIF)).andExpect(status().isOk());

        assertThat(fixture.guid(id, STATUT_COLONNE)).isEqualTo(t.vers().id());
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("horsDesNeuf")
    void r5_horsDesNeuf_409_avecLesStatutsPermis_rienEcrit(Statut depuis, Statut cible) throws Exception {
        UUID id = tache(depuis);

        passer(id, vers(cible).avec(COMMENTAIRE, MOTIF))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(AUTORISES, containsInAnyOrder((Object[]) libelles(depuis))));

        assertAll(
                () -> assertThat(fixture.guid(id, STATUT_COLONNE)).isEqualTo(depuis.id()),
                () -> assertThat(fixture.nombreHistorique(id)).isZero());
    }

    @Test
    void r5_memeStatut_409() throws Exception {
        UUID id = tache(EN_COURS);

        passer(id, vers(EN_COURS)).andExpect(status().isConflict());
    }

    @Test
    void r5_statutInconnu_409() throws Exception {
        UUID id = tache(EN_COURS);

        passer(id, corps().avec(STATUT_ID, UUID.randomUUID()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(AUTORISES, containsInAnyOrder((Object[]) libelles(EN_COURS))));
    }

    // ------------------------------------------------------------------ tâche sans statut

    @Test
    void r5_sansStatut_partDeNonCommencee() throws Exception {
        UUID id = fixture.tache("NUL-" + jeton);

        passer(id, vers(DIFFEREE)).andExpect(status().isOk());

        assertThat(fixture.guid(id, STATUT_COLONNE)).isEqualTo(DIFFEREE.id());
    }

    @Test
    void r5_sansStatut_transitionNonPermiseDepuisNonCommencee_409() throws Exception {
        UUID id = fixture.tache("NUL-" + jeton);

        passer(id, vers(TERMINEE))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(AUTORISES, containsInAnyOrder((Object[]) libelles(NON_COMMENCEE))));
    }

    // ------------------------------------------------------------------ commentaire

    @Test
    void r5_attenteVersAnnulee_sansCommentaire_400_statutConserve() throws Exception {
        UUID id = tache(ATTENTE);

        passer(id, vers(ANNULEE)).andExpect(status().isBadRequest());

        assertAll(
                () -> assertThat(fixture.guid(id, STATUT_COLONNE)).isEqualTo(ATTENTE.id()),
                () -> assertThat(fixture.nombreHistorique(id)).isZero());
    }

    @ParameterizedTest(name = "[{0}]")
    @ValueSource(strings = {"", "   "})
    void r5_attenteVersAnnulee_commentaireVide_400(String commentaire) throws Exception {
        UUID id = tache(ATTENTE);

        passer(id, vers(ANNULEE).avec(COMMENTAIRE, commentaire)).andExpect(status().isBadRequest());
    }

    /** Sans commentaire : les huit autres passent, {@code WithComment} = 0. */
    @ParameterizedTest(name = "{0}")
    @FieldSource("NEUF")
    void r5_commentaireExigeSeulementSiWithComment(Transition t) throws Exception {
        UUID id = tache(t.depuis());

        passer(id, vers(t.vers())).andExpect(t.commentaire() ? status().isBadRequest() : status().isOk());
    }

    @Test
    void r5_commentaireLuEnBase_retireSurAttenteVersAnnulee() throws Exception {
        fixture.commentaireObligatoire(transition(ATTENTE, ANNULEE), false);
        UUID id = tache(ATTENTE);

        passer(id, vers(ANNULEE)).andExpect(status().isOk());
    }

    @Test
    void r5_commentaireLuEnBase_poseSurEnCoursVersTerminee() throws Exception {
        fixture.commentaireObligatoire(transition(EN_COURS, TERMINEE), true);
        UUID id = tache(EN_COURS);

        passer(id, vers(TERMINEE)).andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ StrHistory

    @Test
    void r5_passage_ecritStrHistory() throws Exception {
        Transition t = transition(ATTENTE, ANNULEE);
        UUID id = tache(ATTENTE);

        passer(id, vers(ANNULEE).avec(COMMENTAIRE, MOTIF)).andExpect(status().isOk());

        assertThat(fixture.historique(id, t)).containsExactly(new LigneHistorique(ANNULEE.id(), ENTITE_TACHE,
                "%s: %s(%s & )".formatted(t.description(), t.code(), admin.getCode()), MOTIF, AUTEUR));
    }

    @Test
    void r5_tacheSansStatut_historiseLaTransitionDepuisNonCommencee() throws Exception {
        Transition t = transition(NON_COMMENCEE, EN_COURS);
        UUID id = fixture.tache("NUL-" + jeton);

        passer(id, vers(EN_COURS)).andExpect(status().isOk());

        assertThat(fixture.historique(id, t)).hasSize(1);
    }
}
