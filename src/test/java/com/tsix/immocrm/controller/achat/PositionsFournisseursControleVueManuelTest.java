package com.tsix.immocrm.controller.achat;

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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
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
 * Rapprochement manuel (FEATURE-achat-impayes-soldes.md §7), hors CI : {@code -Dcontrole.prod=true}, sur yesRemsProd. Chaque
 * (société, fournisseur FOU) de {@code BuySupplierPositionView} comparé à l'API, sans période puis sur 2025, écart ≤ 0,01.
 * Compte habilité sur toutes les sociétés et tous les projets : le test mesure le calcul, pas la visibilité.
 */
@EnabledIfSystemProperty(named = "controle.prod", matches = "true")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlserver://localhost:49687;databaseName=yesRemsProd;encrypt=true;trustServerCertificate=true",
        "spring.jpa.show-sql=false"})
@AutoConfigureMockMvc
@Transactional
class PositionsFournisseursControleVueManuelTest {

    private static final BigDecimal TOLERANCE = new BigDecimal("0.01");
    private static final List<String> CHAMPS = List.of("soldeDebut", "netAPayer", "regle", "soldeFin");
    private static final String FOU = "00685105-DFD9-497B-8830-DBF0FBE91668";
    // Fenêtres du §3.2 : début < du, période du..au (bornes incluses), fin ≤ au ; sans période = bornes extrêmes.
    private static final LocalDate DEBUT_DES_TEMPS = LocalDate.of(1900, 1, 1);
    private static final LocalDate FIN_DES_TEMPS = LocalDate.of(9999, 12, 31);
    private static final String REFERENCE = """
            SELECT LOWER(CAST(v.CfgCompanyId AS varchar(36))) + '|' + LOWER(CAST(v.CfgTierId AS varchar(36))) AS Cle,
              ROUND(SUM(CASE WHEN CAST(v.DocDate AS date) < ? THEN v.AmountCredit - v.AmountDebit ELSE 0 END), 3) AS soldeDebut,
              ROUND(SUM(CASE WHEN CAST(v.DocDate AS date) BETWEEN ? AND ? OR (? = 1 AND v.DocDate IS NULL) THEN v.AmountCredit ELSE 0 END), 3) AS netAPayer,
              ROUND(SUM(CASE WHEN CAST(v.DocDate AS date) BETWEEN ? AND ? OR (? = 1 AND v.DocDate IS NULL) THEN v.AmountDebit ELSE 0 END), 3) AS regle,
              ROUND(SUM(CASE WHEN CAST(v.DocDate AS date) <= ? OR (? = 1 AND v.DocDate IS NULL) THEN v.AmountCredit - v.AmountDebit ELSE 0 END), 3) AS soldeFin
            FROM (SELECT DocDate, CfgCompanyId, CfgTierId, ISNULL(AmountCredit, 0) AS AmountCredit,
                ISNULL(AmountDebit, 0) AS AmountDebit FROM BuySupplierPositionView) v JOIN CfgTier t ON t.PKey = v.CfgTierId AND t.CfgTierTypeId = ?
            GROUP BY v.CfgCompanyId, v.CfgTierId""";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    /** Compte actif le plus habilité (sociétés puis projets) ; sa couverture complète est vérifiée par le test. */
    private AdmUser compteLePlusHabilite() {
        Map<String, Object> l = jdbc.queryForMap("""
                SELECT TOP 1 CAST(u.PKey AS varchar(36)) AS Id, u.Code FROM AdmUser u
                JOIN AdmUserHabilitation h ON h.AdmUserId = u.PKey
                WHERE ISNULL(u.Disabled, 0) = 0 GROUP BY u.PKey, u.Code
                ORDER BY COUNT(DISTINCT h.CfgCompanyId) DESC, COUNT(DISTINCT h.PrjProjectId) DESC, u.Code ASC""");
        AdmUser compte = new AdmUser();
        compte.setId(UUID.fromString((String) l.get("Id")));
        compte.setCode((String) l.get("Code"));
        return compte;
    }

