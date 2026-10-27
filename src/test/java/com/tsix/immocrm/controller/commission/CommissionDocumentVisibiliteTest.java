package com.tsix.immocrm.controller.commission;

import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.FixtureCommissions;
import com.tsix.immocrm.fixtures.FixtureCommissions.Compte;
import com.tsix.immocrm.fixtures.FixtureCommissions.Contexte;
import com.tsix.immocrm.fixtures.FixtureCommissions.Dossier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ApiCommissions.DOCUMENT_DE_LIGNE;
import static com.tsix.immocrm.fixtures.ApiCommissions.FACTURES;
import static com.tsix.immocrm.fixtures.ApiCommissions.lire;
import static com.tsix.immocrm.fixtures.FixtureCommissions.CONCRETISATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Montants d'un document à payer couvrant deux tranches (FEATURE-vente-commissions.md, réponses du 27/10) : un appelant
 * qui ne voit qu'une tranche reçoit la part de ses lignes, réglé au prorata ; un appelant qui voit tout, le document.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CommissionDocumentVisibiliteTest {

    private static final LocalDate AVANCE = LocalDate.of(2024, 2, 23);
    private static final BigDecimal PRIX = new BigDecimal("775000");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureCommissions fixture;

    private Contexte c;
    private UUID ligneIci;

    /** Document de 400 : 300 sur la tranche du contexte, 100 sur une tranche d'un autre projet ; 200 réglés. */
    @BeforeEach
    void setUp() {
        c = fixture.contexte("COMDV");
        UUID personne = fixture.personne("P-" + c.jeton(), false);
        CfgTranche ailleurs = fixture.autreTranche(c, "DV", true);
        Dossier ici = fixture.dossier(c, "DI", personne, PRIX, AVANCE);
        Dossier autre = fixture.dossier(c, ailleurs, "DA", personne, PRIX, AVANCE);
        UUID doc = fixture.document(personne, "DOC-" + c.jeton(), new BigDecimal("400"));
        ligneIci = fixture.ligne(ici, CONCRETISATION, personne, new BigDecimal("300"), AVANCE, doc);
        fixture.ligne(autre, CONCRETISATION, personne, new BigDecimal("100"), AVANCE, doc);
        fixture.reglement(doc, new BigDecimal("200"), "IN_WORK");
        fixture.relire();
    }

    private static BigDecimal montant(ResultActions r, String chemin) throws Exception {
        Object valeur = lire(r, chemin);
        return new BigDecimal(String.valueOf(valeur));
    }

    private static void verifier(ResultActions r, String racine, String montant, String regle, String reste) {
        assertAll(
                () -> assertThat(montant(r, racine + ".montant")).isEqualByComparingTo(montant),
                () -> assertThat(montant(r, racine + ".regle")).isEqualByComparingTo(regle),
                () -> assertThat(montant(r, racine + ".reste")).isEqualByComparingTo(reste));
    }

    @Test
    void documentMultiTranche_appelantRestreint_montantsDeLaPartVisible() throws Exception {
        Compte restreint = fixture.compte(c.jeton(), FixtureCommissions.ECRAN);
        fixture.affecter(restreint, c.projet());

        ResultActions parLigne = mockMvc.perform(get(DOCUMENT_DE_LIGNE, ligneIci).with(restreint.qui()))
                .andExpect(status().isOk());
        ResultActions factures = mockMvc.perform(get(FACTURES, c.tranche().getId()).with(restreint.qui()))
                .andExpect(status().isOk());

        assertAll(
                () -> verifier(parLigne, "$", "300", "150", "150"),
                () -> verifier(factures, "$[0]", "300", "150", "150"));
    }

    @Test
    void documentMultiTranche_appelantQuiVoitTout_montantComplet() throws Exception {
        Compte toutVoir = fixture.compte(c.jeton(), FixtureCommissions.ECRAN);

        ResultActions parLigne = mockMvc.perform(get(DOCUMENT_DE_LIGNE, ligneIci).with(toutVoir.qui()))
                .andExpect(status().isOk());

        verifier(parLigne, "$", "400", "200", "200");
    }
}
