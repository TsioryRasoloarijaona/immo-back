package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.FixtureLotFiche;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotFiche.DROIT_ESTIMATION;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Revue L2a, E6 (règle 6) : estimation manuelle et taux de TVA. Lot à HT 1000, TVA 200, TTC 1200, DefaultVat 0,2
 * (cohérent, comme les 155 lots estimés de yesRemsProd).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotEstimationRecalculRevueTest {

    private static final String HT = "EstimatedPriceHTManual";
    private static final String TVA = "EstimatedVatAmountManual";
    private static final String TTC = "EstimatedPriceTTCManual";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotsParite parite;
    @Autowired private FixtureLotFiche fiche;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager em;

    private LotFicheSupport appels;
    private String jeton;
    private UUID projet;
    private UUID lot;

    @BeforeEach
    void setUp() {
        appels = new LotFicheSupport(mockMvc);
        jeton = FixtureProjetLots.jeton();
        parite.genresDeLot();
        projet = parite.projet("RVP-" + jeton, parite.entiteJuridique("RVE-" + jeton));
        lot = parite.lot(projet, "RVL-" + jeton, ENTREPRENEURS, parite.statut(ENTREPRENEURS, "RVS-" + jeton));
        em.flush();
        jdbc.update("UPDATE PrjMarket SET DefaultVat = 0.2, EstimatedPriceHTManual = 1000, EstimatedVatAmountManual = 200,"
                + " EstimatedPriceTTCManual = 1200 WHERE PKey = ?", lot.toString());
        em.clear();
    }

    private RequestPostProcessor sansDroit() {
        return connecte(parite.avecDroits("lots.rv.n." + jeton, List.of(), projet), "Commercial");
    }

    private RequestPostProcessor avecDroit() {
        return connecte(parite.avecDroits("lots.rv.d." + jeton, List.of(DROIT_ESTIMATION), projet), "Commercial");
    }

    private static BigDecimal n(Object v) {
        return LotsPariteSupport.nombre(v);
    }

    // Sans le droit, renvoyer le TTC stocké (« pas un changement ») avec un autre taux de TVA ne doit pas réécrire le HT :
    // OMRAN (setter EstimatedPriceTTCManual) ne recalcule rien quand la valeur est inchangée.
    @Test
    void sansDroit_ttcStockeRenvoyeAvecTvaChangee_htInchange() throws Exception {
        Reponse r = appels.modifier(sansDroit(), lot, corps().avec("tvaDefautPct", 0).avec("montantForfaitaireTTC", 1200));

        Map<String, Object> enBase = r.statut() == 200 ? fiche.lot(lot) : fiche.lotApresRefus(lot);
        assertThat(n(enBase.get(HT))).as("HT de l'estimation manuelle, statut %s : %s", r.statut(), r.corps())
                .isEqualByComparingTo("1000");
    }

    // OMRAN DefaultVat (MarketMonitoring.cs:2199-2213) : changer la TVA recalcule TVA et TTC de l'estimation, HT gardé.
    @Test
    void tvaChangee_estimationRecalculeeCommeOmran() throws Exception {
        Reponse r = appels.modifier(avecDroit(), lot, corps().avec("tvaDefautPct", 10));

        Map<String, Object> enBase = fiche.lot(lot);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(200);
            s.assertThat(n(enBase.get(HT))).as(HT).isEqualByComparingTo("1000");
            s.assertThat(n(enBase.get(TVA))).as(TVA).isEqualByComparingTo("100");
            s.assertThat(n(enBase.get(TTC))).as(TTC).isEqualByComparingTo("1100");
        });
    }
}
