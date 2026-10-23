package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.domain.PorteeCleApi;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.app.AppApiClient;
import com.tsix.immocrm.fixtures.FixtureFaisabilite;
import com.tsix.immocrm.fixtures.FixtureFaisabiliteWorkflow;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.app.AppApiClientRepository;
import com.tsix.immocrm.security.ApiKeyAuthenticationFilter;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.UUID;
import java.util.stream.Stream;

import static com.tsix.immocrm.fixtures.FixtureFaisabilite.RACINE;
import static com.tsix.immocrm.fixtures.FixtureFaisabiliteWorkflow.CHAMPS_OBLIGATOIRES;
import static com.tsix.immocrm.fixtures.FixtureFaisabiliteWorkflow.CONFIG_STATUTS;
import static com.tsix.immocrm.fixtures.FixtureFaisabiliteWorkflow.CONFIG_TRANSITIONS;
import static com.tsix.immocrm.fixtures.FixtureFaisabiliteWorkflow.ROLE_DG;
import static com.tsix.immocrm.fixtures.FixtureFaisabiliteWorkflow.avecRole;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Droits des endpoints P8b-2 — FEATURE-patrimoine-faisabilite-workflow.md §4.2, §4.3, §5, §7 : lecture Administrateur + Direction
 * Générale, écriture de configuration et des pièces jointes Administrateur seul, rôles non lecteurs 403, clés d'API hors portée (403).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FaisabiliteWorkflowSecuriteTest {

    private static final String ETUDE = RACINE + "/1E2F3A4B-5C6D-4E7F-8A9B-0C1D2E3F4A5B";
    private static final String STATUT = CONFIG_STATUTS + "/2F3A4B5C-6D7E-4F8A-9B0C-1D2E3F4A5B6C";
    private static final String TRANSITION = CONFIG_TRANSITIONS + "/3A4B5C6D-7E8F-4A9B-8C1D-2E3F4A5B6C7D";
    private static final String PIECE = ETUDE + "/pieces-jointes/4B5C6D7E-8F9A-4B0C-9D2E-3F4A5B6C7D8E";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureFaisabilite fixture;
    @Autowired private AppApiClientRepository clientRepository;
    @Autowired private CleApiService cleApiService;

    @BeforeEach
    void setUp() {
        fixture.parametrage();
    }

    /** Écritures de configuration (§5) et suppression de pièce jointe (§4.3) : Administrateur seul. */
    static Stream<Arguments> ecrituresAdministrateur() {
        return Stream.of(
                Arguments.of(HttpMethod.POST, CONFIG_STATUTS),
                Arguments.of(HttpMethod.PATCH, STATUT),
                Arguments.of(HttpMethod.DELETE, STATUT),
                Arguments.of(HttpMethod.POST, CONFIG_TRANSITIONS),
                Arguments.of(HttpMethod.PATCH, TRANSITION),
                Arguments.of(HttpMethod.DELETE, TRANSITION),
                Arguments.of(HttpMethod.DELETE, PIECE));
    }

    /** Lectures (§4.2, §4.3, §5, §7) : Administrateur + Direction Générale. */
    static Stream<Arguments> lectures() {
        return Stream.of(
                Arguments.of(HttpMethod.GET, ETUDE + "/transitions"),
                Arguments.of(HttpMethod.GET, ETUDE + "/historique"),
                Arguments.of(HttpMethod.GET, ETUDE + "/pieces-jointes"),
                Arguments.of(HttpMethod.GET, ETUDE + "/export.xlsx"),
                Arguments.of(HttpMethod.GET, CONFIG_STATUTS),
                Arguments.of(HttpMethod.GET, CONFIG_TRANSITIONS),
                Arguments.of(HttpMethod.GET, CHAMPS_OBLIGATOIRES));
    }

    static Stream<Arguments> tousLesEndpoints() {
        return Stream.concat(Stream.concat(ecrituresAdministrateur(), lectures()),
                Stream.of(Arguments.of(HttpMethod.POST, ETUDE + "/transitions"),
                        Arguments.of(HttpMethod.POST, ETUDE + "/pieces-jointes")));
    }

    static Stream<Arguments> lecturesParAutreRole() {
        return Stream.concat(lectures(), Stream.of(Arguments.of(HttpMethod.POST, ETUDE + "/transitions")))
                .flatMap(l -> Stream.of("Commercial", "Comptabilité", "Directeur Technique")
                        .map(r -> Arguments.of(r, l.get()[0], l.get()[1])));
    }

    private AdmUser compte() {
        return patrimoine.administrateur();
    }

    @ParameterizedTest(name = "{0} {1} {2}")
    @MethodSource("lecturesParAutreRole")
    void r4_2_horsAdministrateurEtDirectionGenerale_403(String role, HttpMethod methode, String url) throws Exception {
        mockMvc.perform(request(methode, url).with(avecRole(compte(), role))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("ecrituresAdministrateur")
    void r5_ecriture_directionGenerale_403(HttpMethod methode, String url) throws Exception {
        mockMvc.perform(request(methode, url).with(avecRole(compte(), ROLE_DG))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    /** §4.2 / §5 « clés d'API : hors portée » : toutes portées accordées, 403 quand même. */
    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("tousLesEndpoints")
    void r4_2_cleApi_403_horsPortee(HttpMethod methode, String url) throws Exception {
        String cle = cleApiService.genererCle();
        AppApiClient client = new AppApiClient();
        client.setId(UUID.randomUUID());
        client.setNom("cle-workflow-" + UUID.randomUUID().toString().substring(0, 8));
        client.setCleHash(CleApiService.hacher(cle));
        client.setClePrefixe(CleApiService.prefixeAffiche(cle));
        client.setAdmUser(compte());
        client.setPortees(PorteeCleApi.versColonne(EnumSet.allOf(PorteeCleApi.class)));
        client.setActif(true);
        client.setUserCreateDateTime(LocalDateTime.now());
        clientRepository.save(client);

        mockMvc.perform(request(methode, url).header(ApiKeyAuthenticationFilter.EN_TETE, cle)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ApiKeyAuthenticationFilter.CODE_HORS_PORTEE));
    }

    /** §5 : la Direction Générale lit la configuration. */
    @Test
    void r5_directionGenerale_litLaConfiguration_200() throws Exception {
        mockMvc.perform(get(CONFIG_STATUTS).with(avecRole(compte(), ROLE_DG))).andExpect(status().isOk());
        mockMvc.perform(get(CONFIG_TRANSITIONS).with(avecRole(compte(), ROLE_DG))).andExpect(status().isOk());
        mockMvc.perform(get(CHAMPS_OBLIGATOIRES).with(avecRole(compte(), ROLE_DG))).andExpect(status().isOk());
    }

    /** Témoin du principe FixtureFaisabiliteWorkflow : un compte « Administrateur » passe la configuration. */
    @Test
    void r5_administrateur_litLaConfiguration_200() throws Exception {
        mockMvc.perform(get(CONFIG_STATUTS).with(avecRole(compte(), FixtureFaisabiliteWorkflow.ROLE_ADMINISTRATEUR)))
                .andExpect(status().isOk());
    }
}
