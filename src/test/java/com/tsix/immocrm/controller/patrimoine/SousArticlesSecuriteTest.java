package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.domain.PorteeCleApi;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.app.AppApiClient;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.app.AppApiClientRepository;
import com.tsix.immocrm.security.ApiKeyAuthenticationFilter;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.security.CleApiService;
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
 * Accès du lot P4 — FEATURE-patrimoine-sous-articles.md §0, recalage R2 et R5 : écritures {@code /api/sous-articles}
 * Administrateur (403 commercial, 403 {@code HORS_PORTEE} clé d'API) ; liste lisible par tout utilisateur authentifié
 * mais hors de toute portée de clé ; catalogue {@code /api/referentiels/colonnes/articles} sous {@code REFERENTIELS_LIRE}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SousArticlesSecuriteTest {

    private static final UUID ID = UUID.fromString("2F5E7A9C-3D4E-4F60-8B12-C3D4E5F6A7B8");
    private static final String LISTE = "/api/sous-articles";
    private static final String CATALOGUE = "/api/referentiels/colonnes/articles";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private AppApiClientRepository clientRepository;
    @Autowired private CleApiService cleApiService;

    static Stream<Arguments> ecritures() {
        return Stream.of(
                Arguments.of(HttpMethod.POST, LISTE),
                Arguments.of(HttpMethod.PATCH, LISTE + "/" + ID),
                Arguments.of(HttpMethod.DELETE, LISTE + "/" + ID));
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

    /** Clé portant toutes les portées : seule l'absence de route couverte peut la refuser. */
    private String cleToutesPortees() {
        String cle = cleApiService.genererCle();
        AppApiClient client = new AppApiClient();
        client.setId(UUID.randomUUID());
        client.setNom("cle-p4-" + UUID.randomUUID().toString().substring(0, 8));
        client.setCleHash(CleApiService.hacher(cle));
        client.setClePrefixe(CleApiService.prefixeAffiche(cle));
        client.setAdmUser(compte("service.p4"));
        client.setPortees(PorteeCleApi.versColonne(EnumSet.allOf(PorteeCleApi.class)));
        client.setActif(true);
        client.setUserCreateDateTime(LocalDateTime.now());
        clientRepository.save(client);
        return cle;
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("ecritures")
    void r5_commercial_403_surChaqueEcriture(HttpMethod methode, String url) throws Exception {
        mockMvc.perform(request(methode, url).with(commercial(compte("commercial.p4")))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("ecritures")
    void r5_cleApi_403_horsPortee_surChaqueEcriture(HttpMethod methode, String url) throws Exception {
        mockMvc.perform(request(methode, url).header(ApiKeyAuthenticationFilter.EN_TETE, cleToutesPortees())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ApiKeyAuthenticationFilter.CODE_HORS_PORTEE));
    }

    @Test
    void r5_liste_ouverteAuCommercial_200() throws Exception {
        mockMvc.perform(get(LISTE).with(commercial(compte("commercial.liste.p4"))))
                .andExpect(status().isOk());
    }

    /** R5 : la liste expose prix et clients, aucune portée de clé ne la couvre. */
    @Test
    void r5_liste_cleApi_403_horsPortee() throws Exception {
        mockMvc.perform(get(LISTE).header(ApiKeyAuthenticationFilter.EN_TETE, cleToutesPortees()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ApiKeyAuthenticationFilter.CODE_HORS_PORTEE));
    }

    /** R2 : le catalogue n'a que des libellés, couvert par REFERENTIELS_LIRE (/api/referentiels/**). */
    @Test
    void r2_catalogue_cleApiReferentiels_200() throws Exception {
        mockMvc.perform(get(CATALOGUE).header(ApiKeyAuthenticationFilter.EN_TETE, cleToutesPortees()))
                .andExpect(status().isOk());
    }

    @Test
    void r2_catalogue_ouvertAuCommercial_200() throws Exception {
        mockMvc.perform(get(CATALOGUE).with(commercial(compte("commercial.catalogue.p4"))))
                .andExpect(status().isOk());
    }
}
