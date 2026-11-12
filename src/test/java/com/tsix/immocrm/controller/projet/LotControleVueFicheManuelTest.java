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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Contrôle manuel E2-E4 (FEATURE-lot-fiche-suivi.md, Vérification 4), hors CI : {@code -Dcontrole.prod=true}, sur yesRemsProd.
 * Fiche et suivi de chaque lot de la liste comparés à {@code PrjMarketMonitoringView}, écart ≤ 0,01 ; taux de l'API en % (÷ 100).
 */
@EnabledIfSystemProperty(named = "controle.prod", matches = "true")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlserver://localhost:49687;databaseName=yesRemsProd;encrypt=true;trustServerCertificate=true",
        "spring.jpa.show-sql=false"})
@AutoConfigureMockMvc
@Transactional
class LotControleVueFicheManuelTest {

    private static final BigDecimal TOLERANCE = new BigDecimal("0.01");
    private static final BigDecimal CENT = new BigDecimal("100");
    private static final String PREVU = "$.general.coutPrevisionnel.";
    private static final String REEL = "$.general.coutReel.";
    private static final Map<String, String> FICHE = fiche();
    private static final Map<String, String> SUIVI = suivi();
    private static final Set<String> TAUX = Set.of("$.avanceMarche.taux", "$.retenueGarantie.taux", "$.retenues.compteProrata.taux",
            "$.retenues.assuranceDecennale.taux", "$.retenues.assuranceTrc.taux");

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private static void trio(Map<String, String> m, String chemin, String ht, String tva, String ttc) {
        m.put(chemin + ".ht", ht);
        m.put(chemin + ".tva", tva);
        m.put(chemin + ".ttc", ttc);
    }

    private static Map<String, String> fiche() {
        Map<String, String> m = new LinkedHashMap<>();
        trio(m, "$.general.marche", "OriginalOfferedPriceHT", "OriginalOfferedVatAmount", "OriginalOfferedPriceTTC");
        trio(m, "$.general.estimationBase", "EstimatedPriceHTManual", "EstimatedVatAmountManual", "EstimatedPriceTTCManual");
        trio(m, "$.general.estimationConfidentielle", "EstimatedPriceHT", "EstimatedVatAmount", "EstimatedPriceTTC");
        trio(m, "$.general.modificationBdpf", "ModificationsHT", "ModificationsVAT", "ModificationsTTC");
        trio(m, "$.general.marcheAjuste", "OfferedPriceHT", "OfferedVatAmount", "OfferedPriceTTC");
        trio(m, PREVU + "bordereau", "CostLinesHT", "CostLinesVat", "CostLinesTTC");
        trio(m, PREVU + "approvisionnement", "CostStockHT", "CostStockVat", "CostStockTTC");
        trio(m, PREVU + "depassements", "CostOverHT", "CostOverVat", "CostOverTTC");
        trio(m, PREVU + "travauxSupplementaires", "CostExtraHT", "CostExtraVat", "CostExtraTTC");
        trio(m, PREVU + "fluctuations", "CostFluctuationHT", "CostFluctuationVat", "CostFluctuationTTC");
        trio(m, PREVU + "avenants", "CostClauseHT", "CostClauseVat", "CostClauseTTC");
        trio(m, PREVU + "total", "CostHT", "CostVat", "CostTTC");
        trio(m, REEL + "bordereau", "PositionLinesHT", "PositionLinesVAT", "PositionLinesTTC");
        trio(m, REEL + "approvisionnement", "PositionStockToPayAmountHT", "PositionStockToPayAmountVAT", "PositionStockToPayAmount");
        trio(m, REEL + "depassements", "PositionOverHT", "PositionOverVAT", "PositionOverTTC");
        trio(m, REEL + "travauxSupplementaires", "PositionExtraHT", "PositionExtraVAT", "PositionExtraTTC");
        trio(m, REEL + "fluctuations", "PositionFluctuationsHT", "PositionFluctuationsVAT", "PositionFluctuationsTTC");
        trio(m, REEL + "avenants", "PositionClauseHT", "PositionClauseVAT", "PositionClauseTTC");
        trio(m, REEL + "total", "PositionTotalHT", "PositionTotalVAT", "PositionTotalTTC");
        m.putAll(Map.of(PREVU + "retenueNonRemboursable", "CostNRRAmount", PREVU + "remboursement", "CostREFAmount",
                PREVU + "regulation", "CostREGAmount", PREVU + "netAPayer", "CostToPay", PREVU + "regle", "CostToPaySettled",
                PREVU + "reste", "CostToPayRest"));
        m.putAll(Map.of(REEL + "retenueNonRemboursable", "PositionNRRAmount", REEL + "remboursement", "PositionREFAmount",
                REEL + "regulation", "PositionREGAmount", REEL + "netAPayer", "PositionAmountToPay", REEL + "regle", "PositionAmountSettled",
                REEL + "reste", "PositionAmountRest"));
        m.put("$.avancementFacture", "Achievement");
        m.put("$.avancementDecompte", "AchievementPrev");
        return m;
    }

