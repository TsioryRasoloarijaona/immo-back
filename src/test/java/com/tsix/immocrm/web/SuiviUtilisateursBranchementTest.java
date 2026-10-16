package com.tsix.immocrm.web;

import com.tsix.immocrm.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** L'intercepteur est branché sur la vraie chaîne et Prometheus expose le modèle de route, jamais l'id. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SuiviUtilisateursBranchementTest {

    private static final String ID = "0b1d7c2e-5a3f-4d11-9c00-00000000abcd";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void requeteConnectee_apparaitDansPrometheusAvecLeModeleDeRoute() throws Exception {
        AuthenticatedUser principal = new AuthenticatedUser(null, "suivi-test", null, false, true, 0);
        mockMvc.perform(get("/api/tranches/" + ID)
                .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()))));

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("arpent_utilisateur_requetes_total{"),
                        containsString("uri=\"/api/tranches/{id}\""),
                        containsString("utilisateur=\"suivi-test\""),
                        containsString("arpent_utilisateur_derniere_activite_secondes{"),
                        not(containsString(ID)))));
    }
}
