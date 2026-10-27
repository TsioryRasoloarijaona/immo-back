package com.tsix.immocrm.controller.commission;

import com.tsix.immocrm.fixtures.FixtureCommissions;
import com.tsix.immocrm.fixtures.FixtureCommissions.Contexte;
import com.tsix.immocrm.fixtures.FixtureCommissions.Dossier;
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
import java.util.stream.Stream;

import static com.tsix.immocrm.fixtures.ApiCommissions.DETAIL;
import static com.tsix.immocrm.fixtures.ApiCommissions.montant;
import static com.tsix.immocrm.fixtures.ApiCommissions.nombre;
import static com.tsix.immocrm.fixtures.ApiCommissions.suppression;
import static com.tsix.immocrm.fixtures.FixtureCommissions.CONCRETISATION;
import static com.tsix.immocrm.fixtures.FixtureCommissions.DESISTEMENT;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.decimal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Modifier le montant et supprimer — FEATURE-vente-commissions.md §2 R-X, §3.1 C4 (ligne documentée figée, 409), C5 (signe
 * du type, zéro refusé, 400), §4 ({@code PUT …/montant}, {@code POST …/suppression} tout ou rien, ≤ 500), §8.1 « C4/C5 ».
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CommissionMontantSuppressionTest {

    private static final String MODIFICATION_DOCUMENTEE =
            "vous ne pouvez pas modifier le montant d'une commission qui a un document à régler!";
    private static final String SUPPRESSION_DOCUMENTEE = "Vous ne pouvez pas supprimer une ligne associée à un document !";
    private static final LocalDate JOUR = LocalDate.of(2024, 2, 23);
    private static final BigDecimal TROIS_CENTS = new BigDecimal("300");
    private static final String MONTANT_EN_BASE = "CommissionAmount";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureCommissions fixture;

    private RequestPostProcessor qui;
    private UUID commercial;
    private Dossier d;

    @BeforeEach
    void setUp() {
        Contexte c = fixture.contexte("COMM");
        qui = fixture.compte(c.jeton(), FixtureCommissions.ECRAN, FixtureCommissions.NOUVEAU,
                FixtureCommissions.SUPPRIMER).qui();
        commercial = fixture.personne("COM-" + c.jeton(), false);
        d = fixture.dossier(c, "D1", commercial, new BigDecimal("775000"), JOUR);
    }

    private UUID concretisation(UUID document) {
        return fixture.ligne(d, CONCRETISATION, commercial, TROIS_CENTS, JOUR, document);
    }

    private UUID documentee() {
        return concretisation(fixture.document(commercial, "DOC-" + UUID.randomUUID().toString().substring(0, 8),
                TROIS_CENTS));
    }

    private BigDecimal montantEnBase(UUID ligne) {
        return decimal(fixture.ligneParId(ligne).get(MONTANT_EN_BASE));
    }

    private int lignesExistantes(List<UUID> ids) {
        return (int) ids.stream().filter(id -> fixture.compter("SELECT COUNT(*) FROM ComCommissionLine WHERE PKey = :id",
                id) == 1).count();
    }

    // ------------------------------------------------------------------ PUT montant

    @Test
    void p4_modifierMontant_200_ligneRelueEtEcrite() throws Exception {
        UUID ligne = concretisation(null);

        ResultActions r = mockMvc.perform(montant(ligne, new BigDecimal("250.75")).with(qui))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ligne.toString()))
                .andExpect(jsonPath("$.modifiable").value(true));

        assertAll(
                () -> assertThat(nombre(r, "$.montant")).isEqualByComparingTo("250.75"),
                () -> assertThat(montantEnBase(ligne)).isEqualByComparingTo("250.75"));
    }

    @Test
    void c4_modifierMontantDUneLigneDocumentee_409MessageExact_inchange() throws Exception {
        UUID ligne = documentee();

        mockMvc.perform(montant(ligne, new BigDecimal("250")).with(qui))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(DETAIL).value(MODIFICATION_DOCUMENTEE));

        assertThat(montantEnBase(ligne)).isEqualByComparingTo(TROIS_CENTS);
    }

    @Test
    void c5_montantNegatifSurConcretisation_400_doitEtrePositif() throws Exception {
        UUID ligne = concretisation(null);

        mockMvc.perform(montant(ligne, new BigDecimal("-10")).with(qui))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString("doit être positif")));

        assertThat(montantEnBase(ligne)).isEqualByComparingTo(TROIS_CENTS);
    }

    @Test
    void c5_montantZero_400() throws Exception {
        UUID ligne = concretisation(null);

        mockMvc.perform(montant(ligne, BigDecimal.ZERO).with(qui)).andExpect(status().isBadRequest());

        assertThat(montantEnBase(ligne)).isEqualByComparingTo(TROIS_CENTS);
    }

    @Test
    void c5_montantPositifSurDesistement_400_doitEtreNegatif() throws Exception {
        UUID ligne = fixture.ligne(d, DESISTEMENT, commercial, TROIS_CENTS.negate(), JOUR, null);

        mockMvc.perform(montant(ligne, new BigDecimal("10")).with(qui))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString("doit être négatif")));

        assertThat(montantEnBase(ligne)).isEqualByComparingTo("-300");
    }

    @Test
    void c5_montantNegatifSurDesistement_200() throws Exception {
        UUID ligne = fixture.ligne(d, DESISTEMENT, commercial, TROIS_CENTS.negate(), JOUR, null);

        mockMvc.perform(montant(ligne, new BigDecimal("-120")).with(qui)).andExpect(status().isOk());

        assertThat(montantEnBase(ligne)).isEqualByComparingTo("-120");
    }

    @Test
    void p4_modifierMontant_ligneInconnue_404() throws Exception {
        mockMvc.perform(montant(UUID.randomUUID(), BigDecimal.TEN).with(qui)).andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ POST suppression

    @Test
    void p4_suppression_204_lignesEffacees() throws Exception {
        List<UUID> ids = List.of(concretisation(null), fixture.ligne(d, DESISTEMENT, commercial, TROIS_CENTS.negate(),
                JOUR, null));

        mockMvc.perform(suppression(ids).with(qui)).andExpect(status().isNoContent());

        assertThat(lignesExistantes(ids)).isZero();
    }

    @Test
    void c4_suppressionDUneLigneDocumentee_409MessageExact_rienSupprime() throws Exception {
        UUID ligne = documentee();

        mockMvc.perform(suppression(List.of(ligne)).with(qui))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(DETAIL).value(SUPPRESSION_DOCUMENTEE));

        assertThat(lignesExistantes(List.of(ligne))).isEqualTo(1);
    }

    /** §4 « tout ou rien » : N lignes dont une documentée → aucune supprimée. */
    @Test
    void c4_suppressionDeNLignesDontUneDocumentee_409_rienSupprime() throws Exception {
        List<UUID> ids = List.of(concretisation(null), documentee(),
                fixture.ligne(d, DESISTEMENT, commercial, TROIS_CENTS.negate(), JOUR, null));

        mockMvc.perform(suppression(ids).with(qui)).andExpect(status().isConflict());

        assertThat(lignesExistantes(ids)).isEqualTo(3);
    }

    @Test
    void p4_suppression_plusDe500Lignes_400() throws Exception {
        List<UUID> ids = Stream.generate(UUID::randomUUID).limit(501).toList();

        mockMvc.perform(suppression(ids).with(qui)).andExpect(status().isBadRequest());
    }
}
