package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.domain.PorteeCleApi;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.app.AppApiClient;
import com.tsix.immocrm.fixtures.FixtureFaisabilite;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.app.AppApiClientRepository;
import com.tsix.immocrm.security.ApiKeyAuthenticationFilter;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.security.CleApiService;
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

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static com.tsix.immocrm.fixtures.FixtureFaisabilite.RACINE;
import static com.tsix.immocrm.fixtures.FixtureLienProspection.CHOIX_PROSPECTIONS;
import static com.tsix.immocrm.fixtures.FixtureLienProspection.lien;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Droits des routes du lien étude ↔ prospection — FEATURE-patrimoine-etude-prospection.md §3 : écritures réservées à
 * l'Administrateur, choix des prospections = lecture d'étude ; 401 anonyme, 403 Commercial, 403 clé d'API (hors portée).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LienProspectionEtudeSecuriteTest {

    private static final UUID ID = UUID.fromString("7D8E9F0A-1B2C-4D3E-8F4A-5B6C7D8E9F0A");
    private static final UUID PROSPECTION = UUID.fromString("8E9F0A1B-2C3D-4E4F-9A5B-6C7D8E9F0A1B");
    private static final String DIRECTION_GENERALE = "Direction Générale";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureFaisabilite fixture;
    @Autowired private AppApiClientRepository clientRepository;
    @Autowired private CleApiService cleApiService;

    @BeforeEach
    void setUp() {
        fixture.parametrage();
    }

    static Stream<Arguments> ecritures() {
        String lienProspection = RACINE + "/" + ID + "/prospection-terrain";
        return Stream.of(
                Arguments.of(HttpMethod.PUT, lienProspection),
                Arguments.of(HttpMethod.POST, lienProspection + "/recopier"),
                Arguments.of(HttpMethod.DELETE, lienProspection));
    }

    static Stream<Arguments> tousLesEndpoints() {
        return Stream.concat(ecritures(), Stream.of(Arguments.of(HttpMethod.GET, CHOIX_PROSPECTIONS)));
    }

    private AdmUser compte(String prefixe) {
        AdmUser user = patrimoine.administrateur();
        user.setCode(prefixe + "." + UUID.randomUUID());
        return user;
    }

    private static RequestPostProcessor avecRole(AdmUser user, String role) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getCode(), null, false, true, 0);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    /** Corps valide : un 400 de lecture du corps ne doit pas masquer le refus de droits. */
    private static MockHttpServletRequestBuilder appel(HttpMethod methode, String url) {
        return request(methode, url).contentType(MediaType.APPLICATION_JSON).content(lien(PROSPECTION));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("tousLesEndpoints")
    void s3_anonyme_401(HttpMethod methode, String url) throws Exception {
        mockMvc.perform(appel(methode, url)).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("tousLesEndpoints")
    void s3_commercial_403(HttpMethod methode, String url) throws Exception {
        mockMvc.perform(appel(methode, url).with(avecRole(compte("commercial.lien"), "Commercial")))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("ecritures")
    void s3_ecriture_403_directionGenerale(HttpMethod methode, String url) throws Exception {
        mockMvc.perform(appel(methode, url).with(avecRole(compte("dg.lien"), DIRECTION_GENERALE)))
                .andExpect(status().isForbidden());
    }

    @Test
    void s3_choixProspections_directionGenerale_200() throws Exception {
        mockMvc.perform(get(CHOIX_PROSPECTIONS).with(avecRole(compte("dg.choix"), DIRECTION_GENERALE)))
                .andExpect(status().isOk());
    }

    /** {@code /api/etudes-faisabilite} n'est dans aucun préfixe de {@code PorteeCleApi} : 403 même avec toutes les portées. */
    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("tousLesEndpoints")
    void s3_cleApi_403_horsPortee(HttpMethod methode, String url) throws Exception {
        String cle = cleApiService.genererCle();
        AppApiClient client = new AppApiClient();
        client.setId(UUID.randomUUID());
        client.setNom("cle-lien-" + UUID.randomUUID().toString().substring(0, 8));
        client.setCleHash(CleApiService.hacher(cle));
        client.setClePrefixe(CleApiService.prefixeAffiche(cle));
        client.setAdmUser(compte("service.lien"));
        client.setPortees(PorteeCleApi.versColonne(EnumSet.allOf(PorteeCleApi.class)));
        client.setActif(true);
        client.setUserCreateDateTime(LocalDateTime.now());
        clientRepository.save(client);

        mockMvc.perform(appel(methode, url).header(ApiKeyAuthenticationFilter.EN_TETE, cle))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ApiKeyAuthenticationFilter.CODE_HORS_PORTEE));
    }
}
