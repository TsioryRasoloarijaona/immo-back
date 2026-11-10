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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.LectureConfigurationAchat.STATUTS;
import static com.tsix.immocrm.controller.achat.LectureConfigurationAchat.ligne;
import static com.tsix.immocrm.controller.achat.LectureConfigurationAchat.lire;
import static com.tsix.immocrm.controller.achat.LectureConfigurationAchat.rangs;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/** GET /api/achat/config/statuts — FEATURE-achat-regle-commune.md §4.1 ({@code StatutAchatConfigDto}). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ConfigurationAchatStatutsTest {

    private static final String NOMBRE_PIECES = "nombrePieces";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureConfigurationAchat fixture;
    @Autowired private FixturePatrimoine patrimoine;

    private String jeton;
    private RequestPostProcessor qui;

    @BeforeEach
    void setUp() {
        jeton = FixtureConfigurationAchat.jeton("A4S");
        qui = connecte(patrimoine.administrateur());
    }

    private List<Map<String, Object>> statuts() throws Exception {
        return lire(mockMvc, STATUTS, qui);
    }

    /** §4.1 : chaque champ du DTO lu de sa colonne ; categorie = libellé AdmWorkFlowStateType. */
    @Test
    void statut_champsLusDeLeursColonnes() throws Exception {
        String categorie = "Términé avec succès " + jeton;
        UUID type = fixture.typeEtat(categorie);
        UUID id = fixture.statut(new Statut("VAL-" + jeton, "Validé " + jeton, 7, type, true, true, true,
                "LightGreen", 15));

        Map<String, Object> s = ligne(statuts(), id);

        assertThat(s).isNotNull()
                .containsEntry("code", "VAL-" + jeton)
                .containsEntry("description", "Validé " + jeton)
                .containsEntry("ordre", 7)
                .containsEntry("categorie", categorie)
                .containsEntry("couleur", "LightGreen")
                .containsEntry("okPourPaiement", true)
                .containsEntry("valideComptabilite", true)
                .containsEntry("lectureSeule", true)
                .containsEntry("dureeVieJours", 15)
                .containsEntry(NOMBRE_PIECES, 0);
    }

    /** §4.1 : booléens NULL → false ; sans catégorie → categorie null. */
    @Test
    void statut_booleensNull_false_sansCategorie_null() throws Exception {
        UUID id = fixture.statut(Statut.simple("NUL-" + jeton, 1));

        Map<String, Object> s = ligne(statuts(), id);

        assertThat(s).isNotNull()
                .containsEntry("okPourPaiement", false)
                .containsEntry("valideComptabilite", false)
                .containsEntry("lectureSeule", false)
                .containsEntry("categorie", null)
                .containsEntry("couleur", null);
    }

    /** §4.1 : tri Sorting puis Code. */
    @Test
    void statuts_triSortingPuisCode() throws Exception {
        UUID b = fixture.statut(Statut.simple("B-" + jeton, 5));
        UUID a = fixture.statut(Statut.simple("A-" + jeton, 5));
        UUID z = fixture.statut(Statut.simple("Z-" + jeton, 3));

        List<Integer> rangs = rangs(statuts(), z, a, b);

        assertThat(rangs).doesNotContain(-1).isSorted();
    }

    /** §4.1 : nombrePieces = pièces portant ce statut aujourd'hui. */
    @Test
    void statut_nombrePieces_piecesPortantLeStatut() throws Exception {
        UUID porte = fixture.statut(Statut.simple("P-" + jeton, 1));
        UUID vide = fixture.statut(Statut.simple("V-" + jeton, 1));
        fixture.piece(porte, null);
        fixture.piece(porte, null);

        List<Map<String, Object>> statuts = statuts();

        assertAll(
                () -> assertThat(ligne(statuts, porte)).isNotNull().containsEntry(NOMBRE_PIECES, 2),
                () -> assertThat(ligne(statuts, vide)).isNotNull().containsEntry(NOMBRE_PIECES, 0));
    }
}
