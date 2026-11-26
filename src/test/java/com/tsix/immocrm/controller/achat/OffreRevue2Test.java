package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.fixtures.ClasseurDeTest;
import com.tsix.immocrm.fixtures.FixtureDemandesAchat;
import com.tsix.immocrm.fixtures.FixtureDemandesAchat.Cadre;
import com.tsix.immocrm.fixtures.FixtureDemandesAchatB;
import com.tsix.immocrm.fixtures.FixtureAchat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.DemandesAchatAppels.RACINE;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchat.qui;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

/** Revue 2 LC7 : l'import (R7) passe par le même recalcul que la modification, net à payer compris (comparé par R6). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OffreRevue2Test {

    private static final List<String> ENTETES = List.of("O.D.T.", "Code", "Description", "Unité", "Quantité", "P.U. HT", "TVA",
            "Remise", "FODEC", "P.U. TTC", "Notes", "Code groupe");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDemandesAchat demandes;
    @Autowired private FixtureDemandesAchatB b;
    @Autowired private FixtureAchat achat;

    @Test
    void r7_import_netAPayerRecalculeDepuisLeTTC() throws Exception {
        Cadre c = b.cadre("RV2");
        UUID dia = demandes.demande(c, c.code("NAP"), c.prep());
        UUID tiers = achat.fournisseur("TNAP-" + c.jeton(), "Fournisseur NAP " + c.jeton());
        b.preselection(dia, tiers, LocalDate.of(2026, 3, 20));
        UUID offre = b.offre(dia, tiers, c.code("ONAP"), false);
        b.ligneOffre(offre, 1, "A1", "0", "0");
        byte[] octets = ClasseurDeTest.xlsx(ENTETES,
                List.of(ClasseurDeTest.ligne(null, "A1", "Article A1", "U", 1, 1000, 20, 0, 0, null, null, null)));

        MockHttpServletResponse r = mockMvc.perform(multipart(RACINE + "/" + dia + "/offres/" + offre + "/import")
                .file(new MockMultipartFile("fichier", "offre.xlsx",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", octets))
                .with(qui(b.libre(c, "lc7.rv2.libre." + c.jeton())))).andReturn().getResponse();

        assertThat(r.getStatus()).as(r.getContentAsString(StandardCharsets.UTF_8)).isEqualTo(200);
        Map<String, Object> enTete = b.lignes("SELECT AmountTTC, AmountToPay FROM BuyDocument WHERE PKey = ?", offre.toString())
                .getFirst();
        assertThat((BigDecimal) enTete.get("AmountTTC")).isPositive();
        assertThat((BigDecimal) enTete.get("AmountToPay")).as("net à payer lu par R6").isEqualByComparingTo((BigDecimal) enTete.get("AmountTTC"));
    }
}
