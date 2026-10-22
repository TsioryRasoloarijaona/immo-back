package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.domain.PorteeCleApi;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.app.AppApiClient;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.fixtures.ApiFaisabilite;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static com.tsix.immocrm.fixtures.FixtureFaisabilite.RACINE;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.fiche;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.jeton;
import static com.tsix.immocrm.fixtures.FixtureFaisabiliteFinance.FINANCEMENTS;
import static com.tsix.immocrm.fixtures.FixtureFaisabiliteFinance.PREVISIONS;
import static com.tsix.immocrm.fixtures.FixtureFaisabiliteFinance.TRESORERIE;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Droits des endpoints P8b-1 — FEATURE-patrimoine-faisabilite-workflow.md §1.2, §2.3, §2.6 : lecture Administrateur +
 * Direction Générale, écriture (et génération d'échéancier) Administrateur seul, clés d'API hors portée (403).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FaisabiliteFinanceSecuriteTest {

    private static final String ETUDE = RACINE + "/0D3C5E7A-1B2C-4D5E-8F90-A1B2C3D4E5F6";
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

    /** §1.2 et §2.3 : écritures et génération de l'échéancier (Administrateur seul). */
    static Stream<Arguments> ecritures() {
        return Stream.of(
                Arguments.of(HttpMethod.PUT, ETUDE + "/financements"),
                Arguments.of(HttpMethod.POST, ETUDE + "/financements/echeancier"),
                Arguments.of(HttpMethod.PUT, ETUDE + "/previsions"));
    }

    /** §1.2, §2.3, §2.6 : lectures et aperçus (Administrateur + Direction Générale). */
    static Stream<Arguments> lectures() {
        return Stream.of(
                Arguments.of(HttpMethod.GET, ETUDE + "/financements"),
                Arguments.of(HttpMethod.GET, ETUDE + "/previsions"),
                Arguments.of(HttpMethod.POST, ETUDE + "/previsions/calcul"),
                Arguments.of(HttpMethod.GET, ETUDE + "/tresorerie"),
                Arguments.of(HttpMethod.POST, ETUDE + "/tresorerie/calcul"));
    }

    static Stream<Arguments> tousLesEndpoints() {
        return Stream.concat(ecritures(), lectures());
    }

    static Stream<Arguments> lecturesParAutreRole() {
        return lectures().flatMap(l -> Stream.of("Commercial", "Comptabilité", "Directeur Technique")
                .map(r -> Arguments.of(r, l.get()[0], l.get()[1])));
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

    @ParameterizedTest(name = "{0} {1} {2}")
    @MethodSource("lecturesParAutreRole")
    void r1_2_lecture_403_horsAdministrateurEtDirectionGenerale(String role, HttpMethod methode, String url) throws Exception {
        mockMvc.perform(request(methode, url).with(avecRole(compte("lecteur.finance"), role))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("ecritures")
    void r1_2_ecriture_403_directionGenerale(HttpMethod methode, String url) throws Exception {
        mockMvc.perform(request(methode, url).with(avecRole(compte("dg.finance"), DIRECTION_GENERALE))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    /** §1.2 « 403 … clé d'API (hors PorteeCleApi.REFERENTIELS_LIRE, à prouver) » : toutes portées accordées, 403 quand même. */
    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("tousLesEndpoints")
    void r1_2_cleApi_403_horsPortee(HttpMethod methode, String url) throws Exception {
        String cle = cleApiService.genererCle();
        AppApiClient client = new AppApiClient();
        client.setId(UUID.randomUUID());
        client.setNom("cle-finance-" + UUID.randomUUID().toString().substring(0, 8));
        client.setCleHash(CleApiService.hacher(cle));
        client.setClePrefixe(CleApiService.prefixeAffiche(cle));
        client.setAdmUser(compte("service.finance"));
        client.setPortees(PorteeCleApi.versColonne(EnumSet.allOf(PorteeCleApi.class)));
        client.setActif(true);
        client.setUserCreateDateTime(LocalDateTime.now());
        clientRepository.save(client);

        mockMvc.perform(request(methode, url).header(ApiKeyAuthenticationFilter.EN_TETE, cle)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ApiKeyAuthenticationFilter.CODE_HORS_PORTEE));
    }

    /** Accès accordé : la Direction Générale lit le financement, les prévisions et la trésorerie d'une étude. */
    @Test
    void r1_2_directionGenerale_lit_200() throws Exception {
        String jeton = jeton();
        CfgCompany societe = patrimoine.entite("ENT-" + jeton, true);
        UUID id = new ApiFaisabilite(mockMvc, compte("admin.finance")).etude(fiche("SEC-" + jeton, societe.getId()));
        RequestPostProcessor dg = avecRole(compte("dg.lecture"), DIRECTION_GENERALE);

        mockMvc.perform(get(FINANCEMENTS, id).with(dg)).andExpect(status().isOk());
        mockMvc.perform(get(PREVISIONS, id).with(dg)).andExpect(status().isOk());
        mockMvc.perform(get(TRESORERIE, id).with(dg)).andExpect(status().isOk());
    }
}
