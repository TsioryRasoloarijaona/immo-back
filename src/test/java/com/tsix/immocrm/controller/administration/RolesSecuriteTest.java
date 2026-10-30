package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureFaisabiliteWorkflow;
import com.tsix.immocrm.fixtures.FixtureRoles;
import com.tsix.immocrm.fixtures.FixtureRoles.Etat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Arrays;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static com.tsix.immocrm.controller.administration.AppelsRoles.json;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureRoles.ARBRE;
import static com.tsix.immocrm.fixtures.FixtureRoles.CODE;
import static com.tsix.immocrm.fixtures.FixtureRoles.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureRoles.DROITS;
import static com.tsix.immocrm.fixtures.FixtureRoles.EXPORT;
import static com.tsix.immocrm.fixtures.FixtureRoles.FICHE;
import static com.tsix.immocrm.fixtures.FixtureRoles.RACINE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Règle 1 de FEATURE-admin-roles.md. La règle d'URL refuse déjà {@code /api/administration/**} : chaque refus est suivi
 * d'un témoin, la même requête par un Administrateur, sans quoi ces tests seraient verts avant le code.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RolesSecuriteTest {

    private static final String REFUS = "Refus";
    private static final String TROP_LONG = "x".repeat(256);
    private static final String ILLISIBLE = "pas-un-uuid";

    enum Route {
        LISTE(200, (id, code) -> get(RACINE)),
        EXPORT_LISTE(200, (id, code) -> get(EXPORT)),
        ARBRE_DROITS(200, (id, code) -> get(ARBRE)),
        FICHE_ROLE(200, (id, code) -> get(FICHE, id)),
        CREATION(201, (id, code) -> json(post(RACINE), corps().avec(CODE, code).json())),
        MODIFICATION(200, (id, code) -> json(patch(FICHE, id), corps().avec(DESCRIPTION, REFUS).json())),
        DROITS_ROLE(200, (id, code) -> json(put(DROITS, id), AppelsRoles.codes().json())),
        SUPPRESSION(204, (id, code) -> delete(FICHE, id));

        private final int attendu;
        private final BiFunction<UUID, String, MockHttpServletRequestBuilder> requete;

        Route(int attendu, BiFunction<UUID, String, MockHttpServletRequestBuilder> requete) {
            this.attendu = attendu;
            this.requete = requete;
        }
    }

    /** Requêtes qu'un Administrateur voit refusées en 400 ; le 403 doit passer avant. */
    enum Invalide {
        CREATION_VIDE((id, code) -> json(post(RACINE), "{}")),
        CREATION_CODE_TROP_LONG((id, code) -> json(post(RACINE), corps().avec(CODE, TROP_LONG).json())),
        MODIFICATION_TROP_LONGUE((id, code) -> json(patch(FICHE, id), corps().avec(DESCRIPTION, TROP_LONG).json())),
        DROITS_CODE_INCONNU((id, code) -> json(put(DROITS, id), AppelsRoles.codes(code).json()));

        private final BiFunction<UUID, String, MockHttpServletRequestBuilder> requete;

        Invalide(BiFunction<UUID, String, MockHttpServletRequestBuilder> requete) {
            this.requete = requete;
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRoles fixture;

    private String jeton;
    private AdmUser admin;

    @BeforeEach
    void setUp() {
        jeton = fixture.preparer();
        admin = fixture.compte();
    }

    @AfterEach
    void nettoyer() {
        fixture.nettoyer();
    }

    static Stream<Arguments> refus() {
        return Stream.of("Commercial", "Direction Générale", "Assistante Commerciale", "administrateur")
                .flatMap(role -> Arrays.stream(Route.values()).map(route -> Arguments.of(route, role)));
    }

    private void refuseSansEcrire(Route route, RequestPostProcessor compte, int statut, UUID id, String code) throws Exception {
        Etat avant = fixture.etat(id);

        mockMvc.perform(route.requete.apply(id, code).with(compte)).andExpect(status().is(statut));

        assertThat(fixture.etat(id)).isEqualTo(avant);
        assertThat(fixture.nombreRoles(code)).isZero();
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("refus")
    void r1_roleNonAdministrateur_403_puisTemoinAdministrateur(Route route, String role) throws Exception {
        UUID id = fixture.role("SEC-" + jeton);
        String code = "NEW-" + jeton;

        refuseSansEcrire(route, FixtureFaisabiliteWorkflow.avecRole(admin, role), 403, id, code);

        mockMvc.perform(route.requete.apply(id, code).with(connecte(admin))).andExpect(status().is(route.attendu));
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Route.class)
    void r1_sansConnexion_401_puisTemoinAdministrateur(Route route) throws Exception {
        UUID id = fixture.role("SEC-" + jeton);
        String code = "NEW-" + jeton;

        refuseSansEcrire(route, requete -> requete, 401, id, code);

        mockMvc.perform(route.requete.apply(id, code).with(connecte(admin))).andExpect(status().is(route.attendu));
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Invalide.class)
    void r1_403_avantTout400(Invalide invalide) throws Exception {
        UUID id = fixture.role("SEC-" + jeton);
        String inconnu = "INC-" + jeton;

        mockMvc.perform(invalide.requete.apply(id, inconnu).with(FixtureFaisabiliteWorkflow.avecRole(admin, "Commercial")))
                .andExpect(status().isForbidden());

        mockMvc.perform(invalide.requete.apply(id, inconnu).with(connecte(admin))).andExpect(status().isBadRequest());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"GET", "PATCH", "DELETE"})
    void r1_identifiantIllisible_403_pas400(String verbe) throws Exception {
        HttpMethod methode = HttpMethod.valueOf(verbe);

        mockMvc.perform(json(request(methode, FICHE, ILLISIBLE), "{}")
                        .with(FixtureFaisabiliteWorkflow.avecRole(admin, "Commercial")))
                .andExpect(status().isForbidden());

        mockMvc.perform(json(request(methode, FICHE, ILLISIBLE), "{}").with(connecte(admin))).andExpect(status().isBadRequest());
    }
}
