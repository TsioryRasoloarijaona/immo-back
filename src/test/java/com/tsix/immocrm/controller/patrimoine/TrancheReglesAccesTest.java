package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
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

import com.tsix.immocrm.security.AuthenticatedUser;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Accès du lot P1c — FEATURE-patrimoine-tranche-regles.md §0 (écriture Administrateur), §5 (lecture seule pour un
 * non-admin), convention P1b §8.6 (404 tranche inconnue ou d'une entité inactive, {@code trancheVisible}).
 * Le 403 des écritures (Commercial, clé d'API) est dans PatrimoineSecuriteTest.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TrancheReglesAccesTest {

    private static final String RESSOURCE = "/api/tranches/{id}/{ressource}";
    private static final String LISTE_VIDE = "{\"lignes\":[]}";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;

    private AdmUser admin;
    private CfgTranche visible;
    private CfgTranche inactive;

    @BeforeEach
    void setUp() {
        admin = fixture.administrateur();
        String jeton = "P1CA" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        visible = fixture.tranche("TR-" + jeton, fixture.projet("PRJ-" + jeton, fixture.entite("ENT-" + jeton, true)));
        inactive = fixture.tranche("TR-I-" + jeton,
                fixture.projet("PRJ-I-" + jeton, fixture.entite("ENT-I-" + jeton, false)));
    }

    /** Concaténé : une variable d'URI contenant « / » serait encodée. */
    private static String url(UUID trancheId, String ressource) {
        return "/api/tranches/" + trancheId + "/" + ressource;
    }

    private RequestPostProcessor commercial() {
        AdmUser user = fixture.administrateur();
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getCode(), null, false, true, 0);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_Commercial"))));
    }

    // ------------------------------------------------------------------ §5 lecture ouverte

    @ParameterizedTest(name = "GET {0}")
    @ValueSource(strings = {"finalisations", "commissions", "previsions-base", "previsions-base/stock"})
    void r5_lecture_ouverteAuCommercial_200(String ressource) throws Exception {
        mockMvc.perform(get(url(visible.getId(), ressource)).with(commercial()))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ 404 tranche inconnue / entité inactive

    @ParameterizedTest(name = "GET {0}")
    @ValueSource(strings = {"finalisations", "commissions", "previsions-base", "previsions-base/stock",
            "mettre-a-jour-schemas/apercu"})
    void r8_6_get_trancheInconnue_404(String ressource) throws Exception {
        mockMvc.perform(get(url(UUID.randomUUID(), ressource)).with(connecte(admin)))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest(name = "GET {0}")
    @ValueSource(strings = {"finalisations", "commissions", "previsions-base", "previsions-base/stock",
            "mettre-a-jour-schemas/apercu"})
    void r8_6_get_trancheDUneEntiteInactive_404(String ressource) throws Exception {
        mockMvc.perform(get(url(inactive.getId(), ressource)).with(connecte(admin)))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest(name = "PUT {0}")
    @ValueSource(strings = {"finalisations", "commissions"})
    void r8_6_put_trancheDUneEntiteInactive_404(String ressource) throws Exception {
        mockMvc.perform(put(RESSOURCE, inactive.getId(), ressource).with(connecte(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(LISTE_VIDE))
                .andExpect(status().isNotFound());
    }

    @Test
    void r8_6_putPrevisions_trancheDUneEntiteInactive_404() throws Exception {
        mockMvc.perform(put(RESSOURCE, inactive.getId(), "previsions-base").with(connecte(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombreArticles\":0,\"lignes\":[]}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void r8_6_miseAJourSchemas_trancheDUneEntiteInactive_404() throws Exception {
        mockMvc.perform(post(RESSOURCE, inactive.getId(), "mettre-a-jour-schemas").with(connecte(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    void r8_6_miseAJourSchemas_trancheInconnue_404() throws Exception {
        mockMvc.perform(post(RESSOURCE, UUID.randomUUID(), "mettre-a-jour-schemas").with(connecte(admin)))
                .andExpect(status().isNotFound());
    }
}
