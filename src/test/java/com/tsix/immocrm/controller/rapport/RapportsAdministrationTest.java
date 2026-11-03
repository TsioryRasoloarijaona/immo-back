package com.tsix.immocrm.controller.rapport;

import com.tsix.immocrm.fixtures.FixtureRapports;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.liste;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Rapports d'administration — FEATURE-admin-fichiers-attaches.md, réponse R3 : {@code ModuleRapport.ADMINISTRATION}
 * ({@code AdmReportData.Type = Yes.Adm.Front.Tdb.Reports}) servi par le socle des rapports sous le slug du module.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RapportsAdministrationTest {

    private static final String ADMINISTRATION = "administration";
    private static final String TYPE_ADMINISTRATION = "Yes.Adm.Front.Tdb.Reports";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports fixture;

    private RequestPostProcessor administrateur;

    @BeforeEach
    void setUp() {
        fixture.tables();
        String prefixe = "RADM" + UUID.randomUUID().toString().substring(0, 6);
        fixture.rapport(TYPE_ADMINISTRATION, "AdmUserRole", "UTILISATEURS-ROLES", "HABILITATIONS", 10);
        administrateur = connecte(fixture.compte("admin." + prefixe, fixture.role("Libre " + prefixe, false)),
                "Administrateur");
        fixture.relire();
    }

    // R3 : le module existe (un slug inconnu répond 404 aujourd'hui).
    @Test
    void r3_moduleAdministration_listeServie_200() throws Exception {
        mockMvc.perform(get(liste(ADMINISTRATION)).with(administrateur))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // R3 : un rapport de vente n'apparaît pas sous le module administration.
    @Test
    void r3_moduleAdministration_neListeAucunRapportDeVente() throws Exception {
        fixture.rapportVente(FixtureRapports.SUIVI_GLOBAL, "SUIVI GLOBAL", "VENTE", 1);

        mockMvc.perform(get(liste(ADMINISTRATION)).with(administrateur))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].rapports[*].code", not(hasItem(FixtureRapports.SUIVI_GLOBAL))));
    }
}
