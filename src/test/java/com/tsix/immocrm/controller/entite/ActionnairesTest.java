package com.tsix.immocrm.controller.entite;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureEntites;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Actionnaires {@code /api/actionnaires} — FEATURE-entites-juridiques.md §2 (« Actionnaire : Code O U, Description O,
 * Notes » ; CRUD, DELETE 204/409 FK) (ActionnairesTest).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ActionnairesTest {

    private static final String ACTIONNAIRES = "/api/actionnaires";
    private static final String ACTIONNAIRE = "/api/actionnaires/{id}";
    private static final String CODE = "code";
    private static final String DESCRIPTION = "description";
    private static final String NOTES = "notes";
    private static final String COMPTE = "SELECT COUNT(*) FROM CfgShareholder WHERE PKey = :id";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureEntites entites;

    private AdmUser admin;
    private String jeton;

    @BeforeEach
    void setUp() {
        admin = fixture.administrateur();
        jeton = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private ResultActions creer(CorpsJson corps) throws Exception {
        return mockMvc.perform(post(ACTIONNAIRES).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    private CorpsJson corpsComplet(String code) {
        return corps().avec(CODE, code).avec(DESCRIPTION, "Actionnaire " + code).avec(NOTES, "Notes " + code);
    }

    @Test
    void r2_creation_201_codeDescriptionNotesEcrits() throws Exception {
        String code = "ACT-CRE-" + jeton;

        String json = creer(corpsComplet(code)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(JsonPath.read(json, "$.id"));

        assertThat(entites.compter("SELECT COUNT(*) FROM CfgShareholder WHERE PKey = :id AND Code = N'" + code
                + "' AND Description = N'Actionnaire " + code + "' AND Notes = N'Notes " + code + "'", id)).isEqualTo(1);
    }

    @Test
    void r2_creation_400_sansCode() throws Exception {
        creer(corpsComplet("x").sans(CODE)).andExpect(status().isBadRequest());
    }

    @Test
    void r2_creation_400_sansDescription() throws Exception {
        creer(corpsComplet("ACT-SD-" + jeton).sans(DESCRIPTION)).andExpect(status().isBadRequest());
    }

    @Test
    void r2_fiche_200_et_liste() throws Exception {
        UUID id = entites.actionnaire("ACT-FI-" + jeton);

        mockMvc.perform(get(ACTIONNAIRE, id).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.code").value("ACT-FI-" + jeton))
                .andExpect(jsonPath("$.description").value(FixtureEntites.libelle("ACT-FI-" + jeton)));
        mockMvc.perform(get(ACTIONNAIRES).with(connecte(admin)).param("size", "2000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$..[?(@.id=='" + id + "')]").exists());
    }

    @Test
    void r2_fiche_404_idInconnu() throws Exception {
        mockMvc.perform(get(ACTIONNAIRE, UUID.randomUUID()).with(connecte(admin))).andExpect(status().isNotFound());
    }

    @Test
    void r2_patch_200_descriptionModifiee() throws Exception {
        UUID id = entites.actionnaire("ACT-MOD-" + jeton);

        mockMvc.perform(patch(ACTIONNAIRE, id).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(corps().avec(DESCRIPTION, "Nouveau nom " + jeton).json()))
                .andExpect(status().isOk());

        assertThat(entites.compter("SELECT COUNT(*) FROM CfgShareholder WHERE PKey = :id AND Description = N'Nouveau nom "
                + jeton + "'", id)).isEqualTo(1);
    }

    @Test
    void r2_suppression_204() throws Exception {
        UUID id = entites.actionnaire("ACT-DEL-" + jeton);

        mockMvc.perform(delete(ACTIONNAIRE, id).with(connecte(admin))).andExpect(status().isNoContent());

        assertThat(entites.compter(COMPTE, id)).isZero();
    }

    /** {@code CfgCompanyShareholder → CfgShareholder} NO_ACTION en production. */
    @Test
    void r2_suppression_409_actionnaireDansUneRepartition_conserve() throws Exception {
        UUID id = entites.actionnaire("ACT-409-" + jeton);
        CfgCompany entite = fixture.entite("EJ-ACT409-" + jeton, true);
        entites.repartition(entite.getId(), id, 10);

        mockMvc.perform(delete(ACTIONNAIRE, id).with(connecte(admin))).andExpect(status().isConflict());

        assertThat(entites.compter(COMPTE, id)).isEqualTo(1);
    }

    @Test
    void r2_suppression_404_idInconnu() throws Exception {
        mockMvc.perform(delete(ACTIONNAIRE, UUID.randomUUID()).with(connecte(admin))).andExpect(status().isNotFound());
    }
}
