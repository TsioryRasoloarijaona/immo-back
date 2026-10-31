package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureUtilisateurs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.CODE;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.DESCRIPTION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Défauts relevés en revue de FEATURE-admin-utilisateurs.md, hors tests verrouillés. */
@SpringBootTest
@AutoConfigureMockMvc
class UtilisateursRevueTest {

    private static final String LIBELLE = "Libelle compte";
    private static final String DEFAUT_REVUE = "DF_AdmUser_Revue";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureUtilisateurs fixture;
    @Autowired private JdbcTemplate jdbc;

    private String jeton;
    private AppelsUtilisateurs appels;
    private AdmUser admin;

    @BeforeEach
    void setUp() {
        jeton = fixture.preparer();
        admin = fixture.compte();
        appels = new AppelsUtilisateurs(mockMvc, connecte(admin));
    }

    @AfterEach
    void nettoyer() {
        fixture.nettoyer();
    }

    private static CorpsJson corpsAvec(String code) {
        return corps().avec(CODE, code).avec(DESCRIPTION, LIBELLE);
    }

    private List<String> codesCrees() {
        return fixture.codesDuJeton().stream().filter(c -> !c.equals(admin.getCode()))
                .map(c -> c.toUpperCase(Locale.ROOT)).toList();
    }

    /** yesRemsProd et ci/schema portent DEFAULT N'' sur Pass : posé ici si la base locale ne l'a pas. */
    @Test
    void r6_passResteNull_sousLeDefautDeProd() throws Exception {
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
            String code = "Pass." + jeton;
            appels.creer(corpsAvec(code)).andExpect(status().isCreated());
            ligne = fixture.ligne(fixture.idDe(code));
        } finally {
            if (pose) {
                jdbc.execute("ALTER TABLE dbo.AdmUser DROP CONSTRAINT " + DEFAUT_REVUE);
            }
        }

        assertThat(ligne).containsEntry("Pass", null);
    }

    /** Code est un varchar CP1252 : deux codes arabes distincts s'y écrivent à l'identique. */
    @Test
    void r3_deuxCodesHorsCp1252_neDonnentPasDeuxLignesIdentiques() throws Exception {
        appels.creer(corpsAvec("سارة." + jeton));
        appels.creer(corpsAvec("مريم." + jeton));

        assertThat(codesCrees()).doesNotHaveDuplicates();
    }

    /** La conversion vers CP1252 remplace le a macron par a : sosie de Sarah une fois écrit. */
    @Test
    void r3_sosieRameneParLaConversionCp1252_refuse() throws Exception {
        appels.creer(corpsAvec("Sarah." + jeton)).andExpect(status().isCreated());

        appels.creer(corpsAvec("Sarāh." + jeton));

        assertThat(codesCrees()).doesNotHaveDuplicates();
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", " "})
    void r5_descriptionFaiteDeBlancsInsecables_400_auPost(String blanc) throws Exception {
        String code = "Blanche." + jeton;

        appels.creer(corps().avec(CODE, code).avec(DESCRIPTION, blanc)).andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", " "})
    void reponse12_descriptionFaiteDeBlancsInsecables_400_auPatch(String blanc) throws Exception {
        AdmUser cible = fixture.compte();

        appels.modifier(cible.getId(), corps().avec(DESCRIPTION, blanc)).andExpect(status().isBadRequest());
    }
}
