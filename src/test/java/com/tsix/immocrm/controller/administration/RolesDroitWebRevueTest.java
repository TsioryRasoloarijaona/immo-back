package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.fixtures.FixtureRoles;
import com.tsix.immocrm.fixtures.FixtureRoles.Etat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Décision du manager sur R12 : le lien AdmWebRights, absent de l'énumération de la fixture, refuse aussi la suppression. */
@SpringBootTest
@AutoConfigureMockMvc
class RolesDroitWebRevueTest {

    private static final String COMPTE_LIGNES = "SELECT COUNT(*) FROM AdmWebRights WHERE RoleId = ?";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private FixtureRoles fixture;

    private String jeton;
    private AppelsRoles appels;
    private UUID role;

    @BeforeEach
    void setUp() {
        jeton = fixture.preparer();
        appels = new AppelsRoles(mockMvc, connecte(fixture.compte()));
    }

    @AfterEach
    void nettoyer() {
        if (role != null) {
            jdbc.update("DELETE FROM AdmWebRights WHERE RoleId = ?", role.toString());
        }
        fixture.nettoyer();
    }

    @Test
    void r12_droitWeb_409_causeNommee_rienSupprime() throws Exception {
        role = fixture.role("WEB-" + jeton);
        fixture.droit(role, "HORS-" + jeton);
        jdbc.update("INSERT INTO AdmWebRights (PKey, NodeKey, RoleId) VALUES (?, ?, ?)",
                UUID.randomUUID().toString(), "noeud-" + jeton, role.toString());
        Etat avant = fixture.etat(role);

        appels.supprimer(role).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("droit web")));

        assertThat(fixture.etat(role)).isEqualTo(avant);
        assertThat(jdbc.queryForObject(COMPTE_LIGNES, Integer.class, role.toString())).isEqualTo(1);
    }
}
