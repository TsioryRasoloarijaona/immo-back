package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.fixtures.FixtureRoles;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureRoles.CODE;
import static com.tsix.immocrm.fixtures.FixtureRoles.DESCRIPTION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Règle 14 de FEATURE-admin-roles.md. Sans transaction de test : la trace n'existe qu'après validation. */
@SpringBootTest
@AutoConfigureMockMvc
class RolesAuditTest {

    private static final String AJOUT = "Added";
    private static final String MODIFICATION = "Modified";
    private static final String SUPPRESSION = "Deleted";

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

    @Test
    void r14_creation_tracee() throws Exception {
        String code = "AUD-" + jeton;

        appels.creer(corps().avec(CODE, code)).andExpect(status().isCreated());

        assertThat(fixture.traces(fixture.idDe(code), AJOUT)).hasSize(1);
    }

    @Test
    void r14_modification_tracee() throws Exception {
        UUID role = fixture.role("AUD-" + jeton);

        appels.modifier(role, corps().avec(DESCRIPTION, "Modifiee")).andExpect(status().isOk());

        assertThat(fixture.traces(role, MODIFICATION)).hasSize(1);
    }

    @Test
    void r14_suppression_tracee() throws Exception {
        UUID role = fixture.role("AUD-" + jeton);

        appels.supprimer(role).andExpect(status().isNoContent());

        assertThat(fixture.traces(role, SUPPRESSION)).hasSize(1);
    }

    /** Trois ajoutés, deux retirés : des nombres distincts, pour qu'un inversé ou un total ne passe pas pour le bon texte. */
    @Test
    void r14_droits_traceLesNombres_pasLaListe() throws Exception {
        UUID role = fixture.role("AUD-" + jeton);
        List<String> ajoutes = List.of("AJ1-" + jeton, "AJ2-" + jeton, "AJ3-" + jeton);
        List<String> retires = List.of("RE1-" + jeton, "RE2-" + jeton);
        ajoutes.forEach(fixture::noeud);
        retires.forEach(fixture::noeud);
        retires.forEach(code -> fixture.droit(role, code));

        appels.droits(role, ajoutes).andExpect(status().isOk());

        assertThat(String.join(" ", fixture.traces(role, MODIFICATION)))
                .containsPattern("(?<!\\d)3(?!\\d)").containsPattern("(?<!\\d)2(?!\\d)").doesNotContain(jeton);
    }

    /** « Pas la liste » : aucune trace ligne à ligne sur {@code AdmRight}. */
    @Test
    void r14_droits_aucuneTraceParLigne() throws Exception {
        UUID role = fixture.role("AUD-" + jeton);
        String code = "AJ-" + jeton;
        fixture.noeud(code);

        appels.droits(role, List.of(code)).andExpect(status().isOk());

        assertThat(fixture.tracesObjet(fixture.lignesDroit(role, code).getFirst())).isZero();
    }

    /** Témoin : la même écriture acceptée est tracée (voir {@link #r14_modification_tracee}). */
    @Test
    void r14_refus_pasDeTrace() throws Exception {
        UUID role = fixture.role("AUD-" + jeton);

        appels.droits(role, List.of("INCONNU-" + jeton)).andExpect(status().isBadRequest());

        assertThat(fixture.traces(role, MODIFICATION)).isEmpty();
    }
}
