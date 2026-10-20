package com.tsix.immocrm.controller.referentiel;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureReferentielsAdmin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.stream.Stream;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.CODE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.LIGNE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.NATURES;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.NATURE_TER;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.VOCATIONS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lignes protégées — FEATURE-patrimoine-referentiels.md §8 (écarts du §5) : vocations VENTE, PROJET et natures BAT, TER
 * (code et PKey) refusent la suppression et le changement de code (409) ; la protection tient par PKey comme par code.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReferentielAdminProtegesTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureReferentielsAdmin fixture;

    private AdmUser admin;

    @BeforeEach
    void setUp() {
        admin = patrimoine.administrateur();
        patrimoine.vocationVente();
        patrimoine.vocationProjet();
        patrimoine.natureBat();
        fixture.nature(NATURE_TER, "TER");
    }

    record Protegee(String slug, String table, UUID id, String code) {
        @Override
        public String toString() {
            return slug + " " + code;
        }
    }

    static Stream<Protegee> protegees() {
        return Stream.of(
                new Protegee(VOCATIONS, "StkVocation", FixturePatrimoine.VOCATION_VENTE, "VENTE"),
                new Protegee(VOCATIONS, "StkVocation", FixturePatrimoine.VOCATION_PROJET, "PROJET"),
                new Protegee(NATURES, "StkKind", FixturePatrimoine.NATURE_BAT, "BAT"),
                new Protegee(NATURES, "StkKind", NATURE_TER, "TER"));
    }

    private int lignesDeCode(Protegee p, String code) {
        return fixture.compter("SELECT COUNT(*) FROM " + p.table() + " WHERE PKey = :id AND Code = N'" + code + "'", p.id());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("protegees")
    void r8_protegee_suppression_409_conservee(Protegee p) throws Exception {
        mockMvc.perform(delete(LIGNE, p.slug(), p.id()).with(connecte(admin))).andExpect(status().isConflict());

        assertThat(lignesDeCode(p, p.code())).isEqualTo(1);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("protegees")
    void r8_protegee_changementDeCode_409_codeConserve(Protegee p) throws Exception {
        mockMvc.perform(patch(LIGNE, p.slug(), p.id()).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(corps().avec(CODE, p.code() + "-RECODE").json()))
                .andExpect(status().isConflict());

        assertThat(lignesDeCode(p, p.code())).isEqualTo(1);
    }

    /** Protection par PKey : la ligne VENTE renommée en base reste protégée. */
    @Test
    void r8_protegeeParPKey_vocationVenteRenommee_suppression409() throws Exception {
        fixture.recoder("StkVocation", FixturePatrimoine.VOCATION_VENTE, "VENTE-RENOMMEE");

        mockMvc.perform(delete(LIGNE, VOCATIONS, FixturePatrimoine.VOCATION_VENTE).with(connecte(admin)))
                .andExpect(status().isConflict());

        assertThat(fixture.lignes("StkVocation", FixturePatrimoine.VOCATION_VENTE)).isEqualTo(1);
    }

    /** Protection par code : une nature « TER » hors de la PKey de production reste protégée. */
    @Test
    void r8_protegeeParCode_natureTerAutrePKey_suppression409() throws Exception {
        UUID autre = fixture.nature("TER");

        mockMvc.perform(delete(LIGNE, NATURES, autre).with(connecte(admin))).andExpect(status().isConflict());

        assertThat(fixture.lignes("StkKind", autre)).isEqualTo(1);
    }

    /** Protection par code : une vocation « PROJET » hors de la PKey de production refuse le changement de code. */
    @Test
    void r8_protegeeParCode_vocationProjetAutrePKey_changementDeCode409() throws Exception {
        UUID autre = fixture.vocation("PROJET");

        mockMvc.perform(patch(LIGNE, VOCATIONS, autre).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(corps().avec(CODE, "PROJET-RECODE").json()))
                .andExpect(status().isConflict());

        assertThat(fixture.compter("SELECT COUNT(*) FROM StkVocation WHERE PKey = :id AND Code = N'PROJET'", autre))
                .isEqualTo(1);
    }
}
