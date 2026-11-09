package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureReferentielsAdmin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureProjetConfig.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Règle 1 (FEATURE-projet-config.md §2.1, §7.1) : projet et habilitation du créateur dans la même transaction.
 * Sans {@code @Transactional} : les lignes sont validées pour de bon, un déclencheur de test fait échouer l'habilitation,
 * tout est retiré en {@code @AfterEach}. Les lignes de prod StrEntity « Projet » et statut « Nouveau » restent.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProjetConfigTransactionTest {

    private static final String CODE_ECHEC = "RB-ECHEC-HABILITATION";
    private static final String RETRAIT_DECLENCHEUR = """
            IF OBJECT_ID('dbo.trgTestEchecHabilitationProjet', 'TR') IS NOT NULL
                DROP TRIGGER dbo.trgTestEchecHabilitationProjet""";
    private static final String P_ID = "id";
    private static final String P_CODE = "code";

    @Autowired private MockMvc mockMvc;
    @Autowired private DataSource dataSource;

    private NamedParameterJdbcTemplate jdbc;
    private AdmUser admin;
    private UUID societe;
    private String codeDouble;

    @BeforeEach
    void setUp() {
        jdbc = new NamedParameterJdbcTemplate(dataSource);
        String jeton = FixtureReferentielsAdmin.jeton();
        codeDouble = "RB409-" + jeton;
        retirerDeclencheur();
        jdbc.update("DELETE FROM PrjProject WHERE Code = :code", new MapSqlParameterSource(P_CODE, CODE_ECHEC));

        admin = new AdmUser();
        admin.setId(UUID.randomUUID());
        admin.setCode("admin.projet.tx." + jeton);
        jdbc.update("INSERT INTO AdmUser (PKey, Code, Description, Disabled) VALUES (:id, :code, N'Admin Projet', 0)",
                new MapSqlParameterSource(P_ID, admin.getId()).addValue(P_CODE, admin.getCode()));
        societe = UUID.randomUUID();
        jdbc.update("INSERT INTO CfgCompany (PKey, Code, Description, IsActive) VALUES (:id, :code, :code, 1)",
                new MapSqlParameterSource(P_ID, societe).addValue(P_CODE, "SOC-TX-" + jeton));
        parametrageDeProd();
    }

    private void parametrageDeProd() {
        jdbc.update("""
                        IF NOT EXISTS (SELECT 1 FROM StrEntity WHERE PKey = :id)
                            INSERT INTO StrEntity (PKey, Code, Description) VALUES (:id, N'PrjProject', N'Projet')""",
                new MapSqlParameterSource(P_ID, ENTITE_PROJET));
        jdbc.update("""
                        IF NOT EXISTS (SELECT 1 FROM StrStatus WHERE PKey = :id)
                            INSERT INTO StrStatus (PKey, Code, Description, Sorting, StrConfigEntityId)
                            VALUES (:id, N'Nouveau', N'Nouveau', 10, :entite)""",
                new MapSqlParameterSource(P_ID, STATUT_NOUVEAU).addValue("entite", ENTITE_PROJET));
    }

    @AfterEach
    void nettoyer() {
        retirerDeclencheur();
        MapSqlParameterSource p = new MapSqlParameterSource(P_ID, admin.getId()).addValue("societe", societe)
                .addValue(P_CODE, codeDouble).addValue("echec", CODE_ECHEC);
        jdbc.update("DELETE FROM AdmUserHabilitation WHERE AdmUserId = :id", p);
        jdbc.update("DELETE FROM PrjProject WHERE CfgCompanyId = :societe OR Code IN (:code, :echec)", p);
        jdbc.update("DELETE FROM CfgCompany WHERE PKey = :societe", p);
        jdbc.update("DELETE FROM AdmUser WHERE PKey = :id", p);
    }

    private void retirerDeclencheur() {
        jdbc.getJdbcTemplate().execute(RETRAIT_DECLENCHEUR);
    }

    private CorpsJson corpsProjet(String code) {
        return corps().avec(CODE, code).avec(DESCRIPTION, "Projet " + code).avec(SOCIETE_ID, societe);
    }

    private int compter(String sql, MapSqlParameterSource parametres) {
        Integer n = jdbc.queryForObject(sql, parametres, Integer.class);
        return n == null ? 0 : n;
    }

    @Test
    void r1_codeEnDouble_409_niNouveauProjetNiHabilitation() throws Exception {
        jdbc.update("""
                        INSERT INTO PrjProject (PKey, Code, Description, CfgCompanyId, StrEntityId, StrStatusId)
                        VALUES (:id, :code, :code, :societe, :entite, :statut)""",
                new MapSqlParameterSource(P_ID, UUID.randomUUID()).addValue(P_CODE, codeDouble)
                        .addValue("societe", societe).addValue("entite", ENTITE_PROJET)
                        .addValue("statut", STATUT_NOUVEAU));

        mockMvc.perform(post(PROJETS).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(corpsProjet(" " + codeDouble.toLowerCase(Locale.ROOT) + " ").json()))
                .andExpect(status().isConflict());

        MapSqlParameterSource p = new MapSqlParameterSource(P_ID, admin.getId()).addValue(P_CODE, codeDouble);
        assertThat(new int[]{
                compter("SELECT COUNT(*) FROM AdmUserHabilitation WHERE AdmUserId = :id", p),
                compter("SELECT COUNT(*) FROM PrjProject WHERE UPPER(LTRIM(RTRIM(Code))) = UPPER(:code)", p)})
                .containsExactly(0, 1);
    }

    @Test
    void r1_echecDeLHabilitation_aucunProjetCree() throws Exception {
        jdbc.getJdbcTemplate().execute("""
                CREATE TRIGGER dbo.trgTestEchecHabilitationProjet ON dbo.AdmUserHabilitation AFTER INSERT AS
                BEGIN
                    IF EXISTS (SELECT 1 FROM inserted i JOIN dbo.PrjProject p ON p.PKey = i.PrjProjectId
                               WHERE p.Code = N'RB-ECHEC-HABILITATION')
                        THROW 50001, N'Echec force de l''habilitation (test)', 1;
                END""");

        int statut = mockMvc.perform(post(PROJETS).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(corpsProjet(CODE_ECHEC).json()))
                .andReturn().getResponse().getStatus();

        assertThat(statut).as("route existante, création refusée").isGreaterThanOrEqualTo(400).isNotIn(404, 405);
        assertThat(compter("SELECT COUNT(*) FROM PrjProject WHERE Code = :code",
                new MapSqlParameterSource(P_CODE, CODE_ECHEC))).isZero();
    }
}
