package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.FixturePiecesLot;
import com.tsix.immocrm.fixtures.FixturePiecesLot.Cadre;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixturePiecesLot.GESTION_FACTURE_DECOMPTE;
import static org.assertj.core.api.Assertions.assertThat;

/** Revue du lot L3b-2b (FEATURE-lot-pieces-a-payer.md) : défauts trouvés hors des tests verrouillés. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotPiecesRevueTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePiecesLot pf;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager em;

    private PiecesLotSupport appels;
    private Cadre c;
    private RequestPostProcessor admin;

    @BeforeEach
    void setUp() {
        c = pf.cadre("PR");
        appels = new PiecesLotSupport(mockMvc);
        admin = connecte(pf.administrateur(c, "pr.admin"), "Administrateur");
    }

    private void maj(String sql, Object... parametres) {
        em.flush();
        jdbc.update(sql, parametres);
        em.clear();
    }

    // OMRAN Refund.AfterDelete → UpdateInvoices (Yes.Stl.Debit/Refund.cs l.101-115) : la facture source retrouve son net à payer
    // sans l'avoir supprimé (AmountToPay = AmountTTC + TotalRetainedAmount, AmountRefunded relu). Ici : facture 1000, avoir non
    // financier 100 → facture à 900 ; après suppression de l'avoir, la facture doit revenir à 1000.
    @Test
    void supprimerAvoir_recalculeLeNetAPayerDeLaFactureSource() throws Exception {
        UUID lot = pf.lot(c, "PR1", c.statutLibre());
        UUID facture = pf.facture(c, lot, "PRF", GESTION_FACTURE_DECOMPTE, "0", "0");
        UUID avoir = pf.avoir(c, lot, "PRA", c.enCours());
        maj("UPDATE BuyDocument SET InvoiceSourceId = ?, IsFinancialRefund = 0 WHERE PKey = ?", facture.toString(),
                avoir.toString());
        maj("UPDATE BuyDocument SET AmountToPay = 900 WHERE PKey = ?", facture.toString());

        Reponse r = appels.supprimerAvoir(admin, lot, avoir);

        em.flush();
        BigDecimal aPayer = jdbc.queryForObject("SELECT AmountToPay FROM BuyDocument WHERE PKey = ?", BigDecimal.class,
                facture.toString());
        assertThat(r.statut()).as(r.corps()).isEqualTo(204);
        assertThat(aPayer).as("net à payer de la facture source après suppression de l'avoir")
                .isEqualByComparingTo("1000");
    }
}
