package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureAchat.Cadre;
import com.tsix.immocrm.fixtures.FixtureAchat.Saisie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.ROLE_JWT;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.appeler;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.statut;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.*;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Seconde revue A2b-2a (règle 6) : le PUT d'une facture recalcule {@code AmountToPay} ; OMRAN y retranche les avoirs non
 * financiers qui la citent ({@code Invoice.cs} l.456 {@code TotalRetainedAmountWithoutRegul … - AmountRefunded}, l.580).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PieceAchatAvoirsNetTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;

    @Test
    void put_factureAvecAvoirNonFinancier_netAPayerRetrancheLAvoir() throws Exception {
        Cadre c = fixture.cadre("AVN");
        Saisie s = fixture.configurationSaisie(c.jeton());
        AdmUser redacteur = fixture.compteHabilite(c, "AVN", ROLE_JWT, NOUVEAU_FACTURE);
        fixture.sansPersonnel(redacteur);
        UUID facture = fixture.piece(c, "-FAC", FixtureAchat.FACTURE);
        fixture.poser(facture, "StrEntityId", s.gestion(FixtureAchat.GESTION_FACTURE));
        fixture.poser(facture, "BuyDocumentStatusId", s.enCours());
        UUID avoir = fixture.piece(c, "-AVR", "REFUND");
        fixture.poser(avoir, "InvoiceSourceId", facture);
        fixture.poser(avoir, "IsFinancialRefund", false);
        fixture.montants(avoir, "200", "200", "200");

        MvcResult r = appeler(mockMvc, modifier(facture, corpsValide(c, s, FixtureAchat.FACTURE)
                .avec("lignes", List.of(ligne("ART-1", "10", "100", s.categorie())))), connecte(redacteur, ROLE_JWT));

        assertThat(statut(r)).as(corps(r)).isEqualTo(200);
        fixture.oublier();
        Map<String, Object> d = fixture.pieceEnBase(facture);
        assertThat(decimalDe(d, "AmountTTC")).isEqualByComparingTo("1200");
        assertThat(decimalDe(d, "AmountToPay")).as("TTC 1 200 − avoir non financier 200").isEqualByComparingTo("1000");
    }
}
