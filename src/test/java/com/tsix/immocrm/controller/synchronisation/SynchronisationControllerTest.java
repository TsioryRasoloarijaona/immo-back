package com.tsix.immocrm.controller.synchronisation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * L'endpoint de synchronisation manuelle, vu depuis la suite — qui tourne en <b>mono-base</b>.
 * Le service {@code SynchronisationDescendante} n'existe donc pas ici : le contrôleur reste
 * présent (injection par {@code ObjectProvider}) et doit répondre 409, pas planter au démarrage.
 * Le chemin nominal deux-bases se vérifie à la main (cf. FEATURE-source-de-verite-115.md).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SynchronisationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "Administrateur")
    void enMonoBaseLAdministrateurRecoit409() throws Exception {
        mockMvc.perform(post("/api/synchronisation"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser // rôle USER par défaut, pas Administrateur
    void sansLeRoleAdministrateurCEst403() throws Exception {
        mockMvc.perform(post("/api/synchronisation"))
                .andExpect(status().isForbidden());
    }
}
