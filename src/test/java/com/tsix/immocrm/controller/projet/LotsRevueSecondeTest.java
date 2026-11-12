package com.tsix.immocrm.controller.projet;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.controller.projet.LotsPariteSupport.Creation;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureProjetConfig;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.LotsPariteSupport.CATEGORIE_ID;
import static com.tsix.immocrm.controller.projet.LotsPariteSupport.DESCRIPTION;
import static com.tsix.immocrm.controller.projet.LotsPariteSupport.GENRE;
import static com.tsix.immocrm.controller.projet.LotsPariteSupport.PROJET_ID;
import static com.tsix.immocrm.controller.projet.LotsPariteSupport.TYPE_ID;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.FOURNISSEURS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * 2ᵉ revue L1 (FEATURE-lots-liste-parite.md) : correctif 918f8c04 (WithPassword, corps absent, description ≤ 255) et
 * dépassement d'une commande multi-fournisseur comme {@code BuyDocumentOrderView.OverTTC} (condition sur le HT).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotsRevueSecondeTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotsParite fixture;
    @Autowired private FixtureProjetConfig config;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager em;

    private LotsPariteSupport appels;
    private String jeton;
    private UUID projet;

    @BeforeEach
    void setUp() {
        appels = new LotsPariteSupport(mockMvc);
        jeton = FixtureProjetLots.jeton();
        fixture.genresDeLot();
        projet = fixture.projet("RV2-" + jeton, fixture.entiteJuridique("RV2E-" + jeton));
    }

    private RequestPostProcessor admin() {
        return connecte(fixture.administrateur("lots.rv2." + jeton, projet), "Administrateur");
    }

    private void maj(String sql, Object... parametres) {
        em.flush();
        jdbc.update(sql, parametres);
        em.clear();
    }

    private UUID piece(String docType, UUID lot, UUID statut, UUID commande, String ht, String ttc) {
        UUID id = UUID.randomUUID();
        maj("""
                INSERT INTO BuyDocument (PKey, Code, DocType, DocDate, AmountHT, AmountTTC, AmountToPay, PrjMarketId,
                    BuyDocumentStatusId, BuyOrderId)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                id.toString(), docType + "-" + id.toString().substring(0, 8), docType,
                Timestamp.valueOf(LocalDateTime.of(2026, 3, 20, 0, 0)), new BigDecimal(ht), new BigDecimal(ttc),
                new BigDecimal(ttc), lot.toString(), statut == null ? null : statut.toString(),
                commande == null ? null : commande.toString());
        return id;
    }

    // Commande validée HT 100 / TTC 110, facturée HT 100 / TTC 120 : pas de dépassement HT, donc OverTTC = 0 dans la vue.
    @Test
    void multiFournisseur_factureSansDepassementHT_coutTTCSansDepassement_commeLaVue() throws Exception {
        UUID lot = fixture.lot(projet, "RV2L-" + jeton, FOURNISSEURS, fixture.statut(FOURNISSEURS, "RV2S-" + jeton));
        UUID valide = UUID.randomUUID();
        maj("INSERT INTO BuyDocumentStatus (PKey, Code, Description, IsOkForPayement) VALUES (?, ?, ?, 1)",
                valide.toString(), "RV2V-" + jeton, "Validé");
        UUID commande = piece("ORDER", lot, valide, null, "100.00", "110.00");
        piece("INVOICE", lot, null, commande, "100.00", "120.00");

        Map<String, Object> ligne = appels.ligne(admin(), lot, "RV2L-" + jeton);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(LotsPariteSupport.nombre(ligne.get("coutPrevisionnelHT"))).isEqualByComparingTo("100.00");
            s.assertThat(LotsPariteSupport.nombre(ligne.get("coutPrevisionnelTTC")))
                    .as("OverTTC = IIF(InvoicedHT - AmountHT <= 0, 0, …)").isEqualByComparingTo("110.00");
        });
    }

    @Test
    void transitionASecondValideur_jamaisProposee_executee409() throws Exception {
        UUID depart = fixture.statut(ENTREPRENEURS, "RV2A-" + jeton);
        UUID lot = fixture.lot(projet, "RV2T-" + jeton, ENTREPRENEURS, depart);
        UUID t = fixture.transition(ENTREPRENEURS, depart, fixture.statut(ENTREPRENEURS, "RV2B-" + jeton), "RV2W-" + jeton);
        maj("UPDATE StrWorkFlow SET WithPassword = 1 WHERE PKey = ?", t.toString());
        RequestPostProcessor qui = admin();

        Reponse liste = appels.transitions(qui, lot);
        Reponse execution = appels.executer(qui, lot, t);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(liste.statut()).isEqualTo(200);
            List<String> ids = JsonPath.read(liste.corps(), "$[*].id");
            s.assertThat(ids).doesNotContain(t.toString());
            s.assertThat(execution.statut()).as(execution.corps()).isEqualTo(409);
        });
    }

    @Test
    void creation_corpsAbsent_sansDroit_403() throws Exception {
        RequestPostProcessor sansDroit = connecte(fixture.avecDroits("lots.rv2n." + jeton, List.of(), projet), "Commercial");

        int statut = mockMvc.perform(post(LotsPariteSupport.LOTS).with(sansDroit).contentType(MediaType.APPLICATION_JSON))
                .andReturn().getResponse().getStatus();

        assertThat(statut).isEqualTo(403);
    }

    @Test
    void creation_description256_400() throws Exception {
        Creation c = appels.creer(admin(), corps().avec(GENRE, ENTREPRENEURS).avec(DESCRIPTION, "x".repeat(256))
                .avec(PROJET_ID, projet).avec(CATEGORIE_ID, config.categorie("RV2C-" + jeton, false))
                .avec(TYPE_ID, config.typeLot("RV2Y-" + jeton)));

        assertThat(c.statut()).as(c.reponse().corps()).isEqualTo(400);
    }
}
