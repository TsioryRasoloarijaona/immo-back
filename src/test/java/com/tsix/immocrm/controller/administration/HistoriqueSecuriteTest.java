package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.fixtures.FixtureDossierVente;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Stream;

import static com.tsix.immocrm.controller.administration.AppelsHistorique.ACTION;
import static com.tsix.immocrm.controller.administration.AppelsHistorique.DATE_DEBUT;
import static com.tsix.immocrm.controller.administration.AppelsHistorique.EXPORT;
import static com.tsix.immocrm.controller.administration.AppelsHistorique.LISTE;
import static com.tsix.immocrm.controller.administration.AppelsHistorique.SIZE;
import static com.tsix.immocrm.controller.administration.AppelsHistorique.UTILISATEURS;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Refus d'accès aux trois routes : règles 7 et 10. FEATURE-admin-historique.md. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class HistoriqueSecuriteTest {

    private static final String COMMERCIAL = "Commercial";
    private static final String DIRECTION = "Direction Générale";
    private static final String PAS_UNE_DATE = "pas-une-date";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDossierVente dossiers;

    static Stream<Arguments> routesEtRoles() {
        return Stream.of(LISTE, UTILISATEURS, EXPORT).flatMap(route -> Stream.of(
                Arguments.of(route, COMMERCIAL),
                Arguments.of(route, DIRECTION)));
    }

    // Un paramètre typé répond 400 avant @PreAuthorize : le refus doit passer avant la validation.
    static Stream<Arguments> parametresInvalides() {
        return Stream.of(
                Arguments.of(LISTE, ACTION, "Viewed"),
                Arguments.of(LISTE, SIZE, "0"),
                Arguments.of(LISTE, DATE_DEBUT, PAS_UNE_DATE),
                Arguments.of(UTILISATEURS, DATE_DEBUT, PAS_UNE_DATE),
                Arguments.of(EXPORT, ACTION, "Viewed"),
                Arguments.of(EXPORT, DATE_DEBUT, PAS_UNE_DATE));
    }

    private AppelsHistorique appelsAvec(String role) {
        return new AppelsHistorique(mockMvc, connecte(dossiers.compte("ROLE-HIST", role), role));
    }

    @ParameterizedTest
    @MethodSource("routesEtRoles")
    void r7_r10_sansRoleAdministrateur_403(String route, String role) throws Exception {
        appelsAvec(role).appeler(route, Map.of()).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @MethodSource("parametresInvalides")
    void r7_r10_sansRoleAdministrateur_parametreInvalide_403(String route, String nom, String valeur) throws Exception {
        appelsAvec(COMMERCIAL).appeler(route, Map.of(nom, valeur)).andExpect(status().isForbidden());
    }
}
