package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureDepensesProjet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/** Paramètres mal formés → 400 (FEATURE-achat-depenses-projet.md §4.2 ; §6 test 8). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DepensesProjetSecuriteTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat achat;

    private AppelsProjet appels;
    private RequestPostProcessor qui;

    @BeforeEach
    void setUp() {
        appels = new AppelsProjet(mockMvc);
        AdmUser compte = achat.compte("DEPSEC-" + FixtureAchat.jeton(""), FixtureDepensesProjet.ROLE);
        qui = connecte(compte, FixtureDepensesProjet.ROLE);
    }

    @ParameterizedTest(name = "{0} ?{1}={2} → 400")
    @CsvSource({
            "/api/projet/depenses, projetIds, x",
            "/api/projet/depenses/reglements, projetIds, x",
            "/api/projet/depenses/reglements, lotId, x",
            "/api/projet/depenses/reglements, factureId, x",
            "/api/projet/depenses/reglements, du, 2026-13-01",
            "/api/projet/depenses/reglements, au, 2026-13-01"
    })
    void t8_parametreMalForme_400(String url, String nom, String valeur) throws Exception {
        Reponse r = appels.lire(qui, url, nom, valeur);

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
    }
}
