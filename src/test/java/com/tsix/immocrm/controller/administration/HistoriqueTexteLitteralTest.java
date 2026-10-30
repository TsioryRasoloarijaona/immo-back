package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureHistorique;
import com.tsix.immocrm.fixtures.FixtureHistorique.Ligne;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static com.tsix.immocrm.controller.administration.AppelsHistorique.TEXTE;
import static com.tsix.immocrm.controller.administration.AppelsHistorique.periode;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/** {@code %}, {@code _} et {@code [} de {@code texte} sont cherchés à la lettre. FEATURE-admin-historique.md. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class HistoriqueTexteLitteralTest {

    private static final String ADMINISTRATEUR = "Administrateur";
    private static final LocalDate JOUR = LocalDate.of(2012, 3, 11);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDossierVente dossiers;
    @Autowired private FixtureHistorique fixture;

    private AppelsHistorique appels;

    @BeforeEach
    void connecterAdministrateur() {
        appels = new AppelsHistorique(mockMvc, connecte(dossiers.compte("ADM-HIST", ADMINISTRATEUR), ADMINISTRATEUR));
    }

    @ParameterizedTest
    @ValueSource(strings = {"%", "_", "["})
    void caractereJoker_prisALaLettre(String joker) throws Exception {
        String jeton = FixtureHistorique.jeton();
        String motif = "A" + joker + "B" + jeton;
        Ligne ligne = Ligne.de(JOUR.atTime(10, 0), jeton).avecType(FixtureHistorique.TYPE_NEUTRE);
        UUID litterale = fixture.ligne(ligne.avecLibelle(motif));
        fixture.ligne(ligne.avecLibelle("AxB" + jeton));

        assertThat(appels.ids(periode(JOUR, JOUR, TEXTE, motif))).containsExactly(litterale.toString());
    }
}
