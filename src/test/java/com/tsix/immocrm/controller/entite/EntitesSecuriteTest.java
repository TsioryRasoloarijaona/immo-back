package com.tsix.immocrm.controller.entite;

import com.tsix.immocrm.domain.PorteeCleApi;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.app.AppApiClient;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.app.AppApiClientRepository;
import com.tsix.immocrm.security.ApiKeyAuthenticationFilter;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.security.CleApiService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sécurité du lot P5 — FEATURE-entites-juridiques.md §0, §2 : écritures réservées à l'Administrateur (403), aucune
 * route lue ni écrite par clé d'API (« hors PorteeCleApi ») ; lecture ouverte aux comptes authentifiés.
 * Complète PatrimoineSecuriteTest pour les routes du lot (EntitesSecuriteTest).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EntitesSecuriteTest {

    private static final UUID ID = UUID.fromString("2F5E7A9C-3D4E-4F60-8B12-C3D4E5F6A7B8");
    private static final UUID AUTRE_ID = UUID.fromString("3A6F8B0D-4E5F-4071-9C23-D4E5F6A7B8C9");
    private static final String ENTITES = "/api/entites-juridiques";
    private static final String ACTIONNAIRES = "/api/actionnaires";
    private static final String GROUPES = "/api/groupes-actionnaires";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private AppApiClientRepository clientRepository;
    @Autowired private CleApiService cleApiService;

    static Stream<Arguments> endpointsDEcriture() {
        return Stream.of(
                Arguments.of(HttpMethod.POST, ENTITES),
                Arguments.of(HttpMethod.PATCH, ENTITES + "/" + ID),
                Arguments.of(HttpMethod.DELETE, ENTITES + "/" + ID),
                Arguments.of(HttpMethod.PUT, ENTITES + "/" + ID + "/repartition"),
                Arguments.of(HttpMethod.POST, ENTITES + "/" + ID + "/repartition/depuis-groupe/" + AUTRE_ID),
                Arguments.of(HttpMethod.POST, ACTIONNAIRES),
                Arguments.of(HttpMethod.PATCH, ACTIONNAIRES + "/" + ID),
                Arguments.of(HttpMethod.DELETE, ACTIONNAIRES + "/" + ID),
                Arguments.of(HttpMethod.POST, GROUPES),
                Arguments.of(HttpMethod.PATCH, GROUPES + "/" + ID),
                Arguments.of(HttpMethod.DELETE, GROUPES + "/" + ID));
    }

    private static RequestPostProcessor commercial(AdmUser user) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getCode(), null, false, true, 0);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_Commercial"))));
    }

    private String cleToutesPortees() {
        String cle = cleApiService.genererCle();
        AppApiClient client = new AppApiClient();
        client.setId(UUID.randomUUID());
        client.setNom("cle-entites-" + UUID.randomUUID().toString().substring(0, 8));
        client.setCleHash(CleApiService.hacher(cle));
        client.setClePrefixe(CleApiService.prefixeAffiche(cle));
        client.setAdmUser(fixture.administrateur());
        client.setPortees(PorteeCleApi.versColonne(EnumSet.allOf(PorteeCleApi.class)));
        client.setActif(true);
        client.setUserCreateDateTime(LocalDateTime.now());
        clientRepository.save(client);
        return cle;
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("endpointsDEcriture")
    void r0_commercial_403_surChaqueEcriture(HttpMethod methode, String url) throws Exception {
        mockMvc.perform(request(methode, url).with(commercial(fixture.administrateur()))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("endpointsDEcriture")
    void r2_cleApi_403_horsPortee_ecriture(HttpMethod methode, String url) throws Exception {
        mockMvc.perform(request(methode, url).header(ApiKeyAuthenticationFilter.EN_TETE, cleToutesPortees())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ApiKeyAuthenticationFilter.CODE_HORS_PORTEE));
    }

    @ParameterizedTest(name = "GET {0}")
    @ValueSource(strings = {ENTITES, ACTIONNAIRES, GROUPES})
    void r2_cleApi_403_horsPortee_lecture(String url) throws Exception {
        mockMvc.perform(get(url).header(ApiKeyAuthenticationFilter.EN_TETE, cleToutesPortees()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ApiKeyAuthenticationFilter.CODE_HORS_PORTEE));
    }

    @ParameterizedTest(name = "GET {0}")
    @ValueSource(strings = {ENTITES, ACTIONNAIRES, GROUPES})
    void r0_commercial_lectureOuverte_200(String url) throws Exception {
        mockMvc.perform(get(url).with(commercial(fixture.administrateur()))).andExpect(status().isOk());
    }
}
