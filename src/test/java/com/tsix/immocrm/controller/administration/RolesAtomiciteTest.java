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
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureRoles.CODE;
import static com.tsix.immocrm.fixtures.FixtureRoles.DESCRIPTION;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Règle 16 de FEATURE-admin-roles.md. Un déclencheur de test fait échouer en base toute ligne au code {@code ECHEC-R16…},
 * sans dépendre de la façon d'écrire du lot. Il ne vise que ce préfixe, et il est retiré avant le nettoyage.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RolesAtomiciteTest {

    private static final String PREFIXE = "ECHEC-R16-";
    private static final String REFUS_DROIT = """
            CREATE OR ALTER TRIGGER dbo.trgTestRolesRefusDroit ON dbo.AdmRight AFTER INSERT, DELETE AS
            IF EXISTS (SELECT 1 FROM inserted WHERE Code LIKE 'ECHEC-R16-%')
               OR EXISTS (SELECT 1 FROM deleted WHERE Code LIKE 'ECHEC-R16-%')
                THROW 50000, 'Refus de test R16', 1;""";
    private static final String REFUS_ROLE = """
            CREATE OR ALTER TRIGGER dbo.trgTestRolesRefusRole ON dbo.AdmRole AFTER INSERT, UPDATE, DELETE AS
            IF EXISTS (SELECT 1 FROM inserted WHERE Code LIKE 'ECHEC-R16-%')
               OR EXISTS (SELECT 1 FROM deleted WHERE Code LIKE 'ECHEC-R16-%')
                THROW 50000, 'Refus de test R16', 1;""";
    private static final String RETRAIT_DROIT = "DROP TRIGGER IF EXISTS dbo.trgTestRolesRefusDroit";
    private static final String RETRAIT_ROLE = "DROP TRIGGER IF EXISTS dbo.trgTestRolesRefusRole";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
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
        jdbc.execute(RETRAIT_DROIT);
        jdbc.execute(RETRAIT_ROLE);
        fixture.nettoyer();
    }

    /** Une panne en base, pas un 404 de route absente ni une réponse de succès. */
    private static void enEchec(ResultActions reponse) {
        assertThat(reponse.andReturn().getResponse().getStatus()).isGreaterThanOrEqualTo(400).isNotEqualTo(404);
    }

    private String noeud(String prefixe) {
        String code = prefixe + jeton;
        fixture.noeud(code);
        return code;
    }

    @Test
    void r16_put_insertionEnEchec_retraitsAnnules() throws Exception {
        UUID role = fixture.role("PUT-" + jeton);
        String garde = noeud("GAR-");
        fixture.droit(role, garde);
        fixture.droit(role, noeud("RET-"));
        List<String> coches = List.of(garde, noeud("NEW-"), noeud(PREFIXE));
        Etat avant = fixture.etat(role);
        jdbc.execute(REFUS_DROIT);

        enEchec(appels.droits(role, coches));

        assertThat(fixture.etat(role)).isEqualTo(avant);
        assertThat(fixture.traces(role, "Modified")).isEmpty();
    }

    @Test
    void r16_put_retraitEnEchec_insertionsAnnulees() throws Exception {
        UUID role = fixture.role("PUT-" + jeton);
        fixture.droit(role, noeud(PREFIXE));
        fixture.droit(role, noeud("RET-"));
        List<String> coches = List.of(noeud("NEW-"));
        Etat avant = fixture.etat(role);
        jdbc.execute(REFUS_DROIT);

        enEchec(appels.droits(role, coches));

        assertThat(fixture.etat(role)).isEqualTo(avant);
    }

    @Test
    void r16_delete_retraitDroitEnEchec_roleConserve() throws Exception {
        UUID role = fixture.role("DEL-" + jeton);
        fixture.droit(role, "A-" + jeton);
        fixture.droit(role, PREFIXE + jeton);
        Etat avant = fixture.etat(role);
        jdbc.execute(REFUS_DROIT);

        enEchec(appels.supprimer(role));

        assertThat(fixture.etat(role)).isEqualTo(avant);
    }

    @Test
    void r16_delete_retraitRoleEnEchec_droitsConserves() throws Exception {
        UUID role = fixture.role(PREFIXE + jeton);
        fixture.droit(role, "A-" + jeton);
        fixture.droit(role, "B-" + jeton);
        Etat avant = fixture.etat(role);
        jdbc.execute(REFUS_ROLE);

        enEchec(appels.supprimer(role));

        assertThat(fixture.etat(role)).isEqualTo(avant);
        assertThat(fixture.traces(role, "Deleted")).isEmpty();
    }

    @Test
    void r16_post_enEchec_rienNiTrace() throws Exception {
        String code = PREFIXE + jeton;
        jdbc.execute(REFUS_ROLE);

        enEchec(appels.creer(corps().avec(CODE, code)));

        assertThat(fixture.nombreRoles(code)).isZero();
        assertThat(fixture.tracesDuJeton()).isZero();
    }

    @Test
    void r16_patch_enEchec_rienNiTrace() throws Exception {
        UUID role = fixture.role(PREFIXE + jeton, "Depart");
        Etat avant = fixture.etat(role);
        jdbc.execute(REFUS_ROLE);

        enEchec(appels.modifier(role, corps().avec(DESCRIPTION, "Nouvelle")));

        assertThat(fixture.etat(role)).isEqualTo(avant);
        assertThat(fixture.traces(role, "Modified")).isEmpty();
    }
}
