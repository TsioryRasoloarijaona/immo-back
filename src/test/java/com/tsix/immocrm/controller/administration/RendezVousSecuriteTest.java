package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureFaisabiliteWorkflow;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureReferentielsAdmin;
import com.tsix.immocrm.fixtures.FixtureRendezVous;
import com.tsix.immocrm.fixtures.FixtureTaches;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureRendezVous.CALENDRIER;
import static com.tsix.immocrm.fixtures.FixtureRendezVous.DEBUT;
import static com.tsix.immocrm.fixtures.FixtureRendezVous.EXPORT;
import static com.tsix.immocrm.fixtures.FixtureRendezVous.FICHE;
import static com.tsix.immocrm.fixtures.FixtureRendezVous.FIN;
import static com.tsix.immocrm.fixtures.FixtureRendezVous.RACINE;
import static com.tsix.immocrm.fixtures.FixtureRendezVous.TIER_ID;
import static com.tsix.immocrm.fixtures.FixtureTaches.DATE_DEBUT;
import static com.tsix.immocrm.fixtures.FixtureTaches.DATE_FIN;
import static com.tsix.immocrm.fixtures.FixtureTaches.DATE_PREVUE;
import static com.tsix.immocrm.fixtures.FixtureTaches.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureTaches.TEXTE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Règles 11 et 13 de FEATURE-admin-rendez-vous.md : 403 sans rôle Administrateur sur toutes les routes, avant tout 400.
 * La règle d'URL refuse déjà tout {@code /api/administration/**} : la relecture Administrateur prouve que la route existe.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RendezVousSecuriteTest {

    private static final String REFUS = "Refus";
    private static final String MARS = "2031-03-01";
    private static final String JANVIER = "2031-01-01";
    private static final String MAI = "2031-05-01";
    private static final LocalDateTime JOUR = LocalDateTime.of(2031, 3, 10, 9, 30);

    enum Route {
        LISTE(id -> get(RACINE)),
        CALENDRIER_PERIODE(id -> get(CALENDRIER).param(DEBUT, MARS).param(FIN, MARS)),
        EXPORT_LISTE(id -> get(EXPORT)),
        FICHE_RENDEZ_VOUS(id -> get(FICHE, id)),
        CREATION(id -> json(post(RACINE), corps().avec(DESCRIPTION, REFUS).avec(TIER_ID, id)
                .avec(DATE_PREVUE, "2031-03-10T09:30:00").json())),
        MODIFICATION(id -> json(patch(FICHE, id), corps().avec(DESCRIPTION, REFUS).json())),
        SUPPRESSION(id -> delete(FICHE, id));

        private final Function<UUID, MockHttpServletRequestBuilder> requete;

        Route(Function<UUID, MockHttpServletRequestBuilder> requete) {
            this.requete = requete;
        }
    }

    /** Requêtes qu'un Administrateur voit refusées en 400. */
    enum Invalide {
        CALENDRIER_SANS_DEBUT(() -> get(CALENDRIER).param(FIN, MARS)),
        CALENDRIER_SANS_FIN(() -> get(CALENDRIER).param(DEBUT, MARS)),
        CALENDRIER_TROP_LONG(() -> get(CALENDRIER).param(DEBUT, JANVIER).param(FIN, MAI)),
        LISTE_DATES_INVERSEES(() -> get(RACINE).param(DATE_DEBUT, MAI).param(DATE_FIN, JANVIER)),
        EXPORT_DATES_INVERSEES(() -> get(EXPORT).param(DATE_DEBUT, MAI).param(DATE_FIN, JANVIER)),
        CREATION_VIDE(() -> json(post(RACINE), "{}"));

        private final Supplier<MockHttpServletRequestBuilder> requete;

        Invalide(Supplier<MockHttpServletRequestBuilder> requete) {
            this.requete = requete;
        }
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder requete, String corps) {
        return requete.contentType(MediaType.APPLICATION_JSON).content(corps);
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureTaches taches;
    @Autowired private FixtureRendezVous fixture;

    private AdmUser admin;
    private RequestPostProcessor commercial;
    private String jeton;

    @BeforeEach
    void setUp() {
        taches.assurer();
        admin = patrimoine.administrateur();
        commercial = FixtureFaisabiliteWorkflow.avecRole(admin, "Commercial");
        jeton = FixtureReferentielsAdmin.jeton();
    }

    private void seulLeRendezVousInitial(UUID id, String code) throws Exception {
        mockMvc.perform(get(RACINE).with(connecte(admin)).param(TEXTE, jeton))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(id.toString()))
                .andExpect(jsonPath("$.content[0].description").value("Tache " + code));
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Route.class)
    void r11_403_sansRoleAdministrateur_rienEcrit(Route route) throws Exception {
        String code = "SEC-" + jeton;
        UUID id = fixture.rendezVous(code, JOUR);

        mockMvc.perform(route.requete.apply(id).with(commercial)).andExpect(status().isForbidden());

        seulLeRendezVousInitial(id, code);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Invalide.class)
    void r11_403_avantTout400(Invalide requete) throws Exception {
        mockMvc.perform(requete.requete.get().with(commercial)).andExpect(status().isForbidden());

        mockMvc.perform(requete.requete.get().with(connecte(admin))).andExpect(status().isBadRequest());
    }

    /** Le contrôle de rôle passe avant la conversion de l'identifiant. */
    @Test
    void r11_identifiantIllisible_403_pas400() throws Exception {
        String code = "SEC-" + jeton;
        UUID id = fixture.rendezVous(code, JOUR);

        mockMvc.perform(get(RACINE + "/pas-un-uuid").with(commercial)).andExpect(status().isForbidden());

        seulLeRendezVousInitial(id, code);
    }
}
