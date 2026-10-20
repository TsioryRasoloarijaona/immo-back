package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.domain.PorteeCleApi;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.app.AppApiClient;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgTranche;
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

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FEATURE-patrimoine-ecriture.md §7 et §11 (PatrimoineSecuriteTest) : écritures réservées à l'Administrateur. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PatrimoineSecuriteTest {

    private static final UUID ID = UUID.fromString("0D3C5E7A-1B2C-4D5E-8F90-A1B2C3D4E5F6");
    private static final UUID SOUS_ID = UUID.fromString("1E4D6F8B-2C3D-4E5F-9A01-B2C3D4E5F6A7");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private AppApiClientRepository clientRepository;
    @Autowired private CleApiService cleApiService;

    /** §7 : chaque endpoint d'écriture des §3.1, §4.1, §5.1. */
    static Stream<Arguments> endpointsDEcriture() {
        return Stream.of(
                Arguments.of(HttpMethod.POST, "/api/projets"),
                Arguments.of(HttpMethod.PATCH, "/api/projets/" + ID),
                Arguments.of(HttpMethod.POST, "/api/tranches"),
                Arguments.of(HttpMethod.PATCH, "/api/tranches/" + ID),
                Arguments.of(HttpMethod.POST, "/api/articles"),
                Arguments.of(HttpMethod.PATCH, "/api/articles/" + ID),
                Arguments.of(HttpMethod.POST, "/api/articles/" + ID + "/sous-articles"),
                Arguments.of(HttpMethod.POST, "/api/articles/" + ID + "/sous-articles/" + SOUS_ID + "/affecter"),
                Arguments.of(HttpMethod.POST, "/api/articles/" + ID + "/sous-articles/" + SOUS_ID + "/desaffecter"),
                // FEATURE-patrimoine-article-fiche.md §2, §3 : écriture Administrateur.
                Arguments.of(HttpMethod.DELETE, "/api/articles/" + ID),
                Arguments.of(HttpMethod.PUT, "/api/articles/" + ID + "/contenances"),
                // FEATURE-patrimoine-tranche-regles.md §1-§4 : écritures Administrateur du lot P1c.
                Arguments.of(HttpMethod.PUT, "/api/tranches/" + ID + "/finalisations"),
                Arguments.of(HttpMethod.PUT, "/api/tranches/" + ID + "/commissions"),
                Arguments.of(HttpMethod.PUT, "/api/tranches/" + ID + "/previsions-base"),
                Arguments.of(HttpMethod.POST, "/api/tranches/" + ID + "/mettre-a-jour-schemas"),
                // FEATURE-patrimoine-article-onglets.md §4 : pièces jointes (les POST multipart, en JSON ici → 415,
                // sont couverts par PiecesJointesEcritureTest et AttachementMasseTest).
                Arguments.of(HttpMethod.DELETE, "/api/articles/" + ID + "/pieces-jointes/" + SOUS_ID),
                Arguments.of(HttpMethod.DELETE, "/api/tranches/" + ID + "/pieces-jointes/" + SOUS_ID),
                Arguments.of(HttpMethod.DELETE, "/api/projets/" + ID + "/pieces-jointes/" + SOUS_ID),
                Arguments.of(HttpMethod.GET, "/api/articles/groupes-piece-jointe"),
                // FEATURE-patrimoine-terrains.md §0, §1.2, §2, §3 : écritures Administrateur du lot P7a.
                Arguments.of(HttpMethod.POST, "/api/terrains"),
                Arguments.of(HttpMethod.PATCH, "/api/terrains/" + ID),
                Arguments.of(HttpMethod.DELETE, "/api/terrains/" + ID),
                Arguments.of(HttpMethod.POST, "/api/terrains/" + ID + "/sous-terrains"),
                Arguments.of(HttpMethod.PUT, "/api/terrains/" + ID + "/frais"),
                Arguments.of(HttpMethod.POST, "/api/terrains/" + ID + "/frais/" + SOUS_ID + "/valider"));
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
    @MethodSource("endpointsDEcriture")
    void r7_commercial_403_surChaqueEndpointDEcriture(HttpMethod methode, String url) throws Exception {
        mockMvc.perform(request(methode, url).with(commercial(compte("commercial.patrimoine")))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("endpointsDEcriture")
    void r7_cleApi_403_horsPortee(HttpMethod methode, String url) throws Exception {
        String cle = cleApiService.genererCle();
        AppApiClient client = new AppApiClient();
        client.setId(UUID.randomUUID());
        client.setNom("cle-patrimoine-" + UUID.randomUUID().toString().substring(0, 8));
        client.setCleHash(CleApiService.hacher(cle));
        client.setClePrefixe(CleApiService.prefixeAffiche(cle));
        client.setAdmUser(compte("service.patrimoine"));
        client.setPortees(PorteeCleApi.versColonne(EnumSet.allOf(PorteeCleApi.class)));
        client.setActif(true);
        client.setUserCreateDateTime(LocalDateTime.now());
        clientRepository.save(client);

        mockMvc.perform(request(methode, url).header(ApiKeyAuthenticationFilter.EN_TETE, cle)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ApiKeyAuthenticationFilter.CODE_HORS_PORTEE));
    }

    @Test
    void r7_administrateur_creeUnProgramme_201() throws Exception {
        CfgCompany entite = fixture.entite("ENT-SEC-PAT", true);

        mockMvc.perform(post("/api/projets").with(connecte(compte("admin.patrimoine")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps().avec("entiteId", entite.getId()).avec("code", "PRJ-SEC-PAT")
                                .avec("description", "Programme securite").json()))
                .andExpect(status().isCreated());
    }

    /** §7 : la liste des sous-articles libres reste ouverte à tout utilisateur authentifié. */
    @Test
    void r7_sousArticlesLibres_ouvertAuCommercial_200() throws Exception {
        CfgTranche tranche = fixture.tranche("TR-SEC-PAT", fixture.projet("PRJ-SEC-PAT-2",
                fixture.entite("ENT-SEC-PAT-2", true)));

        mockMvc.perform(get("/api/tranches/{id}/sous-articles-libres", tranche.getId())
                        .with(commercial(compte("commercial.libres"))))
                .andExpect(status().isOk());
    }
}
