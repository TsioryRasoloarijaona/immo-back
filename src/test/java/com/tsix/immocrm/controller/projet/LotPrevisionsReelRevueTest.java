package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.FixtureLotPrevisions;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureModelesSimulation;
import com.tsix.immocrm.fixtures.FixturePrevisionsReel;
import com.tsix.immocrm.fixtures.FixturePrevisionsReel.TauxStockes;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.DEBUT_2026;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.DUREE_90;
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.FIN_2026;
import static com.tsix.immocrm.controller.projet.PrevisionsReelSupport.SYSTEME;
import static com.tsix.immocrm.controller.projet.PrevisionsReelSupport.mode;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Revue L3a-ter : A4 — le refus « date avant le dernier décompte / facture » ne vaut que pour le mode FIXEE
 * (OMRAN {@code ProjectPrevisionMarket.cs} l.504-516 : le test n'est fait que pour {@code userfixedEndDateRadioButton}).
 * Lot en avance : réel le 11/01 (J10) à 0,6 ; points (J0, 0), (J45, 0,6), (J90, 1) → Dr = J45, écart = J10 − J45 = −35 j,
 * finSysteme = J10 − 35 j, avant le dernier réel. OMRAN propose quand même ; Arpent ne doit pas répondre 409 en SYSTEME.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotPrevisionsReelRevueTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotsParite parite;
    @Autowired private FixtureLotPrevisions prev;
    @Autowired private FixturePrevisionsReel reel;
    @Autowired private FixtureModelesSimulation modeles;

    private PrevisionsReelSupport appels;
    private String jeton;
    private UUID projet;
    private RequestPostProcessor admin;

    @BeforeEach
    void setUp() {
        modeles.tables();
        appels = new PrevisionsReelSupport(mockMvc);
        jeton = FixtureProjetLots.jeton();
        parite.genresDeLot();
        prev.preparer();
        projet = parite.projet("PRV-" + jeton, parite.entiteJuridique("PRW-" + jeton));
        admin = connecte(parite.administrateur("lot.prv." + jeton, projet), "Administrateur");
    }

    @Test
    void a4_systeme_finSystemeAvantLeDernierReel_pasDe409() throws Exception {
        UUID lot = parite.lot(projet, "PRVL-" + jeton, ENTREPRENEURS, parite.statut(ENTREPRENEURS, "PRVS-" + jeton));
        prev.planning(lot, DEBUT_2026, DUREE_90, DEBUT_2026, DUREE_90);
        prev.estimationBase(lot, "1000", "200", "1200", "0.2");
        prev.retenues(lot, "0", "0", "0");
        reel.bordereau(lot, "1000");
        prev.pointPrevision(lot, DEBUT_2026, "0", "0", "0", "0");
        prev.pointPrevision(lot, DEBUT_2026.plusDays(45), "0.6", "0.5", "0.6", "720");
        prev.pointPrevision(lot, FIN_2026, "1", "1", "0.4", "480");
        LocalDate dernierReel = DEBUT_2026.plusDays(10);
        reel.decompteFacture(lot, "PRVD-" + jeton, 1, dernierReel, "600", "720", "720", TauxStockes.de("0", "0"));

        Reponse ecarts = appels.ecarts(admin, lot);
        assertThat(ecarts.statut()).as(ecarts.corps()).isEqualTo(200);
        assertThat(LocalDate.parse(String.valueOf(ecarts.valeur("$.finSysteme")))).as("préalable : finSysteme < dernier réel")
                .isBefore(dernierReel);

        Reponse r = appels.aligner(admin, lot, mode(SYSTEME, null));

        assertThat(r.statut()).as("OMRAN ne refuse qu'en mode « date fixée » : " + r.corps()).isEqualTo(200);
    }
}
