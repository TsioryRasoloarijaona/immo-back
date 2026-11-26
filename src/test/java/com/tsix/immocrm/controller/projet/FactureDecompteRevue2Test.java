package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.FixtureFactureDecompte;
import com.tsix.immocrm.fixtures.FixturePiecesLot;
import com.tsix.immocrm.fixtures.FixturePiecesLot.Cadre;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import org.assertj.core.api.SoftAssertions;
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

import static com.tsix.immocrm.controller.projet.FactureDecompteSupport.FACTURE;
import static com.tsix.immocrm.controller.projet.FactureDecompteSupport.factureId;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/** D2 (parité OMRAN) : décompte sans ligne ni extra, la facture reprend les montants d'en-tête du décompte. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FactureDecompteRevue2Test {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePiecesLot pf;
    @Autowired private FixtureFactureDecompte fd;

    private Cadre c;
    private RequestPostProcessor admin;

    @BeforeEach
    void setUp() {
        c = fd.cadre("FR2");
        admin = connecte(pf.administrateur(c, "fr2.admin"), "Administrateur");
    }

    @Test
    void d2_sansLigne_montantsEnTeteRecopies() throws Exception {
        UUID lot = pf.lot(c, "FR2A", c.statutLibre());
        UUID d = fd.decompte(c, lot, 20, Map.of(FixtureProjetLots.TTC, new BigDecimal("600"),
                FixtureProjetLots.A_PAYER, new BigDecimal("600")));
        fd.flush();

        Reponse r = new FactureDecompteSupport(mockMvc).generer(admin, d, FACTURE);

        assertThat(r.statut()).as(r.corps()).isEqualTo(201);
        Map<String, Object> f = pf.pieceEnBase(factureId(r));
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(new BigDecimal(f.get("AmountTTC").toString())).isEqualByComparingTo("600");
            s.assertThat(new BigDecimal(f.get("AmountToPay").toString())).isEqualByComparingTo("600");
        });
    }
}
