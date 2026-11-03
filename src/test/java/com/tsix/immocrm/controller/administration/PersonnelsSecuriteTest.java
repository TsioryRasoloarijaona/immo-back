package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.fixtures.FixturePersonnels;
import com.tsix.immocrm.fixtures.FixtureTiers;
import com.tsix.immocrm.fixtures.FixtureTiers.Jeu;
import com.tsix.immocrm.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static com.tsix.immocrm.fixtures.AppelsTiers.corpsPhysique;
import static com.tsix.immocrm.fixtures.AppelsTiers.json;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Accès — FEATURE-admin-personnels.md, règle 11. Les 403 sont verts avant le code (règle d'URL de {@code SecurityConfig}), le
 * témoin Administrateur sur la même route est rouge jusqu'à ce qu'elle existe. Import : {@link PersonnelsImportTest}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PersonnelsSecuriteTest {

    private static final String LISTE = "/api/administration/tiers/personnels";
    private static final String FICHE = LISTE + "/:id";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureTiers fixture;

    private Jeu j;
    private UUID employe;

    @BeforeEach
    void setUp() {
        j = fixture.jeu();
        employe = FixturePersonnels.employe(j, "E1").inserer();
    }

    private static Stream<Arguments> routes() {
        return Stream.of(
                Arguments.of("GET", LISTE),
                Arguments.of("GET", LISTE + "/natures"),
                Arguments.of("GET", FICHE),
                Arguments.of("POST", LISTE),
                Arguments.of("PUT", FICHE),
                Arguments.of("DELETE", FICHE),
                Arguments.of("POST", FixturePersonnels.TRANSFORMER.replace("{id}", ":id")));
    }

    private static Stream<Arguments> routesEtRoles() {
        return routes().flatMap(r -> List.of("Commercial", "Direction Générale").stream()
                .map(role -> Arguments.of(r.get()[0], r.get()[1], role)));
    }

    private MockHttpServletRequestBuilder appel(String methode, String gabarit) {
        MockHttpServletRequestBuilder requete = request(HttpMethod.valueOf(methode), gabarit.replace(":id", employe.toString()));
        if (gabarit.endsWith("transformer-en-contact") || !List.of("POST", "PUT").contains(methode)) {
            return requete;
        }
        Map<String, Object> corps = corpsPhysique(j, "Amal", "Benali " + j.jeton());
        return requete.contentType(MediaType.APPLICATION_JSON).content(json(corps));
    }

    private RequestPostProcessor avecRole(String role) {
        AuthenticatedUser principal = new AuthenticatedUser(j.admin().getId(), j.admin().getCode(), null, false, true, 0);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    @ParameterizedTest
    @MethodSource("routes")
    void r11_temoin_administrateur_2xx(String methode, String gabarit) throws Exception {
        mockMvc.perform(appel(methode, gabarit).with(connecte(j.admin()))).andExpect(status().is2xxSuccessful());
    }

    @Test
    void r11_temoin_transformation_200() throws Exception {
        mockMvc.perform(FixturePersonnels.transformer(employe, connecte(j.admin()))).andExpect(status().isOk());
    }

    @ParameterizedTest
    @MethodSource("routesEtRoles")
    void r11_sansRoleAdministrateur_403(String methode, String gabarit, String role) throws Exception {
        mockMvc.perform(appel(methode, gabarit).with(avecRole(role))).andExpect(status().isForbidden());
    }
}
