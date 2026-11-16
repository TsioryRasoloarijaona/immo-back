package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.FixtureLotPrevisions;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
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
import static com.tsix.immocrm.controller.projet.PrevisionsLotSupport.MOIS;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static org.assertj.core.api.Assertions.assertThat;

/** Revue : E8 « avancementEstimation = null quand la valeur vaut 0 et que le mois est après StartDateManual » (l.320). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotPrevisionsAvancementNulRevueTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotsParite parite;
    @Autowired private FixtureLotPrevisions prev;

    /** Base 01/01/2026 + 90 j ; points saisis 15/02 (0) et 15/03 (1) : février vaut 0 après le début → null. */
    @Test
    void e8_avancementZeroApresLeDebut_null() throws Exception {
        String jeton = FixtureProjetLots.jeton();
        parite.genresDeLot();
        prev.preparer();
        UUID projet = parite.projet("PVR-" + jeton, parite.entiteJuridique("PVRF-" + jeton));
        RequestPostProcessor admin = connecte(parite.administrateur("lot.pvr." + jeton, projet), "Administrateur");
        UUID lot = parite.lot(projet, "PVR1-" + jeton, ENTREPRENEURS, parite.statut(ENTREPRENEURS, "PVRS1-" + jeton));
        prev.planning(lot, DEBUT_2026, DUREE_90, DEBUT_2026, DUREE_90);
        prev.estimationBase(lot, "1000", "200", "1200", "0.2");
        prev.retenues(lot, "0.1", "0.1", "1");
        prev.pointBase(lot, LocalDate.of(2026, 2, 15), "0", "0.5", "0", "0");
        prev.pointBase(lot, LocalDate.of(2026, 3, 15), "1", "0.8111111111111111", "1", "1200");

        Reponse r = new PrevisionsLotSupport(mockMvc).graphe(admin, lot);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(r.ids(MOIS)).contains("2026-02-01");
        int fevrier = r.ids(MOIS).indexOf("2026-02-01");
        assertThat(r.valeur("$.mois[" + fevrier + "].avancementEstimation")).as(r.corps()).isNull();
    }
}
