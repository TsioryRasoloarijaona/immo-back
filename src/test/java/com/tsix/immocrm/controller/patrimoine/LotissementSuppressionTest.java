package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.fixtures.FixtureLotissements;
import com.tsix.immocrm.fixtures.FixtureLotissements.Jeu;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotissements.DELETED;
import static com.tsix.immocrm.fixtures.FixtureLotissements.LOTISSEMENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Suppression d'un lot — FEATURE-patrimoine-lotissements.md §1.4, règles L10, L11 ({@code LotLandList.cs:Delete}).
 * Sans {@code @Transactional} : la suppression et sa ligne d'audit {@code Deleted} sont validées et relues en base.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LotissementSuppressionTest {

    private static final String PAR_ARTICLE = " WHERE StkItemId = ?";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotissements fixture;

    private Jeu j;

    @BeforeEach
    void setUp() {
        j = fixture.preparer("P7CS");
    }

    @AfterEach
    void nettoyer() {
        fixture.nettoyer(j);
    }

    private ResultActions supprimer(UUID id) throws Exception {
        return mockMvc.perform(delete(LOTISSEMENT, id).with(connecte(j.admin())));
    }

    private int lignes(String table, String colonne, UUID id) {
        return fixture.compter("SELECT COUNT(*) FROM " + table + " WHERE " + colonne + " = ?", id.toString());
    }

    /** L10 : 204 ; lot, contenances, tracés de plan, notes et pièces jointes supprimés ; une ligne Deleted de type lot. */
    @Test
    void l10_lotLibre_204_lotEtEnfantsSupprimes_ligneDeleted() throws Exception {
        UUID lot = fixture.lot(j, j.code("SLL"));
        fixture.contenance(j, lot);
        fixture.tracePlan(lot);
        fixture.note(lot);
        fixture.pieceJointe(lot);

        supprimer(lot).andExpect(status().isNoContent());

        assertAll(
                () -> assertThat(fixture.existe(lot)).as("lot").isFalse(),
                () -> assertThat(fixture.compter("SELECT COUNT(*) FROM StkItemContenance" + PAR_ARTICLE, lot.toString()))
                        .as("contenances").isZero(),
                () -> assertThat(fixture.compter("SELECT COUNT(*) FROM StkHierarchyPath" + PAR_ARTICLE, lot.toString()))
                        .as("tracés").isZero(),
                () -> assertThat(lignes("AdmMessage", "CategoryId", lot)).as("notes").isZero(),
                () -> assertThat(lignes("AdmAttachedFile", "CategoryId", lot)).as("pièces jointes").isZero(),
                () -> assertThat(fixture.audit(lot, DELETED)).as("audit Deleted").isEqualTo(1));
    }

    /** L11 : lot affecté à un dossier ⇒ 409, rien supprimé. */
    @Test
    void l11_lotOccupe_409_rienSupprime() throws Exception {
        UUID lot = fixture.lot(j, j.code("SLO"));
        fixture.occuper(j, lot);
        fixture.contenance(j, lot);

        supprimer(lot).andExpect(status().isConflict());

        assertAll(
                () -> assertThat(fixture.existe(lot)).isTrue(),
                () -> assertThat(fixture.compter("SELECT COUNT(*) FROM StkItemContenance" + PAR_ARTICLE, lot.toString()))
                        .isEqualTo(1),
                () -> assertThat(fixture.audit(lot, DELETED)).isZero());
    }

    /** L11 : référencé par une FK non couverte (prospection) ⇒ 409 nommant la cause, rien supprimé. */
    @Test
    void l11_lotCiteParUneProspection_409_messageNommeLaCause_rienSupprime() throws Exception {
        UUID lot = fixture.lot(j, j.code("SLP"));
        fixture.prospection(j, lot);
        fixture.note(lot);

        supprimer(lot)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(matchesPattern("(?is).*(ComProspection|prospection).*")));

        assertAll(
                () -> assertThat(fixture.existe(lot)).isTrue(),
                () -> assertThat(lignes("AdmMessage", "CategoryId", lot)).as("notes gardées").isEqualTo(1));
    }

    /** §1.4 : 404 si ce n'est pas un lot (terrain sans tranche, article) ; rien supprimé. */
    @Test
    void l1_4_terrainSansTranche_404_intact() throws Exception {
        UUID terrain = fixture.terrainSansTranche(j, j.code("STS"));

        supprimer(terrain).andExpect(status().isNotFound());

        assertThat(fixture.existe(terrain)).isTrue();
    }

    @Test
    void l1_4_articleBati_404_intact() throws Exception {
        UUID article = fixture.article(j, j.code("SAB"));

        supprimer(article).andExpect(status().isNotFound());

        assertThat(fixture.existe(article)).isTrue();
    }
}
