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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ApiFaisabilite.d;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.*;
import static com.tsix.immocrm.fixtures.FixtureFaisabiliteFinance.*;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.decimal;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.guid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lignes de trésorerie ({@code StkFSPrevisionLine}) dérivées au recalcul et courbes en lecture —
 * FEATURE-patrimoine-faisabilite-workflow.md §2.5, §2.6 et invariant I6 (types, fenêtres, retenue de garantie, indivis).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TresorerieFaisabiliteTest {

    private static final BigDecimal TOLERANCE = new BigDecimal("0.00001");
    private static final String DEBUT = "2026-01-01";
    private static final String EXPENSE = "Expense";
    private static final String GARANTIE = "ExpenseBackGuarantee";
    private static final String LIGNES_ETUDE = "SELECT COUNT(*) FROM StkFSPrevisionLine WHERE StkFeasibilityStudyId = :id";

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

    private UUID[] etude(CorpsJson fiche, String quantite) throws Exception {
        UUID etude = api.etude(fiche);
        UUID base = api.unite("U-" + jeton, false, null);
        api.unites(etude, lignes(ligneUniteCorps(1, base, d(quantite)))).andExpect(status().isOk());
        return new UUID[]{etude, fixture.ligneUnite(etude, base)};
    }

    private CorpsJson fiche(int dureeTravaux) {
        return phases(FixtureFaisabilite.fiche("TRE-" + jeton, societe.getId()), DEBUT, dureeTravaux, 0, 0)
                .avec(ECART_TYPE_TRAVAUX, d("1"));
    }

    private void previsions(UUID etude, CorpsJson... lignes) throws Exception {
        api.remplacer(PREVISIONS, etude, lignes(lignes)).andExpect(status().isOk());
    }

    private static BigDecimal somme(List<Tuple> lignes) {
        return lignes.stream().map(l -> decimal(l.get("Amount"))).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ------------------------------------------------------------------ §2.5 YESTEST

    /** YESTEST : tranche unique, toutes dates au 13/12/2021 → 1 ligne Expense de 7 306 200 au 13/12/2021 (ligne de prod). */
    @Test
    void r2_5_yestest_uneLigneExpense_7306200_le13_12_2021() throws Exception {
        UUID[] e = etude(phases(yestest(FixtureFaisabilite.fiche("YES-" + jeton, societe.getId())), "2021-12-13", 0, 0, 0),
                "2970");
        UUID tranche = patrimoine.tranche("T1-" + jeton, patrimoine.projet("PRJ-" + jeton, societe)).getId();
        api.tranches(e[0], lignes(tranche(ligneTrancheCorps("Tranche 1", 1, tranche, d("1"))
                        .avec("debutTravaux", "2021-12-13T00:00:00").avec("finTravaux", "2021-12-13T00:00:00"),
                ventilation(e[1], d("2970"))))).andExpect(status().isOk());
        UUID ligneTranche = fixture.ligneTranche(e[0], "Tranche 1");

        previsions(e[0], parUnite(cat.grosOeuvres(), e[1], "2000", "0.2"),
                enPourcentage(cat.architecte(), cat.grosOeuvres(), "0.025", "0.2"));

        assertThat(finance.tresorerie(e[0])).singleElement().satisfies(l -> assertAll(
                () -> assertThat(l.get("type")).isEqualTo(EXPENSE),
                () -> assertThat(decimal(l.get("Amount"))).isEqualByComparingTo("7306200"),
                () -> assertThat(String.valueOf(l.get("DocDate"))).startsWith("2021-12-13"),
                () -> assertThat(((Number) l.get("DateMonth")).intValue()).isEqualTo(12),
                () -> assertThat(((Number) l.get("DateYear")).intValue()).isEqualTo(2021),
                () -> assertThat(l.get("IsCredit")).isEqualTo(Boolean.FALSE),
                () -> assertThat(guid(l.get("tranche"))).isEqualTo(guid(ligneTranche)),
                () -> assertThat(decimal(l.get("AmountHT"))).isEqualByComparingTo("0"),
                () -> assertThat(l.get("VatRatio")).isNull(),
                () -> assertThat(l.get("Notes")).isEqualTo("")));
    }

    // ------------------------------------------------------------------ §2.5 retenue de garantie

    /**
     * D = 1 000 000, application 0,5, retenue 0,1 : Expense = D − D × 0,05 = 950 000 sur 3 mois ; garantie 50 000 sur
     * [fin travaux, fin + 10 j] (n = 1), datée DÉBUT des travaux + 10 jours sans tranche (défaut OMRAN repris, décision 7a).
     */
    @Test
    void i6_retenueDeGarantie_sommesEtDateDuDefautOmran() throws Exception {
        UUID[] e = etude(fiche(2).avec(TAUX_APPLICATION_RETENUE, d("0.5")).avec(TAUX_RETENUE, d("0.1"))
                .avec(DELAI_RETOUR, 10), "1");

        previsions(e[0], parUnite(cat.grosOeuvres(), e[1], "1000000", "0"));

        List<Tuple> expense = finance.tresorerie(e[0], EXPENSE);
        List<Tuple> garantie = finance.tresorerie(e[0], GARANTIE);
        assertAll(
                () -> assertThat(expense).hasSize(3),
                () -> assertThat(somme(expense)).isCloseTo(d("950000"), within(TOLERANCE)),
                () -> assertThat(garantie).singleElement().satisfies(g -> assertAll(
                        () -> assertThat(decimal(g.get("Amount"))).isEqualByComparingTo("50000"),
                        () -> assertThat(String.valueOf(g.get("DocDate"))).startsWith("2026-01-11"))),
                () -> assertThat(somme(expense).add(somme(garantie))).isCloseTo(d("1000000"), within(TOLERANCE)));
    }

    @Test
    void r2_5_tauxDeRetenueNul_aucuneLigneDeGarantie() throws Exception {
        UUID[] e = etude(fiche(0).avec(TAUX_APPLICATION_RETENUE, d("0.5")).avec(TAUX_RETENUE, BigDecimal.ZERO), "1");

        previsions(e[0], parUnite(cat.grosOeuvres(), e[1], "1000000", "0"));

        assertAll(
                () -> assertThat(finance.tresorerie(e[0], GARANTIE)).isEmpty(),
                () -> assertThat(somme(finance.tresorerie(e[0], EXPENSE))).isCloseTo(d("1000000"), within(TOLERANCE)));
    }

    /** Arrêt si le coût d'exécution est nul : aucune ligne Expense. */
    @Test
    void r2_5_depenseNulle_aucuneLigneExpense() throws Exception {
        UUID[] e = etude(fiche(0), "1");

        previsions(e[0], parUnite(cat.ventes(), e[1], "1000", "0"));

        assertThat(finance.tresorerie(e[0], EXPENSE)).isEmpty();
    }

    // ------------------------------------------------------------------ §2.5 indivis (« Traiter au début du projet »)

    /** Une ligne datée sort de D (hors indivis) et devient une ligne ProjectExpense à sa date, montant propre. */
    @Test
    void r2_5_ligneIndivise_horsDepenseExecution_ligneProjectExpenseASaDate() throws Exception {
        UUID[] e = etude(fiche(0), "1");

        previsions(e[0], parUnite(cat.grosOeuvres(), e[1], "1000000", "0"),
                parUnite(cat.divers(), e[1], "200000", "0").avec(DATE_APPLICATION, "2026-06-15T00:00:00"));

        Tuple divers = finance.prevision(e[0], cat.divers());
        assertAll(
                () -> assertThat(somme(finance.tresorerie(e[0], EXPENSE))).isCloseTo(d("1000000"), within(TOLERANCE)),
                () -> assertThat(finance.tresorerie(e[0], "ProjectExpense")).singleElement().satisfies(l -> assertAll(
                        () -> assertThat(decimal(l.get("Amount"))).isEqualByComparingTo("200000"),
                        () -> assertThat(String.valueOf(l.get("DocDate"))).startsWith("2026-06-15"))),
                () -> assertThat(divers.get("Undivided")).isEqualTo(Boolean.TRUE),
                () -> assertThat(String.valueOf(divers.get("ApplyOnDate"))).startsWith("2026-06-15"));
    }

    // ------------------------------------------------------------------ §2.5 concrétisation / finalisation, montants nuls

    /** Pourcentage 1 : finalisation nulle → aucune ligne Finalization (seuls les montants &gt; 0 sont écrits). */
    @Test
    void r2_5_montantNul_nonEcrit() throws Exception {
        UUID categorie = patrimoine.categorie("CAT-" + jeton, patrimoine.natureBat(), false).getId();
        UUID etude = api.etude(fiche(0).avec(POURCENTAGE_CONCRETISATION, d("1")));
        UUID base = api.uniteArticle("UA-" + jeton, false, categorie);
        api.unites(etude, lignes(ligneUniteCorps(1, base, d("10")))).andExpect(status().isOk());

        previsions(etude, parUnite(cat.ventes(), fixture.ligneUnite(etude, base), "100000", "0"));

        assertAll(
                () -> assertThat(finance.tresorerie(etude, "Finalization")).isEmpty(),
                () -> assertThat(somme(finance.tresorerie(etude, "Concretisation"))).isCloseTo(d("1000000"), within(TOLERANCE)),
                () -> assertThat(fixture.compter(LIGNES_ETUDE + " AND Amount <= 0", etude)).isZero());
    }

    /** I6 : Σ Concretisation = ConretisationAmount, Σ Finalization = FinalizationAmount. */
    @Test
    void i6_sommesConcretisationEtFinalisation_egalesAuxColonnesStockees() throws Exception {
        UUID categorie = patrimoine.categorie("CAT-" + jeton, patrimoine.natureBat(), false).getId();
        UUID etude = api.etude(phases(FixtureFaisabilite.fiche("CF-" + jeton, societe.getId()), DEBUT, 2, 2, 2)
                .avec(ECART_TYPE_TRAVAUX, d("1")).avec(ECART_TYPE_CONCRETISATION, d("1"))
                .avec(ECART_TYPE_FINALISATION, d("1")).avec(POURCENTAGE_CONCRETISATION, d("0.3")));
        UUID base = api.uniteArticle("UA-" + jeton, false, categorie);
        api.unites(etude, lignes(ligneUniteCorps(1, base, d("10")))).andExpect(status().isOk());

        previsions(etude, parUnite(cat.ventes(), fixture.ligneUnite(etude, base), "100000", "0"));

        Tuple e = fixture.etude(etude);
        assertAll(
                () -> assertThat(somme(finance.tresorerie(etude, "Concretisation")))
                        .isCloseTo(decimal(e.get("ConretisationAmount")), within(TOLERANCE))
                        .isCloseTo(d("300000"), within(TOLERANCE)),
                () -> assertThat(somme(finance.tresorerie(etude, "Finalization")))
                        .isCloseTo(decimal(e.get("FinalizationAmount")), within(TOLERANCE))
                        .isCloseTo(d("700000"), within(TOLERANCE)),
                () -> assertThat(finance.tresorerie(etude, "Concretisation")).isNotEmpty()
                        .allSatisfy(l -> assertThat(l.get("IsCredit")).isEqualTo(Boolean.TRUE)));
    }

    // ------------------------------------------------------------------ §2.6 lecture et aperçu

    @Test
    void r2_6_get_tresorerie_200() throws Exception {
        UUID[] e = etude(fiche(0), "1");
        previsions(e[0], parUnite(cat.grosOeuvres(), e[1], "1000000", "0"));

        api.lire(TRESORERIE, e[0]).andExpect(status().isOk());
    }

    /** L'aperçu (écarts-types modifiés) répond sans écrire : les lignes stockées restent celles du dernier recalcul. */
    @Test
    void r2_6_calcul_apercu_nEcritRien() throws Exception {
        UUID[] e = etude(fiche(2), "1");
        previsions(e[0], parUnite(cat.grosOeuvres(), e[1], "1000000", "0"));
        List<BigDecimal> avant = finance.tresorerie(e[0], EXPENSE).stream().map(l -> decimal(l.get("Amount"))).toList();

        api.envoyer(CALCUL_TRESORERIE, e[0], corps().avec(ECART_TYPE_TRAVAUX, d("0.5")).json()).andExpect(status().isOk());

        assertThat(finance.tresorerie(e[0], EXPENSE).stream().map(l -> decimal(l.get("Amount"))).toList()).isEqualTo(avant);
    }

    @Test
    void r2_5_calcul_n3_sigmaSuperieurA1_400() throws Exception {
        UUID[] e = etude(fiche(2), "1");
        previsions(e[0], parUnite(cat.grosOeuvres(), e[1], "1000000", "0"));

        api.envoyer(CALCUL_TRESORERIE, e[0], corps().avec(ECART_TYPE_TRAVAUX, d("1.5")).json())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString(msgEcartType(3))));
    }

    @Test
    void r2_6_404_etudeInconnue() throws Exception {
        api.envoyer(CALCUL_TRESORERIE, UUID.randomUUID(), "{}").andExpect(status().isNotFound());
    }
}
