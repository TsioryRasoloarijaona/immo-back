package com.tsix.immocrm.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class CorsConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiEndpoint_echoesLocalhostOriginInCorsHeader() throws Exception {
        mockMvc.perform(get("/api/projets").header(HttpHeaders.ORIGIN, "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    /**
     * L'origine de production est listée explicitement : {@code allowCredentials(true)} interdit
     * le joker {@code *}, donc rien d'autre que cette liste ne passera le contrôle du navigateur.
     * Ce test échoue si quelqu'un retire le domaine déployé de {@link CorsConfig}.
     */
    @Test
    void apiEndpoint_echoesDeployedFrontendOriginInCorsHeader() throws Exception {
        mockMvc.perform(get("/api/projets").header(HttpHeaders.ORIGIN, "https://immo.tsiory.tech"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://immo.tsiory.tech"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    /** Le schéma fait partie de l'origine : la variante en clair n'est pas autorisée. */
    @Test
    void apiEndpoint_rejectsDeployedFrontendOriginOverPlainHttp() throws Exception {
        mockMvc.perform(get("/api/projets").header(HttpHeaders.ORIGIN, "http://immo.tsiory.tech"))
                .andExpect(status().isForbidden());
    }

    @Test
    void preflight_allowsPatchFromLocalhostOrigin() throws Exception {
        mockMvc.perform(options("/api/dossiers/{id}/statut", "00000000-0000-0000-0000-000000000000")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PATCH"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"));
    }
}
