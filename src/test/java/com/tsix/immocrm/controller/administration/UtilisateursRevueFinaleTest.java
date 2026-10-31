package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.fixtures.FixtureUtilisateurs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Revue finale de FEATURE-admin-utilisateurs.md : défaut hors des routes du lot, créé par le retrait de {@code AdmUser.pass}. */
@SpringBootTest
@AutoConfigureMockMvc
class UtilisateursRevueFinaleTest {

    private static final String DEFAUT_REVUE = "DF_AdmUser_RevueFinale";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureUtilisateurs fixture;
    @Autowired private JdbcTemplate jdbc;

    private String nom;

    @BeforeEach
    void setUp() {
        nom = "Service." + fixture.preparer();
    }

    @AfterEach
    void nettoyer() {
        jdbc.update("DELETE FROM AppApiClient WHERE Nom = ?", nom);
        fixture.nettoyer();
    }

    /** Pass mappé, l'INSERT posait NULL ; non mappé, le DEFAULT N'' de yesRemsProd et de ci/schema s'applique. */
    @Test
    void r6_compteDeServiceDUneCleApi_passResteNull_sousLeDefautDeProd() throws Exception {
        Integer defauts = jdbc.queryForObject("""
                SELECT COUNT(*) FROM sys.default_constraints
                WHERE parent_object_id = OBJECT_ID('dbo.AdmUser')
                  AND COL_NAME(parent_object_id, parent_column_id) = 'Pass'""", Integer.class);
        boolean pose = defauts != null && defauts == 0;
        if (pose) {
            jdbc.execute("ALTER TABLE dbo.AdmUser ADD CONSTRAINT " + DEFAUT_REVUE + " DEFAULT (N'') FOR Pass");
        }
        Map<String, Object> ligne;
        try {
            mockMvc.perform(post("/api/cles-api").with(connecte(fixture.compte()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nom\":\"%s\",\"portees\":[\"REFERENTIELS_LIRE\"]}".formatted(nom)))
                    .andExpect(status().isCreated());
            ligne = fixture.ligne(fixture.idDe(nom));
        } finally {
            if (pose) {
                jdbc.execute("ALTER TABLE dbo.AdmUser DROP CONSTRAINT " + DEFAUT_REVUE);
            }
        }

        assertThat(ligne).containsEntry("Pass", null);
    }
}
