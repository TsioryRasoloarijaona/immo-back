package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.domain.PorteeCleApi;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.app.AppApiClient;
import com.tsix.immocrm.fixtures.ApiProspectionsTerrain;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureProspectionsTerrain;
import com.tsix.immocrm.fixtures.FixtureProspectionsTerrain.Contexte;
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

import static com.tsix.immocrm.fixtures.ApiProspectionsTerrain.corpsProspection;
import static com.tsix.immocrm.fixtures.FixtureProspectionsTerrain.RACINE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Droits des prospections terrain — FEATURE-patrimoine-prospections-terrain.md §0 et §5 (ProspectionTerrainSecuriteTest,
 * classe neuve : PatrimoineSecuriteTest n'est pas étendu) : écriture Administrateur seul (403 sinon), lecture pour tout
 * compte connecté, routes hors de toute portée de clé d'API.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProspectionTerrainSecuriteTest {

    private static final UUID ID = UUID.fromString("5B6C7D8E-9FA0-4B1C-8D2E-3F4A5B6C7D8E");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureProspectionsTerrain fixture;
    @Autowired private AppApiClientRepository clientRepository;
    @Autowired private CleApiService cleApiService;

    /** §5 : écritures (Administrateur seul). */
    static Stream<Arguments> ecritures() {
        return Stream.of(
                Arguments.of(HttpMethod.POST, RACINE),
                Arguments.of(HttpMethod.PATCH, RACINE + "/" + ID),
                Arguments.of(HttpMethod.DELETE, RACINE + "/" + ID),
                Arguments.of(HttpMethod.PUT, RACINE + "/" + ID + "/zones"),
                Arguments.of(HttpMethod.POST, RACINE + "/" + ID + "/transition"),
                Arguments.of(HttpMethod.POST, RACINE + "/" + ID + "/transformer-en-terrain"));
    }

    /** §5 : lectures (tout compte connecté) ; l'aperçu de calcul est une lecture. */
    static Stream<Arguments> lectures() {
        return Stream.of(
                Arguments.of(HttpMethod.GET, RACINE),
                Arguments.of(HttpMethod.GET, RACINE + "/" + ID),
                Arguments.of(HttpMethod.POST, RACINE + "/apercu-calcul"),
                Arguments.of(HttpMethod.GET, RACINE + "/" + ID + "/transitions"),
                Arguments.of(HttpMethod.GET, RACINE + "/" + ID + "/historique"));
    }

    static Stream<Arguments> tousLesEndpoints() {
        return Stream.concat(ecritures(), lectures());
    }

    static Stream<String> autresRoles() {
        return Stream.of("Commercial", "Commercial Chef", "Direction Générale", "Direction fonciere", "Comptabilité");
    }

    static Stream<Arguments> ecrituresParAutreRole() {
        return ecritures().flatMap(e -> autresRoles().map(r -> Arguments.of(r, e.get()[0], e.get()[1])));
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

    // ------------------------------------------------------------------ refus

    @ParameterizedTest(name = "{0} {1} {2}")
    @MethodSource("ecrituresParAutreRole")
    void r0_ecriture_403_pourToutRoleAutreQuAdministrateur(String role, HttpMethod methode, String url) throws Exception {
        mockMvc.perform(request(methode, url).with(avecRole(compte("lecteur.prospection"), role))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    /** §5 : création refusée à un commercial avec un corps valide → 403, rien d'écrit. */
    @Test
    void r0_creation_commercial_403_rienEcrit() throws Exception {
        Contexte c = fixture.contexte("P7S");
        String code = "SEC-" + c.jeton();

        mockMvc.perform(post(RACINE).with(avecRole(compte("commercial.prospection"), "Commercial"))
                        .contentType(MediaType.APPLICATION_JSON).content(corpsProspection(c, code).json()))
                .andExpect(status().isForbidden());

        assertThat(fixture.prospectionsDeCode(code)).isZero();
    }

    /** §5 : la route n'est dans aucun préfixe de PorteeCleApi → 403 hors portée même avec toutes les portées. */
    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("tousLesEndpoints")
    void r5_cleApi_403_horsPortee(HttpMethod methode, String url) throws Exception {
        String cle = cleApiService.genererCle();
        AppApiClient client = new AppApiClient();
        client.setId(UUID.randomUUID());
        client.setNom("cle-prospection-" + UUID.randomUUID().toString().substring(0, 8));
        client.setCleHash(CleApiService.hacher(cle));
        client.setClePrefixe(CleApiService.prefixeAffiche(cle));
        client.setAdmUser(compte("service.prospection"));
        client.setPortees(PorteeCleApi.versColonne(EnumSet.allOf(PorteeCleApi.class)));
        client.setActif(true);
        client.setUserCreateDateTime(LocalDateTime.now());
        clientRepository.save(client);

        mockMvc.perform(request(methode, url).header(ApiKeyAuthenticationFilter.EN_TETE, cle)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ApiKeyAuthenticationFilter.CODE_HORS_PORTEE));
    }

    // ------------------------------------------------------------------ lectures accordées

    /** §0 : lecture pour tout compte connecté — un commercial lit la liste et la fiche. */
    @Test
    void r0_commercial_litLaListeEtLaFiche_200() throws Exception {
        Contexte c = fixture.contexte("P7L");
        UUID id = fixture.prospection(c, "LEC-" + c.jeton());
        RequestPostProcessor commercial = avecRole(compte("commercial.lecture"), "Commercial");

        mockMvc.perform(get(RACINE).with(commercial).param("search", c.jeton()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + id + "')]").isNotEmpty());
        mockMvc.perform(get(RACINE + "/{id}", id).with(commercial))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("LEC-" + c.jeton()));
    }

    /** §5.5 : l'aperçu de calcul est ouvert à tout compte connecté. */
    @Test
    void r5_5_commercial_apercuDeCalcul_200() throws Exception {
        fixture.configuration();

        mockMvc.perform(post(RACINE + "/apercu-calcul").with(avecRole(compte("commercial.calcul"), "Commercial"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"superficie\":1200}"))
                .andExpect(status().isOk());
    }

    /** §5.7 : un non-Administrateur voit peutTransformer faux, même au statut transformable. */
    @Test
    void r5_7_commercial_peutTransformerFaux() throws Exception {
        Contexte c = fixture.contexte("P7P");
        UUID statut = fixture.statut("TRF-" + c.jeton(), 250, FixtureProspectionsTerrain.TYPE_SUCCES,
                new FixtureProspectionsTerrain.Statut(false, true, true, 0));
        UUID id = fixture.prospection(c, "PTC-" + c.jeton(), statut);

        mockMvc.perform(get(RACINE + "/{id}", id).with(avecRole(compte("commercial.fiche"), "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.peutTransformer").value(false));
    }

    /** Témoin : l'Administrateur crée (201) avec le même corps. */
    @Test
    void r0_administrateur_cree_201() throws Exception {
        Contexte c = fixture.contexte("P7A");

        new ApiProspectionsTerrain(mockMvc, compte("admin.prospection")).creer(corpsProspection(c, "ADM-" + c.jeton()))
                .andExpect(status().isCreated());
    }
}
