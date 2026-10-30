package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureReferentielsAdmin;
import com.tsix.immocrm.fixtures.FixtureRendezVous;
import com.tsix.immocrm.fixtures.FixtureTaches;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureRendezVous.CALENDRIER;
import static com.tsix.immocrm.fixtures.FixtureRendezVous.DEBUT;
import static com.tsix.immocrm.fixtures.FixtureRendezVous.FIN;
import static com.tsix.immocrm.fixtures.FixtureTaches.GESTIONNAIRE_ID;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** {@code GET /calendrier} — FEATURE-admin-rendez-vous.md, règle 4 et filtre {@code gestionnaireId}. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RendezVousCalendrierTest {

    private static final String CODES = "$[*].code";
    private static final String LE_10 = "2031-03-10";
    private static final String LE_12 = "2031-03-12";
    private static final String GESTIONNAIRE_COLONNE = "CfgTierManagerId";
    private static final LocalDateTime JOUR = LocalDateTime.of(2031, 3, 10, 9, 30);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureTaches taches;
    @Autowired private FixtureRendezVous fixture;

    private AdmUser admin;
    private String jeton;

    @BeforeEach
    void setUp() {
        taches.assurer();
        admin = patrimoine.administrateur();
        jeton = FixtureReferentielsAdmin.jeton();
    }

    private ResultActions appeler(String... paires) throws Exception {
        MockHttpServletRequestBuilder requete = get(CALENDRIER).with(connecte(admin));
        for (int i = 0; i < paires.length; i += 2) {
            requete.param(paires[i], paires[i + 1]);
        }
        return mockMvc.perform(requete);
    }

    private String code(String suffixe) {
        return suffixe + "-" + jeton;
    }

    @Test
    void r4_bornesIncluses_surToutLeDernierJour() throws Exception {
        fixture.rendezVous(code("AVANT"), LocalDateTime.of(2031, 3, 9, 23, 59));
        fixture.rendezVous(code("DEBUT"), LocalDateTime.of(2031, 3, 10, 0, 0));
        fixture.rendezVous(code("FIN"), LocalDateTime.of(2031, 3, 12, 23, 59));
        fixture.rendezVous(code("APRES"), LocalDateTime.of(2031, 3, 13, 0, 0));

        appeler(DEBUT, LE_10, FIN, LE_12).andExpect(status().isOk())
                .andExpect(jsonPath(CODES, allOf(hasItems(code("DEBUT"), code("FIN")),
                        not(hasItem(code("AVANT"))), not(hasItem(code("APRES"))))));
    }

    @Test
    void r4_neRenvoieJamaisUneTache() throws Exception {
        fixture.rendezVous(code("RDV"), JOUR);
        taches.definir(taches.tache(code("TAC")), "StartDatePrev", JOUR);

        appeler(DEBUT, LE_10, FIN, LE_12).andExpect(status().isOk())
                .andExpect(jsonPath(CODES, allOf(hasItem(code("RDV")), not(hasItem(code("TAC"))))));
    }

    @Test
    void r4_listeNonPaginee_auDelaDUnePageParDefaut() throws Exception {
        String description = "Masse " + jeton;
        fixture.enMasse(60, JOUR, description);

        appeler(DEBUT, LE_10, FIN, LE_12).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.description == '" + description + "')]", hasSize(60)));
    }

    @Test
    void r4_filtre_gestionnaireId() throws Exception {
        UUID gestionnaire = fixture.tier(code("GES"), "Gestionnaire");
        UUID autre = fixture.tier(code("GET"), "Autre");
        taches.definir(fixture.rendezVous(code("A"), JOUR), GESTIONNAIRE_COLONNE, autre);
        taches.definir(fixture.rendezVous(code("B"), JOUR), GESTIONNAIRE_COLONNE, gestionnaire);

        appeler(DEBUT, LE_10, FIN, LE_12, GESTIONNAIRE_ID, gestionnaire.toString()).andExpect(status().isOk())
                .andExpect(jsonPath(CODES, allOf(hasItem(code("B")), not(hasItem(code("A"))))));
    }

    @Test
    void r4_debutAbsent_400() throws Exception {
        appeler(FIN, LE_12).andExpect(status().isBadRequest());
    }

    @Test
    void r4_finAbsente_400() throws Exception {
        appeler(DEBUT, LE_10).andExpect(status().isBadRequest());
    }

    /** 63 jours d'écart : au-delà de 62, que la plage se compte avec ou sans le jour de fin. */
    @Test
    void r4_plageDePlusDe62Jours_400() throws Exception {
        appeler(DEBUT, "2031-01-01", FIN, "2031-03-05").andExpect(status().isBadRequest());
    }

    /** 61 jours d'écart : 62 au plus, que la plage se compte avec ou sans le jour de fin. */
    @Test
    void r4_plageDe62JoursAuPlus_acceptee() throws Exception {
        appeler(DEBUT, "2031-01-01", FIN, "2031-03-03").andExpect(status().isOk());
    }
}
