package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.fixtures.FixtureTiers;
import com.tsix.immocrm.fixtures.FixtureTiers.Jeu;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static com.tsix.immocrm.fixtures.AppelsTiers.corpsMorale;
import static com.tsix.immocrm.fixtures.AppelsTiers.json;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers.CONTACTS;
import static com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers.FOURNISSEURS;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Accès — FEATURE-admin-tiers.md, règle 14. Les 403 sont verts avant le code (règle d'URL de {@code SecurityConfig}), le
 * témoin Administrateur sur la même route prouve qu'elle existe. Import : {@link TiersImportTest}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TiersSecuriteTest {

    private static final String BASE = "/api/administration/tiers";
    private static final String FICHE = BASE + "/fournisseurs/:tiers";
    private static final String LIENS = BASE + "/:tiers/liens";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureTiers fixture;

    private Jeu j;
    private UUID tiers;
    private UUID lien;
    private UUID contactLibre;

    @BeforeEach
    void setUp() {
        j = fixture.jeu();
        tiers = j.tiers(FOURNISSEURS, "F1").inserer();
        contactLibre = j.tiers(CONTACTS, "C2").inserer();
        lien = j.lien(tiers, j.tiers(CONTACTS, "C1").inserer(), j.relation());
    }

    private static Stream<Arguments> routes() {
        return Stream.of(
                Arguments.of("GET", BASE + "/fournisseurs"),
                Arguments.of("GET", BASE + "/contacts"),
                Arguments.of("GET", BASE + "/fournisseurs/natures"),
                Arguments.of("GET", FICHE),
                Arguments.of("POST", BASE + "/fournisseurs"),
                Arguments.of("PUT", FICHE),
                Arguments.of("DELETE", FICHE),
                Arguments.of("GET", LIENS),
                Arguments.of("POST", LIENS),
                Arguments.of("DELETE", LIENS + "/:lien"));
    }

    private static Stream<Arguments> routesEtRoles() {
        return routes().flatMap(r -> List.of("Commercial", "Direction Générale").stream()
                .map(role -> Arguments.of(r.get()[0], r.get()[1], role)));
    }

    private MockHttpServletRequestBuilder appel(String methode, String gabarit) {
        String url = gabarit.replace(":tiers", tiers.toString()).replace(":lien", lien.toString());
        MockHttpServletRequestBuilder requete = request(HttpMethod.valueOf(methode), url);
        if ("POST".equals(methode) || "PUT".equals(methode)) {
            return requete.contentType(MediaType.APPLICATION_JSON).content(json(corps(gabarit)));
        }
        return requete;
    }

    private Map<String, Object> corps(String gabarit) {
        if (gabarit.equals(LIENS)) {
            Map<String, Object> corps = new HashMap<>();
            corps.put("contactId", contactLibre);
            corps.put("relationTypeId", j.relation());
            return corps;
        }
        return corpsMorale(j, "Societe " + j.jeton());
    }

    private RequestPostProcessor avecRole(String role) {
        AuthenticatedUser principal = new AuthenticatedUser(j.admin().getId(), j.admin().getCode(), null, false, true, 0);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    @ParameterizedTest
    @MethodSource("routes")
    void r14_temoin_administrateur_2xx(String methode, String gabarit) throws Exception {
        mockMvc.perform(appel(methode, gabarit).with(connecte(j.admin()))).andExpect(status().is2xxSuccessful());
    }

    @ParameterizedTest
    @MethodSource("routesEtRoles")
    void r14_sansRoleAdministrateur_403(String methode, String gabarit, String role) throws Exception {
        mockMvc.perform(appel(methode, gabarit).with(avecRole(role))).andExpect(status().isForbidden());
    }
}
