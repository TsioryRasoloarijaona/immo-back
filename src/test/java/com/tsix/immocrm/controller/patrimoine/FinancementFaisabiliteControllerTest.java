package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.fixtures.ApiFaisabilite;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureFaisabilite;
import com.tsix.immocrm.fixtures.FixtureFaisabiliteFinance;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ApiFaisabilite.d;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.ID;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.MSG_BLOQUEE;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.fiche;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.jeton;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.lignes;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.yestest;
import static com.tsix.immocrm.fixtures.FixtureFaisabiliteFinance.*;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.decimal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Crédits et remboursements d'une étude ({@code GET/PUT …/financements}, patron P1b imbriqué) —
 * FEATURE-patrimoine-faisabilite-workflow.md §1.1, §1.2, §3 et invariants I1, I6 (lignes Funding / Refunding).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FinancementFaisabiliteControllerTest {

    private static final BigDecimal TOLERANCE = new BigDecimal("0.00001");
    private static final String CREDITS = "SELECT COUNT(*) FROM FSFunding WHERE FeasibilityStudyId = :id";
    private static final String REMBOURSEMENTS_ETUDE = "SELECT COUNT(*) FROM FSReimbursement r "
            + "JOIN FSFunding f ON f.PKey = r.FSFundingId WHERE f.FeasibilityStudyId = :id";
    private static final String PREMIER = "$[0].";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureFaisabilite fixture;
    @Autowired private FixtureFaisabiliteFinance finance;

    private ApiFaisabilite api;
    private UUID etude;

    @BeforeEach
    void setUp() throws Exception {
        fixture.parametrage();
        api = new ApiFaisabilite(mockMvc, patrimoine.administrateur());
        String jeton = jeton();
        CfgCompany societe = patrimoine.entite("ENT-" + jeton, true);
        etude = api.etude(yestest(fiche("FIN-" + jeton, societe.getId())));
    }

    private static CorpsJson avecRemboursements(CorpsJson credit, CorpsJson... remboursements) {
        return credit.avec(REMBOURSEMENTS, List.of(remboursements));
    }

    private String enregistrer(CorpsJson... credits) throws Exception {
        return api.remplacer(FINANCEMENTS, etude, lignes(credits)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private static BigDecimal nombre(String json, String chemin) {
        Object v = JsonPath.read(json, chemin);
        assertThat(v).as(chemin).isNotNull();
        return new BigDecimal(v.toString());
    }

    // ------------------------------------------------------------------ §1.2 création imbriquée, calculés

    /** Montant à régler = Σ à régler des remboursements ; total des intérêts ; écart de base = Σ base − déblocage. */
    @Test
    void r1_2_creation_calculesDuCredit_montantARegler_totalInterets_ecartBase() throws Exception {
        String json = enregistrer(avecRemboursements(credit("2026-01-15", "3000", 2, 1, 0, "0.1"),
                remboursement("2026-01-15", "1000", "0.1"), remboursement("2026-02-15", "1500", "0.1")));

        assertAll(
                () -> assertThat(nombre(json, PREMIER + MONTANT_A_REGLER)).isCloseTo(d("2750"), within(TOLERANCE)),
                () -> assertThat(nombre(json, PREMIER + TOTAL_INTERETS)).isCloseTo(d("250"), within(TOLERANCE)),
                () -> assertThat(nombre(json, PREMIER + ECART_BASE)).isCloseTo(d("-500"), within(TOLERANCE)),
                () -> assertThat(fixture.compter(CREDITS, etude)).isEqualTo(1),
                () -> assertThat(fixture.compter(REMBOURSEMENTS_ETUDE, etude)).isEqualTo(2));
    }

    /** I1 : à régler = base + intérêts, intérêts = base × taux ; {@code montantARegler} n'est jamais saisi. */
    @Test
    void i1_remboursement_aReglerEgalBasePlusInterets_aReglerSaisiIgnore() throws Exception {
        enregistrer(avecRemboursements(credit("2026-01-15", "1000", 1, 1, 0, "0.05"),
                remboursement("2026-01-15", "1000", "0.05").avec(MONTANT_A_REGLER, d("1"))));

        assertThat(remboursementsStockes()).singleElement().satisfies(r -> assertAll(
                () -> assertThat(decimal(r.get("amountInterest"))).isEqualByComparingTo("50"),
                () -> assertThat(decimal(r.get("amountToPay"))).isEqualByComparingTo("1050"),
                () -> assertThat(decimal(r.get("interestRate"))).isEqualByComparingTo("0.05")));
    }

    /** §1.1 : si le corps porte le taux et les intérêts, le taux gagne. */
    @Test
    void r1_1_tauxEtInterets_leTauxGagne() throws Exception {
        enregistrer(avecRemboursements(credit("2026-01-15", "1000", 1, 1, 0, "0.05"),
                remboursement("2026-01-15", "1000", "0.05").avec(MONTANT_INTERETS, d("999"))));

        assertThat(remboursementsStockes()).singleElement().satisfies(r -> assertAll(
                () -> assertThat(decimal(r.get("amountInterest"))).isEqualByComparingTo("50"),
                () -> assertThat(decimal(r.get("amountToPay"))).isEqualByComparingTo("1050")));
    }

    /** §1.1 : intérêts saisis seuls → le taux est recalculé (base ≠ 0). */
    @Test
    void r1_1_interetsSeuls_tauxRecalcule() throws Exception {
        enregistrer(avecRemboursements(credit("2026-01-15", "1000", 1, 1, 0, "0"),
                remboursement("2026-01-15", "1000", "0").sans(TAUX_INTERET).avec(MONTANT_INTERETS, d("30"))));

        assertThat(remboursementsStockes()).singleElement().satisfies(r -> assertAll(
                () -> assertThat(decimal(r.get("interestRate"))).isEqualByComparingTo("0.03"),
                () -> assertThat(decimal(r.get("amountInterest"))).isEqualByComparingTo("30"),
                () -> assertThat(decimal(r.get("amountToPay"))).isEqualByComparingTo("1030")));
    }

    /** §1.1 : texte '' et nombres 0 jamais NULL ; périodicité &lt; 1 stockée 1. */
    @Test
    void r1_1_valeursParDefaut_descriptionVide_grace0_periodicite1() throws Exception {
        enregistrer(credit("2026-01-15", "1000", 1, 0, 0, "0").sans(PERIODE_GRACE_MOIS).sans(TAUX_CHARGE));

        assertThat(creditsStockes()).singleElement().satisfies(c -> assertAll(
                () -> assertThat(c.get("Description")).isEqualTo(""),
                () -> assertThat(((Number) c.get("monthsStep")).intValue()).isEqualTo(1),
                () -> assertThat(((Number) c.get("gracePeriod")).intValue()).isZero(),
                () -> assertThat(decimal(c.get("chargeRatio"))).isEqualByComparingTo("0"),
                () -> assertThat(decimal(c.get("amount"))).isEqualByComparingTo("1000")));
    }

    // ------------------------------------------------------------------ §1.2 réconciliation par id (patron P1b)

    @Test
    void r1_2_reconciliation_idConnuModifie_sansIdCree_absentSupprime() throws Exception {
        String premier = enregistrer(
                avecRemboursements(credit("2026-01-15", "1000", 1, 1, 0, "0"), remboursement("2026-01-15", "1000", "0")),
                credit("2026-06-15", "500", 0, 1, 0, "0").avec(DESCRIPTION, "A supprimer"));
        String garde = JsonPath.read(premier, "$[?(@.description != 'A supprimer')].id").toString()
                .replaceAll("[\\[\\]\"]", "");

        enregistrer(credit("2026-01-15", "2000", 1, 1, 0, "0").avec(ID, UUID.fromString(garde)),
                credit("2026-09-15", "700", 0, 1, 0, "0").avec(DESCRIPTION, "Nouveau"));

        assertAll(
                () -> assertThat(creditsStockes()).extracting(c -> decimal(c.get("amount")).stripTrailingZeros().toPlainString())
                        .containsExactlyInAnyOrder("2000", "700"),
                () -> assertThat(fixture.compter(CREDITS + " AND PKey = '" + garde + "'", etude)).isEqualTo(1),
                () -> assertThat(fixture.compter(CREDITS + " AND Description = N'A supprimer'", etude)).isZero(),
                () -> assertThat(fixture.compter(REMBOURSEMENTS_ETUDE, etude)).as("remboursement absent du corps supprimé")
                        .isZero());
    }

    @Test
    void r1_2_reconciliationDesRemboursements_idConnuModifie_absentSupprime() throws Exception {
        String json = enregistrer(avecRemboursements(credit("2026-01-15", "2000", 2, 1, 0, "0"),
                remboursement("2026-01-15", "1000", "0"), remboursement("2026-02-15", "1000", "0")));
        String creditId = JsonPath.read(json, "$[0].id");
        String rembId = JsonPath.read(json, "$[0].remboursements[?(@.date =~ /2026-01-15.*/)].id").toString()
                .replaceAll("[\\[\\]\"]", "");

        enregistrer(avecRemboursements(credit("2026-01-15", "2000", 2, 1, 0, "0").avec(ID, UUID.fromString(creditId)),
                remboursement("2026-01-15", "1200", "0").avec(ID, UUID.fromString(rembId))));

        assertThat(remboursementsStockes()).singleElement().satisfies(r -> assertAll(
                () -> assertThat(String.valueOf(r.get("id"))).isEqualToIgnoringCase(rembId),
                () -> assertThat(decimal(r.get("amountBase"))).isEqualByComparingTo("1200")));
    }

    @Test
    void r1_2_get_listeParDate_avecRemboursements() throws Exception {
        enregistrer(credit("2026-09-15", "700", 0, 1, 0, "0").avec(DESCRIPTION, "Second"),
                avecRemboursements(credit("2026-01-15", "1000", 1, 1, 0, "0").avec(DESCRIPTION, "Premier"),
                        remboursement("2026-01-15", "1000", "0")));

        api.lire(FINANCEMENTS, etude)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].description").value("Premier"))
                .andExpect(jsonPath("$[1].description").value("Second"))
                .andExpect(jsonPath("$[0].remboursements.length()").value(1));
    }

    // ------------------------------------------------------------------ §1.1 refus (400) ; §1.2 404, 409

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {MONTANT, NOMBRE_REMBOURSEMENTS, TAUX_CHARGE})
    void r1_1_credit_400_valeurNegative(String champ) throws Exception {
        CorpsJson credit = credit("2026-01-15", "1000", 1, 1, 0, "0").avec(champ, -1);

        api.remplacer(FINANCEMENTS, etude, lignes(credit)).andExpect(status().isBadRequest());

        assertThat(fixture.compter(CREDITS, etude)).isZero();
    }

    @Test
    void r1_1_credit_400_dateDeblocageAbsente() throws Exception {
        api.remplacer(FINANCEMENTS, etude, lignes(credit("2026-01-15", "1000", 1, 1, 0, "0").sans(DATE_DEBLOCAGE)))
                .andExpect(status().isBadRequest());

        assertThat(fixture.compter(CREDITS, etude)).isZero();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {MONTANT_BASE, TAUX_INTERET})
    void i1_remboursement_400_valeurNegative(String champ) throws Exception {
        CorpsJson credit = avecRemboursements(credit("2026-01-15", "1000", 1, 1, 0, "0"),
                remboursement("2026-01-15", "1000", "0").avec(champ, -1));

        api.remplacer(FINANCEMENTS, etude, lignes(credit)).andExpect(status().isBadRequest());

        assertThat(fixture.compter(CREDITS, etude)).isZero();
    }

    @Test
    void r1_1_remboursement_400_dateAbsente() throws Exception {
        CorpsJson credit = avecRemboursements(credit("2026-01-15", "1000", 1, 1, 0, "0"),
                remboursement("2026-01-15", "1000", "0").sans(DATE));

        api.remplacer(FINANCEMENTS, etude, lignes(credit)).andExpect(status().isBadRequest());

        assertThat(fixture.compter(CREDITS, etude)).isZero();
    }

    @Test
    void r1_2_404_etudeInconnue() throws Exception {
        api.lire(FINANCEMENTS, UUID.randomUUID()).andExpect(status().isNotFound());
        api.remplacer(FINANCEMENTS, UUID.randomUUID(), lignes(new String[0])).andExpect(status().isNotFound());
    }

    @Test
    void r1_2_409_etudeBloquee_rienEcrit() throws Exception {
        api.bloquer(etude).andExpect(status().is2xxSuccessful());

        api.remplacer(FINANCEMENTS, etude, lignes(credit("2026-01-15", "1000", 1, 1, 0, "0")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString(MSG_BLOQUEE)));

        assertThat(fixture.compter(CREDITS, etude)).isZero();
    }

    // ------------------------------------------------------------------ §3 recalcul dans la transaction (I6)

    /** I6 : une ligne Funding par crédit à sa date (crédit), une ligne Refunding par remboursement (débit, montant à régler). */
    @Test
    void i6_lignesDeTresorerie_fundingParCredit_refundingParRemboursement() throws Exception {
        enregistrer(avecRemboursements(credit("2026-01-15", "2000", 2, 1, 0, "0.1"),
                remboursement("2026-02-15", "1000", "0.1"), remboursement("2026-03-15", "1000", "0.1")));

        List<Tuple> funding = finance.tresorerie(etude, "Funding");
        List<Tuple> refunding = finance.tresorerie(etude, "Refunding");
        assertAll(
                () -> assertThat(funding).singleElement().satisfies(l -> assertAll(
                        () -> assertThat(decimal(l.get("Amount"))).isEqualByComparingTo("2000"),
                        () -> assertThat(l.get("IsCredit")).isEqualTo(Boolean.TRUE),
                        () -> assertThat(String.valueOf(l.get("DocDate"))).startsWith("2026-01-15"))),
                () -> assertThat(refunding).hasSize(2).allSatisfy(l -> assertAll(
                        () -> assertThat(decimal(l.get("Amount"))).isEqualByComparingTo("1100"),
                        () -> assertThat(l.get("IsCredit")).isEqualTo(Boolean.FALSE))),
                () -> assertThat(refunding).extracting(l -> String.valueOf(l.get("DocDate")).substring(0, 10))
                        .containsExactly("2026-02-15", "2026-03-15"));
    }

    /** I6 : Σ Refunding = Σ montantARegler, après une modification (les lignes sont supprimées puis réinsérées). */
    @Test
    void i6_sommeRefunding_egaleSommeARegler_apresModification() throws Exception {
        String json = enregistrer(avecRemboursements(credit("2026-01-15", "2000", 2, 1, 0, "0.1"),
                remboursement("2026-02-15", "1000", "0.1"), remboursement("2026-03-15", "1000", "0.1")));
        String creditId = JsonPath.read(json, "$[0].id");

        String apres = enregistrer(avecRemboursements(credit("2026-01-15", "2000", 2, 1, 0, "0.1")
                        .avec(ID, UUID.fromString(creditId)), remboursement("2026-02-15", "2000", "0.2")));

        assertThat(finance.somme("SELECT SUM(Amount) FROM StkFSPrevisionLine WHERE StkFeasibilityStudyId = :etude "
                + "AND PrevisionType = 'Refunding'", etude))
                .isCloseTo(nombre(apres, PREMIER + MONTANT_A_REGLER), within(TOLERANCE))
                .isCloseTo(d("2400"), within(TOLERANCE));
    }

    // ------------------------------------------------------------------ lecture des lignes stockées

    private List<Tuple> creditsStockes() {
        return fixture.lignes("""
                SELECT Description, monthsStep, gracePeriod, chargeRatio, amount FROM FSFunding WHERE FeasibilityStudyId = :id""",
                etude);
    }

    private List<Tuple> remboursementsStockes() {
        return fixture.lignes("""
                SELECT CONVERT(varchar(36), r.PKey) AS id, r.amountBase, r.interestRate, r.amountInterest, r.amountToPay
                FROM FSReimbursement r JOIN FSFunding f ON f.PKey = r.FSFundingId WHERE f.FeasibilityStudyId = :id""", etude);
    }
}
