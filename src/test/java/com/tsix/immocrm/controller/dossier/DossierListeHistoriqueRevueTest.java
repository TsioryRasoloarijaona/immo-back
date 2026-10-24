package com.tsix.immocrm.controller.dossier;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static com.tsix.immocrm.controller.dossier.HistoriqueVentesSupport.LISTE;
import static com.tsix.immocrm.controller.dossier.HistoriqueVentesSupport.SESSION_DU_TEST;
import static com.tsix.immocrm.controller.dossier.HistoriqueVentesSupport.requete;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Revue du lot H : le 400 de la fourchette de total est le même sur les cinq routes, lots libres compris. */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser
class DossierListeHistoriqueRevueTest {

    private static final String TOTAL_MIN = "totalMin";
    private static final String TOTAL_MAX = "totalMax";

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @ValueSource(strings = {"", "/groupes", "/totaux", "/totaux-colonnes", "/export"})
    void fourchetteInversee_400_nommeLesChamps_surLesCinqRoutes(String suffixe) throws Exception {
        mockMvc.perform(requete(LISTE + suffixe, SESSION_DU_TEST,
                        TOTAL_MIN, "500", TOTAL_MAX, "100", "groupBy", "statut", "lignes", "DOSSIERS_ET_LIBRES"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString(TOTAL_MIN)))
                .andExpect(jsonPath("$.detail", containsString(TOTAL_MAX)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/groupes", "/totaux", "/totaux-colonnes", "/export"})
    void totalMaxNegatif_400_surLesCinqRoutes(String suffixe) throws Exception {
        mockMvc.perform(requete(LISTE + suffixe, SESSION_DU_TEST, TOTAL_MAX, "-0.01", "groupBy", "statut"))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/totaux", "/totaux-colonnes"})
    void fourchetteInversee_400_memeSurLesSeulsLotsLibres(String suffixe) throws Exception {
        mockMvc.perform(requete(LISTE + suffixe, SESSION_DU_TEST,
                        TOTAL_MIN, "500", TOTAL_MAX, "100", "lignes", "LIBRES"))
                .andExpect(status().isBadRequest());
    }
}
