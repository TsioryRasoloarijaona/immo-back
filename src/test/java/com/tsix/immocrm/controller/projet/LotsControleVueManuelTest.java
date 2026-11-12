package com.tsix.immocrm.controller.projet;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Contrôle manuel R1 (FEATURE-lots-liste-parite.md, Vérification 4), hors CI : {@code -Dcontrole.prod=true}, sur yesRemsProd.
 * Chaque lot de la liste, vu par l'administrateur habilité sur le plus de projets, est comparé à {@code PrjMarketMonitoringView}
 * (absente de la CI et de yesRemsTest) : écart ≤ 0,01 sur les cinq montants ; {@code avancement} imprimé seulement (unité au spec).
 */
@EnabledIfSystemProperty(named = "controle.prod", matches = "true")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlserver://localhost:49687;databaseName=yesRemsProd;encrypt=true;trustServerCertificate=true",
        "spring.jpa.show-sql=false"})
@AutoConfigureMockMvc
@Transactional
class LotsControleVueManuelTest {

    private static final BigDecimal TOLERANCE = new BigDecimal("0.01");
    private static final Map<String, String> COLONNES = Map.of(
            "coutPrevisionnelHT", "CostHT", "coutPrevisionnelTTC", "CostTTC", "coutPrevisionnelNetAPayer", "CostToPay",
            "coutPrevisionnelRegle", "AmountSettled", "coutPrevisionnelReste", "CostToPayRest");

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

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
    void r1_cinqMontants_egauxALaVue_aUnCentimePres() throws Exception {
        Map<String, Map<String, Object>> vue = new HashMap<>();
        for (Map<String, Object> l : jdbc.queryForList("SELECT CAST(PKey AS varchar(36)) AS Id, * FROM PrjMarketMonitoringView")) {
            vue.put(((String) l.get("Id")).toLowerCase(), l);
        }
        String corps = mockMvc.perform(get("/api/projet/lots").with(connecte(administrateurLePlusHabilite()))
                .param("size", "2000")).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<Map<String, Object>> lignes = JsonPath.read(corps, "$.content");
        System.out.printf("lots API %d / vue %d%n", lignes.size(), vue.size());

        SoftAssertions s = new SoftAssertions();
        s.assertThat(lignes).isNotEmpty();
        for (Map<String, Object> ligne : lignes) {
            Map<String, Object> v = vue.get(String.valueOf(ligne.get("id")).toLowerCase());
            s.assertThat(v).as("lot %s dans la vue", ligne.get("code")).isNotNull();
            if (v == null) {
                continue;
            }
            COLONNES.forEach((champ, colonne) -> {
                BigDecimal api = new BigDecimal(String.valueOf(ligne.get(champ)));
                BigDecimal attendu = new BigDecimal(String.valueOf(v.get(colonne)));
                s.assertThat(api.subtract(attendu).abs()).as("%s %s", ligne.get("code"), champ).isLessThanOrEqualTo(TOLERANCE);
            });
            System.out.printf("%s avancement API %s / vue Achievement %s%n", ligne.get("code"), ligne.get("avancement"),
                    v.get("Achievement"));
        }
        s.assertAll();
    }
}
