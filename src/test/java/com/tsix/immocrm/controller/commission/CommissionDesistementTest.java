package com.tsix.immocrm.controller.commission;

import com.tsix.immocrm.fixtures.FixtureCommissions;
import com.tsix.immocrm.fixtures.FixtureCommissions.Contexte;
import com.tsix.immocrm.fixtures.FixtureCommissions.Dossier;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ApiCommissions.T_CONCRETISATION;
import static com.tsix.immocrm.fixtures.ApiCommissions.T_DESISTEMENT;
import static com.tsix.immocrm.fixtures.ApiCommissions.generation;
import static com.tsix.immocrm.fixtures.ApiCommissions.raisons;
import static com.tsix.immocrm.fixtures.FixtureCommissions.CONCRETISATION;
import static com.tsix.immocrm.fixtures.FixtureCommissions.DESISTEMENT;
import static com.tsix.immocrm.fixtures.FixtureCommissions.FINALISATION_1;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.decimal;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.guid;
import static com.tsix.immocrm.fixtures.FixtureTerrains.horodatage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Désistement — FEATURE-vente-commissions.md §2 R-D (une reprise par ligne non désistement : même personne, tranche, article,
 * date = {@code CancelDate}, montant opposé), §3.1 C2 (montant &lt; 0), C3 (au plus une reprise par origine, reprises
 * manquantes seules, {@code DOSSIER_DESISTE}), §8.1 « Désistement ». Lignes d'origine écrites en base par la fixture.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CommissionDesistementTest {

    private static final LocalDate AVANCE = LocalDate.of(2024, 2, 23);
    private static final LocalDate ANNULATION = LocalDate.of(2025, 3, 1);
    private static final BigDecimal PRIX = new BigDecimal("775000");
    private static final BigDecimal TROIS_CENTS = new BigDecimal("300");
    private static final BigDecimal CINQ_CENTS = new BigDecimal("500");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureCommissions fixture;

    private Contexte c;
    private RequestPostProcessor qui;
    private UUID commercial;
    private UUID gestionnaire;

    @BeforeEach
    void setUp() {
        c = fixture.contexte("COMD");
        qui = fixture.compte(c.jeton(), FixtureCommissions.ECRAN, FixtureCommissions.NOUVEAU).qui();
        commercial = fixture.personne("COM-" + c.jeton(), false);
        gestionnaire = fixture.personne("GES-" + c.jeton(), false);
    }

    private Dossier desiste(String code) {
        Dossier d = fixture.dossier(c, c.tranche(), code, fixture.statutDesiste(c), commercial, PRIX, AVANCE);
        fixture.poser(d, "CancelDate", ANNULATION);
        return d;
    }

    private ResultActions generer(String type, Dossier d) throws Exception {
        return mockMvc.perform(generation(c.tranche().getId(), type, List.of(d.id())).with(qui));
    }

    private static BigDecimal somme(List<Tuple> lignes) {
        return lignes.stream().map(l -> decimal(l.get("CommissionAmount"))).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sommeDesistementsDe(Dossier d, UUID personne) {
        return somme(fixture.lignesDuDossier(d.id(), DESISTEMENT).stream()
                .filter(l -> guid(personne).equals(guid(l.get("CfgEmployeeId")))).toList());
    }

    @Test
    void rd_uneRepriseParLigne_montantOppose_dateAnnulation_memePersonneTrancheArticle() throws Exception {
        Dossier d = desiste("D1");
        fixture.ligne(d, CONCRETISATION, commercial, TROIS_CENTS, AVANCE, null);
        fixture.ligne(d, FINALISATION_1, gestionnaire, CINQ_CENTS, LocalDate.of(2024, 6, 1), null);

        generer(T_DESISTEMENT, d).andExpect(status().isOk());

        List<Tuple> reprises = fixture.lignesDuDossier(d.id(), DESISTEMENT);
        assertAll(
                () -> assertThat(reprises).hasSize(2)
                        .allSatisfy(l -> assertAll(
                                () -> assertThat(horodatage(l.get("DocDate"))).isEqualTo(ANNULATION.atStartOfDay()),
                                () -> assertThat(guid(l.get("CfgTrancheId"))).isEqualTo(guid(c.tranche().getId())),
                                () -> assertThat(guid(l.get("StkItemId"))).isEqualTo(guid(d.articleId())),
                                () -> assertThat(decimal(l.get("CommissionAmount")).signum()).isNegative())),
                () -> assertThat(sommeDesistementsDe(d, commercial)).isEqualByComparingTo("-300"),
                () -> assertThat(sommeDesistementsDe(d, gestionnaire)).isEqualByComparingTo("-500"));
    }

    /** C3 : somme des désistements = −somme des origines. */
    @Test
    void c3_sommeDesDesistements_egaleMoinsSommeDesOrigines() throws Exception {
        Dossier d = desiste("D1");
        fixture.ligne(d, CONCRETISATION, commercial, new BigDecimal("232.5"), AVANCE, null);
        fixture.ligne(d, FINALISATION_1, gestionnaire, new BigDecimal("542.5"), LocalDate.of(2024, 6, 1), null);

        generer(T_DESISTEMENT, d).andExpect(status().isOk());

        List<Tuple> toutes = fixture.lignesDuDossier(d.id());
        List<Tuple> origines = toutes.stream().filter(l -> !DESISTEMENT.equals(l.get("CommissionType"))).toList();
        assertThat(somme(fixture.lignesDuDossier(d.id(), DESISTEMENT))).isEqualByComparingTo(somme(origines).negate());
    }

    @Test
    void c3_sansDateAnnulation_aucuneLigne_NON_DESISTE() throws Exception {
        Dossier d = fixture.dossier(c, "D1", commercial, PRIX, AVANCE);
        fixture.ligne(d, CONCRETISATION, commercial, TROIS_CENTS, AVANCE, null);

        ResultActions r = generer(T_DESISTEMENT, d).andExpect(status().isOk());

        assertAll(
                () -> assertThat(fixture.lignesDuDossier(d.id(), DESISTEMENT)).isEmpty(),
                () -> assertThat(raisons(r, d.id())).containsExactly("NON_DESISTE"));
    }

    @Test
    void c3_sansLigneAReprendre_SANS_COMMISSION_A_REPRENDRE() throws Exception {
        Dossier d = desiste("D1");

        ResultActions r = generer(T_DESISTEMENT, d).andExpect(status().isOk());

        assertAll(
                () -> assertThat(fixture.lignesDuDossier(d.id())).isEmpty(),
                () -> assertThat(raisons(r, d.id())).containsExactly("SANS_COMMISSION_A_REPRENDRE"));
    }

    @Test
    void c3_deuxiemeGeneration_nAjouteRien() throws Exception {
        Dossier d = desiste("D1");
        fixture.ligne(d, CONCRETISATION, commercial, TROIS_CENTS, AVANCE, null);
        generer(T_DESISTEMENT, d).andExpect(status().isOk());

        generer(T_DESISTEMENT, d).andExpect(status().isOk());

        assertAll(
                () -> assertThat(fixture.lignesDuDossier(d.id(), DESISTEMENT)).hasSize(1),
                () -> assertThat(sommeDesistementsDe(d, commercial)).isEqualByComparingTo("-300"));
    }

    /** C3 : une origine ajoutée après un premier désistement → seule sa reprise est créée. */
    @Test
    void c3_origineAjouteeApres_seuleLaRepriseManquante() throws Exception {
        Dossier d = desiste("D1");
        fixture.ligne(d, CONCRETISATION, commercial, TROIS_CENTS, AVANCE, null);
        fixture.ligne(d, DESISTEMENT, commercial, TROIS_CENTS.negate(), ANNULATION, null);
        fixture.ligne(d, FINALISATION_1, gestionnaire, CINQ_CENTS, LocalDate.of(2024, 6, 1), null);

        generer(T_DESISTEMENT, d).andExpect(status().isOk());

        assertAll(
                () -> assertThat(fixture.lignesDuDossier(d.id(), DESISTEMENT)).hasSize(2),
                () -> assertThat(sommeDesistementsDe(d, commercial)).isEqualByComparingTo("-300"),
                () -> assertThat(sommeDesistementsDe(d, gestionnaire)).isEqualByComparingTo("-500"));
    }

    /** C3 : concrétisation demandée sur un dossier qui a déjà un désistement → ignorée. */
    @Test
    void c3_concretisationSurDossierDejaDesiste_DOSSIER_DESISTE() throws Exception {
        fixture.regleMontant(c.tranche().getId(), new BigDecimal("100"));
        Dossier d = desiste("D1");
        fixture.ligne(d, DESISTEMENT, fixture.personne("ANC-" + c.jeton(), false), new BigDecimal("-50"), ANNULATION,
                null);

        ResultActions r = generer(T_CONCRETISATION, d).andExpect(status().isOk());

        assertAll(
                () -> assertThat(fixture.lignesDuDossier(d.id(), CONCRETISATION)).isEmpty(),
                () -> assertThat(raisons(r, d.id())).containsExactly("DOSSIER_DESISTE"));
    }
}
