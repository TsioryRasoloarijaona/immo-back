package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.fixtures.FixtureOngletsTiers;
import com.tsix.immocrm.fixtures.FixtureTiers;
import com.tsix.immocrm.fixtures.FixtureTiers.Jeu;
import com.tsix.immocrm.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureOngletsTiers.CONGES;
import static com.tsix.immocrm.fixtures.FixtureOngletsTiers.DEPENSES;
import static com.tsix.immocrm.fixtures.FixtureOngletsTiers.EXPORT;
import static com.tsix.immocrm.fixtures.FixtureOngletsTiers.ONGLET;
import static com.tsix.immocrm.fixtures.FixtureOngletsTiers.ONGLETS_COMMUNS;
import static com.tsix.immocrm.fixtures.FixtureOngletsTiers.TOUS_ONGLETS;
import static com.tsix.immocrm.fixtures.FixtureOngletsTiers.onglet;
import static com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers.FOURNISSEURS;
import static com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers.PERSONNELS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Accès et lecture seule — FEATURE-admin-onglets-tiers.md, règles testables 4 et 5, décisions 1 et 9. Les 403 sont verts
 * avant le code (règle d'URL de {@code SecurityConfig}) : le témoin Administrateur prouve que la route existe.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OngletsTiersSecuriteTest {

    private static final String MARQUEUR_ONGLETS = "/onglets/";
    private static final String XLSX = "xlsx";
    private static final String FORMAT = "format";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureTiers tiers;
    @Autowired private FixtureOngletsTiers fixture;
    @Autowired @Qualifier("requestMappingHandlerMapping") private RequestMappingHandlerMapping routes;

    private Jeu j;
    private UUID fournisseur;
    private UUID employe;

    @BeforeEach
    void setUp() {
        j = tiers.jeu();
        fixture.preparer();
        fournisseur = j.tiers(FOURNISSEURS, "F").inserer();
        employe = j.tiers(PERSONNELS, "P").inserer();
    }

    private static Stream<Arguments> ongletsEtRoles() {
        Stream<String> onglets = Stream.concat(ONGLETS_COMMUNS.stream(), Stream.of(CONGES));
        return onglets.flatMap(o -> Stream.of("Commercial", "Direction Générale").map(role -> Arguments.of(o, role)));
    }

    private RequestPostProcessor avecRole(String role) {
        AuthenticatedUser principal = new AuthenticatedUser(j.admin().getId(), j.admin().getCode(), null, false, true, 0);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private UUID cible(String nom) {
        return CONGES.equals(nom) ? employe : fournisseur;
    }

    private String famille(String nom) {
        return CONGES.equals(nom) ? PERSONNELS.famille() : FOURNISSEURS.famille();
    }

    private List<Map.Entry<RequestMappingInfo, HandlerMethod>> routesDesOnglets() {
        return routes.getHandlerMethods().entrySet().stream()
                .filter(route -> route.getKey().getPatternValues().stream().anyMatch(m -> m.contains(MARQUEUR_ONGLETS)))
                .toList();
    }

    // ------------------------------------------------------------------ règle 4 : Administrateur seul

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("ongletsEtRoles")
    void regle4_sansAdministrateur_403(String nom, String role) throws Exception {
        mockMvc.perform(onglet(famille(nom), cible(nom), nom, avecRole(role))).andExpect(status().isForbidden());
    }

    private static Stream<String> tousOnglets() {
        return TOUS_ONGLETS.stream();
    }

    @ParameterizedTest
    @MethodSource("tousOnglets")
    void regle4_temoin_administrateur_200(String nom) throws Exception {
        mockMvc.perform(onglet(famille(nom), cible(nom), nom, connecte(j.admin()))).andExpect(status().isOk());
    }

    // La règle d'URL masque l'oubli : décision 9 demande aussi le contrôle sur la route elle-même.
    @Test
    void decision9_routesDesOnglets_preAuthorizeAdministrateur() {
        assertThat(routesDesOnglets()).isNotEmpty().allSatisfy(route -> {
            HandlerMethod methode = route.getValue();
            PreAuthorize garde = AnnotatedElementUtils.findMergedAnnotation(methode.getMethod(), PreAuthorize.class);
            if (garde == null) {
                garde = AnnotatedElementUtils.findMergedAnnotation(methode.getBeanType(), PreAuthorize.class);
            }
            assertThat(garde).isNotNull().extracting(PreAuthorize::value).isEqualTo("hasRole('Administrateur')");
        });
    }

    // ------------------------------------------------------------------ règle 5 / décision 1 : aucune écriture

    @Test
    void regle5_seulesDesRoutesGet_sousLesOnglets() {
        assertThat(routesDesOnglets()).isNotEmpty().allSatisfy(route ->
                assertThat(route.getKey().getMethodsCondition().getMethods()).isEqualTo(Set.of(RequestMethod.GET)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    void regle5_ecritureSurUnOnglet_405(String methode) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(methode), ONGLET, FOURNISSEURS.famille(), fournisseur, DEPENSES)
                        .with(connecte(j.admin())))
                .andExpect(status().isMethodNotAllowed());
    }

    // ------------------------------------------------------------------ export de liste : même droit que la liste

    @ParameterizedTest
    @ValueSource(strings = {"Commercial", "Direction Générale"})
    void reponseR4_exportListe_sansAdministrateur_403(String role) throws Exception {
        mockMvc.perform(get(EXPORT, FOURNISSEURS.famille()).param(FORMAT, XLSX).with(avecRole(role)))
                .andExpect(status().isForbidden());
    }

    @Test
    void reponseR4_exportListe_temoin_administrateur_200() throws Exception {
        mockMvc.perform(get(EXPORT, FOURNISSEURS.famille()).param(FORMAT, XLSX).param("recherche", j.jeton())
                        .with(connecte(j.admin())))
                .andExpect(status().isOk());
    }
}
