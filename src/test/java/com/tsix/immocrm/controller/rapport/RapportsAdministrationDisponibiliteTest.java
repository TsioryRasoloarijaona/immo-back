package com.tsix.immocrm.controller.rapport;

import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.fixtures.FixtureRapports;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.liste;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Réponses T8 et T9 de FEATURE-admin-fichiers-attaches.md : rapports d'administration. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RapportsAdministrationDisponibiliteTest {

    private static final String ADMINISTRATION = "administration";
    private static final String TYPE_ADMINISTRATION = "Yes.Adm.Front.Tdb.Reports";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports fixture;

    private String prefixe;
    private AdmRole libre;

    @BeforeEach
    void setUp() {
        fixture.tables();
        prefixe = "RADD" + UUID.randomUUID().toString().substring(0, 6);
        libre = fixture.role("Libre " + prefixe, false);
    }

    @Test
    void t9_rapportNonImplemente_listeEtNonDisponible() throws Exception {
        String code = "AdmVerif" + prefixe;
        fixture.rapport(TYPE_ADMINISTRATION, code, "VERIFICATION " + prefixe, "VERIFICATION " + prefixe, 1);
        fixture.relire();

        mockMvc.perform(get(liste(ADMINISTRATION)).with(connecte(fixture.compte("a." + prefixe, libre),
                        "Administrateur")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nom == 'VERIFICATION " + prefixe + "')].rapports[*].code", contains(code)))
                .andExpect(jsonPath("$[?(@.nom == 'VERIFICATION " + prefixe + "')].rapports[*].disponible",
                        contains(false)));
    }

    // Les autres modules ne listent que les rapports implémentés : tous disponibles.
    @Test
    void t9_moduleVente_rapportsDisponibles() throws Exception {
        fixture.rapportVente(FixtureRapports.SUIVI_GLOBAL, "SUIVI " + prefixe, "G " + prefixe, 1);
        fixture.relire();

        mockMvc.perform(get(liste("vente")).with(connecte(fixture.compte("v." + prefixe, libre), "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nom == 'G " + prefixe + "')].rapports[*].disponible", hasItem(true)));
    }

    @Test
    void t8_moduleAdministration_sansRoleAdministrateur_403() throws Exception {
        fixture.relire();

        mockMvc.perform(get(liste(ADMINISTRATION)).with(connecte(fixture.compte("c." + prefixe, libre),
                        "Commercial")))
                .andExpect(status().isForbidden());
    }
}
