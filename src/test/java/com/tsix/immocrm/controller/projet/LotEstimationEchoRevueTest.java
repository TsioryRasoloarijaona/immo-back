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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Seconde revue E6 (réponse 5 du test-writer) : renvoyer l'estimation manuelle telle que la fiche la donne n'est pas une
 * modification. Forfait saisi en TTC comme OMRAN (HT = TTC / 1,2 : 6 décimales en base, cas de 88 lots sur 182 en prod).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotEstimationEchoRevueTest {

    private static final String HT = "EstimatedPriceHTManual";
    private static final String MONTANT_HT = "montantForfaitaireHT";
    private static final String MONTANT_TTC = "montantForfaitaireTTC";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotsParite parite;
    @Autowired private FixtureLotFiche fiche;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager em;

    private LotFicheSupport appels;
    private UUID lot;
    private RequestPostProcessor sansDroit;

    @BeforeEach
    void setUp() {
        appels = new LotFicheSupport(mockMvc);
        String jeton = FixtureProjetLots.jeton();
        parite.genresDeLot();
        UUID projet = parite.projet("REP-" + jeton, parite.entiteJuridique("REE-" + jeton));
        lot = parite.lot(projet, "REL-" + jeton, ENTREPRENEURS, parite.statut(ENTREPRENEURS, "RES-" + jeton));
        sansDroit = connecte(parite.avecDroits("lots.re.n." + jeton, List.of(), projet), "Commercial");
        em.flush();
        jdbc.update("UPDATE PrjMarket SET DefaultVat = 0.2, EstimatedPriceHTManual = 1041.666667,"
                + " EstimatedVatAmountManual = 208.333333, EstimatedPriceTTCManual = 1250 WHERE PKey = ?", lot.toString());
        em.clear();
    }

    @Test
    void sansDroit_estimationRelueSurLaFicheEtRenvoyee_200_estimationIntacte() throws Exception {
        Reponse lu = appels.fiche(sansDroit, lot);
        assertThat(lu.statut()).as(lu.corps()).isEqualTo(200);

        Reponse r = appels.modifier(sansDroit, lot, corps().avec(MONTANT_HT, lu.valeur("$." + MONTANT_HT))
                .avec(MONTANT_TTC, lu.valeur("$." + MONTANT_TTC)).avec(LotFicheSupport.NOTES, "écho"));

        Map<String, Object> enBase = r.statut() == 200 ? fiche.lot(lot) : fiche.lotApresRefus(lot);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(200);
            s.assertThat(LotsPariteSupport.nombre(enBase.get(HT))).as(HT).isEqualByComparingTo("1041.666667");
        });
    }
}
