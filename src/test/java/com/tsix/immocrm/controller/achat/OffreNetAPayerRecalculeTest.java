package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureDemandesAchat;
import com.tsix.immocrm.fixtures.FixtureDemandesAchat.Cadre;
import com.tsix.immocrm.fixtures.FixtureDemandesAchatB;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.DemandesAchatAppels.RACINE;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchat.qui;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** FEATURE-lc7-appel-offres.md R6 (D3) : la modification des prix d'une offre recalcule son net à payer, comparé par R6. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OffreNetAPayerRecalculeTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDemandesAchat demandes;
    @Autowired private FixtureDemandesAchatB b;
    @Autowired private FixtureAchat achat;

    @Test
    void modificationDesPrix_netAPayerRecalculeDepuisLeTTC() throws Exception {
        Cadre c = b.cadre("NP");
        RequestPostProcessor q = qui(b.libre(c, "lc7.np.libre." + c.jeton()));
        UUID dia = demandes.demande(c, c.code("NP"), c.prep());
        UUID tiers = achat.fournisseur("TNP-" + c.jeton(), "Fournisseur NP " + c.jeton());
        UUID o = b.offre(dia, tiers, c.code("ONP"), false);
        achat.montants(o, "0", "0", "1000");

        int statut = mockMvc.perform(put(RACINE + "/" + dia + "/offres/" + o).with(q)
                .contentType(MediaType.APPLICATION_JSON).content("[]")).andReturn().getResponse().getStatus();

        Map<String, Object> piece = b.lignes("SELECT AmountTTC, AmountToPay FROM BuyDocument WHERE PKey = ?", o.toString()).getFirst();
        assertThat(statut).isEqualTo(200);
        assertThat((BigDecimal) piece.get("AmountToPay")).as(piece.toString())
                .isEqualByComparingTo((BigDecimal) piece.get("AmountTTC"));
    }
}