    private static BigDecimal nombre(Object valeur) {
        return valeur == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(valeur));
    }

    private Map<String, Map<String, Object>> reference(LocalDate du, LocalDate au) {
        Map<String, Map<String, Object>> vue = new HashMap<>();
        LocalDate d = du == null ? DEBUT_DES_TEMPS : du;
        LocalDate f = au == null ? FIN_DES_TEMPS : au;
        int sansPeriode = au == null ? 1 : 0;
        for (Map<String, Object> l : jdbc.queryForList(REFERENCE, d, d, f, sansPeriode, d, f, sansPeriode, f, sansPeriode, FOU)) {
            vue.put((String) l.get("Cle"), l);
        }
        return vue;
    }

    private List<Map<String, Object>> api(AdmUser compte, LocalDate du, LocalDate au) throws Exception {
        MockHttpServletRequestBuilder requete = get(AppelsPositions.POSITIONS).with(connecte(compte))
                .param(AppelsPositions.MASQUER, "false");
        if (du != null) {
            requete.param(AppelsPositions.DU, du.toString()).param(AppelsPositions.AU, au.toString());
        }
        String corps = mockMvc.perform(requete).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(corps, "$.lignes");
    }

    private static String cle(Map<String, Object> ligne) {
        return (String.valueOf(ligne.get("societeId")) + "|" + ligne.get("fournisseurId")).toLowerCase();
    }

    private void comparer(SoftAssertions s, AdmUser compte, LocalDate du, LocalDate au) throws Exception {
        Map<String, Map<String, Object>> vue = reference(du, au);
        Map<String, Map<String, Object>> arpent = new HashMap<>();
        api(compte, du, au).forEach(l -> arpent.put(cle(l), l));
        Set<String> cles = new HashSet<>(vue.keySet());
        cles.addAll(arpent.keySet());
        System.out.printf("Période %s..%s : API %d / vue %d%n", du, au, arpent.size(), vue.size());
        s.assertThat(vue).as("vue non vide").isNotEmpty();
        BigDecimal totalApi = BigDecimal.ZERO;
        BigDecimal totalVue = BigDecimal.ZERO;
        for (String c : cles) {
            Map<String, Object> a = arpent.getOrDefault(c, Map.of());
            Map<String, Object> v = vue.getOrDefault(c, Map.of());
            CHAMPS.forEach(champ -> s.assertThat(nombre(a.get(champ)).subtract(nombre(v.get(champ))).abs())
                    .as("%s %s %s..%s (API %s / vue %s)", c, champ, du, au, a.get(champ), v.get(champ))
                    .isLessThanOrEqualTo(TOLERANCE));
            totalApi = totalApi.add(nombre(a.get("soldeFin")));
            totalVue = totalVue.add(nombre(v.get("soldeFin")));
        }
        System.out.printf("Solde fin total API %s / vue %s%n", totalApi, totalVue);
        s.assertThat(totalApi.subtract(totalVue).abs()).as("écart total %s..%s", du, au).isLessThanOrEqualTo(TOLERANCE);
    }

    @Test
    void controle_positionsEgalesALaVue_sansPeriodePuis2025() throws Exception {
        AdmUser compte = compteLePlusHabilite();
        Integer societesNonCouvertes = jdbc.queryForObject("""
                SELECT COUNT(DISTINCT v.CfgCompanyId) FROM BuySupplierPositionView v WHERE NOT EXISTS (SELECT 1
                FROM AdmUserHabilitation h WHERE h.AdmUserId = ? AND h.CfgCompanyId = v.CfgCompanyId)""",
                Integer.class, compte.getId().toString());
        Integer projetsNonCouverts = jdbc.queryForObject("""
                SELECT COUNT(DISTINCT v.PrjProjectId) FROM BuySupplierPositionView v WHERE v.PrjProjectId IS NOT NULL
                AND NOT EXISTS (SELECT 1 FROM AdmUserHabilitation h WHERE h.AdmUserId = ? AND h.PrjProjectId = v.PrjProjectId)""",
                Integer.class, compte.getId().toString());

        SoftAssertions s = new SoftAssertions();
        s.assertThat(societesNonCouvertes).as("sociétés de la vue hors habilitations de %s", compte.getCode()).isZero();
        s.assertThat(projetsNonCouverts).as("projets de la vue hors habilitations de %s", compte.getCode()).isZero();
        comparer(s, compte, null, null);
        comparer(s, compte, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
        long horsSeuil = api(compte, null, null).stream()
                .filter(l -> nombre(l.get("soldeFin")).abs().compareTo(BigDecimal.ONE) > 0).count();
        System.out.printf("Lignes hors ±1 sans période : %d%n", horsSeuil);
        s.assertThat(horsSeuil).as("lignes hors ±1 (spec : 162)").isEqualTo(162L);
        s.assertAll();
    }
}
