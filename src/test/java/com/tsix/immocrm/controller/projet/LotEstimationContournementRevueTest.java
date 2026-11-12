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

/** Seconde revue E6 (règle 6) : un compte sans droit d'estimation ne réécrit HT / TVA / TTC que via la TVA par défaut. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotEstimationContournementRevueTest {

    private static final String HT = "EstimatedPriceHTManual";
    private static final String TVA = "EstimatedVatAmountManual";
    private static final String TTC = "EstimatedPriceTTCManual";
    private static final String TVA_DEFAUT = "tvaDefautPct";
    private static final String MONTANT_HT = "montantForfaitaireHT";
    private static final String MONTANT_TTC = "montantForfaitaireTTC";

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
        projet = parite.projet("RCP-" + jeton, parite.entiteJuridique("RCE-" + jeton));
        lot = parite.lot(projet, "RCL-" + jeton, ENTREPRENEURS, parite.statut(ENTREPRENEURS, "RCS-" + jeton));
        em.flush();
        jdbc.update("UPDATE PrjMarket SET DefaultVat = 0.2, EstimatedPriceHTManual = 1000, EstimatedVatAmountManual = 200,"
                + " EstimatedPriceTTCManual = 1200 WHERE PKey = ?", lot.toString());
        em.clear();
    }

    private RequestPostProcessor sansDroit() {
        return connecte(parite.avecDroits("lots.rc.n." + jeton, List.of(), projet), "Commercial");
    }

    private void verifier(Reponse r, int statut, String ht, String tva, String ttc) {
        Map<String, Object> enBase = r.statut() == 200 ? fiche.lot(lot) : fiche.lotApresRefus(lot);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(statut);
            s.assertThat(LotsPariteSupport.nombre(enBase.get(HT))).as(HT).isEqualByComparingTo(ht);
            s.assertThat(LotsPariteSupport.nombre(enBase.get(TVA))).as(TVA).isEqualByComparingTo(tva);
            s.assertThat(LotsPariteSupport.nombre(enBase.get(TTC))).as(TTC).isEqualByComparingTo(ttc);
        });
    }

    @Test
    void sansDroit_htRenvoyeEtTtcChange_403_estimationIntacte() throws Exception {
        Reponse r = appels.modifier(sansDroit(), lot, corps().avec(MONTANT_HT, 1000).avec(MONTANT_TTC, 5000));
        verifier(r, 403, "1000", "200", "1200");
    }

    @Test
    void sansDroit_htEtTtcRenvoyesAvecTvaChangee_recalculOmranDepuisHtStocke() throws Exception {
        Reponse r = appels.modifier(sansDroit(), lot,
                corps().avec(TVA_DEFAUT, 10).avec(MONTANT_HT, 1000).avec(MONTANT_TTC, 1200));
        verifier(r, 200, "1000", "100", "1100");
    }

    @Test
    void sansDroit_htRenvoyeAvecTvaInchangee_rienNeBouge() throws Exception {
        Reponse r = appels.modifier(sansDroit(), lot, corps().avec(TVA_DEFAUT, 20).avec(MONTANT_HT, 1000));
        verifier(r, 200, "1000", "200", "1200");
    }

    @Test
    void sansDroit_htMisANull_403_estimationIntacte() throws Exception {
        Reponse r = appels.modifier(sansDroit(), lot, corps().avec(MONTANT_HT, null).avec(TVA_DEFAUT, 10));
        verifier(r, 403, "1000", "200", "1200");
    }
}
