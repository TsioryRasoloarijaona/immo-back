package com.tsix.immocrm.controller.referentiel;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureReferentielsAdmin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.CODE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.LIGNE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.TABLE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.TRANCHE_ID;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.TYPES_PRESENTATION;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.jeton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Types de présentation {@code StkPresentationType} et leurs contenances — FEATURE-patrimoine-referentiels.md §2 (Code
 * O U* + tranche O U*) et §8.1 (les contenances {@code StkPresentationTypeContenance} sont supprimées avec le parent).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PresentationTypeContenanceTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureReferentielsAdmin fixture;

    private AdmUser admin;
    private String jeton;
    private CfgTranche tranche;

    @BeforeEach
    void setUp() {
        admin = patrimoine.administrateur();
        jeton = jeton();
        tranche = tranche("TP-" + jeton);
    }

    private CfgTranche tranche(String code) {
        return patrimoine.tranche(code, patrimoine.projet("PRJ-" + code, patrimoine.entite("EJ-" + code, true)));
    }

    private static CorpsJson corpsPresentation(String code, UUID trancheId) {
        return corps().avec(CODE, code).avec(DESCRIPTION, "Presentation " + code).avec(TRANCHE_ID, trancheId);
    }

    private ResultActions poster(CorpsJson corps) throws Exception {
        return mockMvc.perform(post(TABLE, TYPES_PRESENTATION).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    @Test
    void s2_creation_201_trancheEcrite() throws Exception {
        String json = poster(corpsPresentation("PRS-" + jeton, tranche.getId())).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(JsonPath.read(json, "$.id"));

        assertThat(fixture.compter("SELECT COUNT(*) FROM StkPresentationType WHERE PKey = :id AND StkHierarchyId = '"
                + tranche.getId() + "' AND Code = N'PRS-" + jeton + "'", id)).isEqualTo(1);
    }

    @Test
    void s1_uniciteComposite_409_memeCodeMemeTranche() throws Exception {
        fixture.presentation(tranche.getId(), "PRS-" + jeton);

        poster(corpsPresentation("PRS-" + jeton, tranche.getId())).andExpect(status().isConflict());
    }

    @Test
    void s1_uniciteComposite_201_memeCodeAutreTranche() throws Exception {
        fixture.presentation(tranche.getId(), "PRS-" + jeton);

        poster(corpsPresentation("PRS-" + jeton, tranche("TP2-" + jeton).getId())).andExpect(status().isCreated());
    }

    @Test
    void s2_requis_400_sansTranche() throws Exception {
        poster(corpsPresentation("PRS-" + jeton, null).sans(TRANCHE_ID)).andExpect(status().isBadRequest());
    }

    /** §8.1 : seule cascade admise — les contenances de la présentation partent avec elle. */
    @Test
    void r8_1_suppression_204_contenancesSupprimeesAvecLaPresentation() throws Exception {
        UUID presentation = fixture.presentation(tranche.getId(), "PRS-" + jeton);
        UUID ligne1 = fixture.contenanceDePresentation(presentation, fixture.contenance("CT1-" + jeton));
        UUID ligne2 = fixture.contenanceDePresentation(presentation, fixture.contenance("CT2-" + jeton));

        mockMvc.perform(delete(LIGNE, TYPES_PRESENTATION, presentation).with(connecte(admin)))
                .andExpect(status().isNoContent());

        assertThat(fixture.lignes("StkPresentationType", presentation)
                + fixture.lignes("StkPresentationTypeContenance", ligne1)
                + fixture.lignes("StkPresentationTypeContenance", ligne2)).isZero();
    }
}
