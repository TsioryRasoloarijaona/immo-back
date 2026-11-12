package com.tsix.immocrm.controller.projet;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Revue L1 : Vérification 4 exige aussi {@code avancement} ≤ 0,01 de {@code Achievement} (la vue) ; le contrôle manuel
 * ne fait que l'imprimer. Actif seulement si yesRemsProd est joignable (jamais en CI).
 */
@EnabledIf("prodJoignable")
@SpringBootTest(properties = {
        "spring.datasource.url=" + LotsAvancementVueRevueTest.URL, "spring.jpa.show-sql=false"})
@AutoConfigureMockMvc
@Transactional
class LotsAvancementVueRevueTest {

    static final String URL = "jdbc:sqlserver://localhost:49687;databaseName=yesRemsProd;encrypt=true;trustServerCertificate=true";
    private static final BigDecimal TOLERANCE = new BigDecimal("0.01");

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    static boolean prodJoignable() {
        String user = System.getenv("DB_USER");
        String pwd = System.getenv("DB_PWD");
        if (user == null || pwd == null) {
            return false;
        }
        try (Connection c = DriverManager.getConnection(URL + ";loginTimeout=3", user, pwd);
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT 1 WHERE OBJECT_ID('PrjMarketMonitoringView') IS NOT NULL")) {
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }

    private AdmUser administrateurLePlusHabilite() {
        Map<String, Object> l = jdbc.queryForMap("""
                SELECT TOP 1 CAST(u.PKey AS varchar(36)) AS Id, u.Code FROM AdmUser u
                JOIN AdmUserRole ur ON ur.AdmUserId = u.PKey JOIN AdmRole r ON r.PKey = ur.AdmRoleId AND r.Code = N'Administrateur'
                JOIN AdmUserHabilitation h ON h.AdmUserId = u.PKey AND h.PrjProjectId IS NOT NULL
                WHERE ISNULL(u.Disabled, 0) = 0 GROUP BY u.PKey, u.Code ORDER BY COUNT(DISTINCT h.PrjProjectId) DESC, u.Code ASC""");
        AdmUser compte = new AdmUser();
        compte.setId(UUID.fromString((String) l.get("Id")));
        compte.setCode((String) l.get("Code"));
        return compte;
    }

    @Test
    void avancement_egalAAchievementDeLaVue() throws Exception {
        Map<String, Object> vue = new HashMap<>();
        for (Map<String, Object> l : jdbc.queryForList(
                "SELECT CAST(PKey AS varchar(36)) AS Id, Achievement FROM PrjMarketMonitoringView")) {
            vue.put(((String) l.get("Id")).toLowerCase(), l.get("Achievement"));
        }
        String corps = mockMvc.perform(get("/api/projet/lots").with(connecte(administrateurLePlusHabilite()))
                .param("size", "2000")).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<Map<String, Object>> lignes = JsonPath.read(corps, "$.content");

        SoftAssertions s = new SoftAssertions();
        s.assertThat(lignes).isNotEmpty();
        for (Map<String, Object> ligne : lignes) {
            String id = String.valueOf(ligne.get("id")).toLowerCase();
            if (!vue.containsKey(id)) {
                continue;
            }
            BigDecimal api = new BigDecimal(String.valueOf(ligne.get("avancement")));
            Object v = vue.get(id);
            BigDecimal attendu = v == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(v));
            s.assertThat(api.subtract(attendu).abs()).as("%s avancement API %s / vue %s", ligne.get("code"), api, attendu)
                    .isLessThanOrEqualTo(TOLERANCE);
        }
        s.assertAll();
    }
}
