package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureFaisabiliteWorkflow;
import com.tsix.immocrm.fixtures.FixtureUtilisateurs;
import com.tsix.immocrm.fixtures.FixtureUtilisateurs.Etat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.tsix.immocrm.controller.administration.AppelsUtilisateurs.json;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.CODE;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.COMPTES_DEPOT;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.COMPTE_IDS;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.EXPORT;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.FICHE;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.HABILITATIONS;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.NOEUDS;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.RACINE;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.REFERENCES;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.ROLES;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.ROLE_IDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * R1 de FEATURE-admin-utilisateurs.md, sur chaque route. Un refus est suivi d'un témoin, la même requête par un Administrateur,
 * sans quoi un 401 ou un 403 serait vert avant le code.
 */
@SpringBootTest
@AutoConfigureMockMvc
class UtilisateursSecuriteTest {

    private static final String REFUS = "Refus";
    private static final String ILLISIBLE = "pas-un-uuid";
    private static final UUID INCONNU = UUID.fromString("5B6C7D8E-9FA0-4B1C-82D3-E4F506172839");
    private static final List<UUID> AUCUN = List.of();
    private static final Supplier<CorpsJson> INCONNUS = () -> corps().avec(ROLE_IDS, List.of(INCONNU));

    enum Route {
        LISTE((id, code) -> get(RACINE)),
        EXPORT_LISTE((id, code) -> get(EXPORT)),
        REFERENCES_FICHE((id, code) -> get(REFERENCES)),
        FICHE_COMPTE((id, code) -> get(FICHE, id)),
        CREATION((id, code) -> json(post(RACINE), corps().avec(CODE, code).avec(DESCRIPTION, REFUS).json())),
        MODIFICATION((id, code) -> json(patch(FICHE, id), corps().avec(DESCRIPTION, REFUS).json())),
        ROLES_COMPTE((id, code) -> json(put(ROLES, id), corps().avec(ROLE_IDS, AUCUN).json())),
        DEPOTS_COMPTE((id, code) -> json(put(COMPTES_DEPOT, id), corps().avec(COMPTE_IDS, AUCUN).json())),
        HABILITATIONS_LUES((id, code) -> get(HABILITATIONS, id)),
        HABILITATIONS_ECRITES((id, code) -> json(put(HABILITATIONS, id), corps().avec(NOEUDS, AUCUN).json()));

        private final BiFunction<UUID, String, MockHttpServletRequestBuilder> requete;

        Route(BiFunction<UUID, String, MockHttpServletRequestBuilder> requete) {
            this.requete = requete;
        }
    }

    /** Requêtes qu'un Administrateur voit refusées en 400 ; le 403 doit passer avant. */
    enum Invalide {
        CREATION_VIDE((id) -> json(post(RACINE), "{}")),
        CREATION_ILLISIBLE((id) -> json(post(RACINE), "{")),
        CREATION_CODE_TROP_LONG((id) -> json(post(RACINE), corps().avec(CODE, "x".repeat(101)).avec(DESCRIPTION, REFUS).json())),
        MODIFICATION_TROP_LONGUE((id) -> json(patch(FICHE, id), corps().avec(DESCRIPTION, "x".repeat(256)).json())),
        MODIFICATION_ILLISIBLE((id) -> json(patch(FICHE, id), "{")),
        ROLE_INCONNU((id) -> json(put(ROLES, id), INCONNUS.get().json())),
        DEPOT_INCONNU((id) -> json(put(COMPTES_DEPOT, id), corps().avec(COMPTE_IDS, List.of(INCONNU)).json())),
        NOEUD_INCONNU((id) -> json(put(HABILITATIONS, id),
                corps().avec(NOEUDS, List.of(corps().avec("type", "INCONNU").avec("id", INCONNU))).json())),
        FICHE_ILLISIBLE((id) -> get(FICHE, ILLISIBLE)),
        MODIFICATION_ID_ILLISIBLE((id) -> json(patch(FICHE, ILLISIBLE), "{}")),
        ROLES_ID_ILLISIBLE((id) -> json(put(ROLES, ILLISIBLE), corps().avec(ROLE_IDS, AUCUN).json())),
        DEPOTS_ID_ILLISIBLE((id) -> json(put(COMPTES_DEPOT, ILLISIBLE), corps().avec(COMPTE_IDS, AUCUN).json())),
        HABILITATIONS_ID_ILLISIBLE((id) -> get(HABILITATIONS, ILLISIBLE)),
        HABILITATIONS_PUT_ID_ILLISIBLE((id) -> json(put(HABILITATIONS, ILLISIBLE), corps().avec(NOEUDS, AUCUN).json())),
        LISTE_ROLE_ILLISIBLE((id) -> get(RACINE).param("roleId", ILLISIBLE)),
        LISTE_PERSONNEL_ILLISIBLE((id) -> get(RACINE).param("personnelId", ILLISIBLE)),
        EXPORT_ROLE_ILLISIBLE((id) -> get(EXPORT).param("roleId", ILLISIBLE));

        private final java.util.function.Function<UUID, MockHttpServletRequestBuilder> requete;

        Invalide(java.util.function.Function<UUID, MockHttpServletRequestBuilder> requete) {
            this.requete = requete;
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureUtilisateurs fixture;

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
        assertThat(fixture.nombreComptes(code)).isZero();
    }

    private void temoinAdministrateur(Route route, UUID id, String code) throws Exception {
        mockMvc.perform(route.requete.apply(id, code).with(connecte(admin))).andExpect(status().is2xxSuccessful());
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("refus")
    void r1_roleNonAdministrateur_403_puisTemoinAdministrateur(Route route, String role) throws Exception {
        UUID id = fixture.compte().getId();
        String code = "new." + jeton;

        refuseSansEcrire(route, FixtureFaisabiliteWorkflow.avecRole(admin, role), 403, id, code);

        temoinAdministrateur(route, id, code);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Route.class)
    void r1_sansConnexion_401_puisTemoinAdministrateur(Route route) throws Exception {
        UUID id = fixture.compte().getId();
        String code = "new." + jeton;

        refuseSansEcrire(route, requete -> requete, 401, id, code);

        temoinAdministrateur(route, id, code);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Invalide.class)
    void r1_403_avantTout400(Invalide invalide) throws Exception {
        UUID id = fixture.compte().getId();

        mockMvc.perform(invalide.requete.apply(id).with(FixtureFaisabiliteWorkflow.avecRole(admin, "Commercial")))
                .andExpect(status().isForbidden());

        mockMvc.perform(invalide.requete.apply(id).with(connecte(admin))).andExpect(status().isBadRequest());
    }
}
