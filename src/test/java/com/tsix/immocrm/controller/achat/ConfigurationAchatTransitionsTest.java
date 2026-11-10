package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.fixtures.FixtureConfigurationAchat;
import com.tsix.immocrm.fixtures.FixtureConfigurationAchat.Statut;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.LectureConfigurationAchat.TRANSITIONS;
import static com.tsix.immocrm.controller.achat.LectureConfigurationAchat.ligne;
import static com.tsix.immocrm.controller.achat.LectureConfigurationAchat.lire;
import static com.tsix.immocrm.controller.achat.LectureConfigurationAchat.rangs;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * GET /api/achat/config/transitions — FEATURE-achat-regle-commune.md §4.1 ({@code TransitionAchatDto}), R13, R14,
 * paramètre {@code utilisees}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ConfigurationAchatTransitionsTest {

    private static final String NOMBRE_PASSAGES = "nombrePassages";
    private static final String DERNIER_PASSAGE = "dernierPassage";
    private static final String GESTION_CODE = "gestionCode";
    private static final String GESTION_LIBELLE = "gestionLibelle";
    private static final String ROLES = "roles";
    private static final String UTILISEES = "utilisees";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureConfigurationAchat fixture;
    @Autowired private FixturePatrimoine patrimoine;

    private String jeton;
    private RequestPostProcessor qui;
    private UUID enCours;
    private UUID valide;
    private UUID gestionA;
    private UUID gestionB;

    @BeforeEach
    void setUp() {
        jeton = FixtureConfigurationAchat.jeton("A4T");
        qui = connecte(patrimoine.administrateur());
        enCours = fixture.statut(Statut.simple("EC-" + jeton, 1));
        valide = fixture.statut(Statut.simple("VA-" + jeton, 2));
        gestionA = fixture.gestion("GA-" + jeton, "Alpha " + jeton);
        gestionB = fixture.gestion("GB-" + jeton, "Beta " + jeton);
    }

    private List<Map<String, Object>> transitions() throws Exception {
        return lire(mockMvc, TRANSITIONS, qui);
    }

    private List<Map<String, Object>> utilisees() throws Exception {
        return lire(mockMvc, get(TRANSITIONS).param(UTILISEES, "true"), qui);
    }

    // ------------------------------------------------------------------ champs

    /** §4.1 : champs de la transition, gestion, statuts, drapeaux, rôles triés par code, passages. */
    @Test
    void transition_champsRolesTriesEtPassages() throws Exception {
        UUID t = fixture.transition(gestionA, 3, "Valider " + jeton, enCours, valide, true, true);
        UUID roleB = fixture.role("RB-" + jeton);
        UUID roleA = fixture.role("RA-" + jeton);
        fixture.lier(t, roleB);
        fixture.lier(t, roleA);
        UUID piece = fixture.piece(valide, gestionA);
        fixture.passage(piece, null, enCours, LocalDateTime.of(2022, 1, 3, 9, 0));
        fixture.passage(piece, t, valide, LocalDateTime.of(2022, 3, 1, 10, 0));
        fixture.passage(piece, t, valide, LocalDateTime.of(2022, 5, 2, 16, 45));

        Map<String, Object> l = ligne(transitions(), t);

        assertThat(l).isNotNull()
                .containsEntry("ordre", 3)
                .containsEntry("description", "Valider " + jeton)
                .containsEntry(GESTION_CODE, "GA-" + jeton)
                .containsEntry(GESTION_LIBELLE, "Alpha " + jeton)
                .containsEntry("statutDepartCode", "EC-" + jeton)
                .containsEntry("statutArriveeCode", "VA-" + jeton)
                .containsEntry("noteObligatoire", true)
                .containsEntry("valideeParDeclencheur", true)
                .containsEntry(NOMBRE_PASSAGES, 2)
                .containsEntry(DERNIER_PASSAGE, "2022-05-02");
        assertAll(
                () -> assertThat(l.get("statutDepartId").toString()).isEqualToIgnoringCase(enCours.toString()),
                () -> assertThat(l.get("statutArriveeId").toString()).isEqualToIgnoringCase(valide.toString()),
                () -> assertThat(roles(l)).extracting(r -> r.get("code")).containsExactly("RA-" + jeton, "RB-" + jeton),
                () -> assertThat(roles(l)).extracting(r -> r.get("id").toString().toLowerCase())
                        .containsExactly(roleA.toString(), roleB.toString()));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> roles(Map<String, Object> transition) {
        return (List<Map<String, Object>>) transition.get(ROLES);
    }

    /** §4.1 : drapeaux NULL ou 0 → false ; jamais passée → 0 et dernierPassage null. */
    @Test
    void transition_sansPassage_zeroEtDernierPassageNull_drapeauxFaux() throws Exception {
        UUID t = fixture.transition(gestionA, 1, "Jamais " + jeton, enCours, valide, null, null);

        Map<String, Object> l = ligne(transitions(), t);

        assertThat(l).isNotNull()
                .containsEntry("noteObligatoire", false)
                .containsEntry("valideeParDeclencheur", false)
                .containsEntry(NOMBRE_PASSAGES, 0)
                .containsEntry(DERNIER_PASSAGE, null);
    }

    // ------------------------------------------------------------------ R13

    /** R13 : transition sans rôle → roles = []. */
    @Test
    void r13_transitionSansRole_rolesVide() throws Exception {
        UUID t = fixture.transition(gestionA, 1, "Sans role " + jeton, enCours, valide);

        Map<String, Object> l = ligne(transitions(), t);

        assertThat(l).isNotNull();
        assertThat(roles(l)).isNotNull().isEmpty();
    }

    /** R13 : transition sans gestion → gestionCode null, libellé « Sans gestion », après toutes les lignes avec gestion. */
    @Test
    void r13_transitionSansGestion_sansGestionEnDernier() throws Exception {
        UUID sans = fixture.transition(null, 1, "Orpheline " + jeton, enCours, valide);
        fixture.transition(gestionB, 99, "Beta " + jeton, enCours, valide);

        List<Map<String, Object>> lignes = transitions();
        Map<String, Object> l = ligne(lignes, sans);
        int rang = lignes.indexOf(l);

        assertThat(l).isNotNull().containsEntry(GESTION_CODE, null).containsEntry(GESTION_LIBELLE, "Sans gestion");
        assertThat(lignes.subList(rang, lignes.size()))
                .isNotEmpty()
                .allSatisfy(x -> assertThat(x.get(GESTION_CODE)).isNull());
    }

    // ------------------------------------------------------------------ tri

    /** §4.1 : tri libellé de gestion, puis Sorting, puis Description. */
    @Test
    void transitions_triGestionPuisSortingPuisDescription() throws Exception {
        UUID b1 = fixture.transition(gestionB, 1, "a " + jeton, enCours, valide);
        UUID a2 = fixture.transition(gestionA, 2, "a " + jeton, enCours, valide);
        UUID a1b = fixture.transition(gestionA, 1, "b " + jeton, enCours, valide);
        UUID a1a = fixture.transition(gestionA, 1, "a " + jeton, enCours, valide);

        List<Integer> rangs = rangs(transitions(), a1a, a1b, a2, b1);

        assertThat(rangs).doesNotContain(-1).isSorted();
    }

    // ------------------------------------------------------------------ utilisees

    /** §4.1 : utilisees=true → seulement nombrePassages > 0. */
    @Test
    void utilisees_seulementLesTransitionsPassees() throws Exception {
        UUID passee = fixture.transition(gestionA, 1, "Passee " + jeton, enCours, valide);
        UUID jamais = fixture.transition(gestionA, 2, "Jamais " + jeton, valide, enCours);
        fixture.passage(fixture.piece(valide, gestionA), passee, valide, LocalDateTime.of(2023, 6, 1, 8, 0));

        List<Map<String, Object>> lignes = utilisees();

        assertAll(
                () -> assertThat(ligne(lignes, passee)).isNotNull().containsEntry(NOMBRE_PASSAGES, 1),
                () -> assertThat(ligne(lignes, jamais)).isNull(),
                () -> assertThat(lignes).isNotEmpty()
                        .allSatisfy(x -> assertThat(((Number) x.get(NOMBRE_PASSAGES)).intValue()).isPositive()));
    }

    /** §4.1 : sans paramètre (défaut faux), les transitions jamais passées sont listées. */
    @Test
    void utilisees_defautFaux_inclutLesJamaisPassees() throws Exception {
        UUID jamais = fixture.transition(gestionA, 2, "Jamais " + jeton, valide, enCours);

        assertThat(ligne(transitions(), jamais)).isNotNull().containsEntry(NOMBRE_PASSAGES, 0);
    }

    // ------------------------------------------------------------------ R14

    /**
     * R14 : comptage par identifiant de transition — un passage sur la transition de la gestion A compte pour elle même si la
     * pièce est aujourd'hui dans la gestion B ; la transition de B sur la même paire de statuts reste à 0.
     */
    @Test
    void r14_comptageParIdentifiant_pasParPaireDeStatuts() throws Exception {
        UUID deA = fixture.transition(gestionA, 1, "A " + jeton, enCours, valide);
        UUID deB = fixture.transition(gestionB, 1, "B " + jeton, enCours, valide);
        UUID pieceDeB = fixture.piece(valide, gestionB);
        fixture.passage(pieceDeB, null, enCours, LocalDateTime.of(2022, 2, 1, 9, 0));
        fixture.passage(pieceDeB, deA, valide, LocalDateTime.of(2022, 2, 2, 9, 0));

        List<Map<String, Object>> lignes = transitions();

        assertAll(
                () -> assertThat(ligne(lignes, deA)).isNotNull().containsEntry(NOMBRE_PASSAGES, 1)
                        .containsEntry(DERNIER_PASSAGE, "2022-02-02"),
                () -> assertThat(ligne(lignes, deB)).isNotNull().containsEntry(NOMBRE_PASSAGES, 0));
    }
}
