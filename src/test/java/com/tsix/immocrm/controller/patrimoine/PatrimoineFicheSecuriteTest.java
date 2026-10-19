package com.tsix.immocrm.controller.patrimoine;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Extension de {@code PatrimoineSecuriteTest} (FEATURE-patrimoine-tranche-fiche.md §6) : les nouvelles écritures du lot
 * P1a (suppression de programme, zones) sont réservées à l'Administrateur et hors portée des clés d'API.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PatrimoineFicheSecuriteTest {

    private static final UUID ID = UUID.fromString("2F5E7A9C-3D4E-4F60-8B12-C3D4E5F6A7B8");
    private static final UUID ZONE_ID = UUID.fromString("3A6F8B0D-4E5F-4071-9C23-D4E5F6A7B8C9");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private AppApiClientRepository clientRepository;
    @Autowired private CleApiService cleApiService;

    static Stream<Arguments> nouvellesEcritures() {
        return Stream.of(
                Arguments.of(HttpMethod.DELETE, "/api/projets/" + ID),
                Arguments.of(HttpMethod.POST, "/api/projets/" + ID + "/zones"),
                Arguments.of(HttpMethod.PATCH, "/api/projets/" + ID + "/zones/" + ZONE_ID),
                Arguments.of(HttpMethod.DELETE, "/api/projets/" + ID + "/zones/" + ZONE_ID));
    }

    private AdmUser compte(String code) {
        AdmUser user = fixture.administrateur();
        user.setCode(code + "." + UUID.randomUUID());
        return user;
    }

    private static RequestPostProcessor commercial(AdmUser user) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getCode(), null, false, true, 0);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_Commercial"))));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("nouvellesEcritures")
    void r6_commercial_403_surChaqueNouvelleEcriture(HttpMethod methode, String url) throws Exception {
        mockMvc.perform(request(methode, url).with(commercial(compte("commercial.p1a")))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("nouvellesEcritures")
    void r6_cleApi_403_horsPortee(HttpMethod methode, String url) throws Exception {
        String cle = cleApiService.genererCle();
        AppApiClient client = new AppApiClient();
        client.setId(UUID.randomUUID());
        client.setNom("cle-p1a-" + UUID.randomUUID().toString().substring(0, 8));
        client.setCleHash(CleApiService.hacher(cle));
        client.setClePrefixe(CleApiService.prefixeAffiche(cle));
        client.setAdmUser(compte("service.p1a"));
        client.setPortees(PorteeCleApi.versColonne(EnumSet.allOf(PorteeCleApi.class)));
        client.setActif(true);
        client.setUserCreateDateTime(LocalDateTime.now());
        clientRepository.save(client);

        mockMvc.perform(request(methode, url).header(ApiKeyAuthenticationFilter.EN_TETE, cle)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ApiKeyAuthenticationFilter.CODE_HORS_PORTEE));
    }
}
