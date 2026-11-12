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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static java.util.Map.entry;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Rapprochement manuel (FEATURE-achat-bc-agregats.md, cas 14), hors CI : {@code -Dcontrole.prod=true}, sur yesRemsProd.
 * Chaque BC de la liste, vu par l'administrateur habilité sur le plus de sociétés, est comparé à {@code BuyDocumentOrderView}
 * (absente de la CI et de yesRemsTest) : écart ≤ 0,01 par montant, mêmes codes ; écarts attendus : réglé (Q2) seulement.
 */
@EnabledIfSystemProperty(named = "controle.prod", matches = "true")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlserver://localhost:49687;databaseName=yesRemsProd;encrypt=true;trustServerCertificate=true",
        "spring.jpa.show-sql=false"})
@AutoConfigureMockMvc
@Transactional
class AgregatsBonCommandeControleVueManuelTest {

    private static final BigDecimal TOLERANCE = new BigDecimal("0.01");
    private static final Map<String, String> MONTANTS = Map.ofEntries(
            entry("factureHT", "InvoicedAmountHT"), entry("factureTVA", "InvoicedAmountVAT"),
            entry("factureTTC", "InvoicedAmountTTC"), entry("factureNetAPayer", "InvoiceAmountToPay"),
            entry("livreHT", "DeliveryAmountHT"), entry("livreTTC", "DeliveryAmountTTC"),
            entry("livreNetAPayer", "DeliveryAmountToPay"), entry("resteALivrerHT", "RestDeliveredAmountHT"),
            entry("resteALivrerTTC", "RestDeliveredAmountTTC"), entry("resteALivrerNetAPayer", "RestDeliveredAmountToPay"),
            entry("avance", "OrderAdvanceAmount"), entry("avanceTauxPrevu", "OrderAdvanceRatio"),
            entry("avanceTauxEffectif", "OrderAdvanceAmountRealRatio"), entry("avanceAmortie", "AdvanceAmountAmortized"),
            entry("avanceRegle", "OrderAdvanceAmountSettled"), entry("avanceReste", "OrderAdvanceAmountRest"),
            entry("avanceResteAAmortir", "AdvanceAmountRestAmortized"), entry("garantieRetenue", "WarrantyAmountRetained"),
            entry("garantieRestituee", "WarrantyAmountRestitued"), entry("garantieReste", "WarrantyAmountRest"),
            entry("regle", "AmountSettled"), entry("resteAPayerSurFacture", "AmountRestToPayInvoice"),
            entry("resteAPayerTotal", "AmountRestToPay"));
    private static final Map<String, String> CODES = Map.of("bonsLivraisonCodes", "BuyBeliveryCode",
            "facturesCodes", "BuyInvoiceCode", "devisCode", "BuyQuoteCode");

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private AdmUser administrateurLePlusHabilite() {
        Map<String, Object> l = jdbc.queryForMap("""
                SELECT TOP 1 CAST(u.PKey AS varchar(36)) AS Id, u.Code FROM AdmUser u
                JOIN AdmUserRole ur ON ur.AdmUserId = u.PKey JOIN AdmRole r ON r.PKey = ur.AdmRoleId AND r.Code = N'Administrateur'
                JOIN AdmUserHabilitation h ON h.AdmUserId = u.PKey AND h.CfgCompanyId IS NOT NULL
                WHERE ISNULL(u.Disabled, 0) = 0 GROUP BY u.PKey, u.Code ORDER BY COUNT(DISTINCT h.CfgCompanyId) DESC, u.Code ASC""");
        AdmUser compte = new AdmUser();
        compte.setId(UUID.fromString((String) l.get("Id")));
        compte.setCode((String) l.get("Code"));
        return compte;
    }

    private static BigDecimal nombre(Object valeur) {
        return valeur == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(valeur));
    }

    private static Set<String> codes(Object valeur) {
        return valeur == null ? Set.of() : Set.of(String.valueOf(valeur).split(",")).stream().map(String::trim)
                .filter(s -> !s.isEmpty()).collect(Collectors.toSet());
    }

    @Test
    void cas14_agregatsEgauxALaVue() throws Exception {
        Map<String, Map<String, Object>> vue = new HashMap<>();
        for (Map<String, Object> l : jdbc.queryForList("SELECT CAST(PKey AS varchar(36)) AS Id, * FROM BuyDocumentOrderView")) {
            vue.put(((String) l.get("Id")).toLowerCase(), l);
        }
        String corps = mockMvc.perform(get("/api/achat/documents").with(connecte(administrateurLePlusHabilite()))
                .param("natures", "ORDER").param("size", "2000"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<Map<String, Object>> lignes = JsonPath.read(corps, "$.content");
        System.out.printf("BC API %d / vue %d%n", lignes.size(), vue.size());

        SoftAssertions s = new SoftAssertions();
        s.assertThat(lignes).isNotEmpty();
        for (Map<String, Object> ligne : lignes) {
            Map<String, Object> v = vue.get(String.valueOf(ligne.get("id")).toLowerCase());
            @SuppressWarnings("unchecked")
            Map<String, Object> a = (Map<String, Object>) ligne.get("agregats");
            s.assertThat(v).as("BC %s dans la vue", ligne.get("code")).isNotNull();
            if (v == null || a == null) {
                continue;
            }
            MONTANTS.forEach((champ, colonne) -> s.assertThat(nombre(a.get(champ)).subtract(nombre(v.get(colonne))).abs())
                    .as("%s %s (API %s / vue %s)", ligne.get("code"), champ, a.get(champ), v.get(colonne))
                    .isLessThanOrEqualTo(TOLERANCE));
            CODES.forEach((champ, colonne) -> s.assertThat(codes(a.get(champ)))
                    .as("%s %s", ligne.get("code"), champ).isEqualTo(codes(v.get(colonne))));
        }
        s.assertAll();
    }
}
