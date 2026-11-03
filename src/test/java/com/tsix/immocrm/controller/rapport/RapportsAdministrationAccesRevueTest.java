package com.tsix.immocrm.controller.rapport;

import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.fixtures.FixtureRapports;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.UUID;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.descripteur;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.export;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.liste;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.resultat;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Revue (règle 6) : variantes de chemin du module administration, rapports non disponibles, autres modules. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RapportsAdministrationAccesRevueTest {

    private static final String ADMINISTRATION = "administration";
    private static final String TYPE_ADMINISTRATION = "Yes.Adm.Front.Tdb.Reports";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports fixture;

    private String code;
    private RequestPostProcessor commercial;
    private RequestPostProcessor administrateur;

    @BeforeEach
    void setUp() {
        fixture.tables();
        String prefixe = "RADR" + UUID.randomUUID().toString().substring(0, 6);
        code = "AdmVerif" + prefixe;
        fixture.rapport(TYPE_ADMINISTRATION, code, "VERIF " + prefixe, "VERIF " + prefixe, 1);
        AdmRole libre = fixture.role("Libre " + prefixe, false);
        commercial = connecte(fixture.compte("c." + prefixe, libre), "Commercial");
        administrateur = connecte(fixture.compte("a." + prefixe, libre), "Administrateur");
        fixture.relire();
    }

    @Test
    void nonAdmin_routesDuModule_403() throws Exception {
        for (String url : new String[]{liste(ADMINISTRATION) + "/", descripteur(ADMINISTRATION, code),
                resultat(ADMINISTRATION, code), export(ADMINISTRATION, code) + "?format=xlsx",
                descripteur(ADMINISTRATION, code) + "/formulaires"}) {
            mockMvc.perform(get(url).with(commercial)).andExpect(status().isForbidden());
        }
    }

    @Test
    void nonAdmin_slugEncode_403() throws Exception {
        mockMvc.perform(get(URI.create("/api/rapports/%61dministration")).with(commercial))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"vente", "patrimoine"})
    void nonAdmin_autresModules_200(String module) throws Exception {
        mockMvc.perform(get(liste(module)).with(commercial)).andExpect(status().isOk());
    }

    @Test
    void admin_rapportNonDisponible_nonExecutable_404() throws Exception {
        mockMvc.perform(get(descripteur(ADMINISTRATION, code)).with(administrateur)).andExpect(status().isNotFound());
        mockMvc.perform(get(resultat(ADMINISTRATION, code)).with(administrateur)).andExpect(status().isNotFound());
        mockMvc.perform(get(export(ADMINISTRATION, code) + "?format=xlsx").with(administrateur))
                .andExpect(status().isNotFound());
    }
}
