package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.FixtureLotPrevisions;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
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

import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.DEBUT_2026;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.DUREE_90;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.DUREE_BASE;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.ESTIMATION_BASE_HT;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.inchange;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.saisie;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotPrevisions.DROIT_PREVISIONS;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * R2 précisé par le manager : sans {@code ManualEstimationAdministrator}, un champ de base absent, identique, ou valant le
 * défaut que le front pose sur une base vide (début des prévisions, 0, 0) n'est pas une modification : ni 403 ni écriture.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotPrevisionsBaseParDefautTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotsParite parite;
    @Autowired private FixtureLotPrevisions prev;
    @Autowired private JdbcTemplate jdbc;

    private PrevisionsLotSupport appels;
    private UUID lot;
    private RequestPostProcessor saisieSeule;

    @BeforeEach
    void setUp() {
        appels = new PrevisionsLotSupport(mockMvc);
        String jeton = FixtureProjetLots.jeton();
        parite.genresDeLot();
        prev.preparer();
        UUID projet = parite.projet("PVD-" + jeton, parite.entiteJuridique("PVE-" + jeton));
        lot = parite.lot(projet, "PVL-" + jeton, ENTREPRENEURS, parite.statut(ENTREPRENEURS, "PVT-" + jeton));
        prev.planning(lot, DEBUT_2026, DUREE_90, DEBUT_2026, DUREE_90);
        prev.estimationBase(lot, "1000", "200", "1200", "0.2");
        prev.retenues(lot, "0", "0", "0");
        saisieSeule = connecte(parite.avecDroits("lot.pvd." + jeton, List.of(DROIT_PREVISIONS), projet), "Commercial");
    }

    private void baseVide() {
        jdbc.update("UPDATE PrjMarket SET StartDateManual = NULL, DelayManual = NULL, EndDateManual = NULL,"
                + " EstimatedPriceHTManual = NULL, EstimatedVatAmountManual = NULL, EstimatedPriceTTCManual = NULL WHERE PKey = ?",
                lot.toString());
    }

    @Test
    void baseVide_defautsDuFront_200_baseLaisseeVide() throws Exception {
        baseVide();

        Reponse r = appels.enregistrer(saisieSeule, lot, saisie(DEBUT_2026, 60, DEBUT_2026, 0, "0", List.of(), List.of()));

        Map<String, Object> enBase = prev.lot(lot);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.statut()).as(r.corps()).isEqualTo(200);
            s.assertThat(enBase).containsEntry("Delay", 60).containsEntry("StartDateManual", null)
                    .containsEntry("DelayManual", null).containsEntry("EstimatedPriceHTManual", null);
        });
    }

    @Test
    void baseVide_debutModifie_defautPerime_200_baseLaisseeVide() throws Exception {
        baseVide();

        Reponse r = appels.enregistrer(saisieSeule, lot,
                saisie(DEBUT_2026.plusDays(10), 60, DEBUT_2026, 0, "0", List.of(), List.of()));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(prev.lot(lot)).containsEntry("StartDateManual", null).containsEntry("DelayManual", null);
    }

    @Test
    void baseVide_dureeNonNulle_403() throws Exception {
        baseVide();

        Reponse r = appels.enregistrer(saisieSeule, lot, saisie(DEBUT_2026, 60, DEBUT_2026, 30, "0", List.of(), List.of()));

        assertThat(r.statut()).as(r.corps()).isEqualTo(403);
    }

    @Test
    void estimationDeBaseAbsente_200_montantInchange() throws Exception {
        Reponse r = appels.enregistrer(saisieSeule, lot, inchange().avec(ESTIMATION_BASE_HT, null));

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat((BigDecimal) prev.lot(lot).get("EstimatedPriceHTManual")).isEqualByComparingTo("1000");
    }

    @Test
    void baseVide_dureeDeBaseAbsente_resteUn400() throws Exception {
        baseVide();

        Reponse r = appels.enregistrer(saisieSeule, lot, saisie(DEBUT_2026, 60, DEBUT_2026, 0, "0", List.of(), List.of())
                .avec(DUREE_BASE, null));

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
    }
}
