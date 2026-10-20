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

import static com.tsix.immocrm.fixtures.FixtureFaisabilite.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Droits des études de faisabilité — FEATURE-patrimoine-faisabilite.md §2.7 et §8 bis (décision 4) : lecture réservée à
 * Administrateur + Direction Générale (coûts, marges, prix d'achat), écriture à l'Administrateur ; clés d'API hors portée.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EtudeFaisabiliteSecuriteTest {

    private static final UUID ID = UUID.fromString("0D3C5E7A-1B2C-4D5E-8F90-A1B2C3D4E5F6");
    private static final String DIRECTION_GENERALE = "Direction Générale";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureFaisabilite fixture;
    @Autowired private AppApiClientRepository clientRepository;
    @Autowired private CleApiService cleApiService;

    private String jeton;

    @BeforeEach
    void setUp() {
        fixture.parametrage();
        jeton = jeton();
    }

    /** §2.7 : écritures (Administrateur seul). */
    static Stream<Arguments> ecritures() {
        return Stream.of(
                Arguments.of(HttpMethod.POST, RACINE),
                Arguments.of(HttpMethod.PATCH, RACINE + "/" + ID),
                Arguments.of(HttpMethod.DELETE, RACINE + "/" + ID),
                Arguments.of(HttpMethod.PUT, RACINE + "/" + ID + "/unites"),
                Arguments.of(HttpMethod.PUT, RACINE + "/" + ID + "/tranches"),
                Arguments.of(HttpMethod.POST, RACINE + "/" + ID + "/bloquer"),
                Arguments.of(HttpMethod.POST, RACINE + "/" + ID + "/debloquer"));
    }

    /** §2.7 et §8 bis décision 4 : lectures (Administrateur + Direction Générale) ; l'aperçu de calcul est une lecture. */
    static Stream<Arguments> lectures() {
        return Stream.of(
                Arguments.of(HttpMethod.GET, RACINE),
                Arguments.of(HttpMethod.GET, RACINE + "/" + ID),
                Arguments.of(HttpMethod.POST, RACINE + "/calcul"),
                Arguments.of(HttpMethod.GET, RACINE + "/choix/terrains"),
                Arguments.of(HttpMethod.GET, RACINE + "/choix/tranches"));
    }

    static Stream<Arguments> tousLesEndpoints() {
        return Stream.concat(ecritures(), lectures());
    }

    /** Rôles de production autres qu'Administrateur et Direction Générale. */
    static Stream<String> autresRoles() {
        return Stream.of("Commercial", "Commercial Chef", "Commercial Aide", "Assistante Commerciale", "Comptabilité",
                "Direction fonciere", "Directeur Technique", "Assistante direction");
    }

    static Stream<Arguments> lecturesParAutreRole() {
        return lectures().flatMap(l -> autresRoles().map(r -> Arguments.of(r, l.get()[0], l.get()[1])));
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
    @MethodSource("lecturesParAutreRole")
    void d8bis4_lecture_403_pourToutRoleAutreQueAdministrateurEtDirectionGenerale(String role, HttpMethod methode,
                                                                                  String url) throws Exception {
        mockMvc.perform(request(methode, url).with(avecRole(compte("lecteur.faisabilite"), role))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("ecritures")
    void r2_7_ecriture_403_directionGenerale(HttpMethod methode, String url) throws Exception {
        mockMvc.perform(request(methode, url).with(avecRole(compte("dg.faisabilite"), DIRECTION_GENERALE))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("ecritures")
    void r2_7_ecriture_403_commercial(HttpMethod methode, String url) throws Exception {
        mockMvc.perform(request(methode, url).with(avecRole(compte("commercial.faisabilite"), "Commercial"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    /** §2.7 : {@code /api/etudes-faisabilite} n'est dans aucun préfixe de {@code PorteeCleApi} : 403 même avec toutes les portées. */
    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("tousLesEndpoints")
    void r2_7_cleApi_403_horsPortee(HttpMethod methode, String url) throws Exception {
        String cle = cleApiService.genererCle();
        AppApiClient client = new AppApiClient();
        client.setId(UUID.randomUUID());
        client.setNom("cle-faisabilite-" + UUID.randomUUID().toString().substring(0, 8));
        client.setCleHash(CleApiService.hacher(cle));
        client.setClePrefixe(CleApiService.prefixeAffiche(cle));
        client.setAdmUser(compte("service.faisabilite"));
        client.setPortees(PorteeCleApi.versColonne(EnumSet.allOf(PorteeCleApi.class)));
        client.setActif(true);
        client.setUserCreateDateTime(LocalDateTime.now());
        clientRepository.save(client);

        mockMvc.perform(request(methode, url).header(ApiKeyAuthenticationFilter.EN_TETE, cle)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ApiKeyAuthenticationFilter.CODE_HORS_PORTEE));
    }

    // ------------------------------------------------------------------ accès accordés

    @Test
    void d8bis4_directionGenerale_litLaListeEtLaFiche_200() throws Exception {
        CfgCompany societe = patrimoine.entite("ENT-" + jeton, true);
        UUID id = new ApiFaisabilite(mockMvc, compte("admin.faisabilite")).etude(fiche("SEC-" + jeton, societe.getId()));
        RequestPostProcessor dg = avecRole(compte("dg.lecture"), DIRECTION_GENERALE);

        mockMvc.perform(get(RACINE).with(dg).param("search", "SEC-" + jeton))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$..[?(@.id=='" + id + "')]").exists());
        mockMvc.perform(get(FICHE, id).with(dg))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SEC-" + jeton));
    }

    @Test
    void d8bis4_directionGenerale_apercuDeCalcul_200() throws Exception {
        CfgCompany societe = patrimoine.entite("ENT-" + jeton, true);

        mockMvc.perform(post(CALCUL).with(avecRole(compte("dg.calcul"), DIRECTION_GENERALE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(yestest(fiche("CAL-" + jeton, societe.getId())).json()))
                .andExpect(status().isOk());
    }

    @Test
    void r2_7_administrateur_cree_201() throws Exception {
        CfgCompany societe = patrimoine.entite("ENT-" + jeton, true);

        new ApiFaisabilite(mockMvc, compte("admin.creation")).creer(fiche("ADM-" + jeton, societe.getId()))
                .andExpect(status().isCreated());
    }
}
