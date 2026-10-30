package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.fixtures.FixtureRoles;
import com.tsix.immocrm.fixtures.FixtureRoles.Etat;
import com.tsix.immocrm.fixtures.FixtureRoles.Lien;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Suppression — FEATURE-admin-roles.md, règles 12 et 13. */
@SpringBootTest
@AutoConfigureMockMvc
class RolesSuppressionTest {

    private static final UUID INCONNU = UUID.fromString("3A4B5C6D-7E8F-4091-A2B3-C4D5E6F70819");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRoles fixture;

    private String jeton;
    private AppelsRoles appels;

    @BeforeEach
    void setUp() {
        jeton = fixture.preparer();
        appels = new AppelsRoles(mockMvc, connecte(fixture.compte()));
    }

    @AfterEach
    void nettoyer() {
        fixture.nettoyer();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Lien.class)
    void r12_roleLie_409_causeNommee_rienSupprime(Lien lien) throws Exception {
        UUID role = fixture.role("LIE-" + jeton);
        fixture.droit(role, "HORS-" + jeton);
        fixture.lier(lien, role);
        Etat avant = fixture.etat(role);

        appels.supprimer(role).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsStringIgnoringCase(lien.cause())));

        assertThat(fixture.etat(role)).isEqualTo(avant);
        assertThat(fixture.lignesLien(lien, role)).isEqualTo(1);
    }

    @Test
    void r13_roleLibre_204_roleEtDroitsDisparaissent() throws Exception {
        UUID role = fixture.role("LIB-" + jeton);
        fixture.droit(role, "A-" + jeton);
        fixture.droit(role, "HORS-" + jeton);

        appels.supprimer(role).andExpect(status().isNoContent());

        assertThat(fixture.rolesPresents(role)).isZero();
        assertThat(fixture.codesDroits(role)).isEmpty();
    }

    @Test
    void r13_autresRoles_intacts() throws Exception {
        String memeCode = "A-" + jeton;
        UUID role = fixture.role("LIB-" + jeton);
        fixture.droit(role, memeCode);
        UUID autre = fixture.role("AUT-" + jeton);
        fixture.droit(autre, memeCode);
        Etat avant = fixture.etat(autre);

        appels.supprimer(role).andExpect(status().isNoContent());

        assertThat(fixture.etat(autre)).isEqualTo(avant);
    }

    @Test
    void suppression_roleInconnu_404() throws Exception {
        UUID role = fixture.role("LIB-" + jeton);
        appels.supprimer(role).andExpect(status().isNoContent());

        appels.supprimer(INCONNU).andExpect(status().isNotFound());
    }
}
