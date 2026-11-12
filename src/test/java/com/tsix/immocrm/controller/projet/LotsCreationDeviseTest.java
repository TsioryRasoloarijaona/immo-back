package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.LotsPariteSupport.Creation;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureProjetConfig;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static com.tsix.immocrm.controller.projet.LotsPariteSupport.CATEGORIE_ID;
import static com.tsix.immocrm.controller.projet.LotsPariteSupport.DESCRIPTION;
import static com.tsix.immocrm.controller.projet.LotsPariteSupport.GENRE;
import static com.tsix.immocrm.controller.projet.LotsPariteSupport.PROJET_ID;
import static com.tsix.immocrm.controller.projet.LotsPariteSupport.TYPE_ID;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static org.assertj.core.api.Assertions.assertThat;

/** Création d'un lot avec une devise inconnue : 400 avant écriture (FEATURE-lots-liste-parite.md E5). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotsCreationDeviseTest {

    private static final String PAR_DESCRIPTION = "SELECT COUNT(*) FROM PrjMarket WHERE Description = ?";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotsParite fixture;
    @Autowired private FixtureProjetConfig config;

    private LotsPariteSupport appels;
    private String jeton;
    private CorpsJson corps;
    private RequestPostProcessor admin;

    @BeforeEach
    void setUp() {
        appels = new LotsPariteSupport(mockMvc);
        jeton = FixtureProjetLots.jeton();
        fixture.genresDeLot();
        fixture.statutsDepart(jeton);
        fixture.sequenceMarche(ThreadLocalRandom.current().nextInt(50_000, 90_000));
        UUID projet = fixture.projet("CRP-" + jeton, fixture.entiteJuridique("CRE-" + jeton));
        corps = corps().avec(GENRE, ENTREPRENEURS).avec(DESCRIPTION, "Lot devise " + jeton).avec(PROJET_ID, projet)
                .avec(CATEGORIE_ID, config.categorie("CAT-" + jeton, false)).avec(TYPE_ID, config.typeLot("TYP-" + jeton));
        admin = connecte(fixture.administrateur("lots.dev." + jeton, projet), "Administrateur");
    }

    @Test
    void deviseInconnue_400_aucunLot() throws Exception {
        Creation c = appels.creer(admin, corps.avec("deviseId", UUID.randomUUID()));

        assertThat(c.statut()).as(c.reponse().corps()).isEqualTo(400);
        assertThat(fixture.compter(PAR_DESCRIPTION, "Lot devise " + jeton)).isZero();
    }
}
