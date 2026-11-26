package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.FixtureFactureDecompte;
import com.tsix.immocrm.fixtures.FixturePiecesLot;
import com.tsix.immocrm.fixtures.FixturePiecesLot.Cadre;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.FactureDecompteSupport.AVOIR;
import static com.tsix.immocrm.controller.projet.FactureDecompteSupport.FACTURE;
import static com.tsix.immocrm.controller.projet.FactureDecompteSupport.factureId;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

/** Second tour (règle 6) de FEATURE-lc8-facture-decompte.md : effet de l'avoir sur la facture et contrat Swagger. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FactureDecompteRevue3Test {

    private static final String A_PAYER = "AmountToPay";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePiecesLot pf;
    @Autowired private FixtureFactureDecompte fd;

    private FactureDecompteSupport appels;
    private Cadre c;
    private RequestPostProcessor admin;

    @BeforeEach
    void setUp() {
        c = fd.cadre("R3");
        appels = new FactureDecompteSupport(mockMvc);
        admin = connecte(pf.administrateur(c, "r3.admin"), "Administrateur");
    }

    private UUID genererOk(UUID decompte, String route) throws Exception {
        fd.flush();
        Reponse r = appels.generer(admin, decompte, route);
        assertThat(r.statut()).as(r.corps()).isEqualTo(201);
        return factureId(r);
    }

    private static BigDecimal montant(Map<String, Object> ligne, String colonne) {
        Object v = ligne.get(colonne);
        return v == null ? BigDecimal.ZERO : new BigDecimal(v.toString());
    }

    // R6 : un avoir non financier réduit le net à payer de la facture d'origine de son TTC (Refund.UpdateInvoices) :
    // la facture d'origine EST modifiée.
    @Test
    void avoirNonFinancier_reduitLeNetDeLaFacture() throws Exception {
        UUID d = fd.decompteComplet(c, pf.lot(c, "R31", c.statutLibre()), 31);
        UUID facture = genererOk(d, FACTURE);
        BigDecimal netAvant = montant(pf.pieceEnBase(facture), A_PAYER);

        UUID avoir = genererOk(d, AVOIR);
        fd.flush();

        Map<String, Object> a = pf.pieceEnBase(avoir);
        assumeThat(a.get("IsFinancialRefund")).as("avoir financier : sans effet sur la facture")
                .isNotIn(Boolean.TRUE, 1, "1", "true");
        assertThat(montant(pf.pieceEnBase(facture), A_PAYER))
                .isEqualByComparingTo(netAvant.subtract(montant(a, "AmountTTC")));
    }
}
