package com.tsix.immocrm.controller.patrimoine;

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
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static com.tsix.immocrm.fixtures.ApiFaisabilite.d;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.*;
import static com.tsix.immocrm.fixtures.FixtureFaisabiliteFinance.*;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.decimal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Répartition mensuelle de Gauss ({@code Utils.GetGaussGraph}) lue sur les lignes de trésorerie stockées —
 * FEATURE-patrimoine-faisabilite-workflow.md §2.5 et invariant I6. Variante montants : dépense d'exécution de 1 000 000
 * (TVA 0) sur la fenêtre des travaux, sans tranche ; variante quantités : concrétisation de 500 000 sur 10 articles.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RepartitionGaussTest {

    private static final BigDecimal M = new BigDecimal("1000000");
    private static final BigDecimal CENTIME = new BigDecimal("0.01");
    private static final BigDecimal TOLERANCE = new BigDecimal("0.00001");
    private static final String DEBUT = "2026-01-01";
    private static final String EXPENSE = "Expense";
    private static final String CONCRETISATION = "Concretisation";

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

    /** Étude sans tranche, travaux sur {@code dureeTravaux} mois (n = durée + 1), une unité de quantité 1 ; renvoie [étude, unité]. */
    private UUID[] etude(CorpsJson fiche, UUID categorieArticle, String quantite) throws Exception {
        UUID etude = api.etude(fiche);
        UUID base = categorieArticle == null ? api.unite("U-" + jeton, false, null)
                : api.uniteArticle("U-" + jeton, false, categorieArticle);
        api.unites(etude, lignes(ligneUniteCorps(1, base, d(quantite)))).andExpect(status().isOk());
        return new UUID[]{etude, fixture.ligneUnite(etude, base)};
    }

    private CorpsJson fiche(int dureeTravaux, String sigma) {
        return phases(FixtureFaisabilite.fiche("GAU-" + jeton, societe.getId()), DEBUT, dureeTravaux, 0, 0)
                .avec(ECART_TYPE_TRAVAUX, d(sigma));
    }

    /** Dépense d'exécution M répartie sur n = dureeTravaux + 1 mois. */
    private List<Tuple> depense(int dureeTravaux, String sigma) throws Exception {
        UUID[] e = etude(fiche(dureeTravaux, sigma), null, "1");
        api.remplacer(PREVISIONS, e[0], lignes(parUnite(cat.grosOeuvres(), e[1], "1000000", "0"))).andExpect(status().isOk());
        return finance.tresorerie(e[0], EXPENSE);
    }

    private static List<BigDecimal> montants(List<Tuple> lignes) {
        return lignes.stream().map(l -> decimal(l.get("Amount"))).toList();
    }

    private static BigDecimal somme(List<Tuple> lignes) {
        return montants(lignes).stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static void proches(List<Tuple> lignes, String... attendus) {
        List<BigDecimal> montants = montants(lignes);
        assertThat(montants).hasSize(attendus.length);
        assertAll(IntStream.range(0, attendus.length).<Executable>mapToObj(i -> () ->
                assertThat(montants.get(i)).as("ligne %d", i).isCloseTo(new BigDecimal(attendus[i]), within(CENTIME))));
    }

    // ------------------------------------------------------------------ variante montants

    @Test
    void r2_5_n1_toutLeMontantSurUneLigne() throws Exception {
        List<Tuple> lignes = depense(0, "1");

        assertThat(lignes).singleElement().satisfies(l -> assertAll(
                () -> assertThat(decimal(l.get("Amount"))).isEqualByComparingTo(M),
                () -> assertThat(String.valueOf(l.get("DocDate"))).startsWith(DEBUT)));
    }

    @Test
    void r2_5_n2_moitieMoitie() throws Exception {
        proches(depense(1, "1"), "500000", "500000");
    }

    /** Exemple du spec : M = 1 000 000, n = 3, σ = 1 → g = M φ(−0,5) = 352 065,3267 au centre, (M − g)/2 = 323 967,3366 aux bords. */
    @Test
    void r2_5_n3_sigma1_exempleDuSpec() throws Exception {
        proches(depense(2, "1"), "323967.3366", "352065.3267", "323967.3366");
    }

    /** n = 4, σ = 1 : g = M φ((σ−2)/σ) = 241 970,72 ; c = (M − 2g)/2 = 258 029,28 ≥ g → [g, c, c, g]. */
    @Test
    void r2_5_n4_sigma1_calculeALaMain() throws Exception {
        proches(depense(3, "1"), "241970.72", "258029.28", "258029.28", "241970.72");
    }

    /** n = 5, σ = 1, μ = 2 : M φ(i) pour i = 0..4, reliquat sur la ligne μ (408 076,62). */
    @Test
    void r2_5_n5_sigma1_reliquatSurLaLigneCentrale() throws Exception {
        proches(depense(4, "1"), "53990.97", "241970.72", "408076.62", "241970.72", "53990.97");
    }

    /** n = 24 : 24 lignes mensuelles, Σ = M exactement (I6), maximum sur la ligne μ = 11. */
    @Test
    void i6_n24_sommeExacte_datesMensuelles_maximumAuCentre() throws Exception {
        List<Tuple> lignes = depense(23, "3");
        List<BigDecimal> montants = montants(lignes);

        assertAll(
                () -> assertThat(lignes).hasSize(24),
                () -> assertThat(somme(lignes)).isCloseTo(M, within(TOLERANCE)),
                () -> assertThat(String.valueOf(lignes.get(0).get("DocDate"))).startsWith(DEBUT),
                () -> assertThat(String.valueOf(lignes.get(23).get("DocDate"))).startsWith("2027-12-01"),
                () -> assertThat(montants.get(11)).isEqualTo(montants.stream().max(BigDecimal::compareTo).orElseThrow()));
    }

    /** n = 3 ou 4 : σ &gt; 1 refusé (400, message OMRAN), rien n'est écrit. */
    @Test
    void r2_5_n3_sigmaSuperieurA1_400_rienEcrit() throws Exception {
        UUID[] e = etude(fiche(2, "1.5"), null, "1");

        api.remplacer(PREVISIONS, e[0], lignes(parUnite(cat.grosOeuvres(), e[1], "1000000", "0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString(msgEcartType(3))));

        assertThat(fixture.compter("SELECT COUNT(*) FROM FSPrevision WHERE FeasibilityStudyId = :id", e[0])).isZero();
    }

    @Test
    void r2_5_n4_sigmaSuperieurA1_400() throws Exception {
        UUID[] e = etude(fiche(3, "1.2"), null, "1");

        api.remplacer(PREVISIONS, e[0], lignes(parUnite(cat.grosOeuvres(), e[1], "1000000", "0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString(msgEcartType(4))));
    }

    // ------------------------------------------------------------------ variante quantités (concrétisation)

    /** Recette 1 000 000 sur 10 articles, concrétisation 0,5 sur n = dureeConcretisation + 1 mois (σ concrétisation donné). */
    private List<Tuple> concretisation(int dureeConcretisation, String sigma, boolean articles) throws Exception {
        UUID categorie = articles ? patrimoine.categorie("CAT-" + jeton, patrimoine.natureBat(), false).getId() : null;
        CorpsJson fiche = phases(FixtureFaisabilite.fiche("QTE-" + jeton, societe.getId()), DEBUT, 0, dureeConcretisation, 0)
                .avec(ECART_TYPE_CONCRETISATION, d(sigma)).avec(POURCENTAGE_CONCRETISATION, d("0.5"));
        UUID[] e = etude(fiche, categorie, "10");
        api.remplacer(PREVISIONS, e[0], lignes(parUnite(cat.ventes(), e[1], "100000", "0"))).andExpect(status().isOk());
        return finance.tresorerie(e[0], CONCRETISATION);
    }

    /** Exemple du spec : n = 5, q = 10, σ = 2 → [2,2,3,2,2] puis un retrait au centre → [2,2,2,2,2] × 50 000. */
    @Test
    void r2_5_quantites_n5_q10_sigma2_exempleDuSpec() throws Exception {
        List<Tuple> lignes = concretisation(4, "2", true);

        proches(lignes, "100000", "100000", "100000", "100000", "100000");
        assertThat(somme(lignes)).isCloseTo(new BigDecimal("500000"), within(TOLERANCE));
    }

    /** n = 3, q = 10, σ = 1 : parts 0,274/0,452/0,274 → arrondis [3,5,3] (Σ 11) → un retrait au centre → [3,4,3] × 50 000. */
    @Test
    void r2_5_quantites_n3_q10_sigma1_retraitAuCentre() throws Exception {
        proches(concretisation(2, "1", true), "150000", "200000", "150000");
    }

    /** q = 0 (aucune unité d'article) → aucune ligne de concrétisation. */
    @Test
    void r2_5_quantites_q0_aucuneLigne() throws Exception {
        assertThat(concretisation(4, "2", false)).isEmpty();
    }
}
