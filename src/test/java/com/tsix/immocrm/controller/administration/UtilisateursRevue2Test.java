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

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.CODE;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.DESCRIPTION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Seconde revue du correctif de FEATURE-admin-utilisateurs.md, R3 et R6. */
@SpringBootTest
@AutoConfigureMockMvc
class UtilisateursRevue2Test {

    private static final String LIBELLE = "Libelle compte";
    private static final String COLLATION_PROD = "SQL_Latin1_General_CP1_CS_AS";
    private static final String CHANGER_COLLATION = "ALTER TABLE dbo.AdmUser ALTER COLUMN Code varchar(max) COLLATE %s NOT NULL";

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

    /**
     * UPPER laisse U+0192 (dans CP1252) tel quel en varchar et le passe à U+0191 en nvarchar : colonne et paramètre
     * divergent, la comparaison CS_AS ne voit pas le doublon. yesRemsTest est en French_CI_AS, alignée ici sur prod et CI.
     */
    @Test
    void r3_memeCodeAvecFFlorin_409_sousLaCollationDeProd() throws Exception {
        String code = "ƒlorin." + jeton;
        String collation = jdbc.queryForObject("""
                SELECT collation_name FROM sys.columns
                WHERE object_id = OBJECT_ID('dbo.AdmUser') AND name = 'Code'""", String.class);
        boolean aligner = !COLLATION_PROD.equals(collation);
        if (aligner) {
            jdbc.execute(CHANGER_COLLATION.formatted(COLLATION_PROD));
        }
        List<String> codes;
        try {
            appels.creer(corpsAvec(code)).andExpect(status().isCreated());
            appels.creer(corpsAvec(code));
            codes = fixture.codesDuJeton().stream().filter(code::equals).toList();
        } finally {
            if (aligner) {
                jdbc.execute(CHANGER_COLLATION.formatted(collation));
            }
        }

        assertThat(codes).hasSize(1);
    }

    /** U+0080 revient en euro depuis CP1252 ; les 5 autres sont les positions non définies, gardées par SQL Server. */
    @ParameterizedTest
    @ValueSource(strings = {"\u0080", "\u0081", "\u008D", "\u008F", "\u0090", "\u009D"})
    void r3_positionsHorsPage_400_sansLigne(String caractere) throws Exception {
        String code = "Hors" + caractere + "page." + jeton;

        appels.creer(corpsAvec(code)).andExpect(status().isBadRequest());

        assertThat(fixture.codesDuJeton()).containsExactly(admin.getCode());
    }

    @Test
    void r17_flushAnticipe_uneSeuleTraceAdded_etFicheRendue() throws Exception {
        String code = "Audit." + jeton;

        appels.creer(corpsAvec(code)).andExpect(status().isCreated()).andExpect(jsonPath("$.code").value(code));

        assertThat(fixture.traces(fixture.idDe(code), "Added")).hasSize(1);
    }
}
