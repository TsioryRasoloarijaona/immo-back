package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.domain.PorteeCleApi;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.app.AppApiClient;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.app.AppApiClientRepository;
import com.tsix.immocrm.security.ApiKeyAuthenticationFilter;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static com.tsix.immocrm.fixtures.FixturePatrimoine.connecte;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.json;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FEATURE-patrimoine-ecriture.md §7 : toute écriture patrimoine exige le rôle Administrateur. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PatrimoineSecuriteTest {

    private static final String ID = UUID.randomUUID().toString();
    private static final String PROJETS = "/api/projets";
    private static final String ARTICLE = "/api/articles/" + ID;
    private static final String SOUS_ARTICLE = ARTICLE + "/sous-articles/" + UUID.randomUUID();
    private static final String ZONES = PROJETS + "/" + ID + "/zones";
    private static final String ENTITES = "/api/entites-juridiques";
    private static final String ACTIONNAIRES = "/api/actionnaires";
    private static final String GROUPES = "/api/groupes-actionnaires";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private CleApiService cleApiService;
    @Autowired private AppApiClientRepository clientRepository;

    static Stream<Arguments> ecritures() {
        return Stream.of(
                Arguments.of(HttpMethod.POST, PROJETS),
                Arguments.of(HttpMethod.PATCH, PROJETS + "/" + ID),
                Arguments.of(HttpMethod.POST, "/api/tranches"),
                Arguments.of(HttpMethod.PATCH, "/api/tranches/" + ID),
                Arguments.of(HttpMethod.POST, "/api/articles"),
                Arguments.of(HttpMethod.PATCH, ARTICLE),
                Arguments.of(HttpMethod.POST, ARTICLE + "/sous-articles"),
                Arguments.of(HttpMethod.POST, SOUS_ARTICLE + "/affecter"),
                Arguments.of(HttpMethod.POST, SOUS_ARTICLE + "/desaffecter"),
                Arguments.of(HttpMethod.DELETE, PROJETS + "/" + ID),
                Arguments.of(HttpMethod.POST, ZONES),
                Arguments.of(HttpMethod.PATCH, ZONES + "/" + UUID.randomUUID()),
                Arguments.of(HttpMethod.DELETE, ZONES + "/" + UUID.randomUUID()),
                // FEATURE-entites-juridiques.md §2 : entités, actionnaires, groupes.
                Arguments.of(HttpMethod.POST, ENTITES),
                Arguments.of(HttpMethod.PATCH, ENTITES + "/" + ID),
                Arguments.of(HttpMethod.DELETE, ENTITES + "/" + ID),
                Arguments.of(HttpMethod.POST, ENTITES + "/" + ID + "/repartition/depuis-groupe/" + UUID.randomUUID()),
                Arguments.of(HttpMethod.POST, ACTIONNAIRES),
                Arguments.of(HttpMethod.PATCH, ACTIONNAIRES + "/" + ID),
                Arguments.of(HttpMethod.DELETE, ACTIONNAIRES + "/" + ID),
                Arguments.of(HttpMethod.POST, GROUPES),
                Arguments.of(HttpMethod.PATCH, GROUPES + "/" + ID),
                Arguments.of(HttpMethod.DELETE, GROUPES + "/" + ID));
    }

    @Test
    void commercial_403_surRemplacementDeRepartition() throws Exception {
        AdmUser commercial = fixture.utilisateur("COM-SECU-REP");
        mockMvc.perform(request(HttpMethod.PUT, ENTITES + "/" + ID + "/repartition")
                        .with(connecte(commercial, "Commercial")).contentType(MediaType.APPLICATION_JSON).content("[]"))
                .andExpect(status().isForbidden());
    }

    /** Hors {@code PorteeCleApi} : même une clé à toutes les portées ne lit ni entités, ni actionnaires, ni groupes. */
    @ParameterizedTest(name = "GET {0}")
    @MethodSource("lecturesHorsPortee")
    void cleApi_403_lecturesP5HorsPortee(String url) throws Exception {
        String cle = cleToutesPortees("CLE-SECU-P5");

        mockMvc.perform(request(HttpMethod.GET, url).header(ApiKeyAuthenticationFilter.EN_TETE, cle))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ApiKeyAuthenticationFilter.CODE_HORS_PORTEE));
    }

    static Stream<String> lecturesHorsPortee() {
        return Stream.of(ENTITES, ENTITES + "/" + ID, ACTIONNAIRES, GROUPES);
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("ecritures")
    void commercial_403_surChaqueEcriture(HttpMethod methode, String url) throws Exception {
        AdmUser commercial = fixture.utilisateur("COM-SECU-PAT");
        mockMvc.perform(request(methode, url).with(connecte(commercial, "Commercial"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    private String cleToutesPortees(String codeCompte) {
        AdmUser compte = fixture.utilisateur(codeCompte);
        String cle = cleApiService.genererCle();
        AppApiClient client = new AppApiClient();
        client.setId(UUID.randomUUID());
        client.setNom("cle-patrimoine-" + UUID.randomUUID().toString().substring(0, 8));
        client.setCleHash(CleApiService.hacher(cle));
        client.setClePrefixe(CleApiService.prefixeAffiche(cle));
        client.setAdmUser(compte);
        client.setPortees(PorteeCleApi.versColonne(EnumSet.allOf(PorteeCleApi.class)));
        client.setActif(true);
        client.setUserCreateDateTime(LocalDateTime.now());
        clientRepository.save(client);
        return cle;
    }

    @Test
    void cleApi_403_horsPortee() throws Exception {
        String cle = cleToutesPortees("CLE-SECU-PAT");

        mockMvc.perform(request(HttpMethod.POST, PROJETS).header(ApiKeyAuthenticationFilter.EN_TETE, cle)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ApiKeyAuthenticationFilter.CODE_HORS_PORTEE));
    }

    @Test
    void administrateur_201() throws Exception {
        AdmUser admin = fixture.utilisateur("ADM-SECU-PAT");
        Map<String, Object> corps = Map.of("entiteId", fixture.entite("ENT-SECU-PAT", true).getId(),
                "code", "PRJ-SECU-PAT", "description", "Programme sécurité");
        mockMvc.perform(request(HttpMethod.POST, PROJETS).with(connecte(admin, FixturePatrimoine.ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(json(corps)))
                .andExpect(status().isCreated());
    }
}
