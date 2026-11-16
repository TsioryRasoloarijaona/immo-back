package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.fixtures.FixtureConfigurationAchat;
import com.tsix.immocrm.fixtures.FixtureStatutsAchat;
import com.tsix.immocrm.fixtures.FixtureStatutsLot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.STATUT;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.detail;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.ligne;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.suppression;
import static com.tsix.immocrm.fixtures.FixtureStatutsLot.CHAMP_CAUSE_RESILIATION;
import static com.tsix.immocrm.fixtures.FixtureStatutsLot.ENTREPRENEURS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Suppression d'un statut de lot — FEATURE-restes-projet-achat.md §6 : 204 et ses {@code StrStatusMandatoryField} partent avec
 * lui ; 409 avec les comptes si un lot ({@code PrjMarket.StrStatusId}), une transition ({@code StrWorkFlow} départ ou arrivée)
 * ou un historique ({@code StrHistory}) le cite — le service teste l'existence (yesRemsTest n'a pas les FK) ; 404 id inconnu.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StatutsLotSuppressionTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureStatutsLot fixture;
    @Autowired private FixtureStatutsAchat comptes;

    private String jeton;
    private RequestPostProcessor admin;
    private UUID statut;
    private UUID autre;

    @BeforeEach
    void setUp() {
        jeton = FixtureConfigurationAchat.jeton("F1D");
        fixture.genres();
        admin = comptes.administrateur(jeton);
        statut = fixture.statut(ENTREPRENEURS, "F1D-" + jeton, 1);
        autre = fixture.statut(ENTREPRENEURS, "F1D2-" + jeton, 2);
    }

    private MvcResult refuse409() throws Exception {
        return mockMvc.perform(suppression(STATUT, statut).with(admin)).andExpect(status().isConflict()).andReturn();
    }

    @Test
    void suppression_statutLibre_204_absentDeLaListe_champsObligatoiresSupprimes() throws Exception {
        fixture.champObligatoire(statut, CHAMP_CAUSE_RESILIATION);

        mockMvc.perform(suppression(STATUT, statut).with(admin)).andExpect(status().isNoContent());

        assertAll(
                () -> assertThat(fixture.statutsParId(statut)).isZero(),
                () -> assertThat(fixture.champsObligatoires(statut)).isEmpty(),
                () -> assertThat(ligne(mockMvc, admin, statut)).isNull());
    }

    /** 409 avec les comptes : deux lots sur le statut → le {@code detail} donne 2 ; statut et lots conservés. */
    @Test
    void suppression_statutCiteParDesLots_409_avecLeCompte_rienSupprime() throws Exception {
        fixture.lot("L1-" + jeton, ENTREPRENEURS, statut);
        fixture.lot("L2-" + jeton, ENTREPRENEURS, statut);

        MvcResult r = refuse409();

        assertAll(
                () -> assertThat(detail(r)).containsPattern("\\b2\\b"),
                () -> assertThat(fixture.statutsParId(statut)).isEqualTo(1));
    }

    @Test
    void suppression_statutDepartDUneTransition_409() throws Exception {
        fixture.transition(ENTREPRENEURS, "Aller " + jeton, statut, autre);

        refuse409();

        assertThat(fixture.statutsParId(statut)).isEqualTo(1);
    }

    @Test
    void suppression_statutArriveeDUneTransition_409() throws Exception {
        fixture.transition(ENTREPRENEURS, "Retour " + jeton, autre, statut);

        refuse409();

        assertThat(fixture.statutsParId(statut)).isEqualTo(1);
    }

    @Test
    void suppression_statutCiteParLHistorique_409() throws Exception {
        fixture.historique(statut, ENTREPRENEURS, LocalDateTime.of(2024, 5, 2, 10, 0));

        refuse409();

        assertThat(fixture.statutsParId(statut)).isEqualTo(1);
    }

    /** Un refus 409 laisse aussi les champs obligatoires du statut. */
    @Test
    void suppression_refusee_champsObligatoiresConserves() throws Exception {
        fixture.champObligatoire(statut, CHAMP_CAUSE_RESILIATION);
        fixture.lot("L3-" + jeton, ENTREPRENEURS, statut);

        refuse409();

        assertThat(fixture.champsObligatoires(statut)).hasSize(1);
    }

    /** 404 sur id inconnu ; témoin 204 sur un statut libre (la route existe). */
    @Test
    void suppression_idInconnu_404_temoin204() throws Exception {
        mockMvc.perform(suppression(STATUT, autre).with(admin)).andExpect(status().isNoContent());
        mockMvc.perform(suppression(STATUT, UUID.randomUUID()).with(admin)).andExpect(status().isNotFound());
    }
}
