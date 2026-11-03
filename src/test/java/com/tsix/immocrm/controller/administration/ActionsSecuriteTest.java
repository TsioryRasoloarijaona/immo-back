package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureActions;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Accès et lecture seule — FEATURE-admin-actions.md, règles 8 et 9. Les 403 sont verts avant le code (règle d'URL de
 * {@code SecurityConfig}) : le témoin Administrateur prouve que la route existe.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ActionsSecuriteTest {

    private static final String URL = "/api/administration/actions";
    private static final String COMMERCIAL = "Commercial";
    private static final String DIRECTION_GENERALE = "Direction Générale";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureActions fixture;
    @Autowired @Qualifier("requestMappingHandlerMapping") private RequestMappingHandlerMapping routes;

    private AdmUser admin;

    @BeforeEach
    void setUp() {
        fixture.assurerVue();
        admin = patrimoine.administrateur();
    }

    private RequestPostProcessor avecRole(String role) {
        AuthenticatedUser principal = new AuthenticatedUser(admin.getId(), admin.getCode(), null, false, true, 0);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private List<Map.Entry<RequestMappingInfo, HandlerMethod>> routesDesActions() {
        return routes.getHandlerMethods().entrySet().stream()
                .filter(route -> route.getKey().getPatternValues().stream().anyMatch(motif -> motif.startsWith(URL)))
                .toList();
    }

    // ------------------------------------------------------------------ règle 8 : rôle

    @ParameterizedTest
    @ValueSource(strings = {COMMERCIAL, DIRECTION_GENERALE})
    void r8_sansRoleAdministrateur_403(String role) throws Exception {
        mockMvc.perform(get(URL).with(avecRole(role))).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {URL + "?size=0", URL + "?size=201", URL + "?page=-1", URL + "?page=1000001",
            URL + "?creationDebut=2026-03-11&creationFin=2026-03-10",
            URL + "?modificationDebut=2026-03-11&modificationFin=2026-03-10",
            URL + "?creationDebut=pas-une-date"})
    void r8_403_avantToutRefusDeSaisie(String url) throws Exception {
        mockMvc.perform(get(url).with(avecRole(COMMERCIAL))).andExpect(status().isForbidden());
    }

    @Test
    void r8_temoin_administrateur_200() throws Exception {
        mockMvc.perform(get(URL).with(connecte(admin))).andExpect(status().isOk());
    }

    // La règle d'URL masque l'oubli : le spec demande aussi le contrôle sur la route elle-même.
    @Test
    void r8_routeProtegeeParPreAuthorizeAdministrateur() {
        assertThat(routesDesActions()).isNotEmpty().allSatisfy(route -> {
            HandlerMethod methode = route.getValue();
            PreAuthorize garde = AnnotatedElementUtils.findMergedAnnotation(methode.getMethod(), PreAuthorize.class);
            if (garde == null) {
                garde = AnnotatedElementUtils.findMergedAnnotation(methode.getBeanType(), PreAuthorize.class);
            }
            assertThat(garde).isNotNull().extracting(PreAuthorize::value).isEqualTo("hasRole('Administrateur')");
        });
    }

    // ------------------------------------------------------------------ règle 9 : aucune écriture

    @Test
    void r9_seulesDesRoutesGet_sousLesActions() {
        assertThat(routesDesActions()).isNotEmpty().allSatisfy(route ->
                assertThat(route.getKey().getMethodsCondition().getMethods()).isEqualTo(Set.of(RequestMethod.GET)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    void r9_ecritureRefusee_405(String methode) throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.request(HttpMethod.valueOf(methode), URL).with(connecte(admin)))
                .andExpect(status().isMethodNotAllowed());
    }
}
