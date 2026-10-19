package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.domain.PorteeCleApi;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.app.AppApiClient;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureTrancheFiche;
import com.tsix.immocrm.repositories.app.AppApiClientRepository;
import com.tsix.immocrm.security.ApiKeyAuthenticationFilter;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.security.CleApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Accès aux onglets et à la suppression de tranche — FEATURE-patrimoine-tranche-onglets.md §1, §4, §8.6 :
 * GET ouvert aux authentifiés, écriture Administrateur (clé d'API : 403 {@code HORS_PORTEE}), 404 tranche inconnue
 * ou d'une entité inactive ({@code TrancheVisibleRepository.trancheVisible}).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TrancheOngletsAccesTest {

    private static final String ONGLET = "/api/tranches/{id}/{onglet}";
    private static final String TRANCHE = "/api/tranches/{id}";
    private static final String LISTE_VIDE = "{\"lignes\":[]}";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureTrancheFiche fiche;
    @Autowired private AppApiClientRepository clientRepository;
    @Autowired private CleApiService cleApiService;

    private AdmUser admin;
    private CfgTranche visible;
    private CfgTranche inactive;
    private String jeton;

    @BeforeEach
    void setUp() {
        admin = fixture.administrateur();
        jeton = "P1BA" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        visible = fixture.tranche("TR-" + jeton, fixture.projet("PRJ-" + jeton, fixture.entite("ENT-" + jeton, true)));
        inactive = fixture.tranche("TR-I-" + jeton,
                fixture.projet("PRJ-I-" + jeton, fixture.entite("ENT-I-" + jeton, false)));
    }

    private RequestPostProcessor commercial() {
        AdmUser user = fixture.administrateur();
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getCode(), null, false, true, 0);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_Commercial"))));
    }

    private String cleApi() {
        String cle = cleApiService.genererCle();
        AppApiClient client = new AppApiClient();
        client.setId(UUID.randomUUID());
        client.setNom("cle-onglets-" + jeton);
        client.setCleHash(CleApiService.hacher(cle));
        client.setClePrefixe(CleApiService.prefixeAffiche(cle));
        client.setAdmUser(fixture.administrateur());
        client.setPortees(PorteeCleApi.versColonne(EnumSet.allOf(PorteeCleApi.class)));
        client.setActif(true);
        client.setUserCreateDateTime(LocalDateTime.now());
        clientRepository.save(client);
        return cle;
    }

    // ------------------------------------------------------------------ §1 lecture ouverte, écriture Administrateur

    @ParameterizedTest(name = "GET {0}")
    @ValueSource(strings = {"terrains", "etages", "plancher-couvert", "frais"})
    void r1_get_ouvertAuCommercial_200(String onglet) throws Exception {
        mockMvc.perform(get(ONGLET, visible.getId(), onglet).with(commercial()))
                .andExpect(status().isOk());
    }

    @ParameterizedTest(name = "PUT {0}")
    @ValueSource(strings = {"terrains", "etages", "plancher-couvert", "frais"})
    void r1_put_commercial_403(String onglet) throws Exception {
        mockMvc.perform(put(ONGLET, visible.getId(), onglet).with(commercial())
                        .contentType(MediaType.APPLICATION_JSON).content(LISTE_VIDE))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "PUT {0}")
    @ValueSource(strings = {"terrains", "etages", "plancher-couvert", "frais"})
    void r1_put_cleApi_403_horsPortee(String onglet) throws Exception {
        mockMvc.perform(put(ONGLET, visible.getId(), onglet).header(ApiKeyAuthenticationFilter.EN_TETE, cleApi())
                        .contentType(MediaType.APPLICATION_JSON).content(LISTE_VIDE))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ApiKeyAuthenticationFilter.CODE_HORS_PORTEE));
    }

    @ParameterizedTest(name = "GET {0}")
    @ValueSource(strings = {"/api/tranches/terrains-disponibles", "/api/tranches/categories-frais"})
    void r2_listesDeChoix_ouvertesAuCommercial_200(String url) throws Exception {
        mockMvc.perform(get(url).with(commercial())).andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ §1 404 tranche inconnue

    @ParameterizedTest(name = "GET {0}")
    @ValueSource(strings = {"terrains", "etages", "plancher-couvert", "frais"})
    void r1_get_trancheInconnue_404(String onglet) throws Exception {
        mockMvc.perform(get(ONGLET, UUID.randomUUID(), onglet).with(connecte(admin)))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest(name = "PUT {0}")
    @ValueSource(strings = {"terrains", "etages", "plancher-couvert", "frais"})
    void r1_put_trancheInconnue_404(String onglet) throws Exception {
        mockMvc.perform(put(ONGLET, UUID.randomUUID(), onglet).with(connecte(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(LISTE_VIDE))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ §8.6 404 entité inactive

    @ParameterizedTest(name = "GET {0}")
    @ValueSource(strings = {"terrains", "etages", "plancher-couvert", "frais"})
    void r8_6_get_trancheDUneEntiteInactive_404(String onglet) throws Exception {
        mockMvc.perform(get(ONGLET, inactive.getId(), onglet).with(connecte(admin)))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest(name = "PUT {0}")
    @ValueSource(strings = {"terrains", "etages", "plancher-couvert", "frais"})
    void r8_6_put_trancheDUneEntiteInactive_404(String onglet) throws Exception {
        mockMvc.perform(put(ONGLET, inactive.getId(), onglet).with(connecte(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(LISTE_VIDE))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ §4 suppression : refus

    @Test
    void r4_suppression_commercial_403_trancheGardee() throws Exception {
        mockMvc.perform(delete(TRANCHE, visible.getId()).with(commercial()))
                .andExpect(status().isForbidden());

        assertThat(fiche.compter("SELECT COUNT(*) FROM CfgTranche WHERE PKey = :id", visible.getId())).isEqualTo(1);
    }

    @Test
    void r4_suppression_cleApi_403_horsPortee() throws Exception {
        mockMvc.perform(delete(TRANCHE, visible.getId()).header(ApiKeyAuthenticationFilter.EN_TETE, cleApi()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ApiKeyAuthenticationFilter.CODE_HORS_PORTEE));
    }

    @Test
    void r8_6_suppression_trancheDUneEntiteInactive_404_trancheGardee() throws Exception {
        mockMvc.perform(delete(TRANCHE, inactive.getId()).with(connecte(admin)))
                .andExpect(status().isNotFound());

        assertThat(fiche.compter("SELECT COUNT(*) FROM CfgTranche WHERE PKey = :id", inactive.getId())).isEqualTo(1);
    }
}
