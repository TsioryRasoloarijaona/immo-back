package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureTrancheFiche;
import com.tsix.immocrm.fixtures.FixtureTrancheOnglets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Suppression de tranche — FEATURE-patrimoine-tranche-onglets.md §4, §8.5 (SuppressionTrancheTest) : 409 tant qu'un
 * article la référence, sinon 204 avec les cascades OMRAN faites par l'application (yesRemsTest n'a pas de FK).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SuppressionTrancheTest {

    private static final String TRANCHE = "/api/tranches/{id}";
    private static final String TRANCHE_EXISTE = "SELECT COUNT(*) FROM CfgTranche WHERE PKey = :id";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureTrancheFiche fiche;
    @Autowired private FixtureTrancheOnglets onglets;

    private AdmUser admin;
    private CfgTranche tranche;
    private String jeton;

    @BeforeEach
    void setUp() {
        admin = fixture.administrateur();
        jeton = "P1BS" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        tranche = fixture.tranche("TR-" + jeton, fixture.projet("PRJ-" + jeton, fixture.entite("ENT-" + jeton, true)));
    }

    private ResultActions supprimer(UUID id) throws Exception {
        return mockMvc.perform(delete(TRANCHE, id).with(connecte(admin)));
    }

    private int compter(String sql) {
        return fiche.compter(sql, tranche.getId());
    }

    // ------------------------------------------------------------------ §4 bloquants

    @Test
    void r4_409_articleDeLaTranche_messageListeLeBloquantEtSonNombre() throws Exception {
        onglets.article("ART-" + jeton, tranche.getId(), fixture.natureBat(), fixture.vocationVente());

        supprimer(tranche.getId())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        matchesPattern("(?is).*(\\b1\\b.{0,40}(StkItem|article)|(StkItem|article).{0,40}\\b1\\b).*")));

        assertThat(compter(TRANCHE_EXISTE)).isEqualTo(1);
    }

    // ------------------------------------------------------------------ §4 / §8.5 204 et cascades

    @Test
    void r4_204_trancheVideAvecTerrains_liensSupprimes_terrainGarde() throws Exception {
        UUID terrain = onglets.terrain("TER-" + jeton);
        onglets.lienTerrain(tranche.getId(), terrain, BigDecimal.ONE, 1);

        supprimer(tranche.getId()).andExpect(status().isNoContent());

        assertAll(
                () -> assertThat(compter(TRANCHE_EXISTE)).isZero(),
                () -> assertThat(compter("SELECT COUNT(*) FROM CfgTrancheStkItem WHERE CfgTrancheId = :id")).isZero(),
                () -> assertThat(fiche.compter("SELECT COUNT(*) FROM StkItem WHERE PKey = :id", terrain)).isEqualTo(1));
    }

    @Test
    void r8_5_204_ongletsSupprimes() throws Exception {
        onglets.ligneEtage(tranche.getId(), fiche.niveauTerrain("NIV-" + jeton));
        onglets.lignePlancher(tranche.getId(), fixture.categorie("CAT-" + jeton, fixture.natureBat(), false).getId(),
                BigDecimal.TEN);
        onglets.ligneFrais(tranche.getId(), onglets.categorieReglement("FR-" + jeton, true, true), BigDecimal.TEN,
                BigDecimal.ZERO, BigDecimal.TEN);

        supprimer(tranche.getId()).andExpect(status().isNoContent());

        assertAll(
                () -> assertThat(compter("SELECT COUNT(*) FROM CfgTrancheLevel WHERE CfgTrancheId = :id")).isZero(),
                () -> assertThat(compter("SELECT COUNT(*) FROM CfgTrancheCoveredFloor WHERE CfgTrancheId = :id")).isZero(),
                () -> assertThat(compter("SELECT COUNT(*) FROM CfgTrancheSettlementCategory WHERE CfgtrancheId = :id"))
                        .isZero());
    }

    @Test
    void r8_5_204_habilitationsSurLaTranchePartent() throws Exception {
        onglets.habilitationTranche(fixture.administrateur().getId(), tranche.getId());

        supprimer(tranche.getId()).andExpect(status().isNoContent());

        assertThat(compter("SELECT COUNT(*) FROM AdmUserHabilitation WHERE CfgTrancheId = :id")).isZero();
    }

    @Test
    void r4_204_piecesJointesEtNotesDeLaTrancheSupprimees() throws Exception {
        fiche.pieceJointe(tranche.getId());
        fiche.message(tranche.getId());

        supprimer(tranche.getId()).andExpect(status().isNoContent());

        assertAll(
                () -> assertThat(compter("SELECT COUNT(*) FROM AdmAttachedFile WHERE CategoryId = :id")).isZero(),
                () -> assertThat(compter("SELECT COUNT(*) FROM AdmMessage WHERE CategoryId = :id")).isZero());
    }

    // ------------------------------------------------------------------ §4 refus

    @Test
    void r4_404_trancheInconnue() throws Exception {
        supprimer(UUID.randomUUID()).andExpect(status().isNotFound());
    }
}
