package com.tsix.immocrm.controller.projet;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
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
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Contrôle manuel L3a (FEATURE-lot-sous-fenetres.md §8), hors CI : {@code -Dtest=LotsSousFenetresControleManuelTest
 * -Dcontrole.prod=true}, sur yesRemsProd. Lots visibles de l'administrateur le plus habilité : avances décompte (réglé hors
 * annulés, Q3), bordereaux estimatifs groupés, prévisions de paiement, comparés aux tables. Écarts de société imprimés (Q2).
 */
@EnabledIfSystemProperty(named = "controle.prod", matches = "true")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlserver://localhost:49687;databaseName=yesRemsProd;encrypt=true;trustServerCertificate=true",
        "spring.jpa.show-sql=false"})
@AutoConfigureMockMvc
@Transactional
class LotsSousFenetresControleManuelTest {

    private static final BigDecimal TOLERANCE = new BigDecimal("0.01");
    private static final String LOTS = "/api/projet/lots/";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private AdmUser admin;
    private Set<String> lotsVisibles;

    @BeforeEach
    void setUp() throws Exception {
        Map<String, Object> l = jdbc.queryForMap("""
                SELECT TOP 1 CAST(u.PKey AS varchar(36)) AS Id, u.Code FROM AdmUser u
                JOIN AdmUserRole ur ON ur.AdmUserId = u.PKey JOIN AdmRole r ON r.PKey = ur.AdmRoleId AND r.Code = N'Administrateur'
                JOIN AdmUserHabilitation h ON h.AdmUserId = u.PKey AND h.PrjProjectId IS NOT NULL
                WHERE ISNULL(u.Disabled, 0) = 0 GROUP BY u.PKey, u.Code ORDER BY COUNT(DISTINCT h.PrjProjectId) DESC, u.Code ASC""");
        admin = new AdmUser();
        admin.setId(UUID.fromString((String) l.get("Id")));
        admin.setCode((String) l.get("Code"));
        List<String> ids = JsonPath.read(lire("/api/projet/lots?size=2000"), "$.content[*].id");
        lotsVisibles = new HashSet<>(ids.stream().map(String::toLowerCase).toList());
        System.out.printf("lots visibles : %d%n", lotsVisibles.size());
    }

    private String lire(String url) throws Exception {
        return mockMvc.perform(get(url).with(connecte(admin))).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
    }

    private List<String> lotsAvec(String sql) {
        return jdbc.queryForList(sql, String.class).stream().map(String::toLowerCase).filter(lotsVisibles::contains).toList();
    }

