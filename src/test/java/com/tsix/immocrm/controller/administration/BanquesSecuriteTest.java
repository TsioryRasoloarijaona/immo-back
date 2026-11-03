package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.fixtures.FixtureBanques;
import com.tsix.immocrm.fixtures.FixtureTiers.Jeu;
import com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers;
import com.tsix.immocrm.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.UUID;
import java.util.stream.Stream;

import static com.tsix.immocrm.fixtures.AppelsTiers.json;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureBanques.corps;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Accès — FEATURE-admin-banques-organismes.md, règle 11. Les 403 sont verts avant le code (règle d'URL), le témoin
 * Administrateur prouve que la route existe. Import : {@link BanquesImportTest}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BanquesSecuriteTest {

    private static final String BASE = "/api/administration/tiers/:famille";
    private static final String FICHE = BASE + "/:tiers";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureBanques banques;

    private Jeu j;

    @BeforeEach
    void setUp() {
        j = banques.jeu();
    }

    private static Stream<Arguments> routes() {
        return Stream.of(TypeTiers.BANQUES, TypeTiers.ORGANISMES).flatMap(type -> Stream.of(
                Arguments.of(type, "GET", BASE),
                Arguments.of(type, "GET", BASE + "/natures"),
                Arguments.of(type, "GET", FICHE),
                Arguments.of(type, "POST", BASE),
                Arguments.of(type, "PUT", FICHE),
                Arguments.of(type, "DELETE", FICHE)));
    }

    private static Stream<Arguments> routesEtRoles() {
        return routes().flatMap(r -> List.of("Commercial", "Direction Générale").stream()
                .map(role -> Arguments.of(r.get()[0], r.get()[1], r.get()[2], role)));
    }

    private MockHttpServletRequestBuilder appel(TypeTiers type, String methode, String gabarit) {
        UUID tiers = j.tiers(type, "T1").inserer();
        String url = gabarit.replace(":famille", type.famille()).replace(":tiers", tiers.toString());
        MockHttpServletRequestBuilder requete = request(HttpMethod.valueOf(methode), url);
        if ("POST".equals(methode) || "PUT".equals(methode)) {
            return requete.contentType(MediaType.APPLICATION_JSON).content(json(corps(j, j.code("NEUF"), "TIERS " + j.jeton())));
        }
        return requete;
    }

    private RequestPostProcessor avecRole(String role) {
        AuthenticatedUser principal = new AuthenticatedUser(j.admin().getId(), j.admin().getCode(), null, false, true, 0);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    @ParameterizedTest
    @MethodSource("routes")
    void r11_temoin_administrateur_2xx(TypeTiers type, String methode, String gabarit) throws Exception {
        mockMvc.perform(appel(type, methode, gabarit).with(connecte(j.admin()))).andExpect(status().is2xxSuccessful());
    }

    @ParameterizedTest
    @MethodSource("routesEtRoles")
    void r11_sansRoleAdministrateur_403(TypeTiers type, String methode, String gabarit, String role) throws Exception {
        mockMvc.perform(appel(type, methode, gabarit).with(avecRole(role))).andExpect(status().isForbidden());
    }
}
