package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.fixtures.ApiFaisabilite;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureFaisabilite;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureFaisabilite.fiche;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.jeton;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.yestest;
import static com.tsix.immocrm.fixtures.FixtureFaisabiliteFinance.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Échéancier d'un crédit, bouton « Générer » ({@code POST …/financements/echeancier}, aucune écriture) —
 * FEATURE-patrimoine-faisabilite-workflow.md §1.1, §1.4, invariant I2. Tolérance 1e-5 (arrondi au stockage seulement).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FinancementFaisabiliteTest {

    private static final BigDecimal TOLERANCE = new BigDecimal("0.00001");
    private static final String FINANCEMENTS_ETUDE = "SELECT COUNT(*) FROM FSFunding WHERE FeasibilityStudyId = :id";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureFaisabilite fixture;

    private ApiFaisabilite api;
    private UUID etude;

    @BeforeEach
    void setUp() throws Exception {
        fixture.parametrage();
        api = new ApiFaisabilite(mockMvc, patrimoine.administrateur());
        String jeton = jeton();
        CfgCompany societe = patrimoine.entite("ENT-" + jeton, true);
        etude = api.etude(yestest(fiche("ECH-" + jeton, societe.getId())));
    }

    private List<Map<String, Object>> echeancier(CorpsJson demande) throws Exception {
        String json = api.envoyer(ECHEANCIER, etude, demande.json()).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$");
    }

    private static BigDecimal nombre(Map<String, Object> ligne, String champ) {
        Object v = ligne.get(champ);
        assertThat(v).as(champ).isNotNull();
        return new BigDecimal(v.toString());
    }

    private static BigDecimal somme(List<Map<String, Object>> lignes, String champ) {
        return lignes.stream().map(l -> nombre(l, champ)).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static List<String> dates(List<Map<String, Object>> lignes) {
        return lignes.stream().map(l -> String.valueOf(l.get(DATE)).substring(0, 10)).toList();
    }

    // ------------------------------------------------------------------ §1.4 exemple chiffré

    /** 16 000 000, 24 remboursements, pas 1, grâce 0, taux 0,06, 17/03/2017 → 24 × (666 666,666667 + 40 000) = 706 666,666667. */
    @Test
    void r1_4_exempleChiffre_24Lignes_706666_666667() throws Exception {
        List<Map<String, Object>> lignes = echeancier(credit("2017-03-17", "16000000", 24, 1, 0, "0.06"));

        assertThat(lignes).hasSize(24).allSatisfy(l -> assertAll(
                () -> assertThat(nombre(l, MONTANT_BASE)).isCloseTo(new BigDecimal("666666.666667"), within(TOLERANCE)),
                () -> assertThat(nombre(l, TAUX_INTERET)).isEqualByComparingTo("0.06"),
                () -> assertThat(nombre(l, MONTANT_INTERETS)).isCloseTo(new BigDecimal("40000"), within(TOLERANCE)),
                () -> assertThat(nombre(l, MONTANT_A_REGLER)).isCloseTo(new BigDecimal("706666.666667"), within(TOLERANCE))));
    }

    @Test
    void r1_4_exempleChiffre_datesDu17_03_2017_au17_02_2019() throws Exception {
        List<String> dates = dates(echeancier(credit("2017-03-17", "16000000", 24, 1, 0, "0.06")));

        assertThat(dates).hasSize(24).startsWith("2017-03-17", "2017-04-17").endsWith("2019-01-17", "2019-02-17");
    }

    /** Totaux : intérêts 960 000, à régler 16 960 000 ; Σ base = déblocage (I2). */
    @Test
    void r1_4_totaux_interets960000_aRegler16960000_sommeBaseEgaleDeblocage() throws Exception {
        List<Map<String, Object>> lignes = echeancier(credit("2017-03-17", "16000000", 24, 1, 0, "0.06"));

        assertAll(
                () -> assertThat(somme(lignes, MONTANT_INTERETS)).isCloseTo(new BigDecimal("960000"), within(TOLERANCE)),
                () -> assertThat(somme(lignes, MONTANT_A_REGLER)).isCloseTo(new BigDecimal("16960000"), within(TOLERANCE)),
                () -> assertThat(somme(lignes, MONTANT_BASE)).isCloseTo(new BigDecimal("16000000"), within(TOLERANCE)));
    }

    // ------------------------------------------------------------------ §1.1 dates

    @Test
    void r1_4_grace2_premiereDate17_05_2017() throws Exception {
        List<String> dates = dates(echeancier(credit("2017-03-17", "16000000", 24, 1, 2, "0.06")));

        assertThat(dates).hasSize(24).startsWith("2017-05-17", "2017-06-17");
    }

    @Test
    void r1_1_periodicite2_unEcheanceTousLesDeuxMois() throws Exception {
        List<String> dates = dates(echeancier(credit("2026-01-15", "3000", 3, 2, 0, "0")));

        assertThat(dates).containsExactly("2026-01-15", "2026-03-15", "2026-05-15");
    }

    /** {@code AddMonths} depuis la première date : jour borné au dernier du mois, sans dériver d'une échéance à l'autre. */
    @Test
    void r1_1_finDeMois_jourBorne_sansDerive() throws Exception {
        List<String> dates = dates(echeancier(credit("2026-01-31", "3000", 3, 1, 0, "0")));

        assertThat(dates).containsExactly("2026-01-31", "2026-02-28", "2026-03-31");
    }

    @Test
    void r1_1_periodiciteInferieureA1_devient1() throws Exception {
        List<String> dates = dates(echeancier(credit("2026-01-15", "2000", 2, 0, 0, "0")));

        assertThat(dates).containsExactly("2026-01-15", "2026-02-15");
    }

    /** Intérêt = base × taux, appliqué à chaque échéance (pas un taux annuel). */
    @Test
    void r1_1_interetParEcheance_baseFoisTaux() throws Exception {
        List<Map<String, Object>> lignes = echeancier(credit("2026-01-15", "1200", 12, 1, 0, "0.1"));

        assertThat(lignes).hasSize(12).allSatisfy(l -> assertAll(
                () -> assertThat(nombre(l, MONTANT_BASE)).isCloseTo(new BigDecimal("100"), within(TOLERANCE)),
                () -> assertThat(nombre(l, MONTANT_INTERETS)).isCloseTo(new BigDecimal("10"), within(TOLERANCE)),
                () -> assertThat(nombre(l, MONTANT_A_REGLER)).isCloseTo(new BigDecimal("110"), within(TOLERANCE))));
    }

    // ------------------------------------------------------------------ §1.2 sans écriture ; refus

    @Test
    void r1_2_echeancier_nEcritRien() throws Exception {
        echeancier(credit("2017-03-17", "16000000", 24, 1, 0, "0.06"));

        assertThat(fixture.compter(FINANCEMENTS_ETUDE, etude)).isZero();
    }

    @Test
    void r1_2_echeancier_404_etudeInconnue() throws Exception {
        api.envoyer(ECHEANCIER, UUID.randomUUID(), credit("2017-03-17", "1000", 2, 1, 0, "0").json())
                .andExpect(status().isNotFound());
    }
}
