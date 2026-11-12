package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.SoftAssertions;
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
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Règle 6 : l'estimation de la fiche lot est celle de la liste ({@code PrjMarketMonitoringView}), FODEC compris. Lot sans
 * bordereau ni décompte : le coût prévisionnel de la ligne de liste vaut alors l'estimation.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotFicheEstimationPartageeTest {

    private static final String COUT_HT = "coutPrevisionnelHT";
    private static final String COUT_TTC = "coutPrevisionnelTTC";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotsParite fixture;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager em;

    private LotsPariteSupport appels;
    private String jeton;
    private UUID lot;
    private RequestPostProcessor admin;

    @BeforeEach
    void setUp() {
        appels = new LotsPariteSupport(mockMvc);
        jeton = FixtureProjetLots.jeton();
        fixture.genresDeLot();
        UUID projet = fixture.projet("EP-" + jeton, fixture.entiteJuridique("EPE-" + jeton));
        lot = fixture.lot(projet, "EPL-" + jeton, ENTREPRENEURS, fixture.statut(ENTREPRENEURS, "EPS-" + jeton));
        admin = connecte(fixture.administrateur("lots.ep." + jeton, projet), "Administrateur");
        maj("UPDATE PrjMarket SET EstimatedPriceHTManual = 9999, EstimatedVatAmountManual = 1, EstimatedPriceTTCManual = 10000"
                + " WHERE PKey = ?", lot.toString());
    }

    private void maj(String sql, Object... parametres) {
        em.flush();
        jdbc.update(sql, parametres);
        em.clear();
    }

    private void ligneEstimative(String code, String quantite, String puHT, String fodec) {
        maj("""
                INSERT INTO PrjMarketLineEstimation (PKey, Code, Description, Sorting, Unity, Quantity, UnitPriceHT, VatRatio,
                    UnitPriceTTC, FODECRatio, IsStock, PrjMarketId)
                VALUES (?, ?, ?, 1, N'U', ?, ?, 0.2, 0, ?, 0, ?)""",
                UUID.randomUUID().toString(), code, "Ligne " + code, new BigDecimal(quantite), new BigDecimal(puHT),
                new BigDecimal(fodec), lot.toString());
    }

    private Reponse fiche() throws Exception {
        Reponse r = appels.lire(admin, LotsPariteSupport.LOTS + "/" + lot);
        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        return r;
    }

    // 3 × 250 × 1,01 + 2 × 100 × 1,1 = 757,50 + 220 = 977,50 HT ; TTC = HT × 1,2 = 1 173 ; l'ancien calcul donnait 950.
    @Test
    void estimatifAvecFodec_ficheEgaleLigneDeListe_fodecCompris() throws Exception {
        ligneEstimative("A", "3", "250", "0.01");
        ligneEstimative("B", "2", "100", "0.1");

        Reponse fiche = fiche();
        Map<String, Object> ligne = appels.ligne(admin, lot, "EPL-" + jeton);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(fiche.nombre("$.estimationHT")).isEqualByComparingTo("977.50")
                    .isEqualByComparingTo(LotsPariteSupport.nombre(ligne.get(COUT_HT)));
            s.assertThat(fiche.nombre("$.estimationTTC")).isEqualByComparingTo("1173.00")
                    .isEqualByComparingTo(LotsPariteSupport.nombre(ligne.get(COUT_TTC)));
            s.assertThat(fiche.nombre("$.estimationTVA")).isEqualByComparingTo("195.50");
            s.assertThat(fiche.valeur("$.estimationDepuisBordereau")).isEqualTo(true);
        });
    }

    @Test
    void sansEstimatif_ficheEgaleLigneDeListe_forfaitSaisi() throws Exception {
        Reponse fiche = fiche();
        Map<String, Object> ligne = appels.ligne(admin, lot, "EPL-" + jeton);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(fiche.nombre("$.estimationHT")).isEqualByComparingTo("9999")
                    .isEqualByComparingTo(LotsPariteSupport.nombre(ligne.get(COUT_HT)));
            s.assertThat(fiche.nombre("$.estimationTTC")).isEqualByComparingTo("10000")
                    .isEqualByComparingTo(LotsPariteSupport.nombre(ligne.get(COUT_TTC)));
            s.assertThat(fiche.nombre("$.estimationTVA")).isEqualByComparingTo("1");
            s.assertThat(fiche.valeur("$.estimationDepuisBordereau")).isEqualTo(false);
        });
    }
}
