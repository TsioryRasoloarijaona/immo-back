package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * FEATURE-n6-demandes-achat.md, réponse Q1 (19/11) et R11 : la séquence libre vaut aussi pour les lots. Statuts A, B, C ;
 * transitions A→B et B→C seulement ; lot sur A. Un compte libre se voit proposer B→C (seule transition vers C) et peut la passer.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotsTransitionSequenceLibreTest {

    private static final String IDS = "$[*].id";
    private static final String STATUT_DU_LOT = "SELECT COUNT(*) FROM PrjMarket WHERE PKey = ? AND StrStatusId = ?";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotsParite fixture;

    private LotsPariteSupport appels;
    private UUID statutC;
    private UUID ab;
    private UUID bc;
    private UUID lot;
    private RequestPostProcessor libre;

    @BeforeEach
    void setUp() {
        appels = new LotsPariteSupport(mockMvc);
        String jeton = FixtureProjetLots.jeton();
        fixture.genresDeLot();
        UUID entite = fixture.entiteJuridique("SL-" + jeton);
        UUID projet = fixture.projet("SLP-" + jeton, entite);
        UUID statutA = fixture.statut(ENTREPRENEURS, "SA-" + jeton);
        UUID statutB = fixture.statut(ENTREPRENEURS, "SB-" + jeton);
        statutC = fixture.statut(ENTREPRENEURS, "SC-" + jeton);
        ab = fixture.transition(ENTREPRENEURS, statutA, statutB, "A>B " + jeton);
        bc = fixture.transition(ENTREPRENEURS, statutB, statutC, "B>C " + jeton);
        lot = fixture.lot(projet, "SLL-" + jeton, ENTREPRENEURS, statutA);
        libre = connecte(fixture.avecDroits("lots.seq.libre." + jeton, List.of(), projet), "Commercial");
    }

    @Test
    void q1_r11_compteLibre_voitToutesLesTransitionsDeLEntite_horsDepartCourant() throws Exception {
        Reponse r = appels.transitions(libre, lot);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        // contains, pas containsExactly : les transitions de prod du genre sont aussi proposées (R11 : toute l'entité).
        assertThat(r.ids(IDS)).contains(ab.toString(), bc.toString());
    }

    @Test
    void q1_r11_compteLibre_passeUneTransitionQuiNePartPasDuStatutCourant() throws Exception {
        Reponse r = appels.executer(libre, lot, bc);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(fixture.compter(STATUT_DU_LOT, lot.toString(), statutC.toString())).isEqualTo(1);
    }
}