    private static Map<String, String> suivi() {
        String d = "$.colonnes.decomptes.";
        String f = "$.colonnes.factures.";
        String av = "$.colonnes.avoirs.";
        String am = "$.avanceMarche.";
        String ad = "$.avanceDecompte.";
        String rg = "$.retenueGarantie.";
        String rd = "$.retenues.decompte.";
        String rf = "$.retenues.facture.";
        Map<String, String> m = new LinkedHashMap<>();
        m.putAll(Map.of(d + "montantHT", "PrjWorkingOutTotalLinesHT", d + "tva", "PrjWorkingOutTotalLinesVAT",
                d + "montantTTC", "PrjWorkingOutTotalLinesTTC", d + "approTTC", "PrjWorkingOutTotalStockToPayAmount",
                d + "depassementsTTC", "OverAmountTTC", d + "travauxSuppTTC", "ExtraAmountTTC",
                d + "fluctuationsTTC", "PrjWorkingOutTotalFluctuationsToPayAmount", d + "totalTTC", "PrjWorkingOutTotalTTC",
                d + "avenantsTTC", "ClausesAmountTTC", d + "coutTTC", "PrjWorkingOutCostTTC"));
        m.putAll(Map.of(d + "retenues", "PrjWorkingOutCostNRRAmount", d + "remboursement", "PrjWorkingOutCostREFAmount"));
        m.putAll(Map.of(f + "montantHT", "InvoiceTotalLinesHT", f + "tva", "InvoiceTotalLinesVAT", f + "montantTTC", "InvoiceTotalLinesTTC",
                f + "approTTC", "InvoiceTotalStockToPayAmount", f + "depassementsTTC", "OverInvoiceAmountTTC",
                f + "travauxSuppTTC", "InvoiceExtraAmountTTC", f + "fluctuationsTTC", "InvoiceTotalFluctuationsToPayAmount",
                f + "totalTTC", "InvoiceTotalTTC", f + "avenantsTTC", "ClausesInvoicedAmountTTC", f + "coutTTC", "InvoiceCostTTC"));
        m.putAll(Map.of(f + "retenues", "InvoiceCostNRRAmount", f + "remboursement", "InvoiceCostREFAmount",
                f + "regulation", "InvoiceCostREGAmount", av + "montantHT", "RefundsAmountHT", av + "tva", "RefundsAmountVAT",
                av + "montantTTC", "RefundsAmountTTC", av + "regulation", "RefundsREGAmount"));
        m.putAll(Map.of(am + "taux", "AdvanceRatio", am + "estBase", "EstimatedAdvanceAmountManual",
                am + "confidentielle", "EstimatedAdvanceAmount", am + "marche", "AdvanceAmount", am + "validee", "AdvanceAmountValidated",
                am + "reglee", "AdvanceSettled", am + "amortie", "AdvanceAmortized", am + "resteAAmortir", "AdvanceAmortizedRest"));
        m.putAll(Map.of(ad + "validee", "WorkingOutAdvanceAmount", ad + "reglee", "WorkingOutAdvanceAmountSettled",
                ad + "amortie", "WorkingOutAdvanceAmountAmortized", ad + "resteAAmortir", "WorkingOutAdvanceAmountAmortizedRest"));
        m.putAll(Map.of(rg + "taux", "WarrantyRatio", rg + "estBase", "EstimatedWarrantyAmountManual",
                rg + "confidentielle", "EstimatedWarrantyAmount", rg + "marcheAjuste", "OfferedWarrantyAmount",
                rg + "avenants", "ClausesWarrantyAmount", rg + "factures", "WarrantyAmount", rg + "retenue", "WarrantyRetained",
                rg + "validee", "WarrantyAmountValidated", rg + "restituee", "WarrantyRestitued", rg + "resteARestituer", "WarrantyRestituedRest"));
        m.putAll(Map.of(rg + "aRembourser", "ProportionAmountToRefund", rg + "remboursement", "InvoicedProportionRefund",
                "$.retenues.compteProrata.taux", "ProportionRatio", "$.retenues.assuranceDecennale.taux", "InsuranceDECRatio",
                "$.retenues.assuranceDecennale.quotePart", "InsuranceDECAmount", "$.retenues.assuranceTrc.taux", "InsuranceTRCRatio",
                "$.retenues.assuranceTrc.quotePart", "InsuranceTRCAmount"));
        m.putAll(Map.of(rd + "retenueForfaitaire", "PrjWorkingOutCostRetainedLineAmount", rf + "retenueForfaitaire", "InvoiceCostRetainedLineAmount",
                rf + "penaliteCompteProrata", "InvoiceProrataChargedAmountTTC", rd + "penaliteRg", "WarrantyAmountPenality",
                rf + "penaliteRg", "WarrantyAmountPenality", rd + "blocageRg", "PrjWorkingOutBlokRestitutionPenality",
                rf + "blocageRg", "InvoiceBlokRestitutionPenality", rd + "penaliteAvance", "PrjWorkingOutAdvancePenality",
                rf + "penaliteAvance", "InvoiceAdvancePenality"));
        m.putAll(Map.of(rd + "retenues", "PrjWorkingOutCostNRRAmount", rf + "retenues", "InvoiceCostNRRAmount"));
        return m;
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

    private String lire(RequestPostProcessor qui, String url) throws Exception {
        return mockMvc.perform(get(url).with(qui).param("size", "2000")).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
    }

    /** Champ nul côté API (ex. colonne Avoirs hors liste) : non comparé. */
    private static void comparer(SoftAssertions s, String code, String corps, Map<String, String> champs, Map<String, Object> vue) {
        champs.forEach((chemin, colonne) -> {
            Object valeur = JsonPath.read(corps, chemin);
            Object attendu = vue.get(colonne);
            if (valeur == null || attendu == null) {
                return;
            }
            BigDecimal api = new BigDecimal(String.valueOf(valeur));
            if (TAUX.contains(chemin)) {
                api = api.divide(CENT);
            }
            BigDecimal ecart = api.subtract(new BigDecimal(String.valueOf(attendu))).abs();
            s.assertThat(ecart).as("%s %s (%s)", code, chemin, colonne).isLessThanOrEqualTo(TOLERANCE);
        });
    }

    @Test
    void e2e4_ficheEtSuivi_egauxALaVue_aUnCentimePres() throws Exception {
        Map<String, Map<String, Object>> vue = new HashMap<>();
        for (Map<String, Object> l : jdbc.queryForList("SELECT CAST(PKey AS varchar(36)) AS Id, * FROM PrjMarketMonitoringView")) {
            vue.put(((String) l.get("Id")).toLowerCase(), l);
        }
        RequestPostProcessor qui = connecte(administrateurLePlusHabilite());
        List<Map<String, Object>> lignes = JsonPath.read(lire(qui, AppelsProjet.LOTS), "$.content");
        System.out.printf("lots API %d / vue %d%n", lignes.size(), vue.size());

        SoftAssertions s = new SoftAssertions();
        s.assertThat(lignes).isNotEmpty();
        for (Map<String, Object> ligne : lignes) {
            String id = String.valueOf(ligne.get("id"));
            Map<String, Object> v = vue.get(id.toLowerCase());
            s.assertThat(v).as("lot %s dans la vue", ligne.get("code")).isNotNull();
            if (v != null) {
                String code = String.valueOf(ligne.get("code"));
                comparer(s, code, lire(qui, AppelsProjet.LOTS + "/" + id), FICHE, v);
                comparer(s, code, lire(qui, AppelsProjet.LOTS + "/" + id + "/suivi"), SUIVI, v);
            }
        }
        s.assertAll();
    }
}
