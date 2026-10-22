package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.fixtures.ApiFaisabilite;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureFaisabilite;
import com.tsix.immocrm.fixtures.FixtureFaisabiliteFinance;
import com.tsix.immocrm.fixtures.FixtureFaisabiliteFinance.Categories;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ApiFaisabilite.d;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.*;
import static com.tsix.immocrm.fixtures.FixtureFaisabiliteFinance.*;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.decimal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Racines et colonnes stockées du bilan (calcul partagé {@code BilanFaisabilite}) — FEATURE-patrimoine-faisabilite-workflow.md
 * §2.2, §2.4 et invariant I5, lus sur les colonnes de {@code StkFeasibilityStudy} écrites par le recalcul (§3).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BilanFaisabiliteTest {

    private static final BigDecimal TOLERANCE = new BigDecimal("0.000001");
    private static final String TVA = "0.2";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureFaisabilite fixture;
    @Autowired private FixtureFaisabiliteFinance finance;

    private ApiFaisabilite api;
    private CfgCompany societe;
    private String jeton;
    private Categories cat;

    @BeforeEach
    void setUp() {
        fixture.parametrage();
        api = new ApiFaisabilite(mockMvc, patrimoine.administrateur());
        jeton = jeton();
        societe = patrimoine.entite("ENT-" + jeton, true);
        cat = finance.categoriesDeTest(jeton);
    }

    /** Étude YESTEST (plancher vendable 2 970) avec une unité de quantité {@code quantite} ; renvoie la ligne d'unité. */
    private UUID[] etudeAvecUnite(CorpsJson fiche, String quantite, boolean surface, UUID categorieArticle) throws Exception {
        UUID etude = api.etude(fiche);
        UUID base = categorieArticle == null ? api.unite("U-" + jeton, surface, null)
                : api.uniteArticle("U-" + jeton, surface, categorieArticle);
        api.unites(etude, lignes(ligneUniteCorps(1, base, d(quantite)))).andExpect(status().isOk());
        return new UUID[]{etude, fixture.ligneUnite(etude, base)};
    }

    private CorpsJson yestest() {
        return FixtureFaisabilite.yestest(fiche("BIL-" + jeton, societe.getId()));
    }

    private void previsions(UUID etude, CorpsJson... lignes) throws Exception {
        api.remplacer(PREVISIONS, etude, lignes(lignes)).andExpect(status().isOk());
    }

    private static void vaut(Tuple e, String colonne, String attendu) {
        assertThat(decimal(e.get(colonne))).as(colonne).isCloseTo(d(attendu), within(TOLERANCE));
    }

    // ------------------------------------------------------------------ §2.4 YESTEST : marges, IS, retours

    /** CA 0 → marge brute −6 088 500, IS 0, nette −6 088 500, retour coût −1, retour CA NULL, retour fonds propres −1. */
    @Test
    void r2_4_yestest_margesEtRetours() throws Exception {
        UUID[] e = etudeAvecUnite(yestest(), "2970", false, null);
        previsions(e[0], parUnite(cat.grosOeuvres(), e[1], "2000", TVA),
                enPourcentage(cat.architecte(), cat.grosOeuvres(), "0.025", TVA));

        Tuple etude = fixture.etude(e[0]);
        assertAll(
                () -> vaut(etude, "GrossProfitMargin", "-6088500"),
                () -> vaut(etude, "ISAmount", "0"),
                () -> vaut(etude, "NetProfitMargin", "-6088500"),
                () -> vaut(etude, "ProfitRatioByProjectCost", "-1"),
                () -> assertThat(etude.get("ProfitRatioByTurnover")).as("retour sur C.A., dénominateur nul").isNull(),
                () -> vaut(etude, "ProfitRatioByAutofinancementAmount", "-1"));
    }

    /** Taux d'autofinancement 1, avances clients 0, crédit 0 ; fonds propres = coût total. */
    @Test
    void r2_4_yestest_financement_autofinancement1() throws Exception {
        UUID[] e = etudeAvecUnite(yestest(), "2970", false, null);
        previsions(e[0], parUnite(cat.grosOeuvres(), e[1], "2000", TVA),
                enPourcentage(cat.architecte(), cat.grosOeuvres(), "0.025", TVA));

        Tuple etude = fixture.etude(e[0]);
        assertAll(
                () -> vaut(etude, "AutofinancementRate", "1"),
                () -> vaut(etude, "ClientfinancementRate", "0"),
                () -> vaut(etude, "CreditfinancementRate", "0"),
                () -> vaut(etude, "AutofinancementAmount", "7306200"),
                () -> vaut(etude, "Turnover", "0"));
    }

    // ------------------------------------------------------------------ §2.2 chiffre d'affaires, concrétisation, IS, crédit

    /**
     * Recette 10 000 000 (TVA 0), dépense 5 940 000 HT / 7 128 000 TTC, concrétisation 0,2, IS 0,3 :
     * brute 4 060 000, IS 1 218 000, nette 2 842 000, concrétisation 2 000 000, finalisation 8 000 000.
     */
    @Test
    void r2_2_recette_concretisation_is_marges() throws Exception {
        UUID[] e = etudeAvecUnite(yestest().avec(POURCENTAGE_CONCRETISATION, d("0.2")).avec(TAUX_IS, d("0.3")),
                "2970", false, null);
        previsions(e[0], parUnite(cat.grosOeuvres(), e[1], "2000", TVA),
                parUnite(cat.ventes(), e[1], "1", "0").sans(RATIO).avec(MONTANT_HT, d("10000000")));

        Tuple etude = fixture.etude(e[0]);
        assertAll(
                () -> vaut(etude, "Turnover", "10000000"),
                () -> vaut(etude, "TotalCostHT", "5940000"),
                () -> vaut(etude, "TotalCost", "7128000"),
                () -> vaut(etude, "GrossProfitMargin", "4060000"),
                () -> vaut(etude, "ISAmount", "1218000"),
                () -> vaut(etude, "NetProfitMargin", "2842000"),
                () -> vaut(etude, "ConretisationAmount", "2000000"),
                () -> vaut(etude, "FinalizationAmount", "8000000"),
                () -> assertThat(decimal(etude.get("ProfitRatioByProjectCost"))).isCloseTo(d("0.478451"), within(d("0.000001"))),
                () -> vaut(etude, "ProfitRatioByTurnover", "0.2842"));
    }

    /** Marge brute négative → IS 0 (pas d'impôt négatif). */
    @Test
    void r2_2_margeNegative_is0() throws Exception {
        UUID[] e = etudeAvecUnite(yestest().avec(TAUX_IS, d("0.3")), "2970", false, null);
        previsions(e[0], parUnite(cat.grosOeuvres(), e[1], "2000", TVA));

        vaut(fixture.etude(e[0]), "ISAmount", "0");
    }

    /**
     * Avec un crédit de 1 000 000 : client = 2 000 000 / 7 128 000, crédit = 1 000 000 / 7 128 000, autofinancement
     * = 7 128 000 − 2 000 000 − 1 000 000 ; fonds propres HT = 5 940 000 − 2 000 000 − 1 000 000 → retour 2 842 000 / 2 940 000.
     */
    @Test
    void i5_credit_tauxDeFinancement_autofinancement_retourFondsPropres() throws Exception {
        UUID[] e = etudeAvecUnite(yestest().avec(POURCENTAGE_CONCRETISATION, d("0.2")).avec(TAUX_IS, d("0.3")),
                "2970", false, null);
        previsions(e[0], parUnite(cat.grosOeuvres(), e[1], "2000", TVA),
                parUnite(cat.ventes(), e[1], "1", "0").sans(RATIO).avec(MONTANT_HT, d("10000000")));

        api.remplacer(FINANCEMENTS, e[0], lignes(credit("2026-01-15", "1000000", 0, 1, 0, "0"))).andExpect(status().isOk());

        Tuple etude = fixture.etude(e[0]);
        assertAll(
                () -> assertThat(decimal(etude.get("ClientfinancementRate"))).isCloseTo(d("0.280584"), within(d("0.000001"))),
                () -> assertThat(decimal(etude.get("CreditfinancementRate"))).isCloseTo(d("0.140292"), within(d("0.000001"))),
                () -> assertThat(decimal(etude.get("AutofinancementRate"))).isCloseTo(d("0.579125"), within(d("0.000001"))),
                () -> vaut(etude, "AutofinancementAmount", "4128000"),
                () -> assertThat(decimal(etude.get("ProfitRatioByAutofinancementAmount")))
                        .isCloseTo(d("0.966667"), within(d("0.000001"))));
    }

    /** §2.2 : retour sur fonds propres NULL quand les fonds propres HT sont nuls (dénominateur 0). */
    @Test
    void i5_fondsPropresNuls_retourFondsPropresNull() throws Exception {
        UUID[] e = etudeAvecUnite(yestest().avec(POURCENTAGE_CONCRETISATION, d("0.5")), "2970", false, null);
        previsions(e[0], parUnite(cat.grosOeuvres(), e[1], "1", "0").sans(RATIO).avec(MONTANT_HT, d("1000000")),
                parUnite(cat.ventes(), e[1], "1", "0").sans(RATIO).avec(MONTANT_HT, d("2000000")));

        assertThat(fixture.etude(e[0]).get("ProfitRatioByAutofinancementAmount")).isNull();
    }

    // ------------------------------------------------------------------ §2.2 proportion du terrain dans la TVA

    /**
     * Unité surface avec catégorie d'article, proportion terrain : montant = HT + (HT − 4 725 000 / 2 970 × 2 970) × 0,2
     * = 5 940 000 + 1 215 000 × 0,2 = 6 183 000.
     */
    @Test
    void r2_2_proportionTerrainPourTva_uniteSurface() throws Exception {
        UUID categorie = patrimoine.categorie("CAT-" + jeton, patrimoine.natureBat(), false).getId();
        UUID[] e = etudeAvecUnite(yestest().avec(PROPORTION_TERRAIN, true), "2970", true, categorie);
        previsions(e[0], parUnite(cat.grosOeuvres(), e[1], "2000", TVA));

        Tuple go = finance.prevision(e[0], cat.grosOeuvres());
        assertAll(
                () -> assertThat(decimal(go.get("AmountHT"))).isEqualByComparingTo("5940000"),
                () -> assertThat(decimal(go.get("Amount"))).isEqualByComparingTo("6183000"));
    }

    /** Sans la case, même unité : montant = HT × (1 + TVA) (I3). */
    @Test
    void i3_sansProportionTerrain_montantEgalHtFoisUnPlusTva() throws Exception {
        UUID categorie = patrimoine.categorie("CAT-" + jeton, patrimoine.natureBat(), false).getId();
        UUID[] e = etudeAvecUnite(yestest(), "2970", true, categorie);
        previsions(e[0], parUnite(cat.grosOeuvres(), e[1], "2000", TVA));

        assertThat(decimal(finance.prevision(e[0], cat.grosOeuvres()).get("Amount"))).isEqualByComparingTo("7128000");
    }

    // ------------------------------------------------------------------ §3 : PATCH recalcule

    @Test
    void r3_patchPourcentageConcretisation_recalculeLesMontantsStockes() throws Exception {
        UUID[] e = etudeAvecUnite(yestest(), "1", false, null);
        previsions(e[0], parUnite(cat.ventes(), e[1], "1000000", "0"));

        api.modifier(e[0], corps().avec(POURCENTAGE_CONCRETISATION, d("0.4")).json()).andExpect(status().isOk());

        Tuple etude = fixture.etude(e[0]);
        assertAll(
                () -> vaut(etude, "ConretisationAmount", "400000"),
                () -> vaut(etude, "FinalizationAmount", "600000"));
    }

    /** La fiche expose le bilan vivant (mêmes noms qu'en P8a). */
    @Test
    void i5_ficheEtude_bilanVivant() throws Exception {
        UUID[] e = etudeAvecUnite(yestest(), "2970", false, null);
        previsions(e[0], parUnite(cat.grosOeuvres(), e[1], "2000", TVA),
                enPourcentage(cat.architecte(), cat.grosOeuvres(), "0.025", TVA));

        String json = api.lire(FICHE, e[0]).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertAll(
                () -> assertThat(new BigDecimal(String.valueOf((Object) JsonPath.read(json, "$.bilan.coutTotalHt"))))
                        .isCloseTo(d("6088500"), within(TOLERANCE)),
                () -> assertThat(new BigDecimal(String.valueOf((Object) JsonPath.read(json, "$.bilan.coutRevientM2"))))
                        .isCloseTo(d("2460"), within(TOLERANCE)));
    }
}
