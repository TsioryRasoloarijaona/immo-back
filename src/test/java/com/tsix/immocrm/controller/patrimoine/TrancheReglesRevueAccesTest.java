package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Revue P1c — FEATURE-patrimoine-tranche-regles.md §11.5 : aperçu et « Générer » réservés à l'Administrateur (non
 * couverts par PatrimoineSecuriteTest) ; « Générer » sur une tranche d'une entité inactive → 404.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TrancheReglesRevueAccesTest {

    private static final String ENTETE = "{\"nombreArticles\":2,\"debutConcretisation\":\"2026-01-01T00:00:00\","
            + "\"finConcretisation\":\"2026-03-01T00:00:00\",\"debutFinalisation\":\"2026-04-01T00:00:00\","
            + "\"finFinalisation\":\"2026-06-01T00:00:00\"}";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;

    private AdmUser admin;
    private CfgTranche visible;
    private CfgTranche inactive;

    @BeforeEach
    void setUp() {
        admin = fixture.administrateur();
        String jeton = "P1CR" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
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

    private static String generer(CfgTranche tranche) {
        return "/api/tranches/" + tranche.getId() + "/previsions-base/generer";
    }

    @Test
    void r11_5_apercuSchemas_commercial_403() throws Exception {
        mockMvc.perform(get("/api/tranches/" + visible.getId() + "/mettre-a-jour-schemas/apercu").with(commercial()))
                .andExpect(status().isForbidden());
    }

    @Test
    void r11_5_genererPrevisions_commercial_403() throws Exception {
        mockMvc.perform(post(generer(visible)).with(commercial())
                        .contentType(MediaType.APPLICATION_JSON).content(ENTETE))
                .andExpect(status().isForbidden());
    }

    @Test
    void r8_6_genererPrevisions_trancheDUneEntiteInactive_404() throws Exception {
        mockMvc.perform(post(generer(inactive)).with(connecte(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(ENTETE))
                .andExpect(status().isNotFound());
    }

    @Test
    void r11_4_genererPrevisions_admin_200() throws Exception {
        mockMvc.perform(post(generer(visible)).with(connecte(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(ENTETE))
                .andExpect(status().isOk());
    }
}