    private static BigDecimal nombre(Object v) {
        return v == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(v));
    }

    @Test
    void avancesDecompte_netRegleReste_egauxAuxTables() throws Exception {
        List<String> lots = lotsAvec("""
                SELECT DISTINCT CAST(PrjMarketId AS varchar(36)) FROM BuyDocument
                WHERE ReferenceType = 'WORKINGOUT_ADVANCE' AND DocType = 'DOCUMENT_TO_PAY'""");
        SoftAssertions s = new SoftAssertions();
        s.assertThat(lots).as("lots à avances décompte visibles").isNotEmpty();
        for (String lot : lots) {
            Map<String, Map<String, Object>> table = new HashMap<>();
            jdbc.queryForList("""
                    SELECT d.Code, d.AmountToPay, d.CanBePayed, (SELECT SUM(l.Amount) FROM StlSettlementLine l
                        JOIN StlSettlement st ON st.PKey = l.StlSettlementId
                        WHERE l.BuyDocumentId = d.PKey AND (st.Status IS NULL OR st.Status <> 'CANCELED')) AS Regle
                    FROM BuyDocument d WHERE d.PrjMarketId = ? AND d.ReferenceType = 'WORKINGOUT_ADVANCE'
                        AND d.DocType = 'DOCUMENT_TO_PAY'""", lot).forEach(r -> table.put((String) r.get("Code"), r));
            List<Map<String, Object>> api = JsonPath.read(lire(LOTS + lot + "/avances"), "$.surDecompte");
            System.out.printf("lot %s : avances décompte API %d / table %d%n", lot, api.size(), table.size());
            for (Map<String, Object> a : api) {
                Map<String, Object> t = table.get((String) a.get("code"));
                s.assertThat(t).as("avance %s dans la table", a.get("code")).isNotNull();
                if (t != null) {
                    BigDecimal net = nombre(t.get("AmountToPay"));
                    BigDecimal regle = nombre(t.get("Regle"));
                    s.assertThat(nombre(a.get("netAPayer")).subtract(net).abs()).as("net %s", a.get("code")).isLessThanOrEqualTo(TOLERANCE);
                    s.assertThat(nombre(a.get("regle")).subtract(regle).abs()).as("réglé %s", a.get("code")).isLessThanOrEqualTo(TOLERANCE);
                    s.assertThat(nombre(a.get("reste")).subtract(net.subtract(regle)).abs()).as("reste %s", a.get("code"))
                            .isLessThanOrEqualTo(TOLERANCE);
                }
            }
        }
        s.assertAll();
    }

    @Test
    void bordereauEstimatifGroupe_groupesEtLignes_egauxAuxTables() throws Exception {
        List<String> lots = lotsAvec("SELECT DISTINCT CAST(PrjMarketId AS varchar(36)) FROM PrjMarketLineGroupingEstimation");
        SoftAssertions s = new SoftAssertions();
        s.assertThat(lots).as("lots à groupes estimatifs visibles").isNotEmpty();
        for (String lot : lots) {
            String corps = lire(LOTS + lot + "/bordereau-estimatif");
            List<Object> groupes = JsonPath.read(corps, "$.groupes");
            List<Object> lignes = JsonPath.read(corps, "$.lignes");
            Integer nbGroupes = jdbc.queryForObject("SELECT COUNT(*) FROM PrjMarketLineGroupingEstimation WHERE PrjMarketId = ?",
                    Integer.class, lot);
            Map<String, Object> t = jdbc.queryForMap("""
                    SELECT COUNT(*) AS Nb, SUM(Quantity * UnitPriceHT) AS HT FROM PrjMarketLineEstimation
                    WHERE PrjMarketId = ? AND ISNULL(IsStock, 0) = 0""", lot);
            System.out.printf("lot %s : totalHT API %s / table %s%n", lot, JsonPath.read(corps, "$.totaux.totalHT"), t.get("HT"));
            s.assertThat(groupes).as("groupes %s", lot).hasSize(nbGroupes == null ? 0 : nbGroupes);
            s.assertThat((long) lignes.size()).as("lignes %s", lot).isEqualTo(((Number) t.get("Nb")).longValue());
            s.assertThat(JsonPath.<Boolean>read(corps, "$.verrouille")).as("verrouillé %s", lot).isTrue();
        }
        s.assertAll();
    }

    @Test
    void previsions_sommeDesMois_egaleTable_moisConsecutifs() throws Exception {
        List<String> lots = lotsAvec("SELECT DISTINCT CAST(PrjMarketId AS varchar(36)) FROM PrjMarketPaymentPrevision");
        System.out.printf("lots à prévisions visibles : %d%n", lots.size());
        SoftAssertions s = new SoftAssertions();
        s.assertThat(lots).isNotEmpty();
        for (String lot : lots) {
            String corps = lire(LOTS + lot + "/previsions");
            List<String> mois = JsonPath.read(corps, "$.mois[*].mois");
            List<Object> previsions = JsonPath.read(corps, "$.mois[*].prevision");
            BigDecimal api = previsions.stream().map(LotsSousFenetresControleManuelTest::nombre)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal table = nombre(jdbc.queryForObject("SELECT SUM(Amount) FROM PrjMarketPaymentPrevision WHERE PrjMarketId = ?",
                    BigDecimal.class, lot));
            s.assertThat(api.subtract(table).abs()).as("Σ prévision %s", lot).isLessThanOrEqualTo(TOLERANCE);
            s.assertThat(mois).as("mois %s", lot).isNotEmpty();
            for (int i = 1; i < mois.size(); i++) {
                s.assertThat(LocalDate.parse(mois.get(i))).as("mois consécutifs %s", lot)
                        .isEqualTo(LocalDate.parse(mois.get(i - 1)).plusMonths(1));
            }
        }
        s.assertAll();
    }
}
