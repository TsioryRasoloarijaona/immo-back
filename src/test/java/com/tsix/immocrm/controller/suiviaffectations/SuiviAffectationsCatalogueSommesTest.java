package com.tsix.immocrm.controller.suiviaffectations;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Une échéance reçoit plusieurs lignes de règlement : son montant ne se somme pas (FEATURE-suivi-affectations.md). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class SuiviAffectationsCatalogueSommesTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void catalogue_montantEcheance_nonAgregeable() throws Exception {
        mockMvc.perform(get("/api/referentiels/colonnes/suivi-affectations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nom=='montantEcheance')].agregeable").value(false))
                .andExpect(jsonPath("$[?(@.nom=='montantEcheance')].nom").exists());
    }

    @Test
    void totauxColonnes_sommeSurMontantEcheance_refusee() throws Exception {
        mockMvc.perform(get("/api/suivi-affectations/totaux-colonnes").param("agregats", "montantEcheance"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void groupes_sommeSurMontantEcheance_refusee() throws Exception {
        mockMvc.perform(get("/api/suivi-affectations/groupes").param("groupBy", "sens")
                        .param("agregats", "montantEcheance"))
                .andExpect(status().isBadRequest());
    }
}
