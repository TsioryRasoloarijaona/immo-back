package com.tsix.immocrm.controller.administration;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureFichiersAttaches;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureFichiersAttaches.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureFichiersAttaches.GROUPE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.jeton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Sondes de revue de FEATURE-admin-fichiers-attaches.md : bornes, échappement, droits des routes de rapports. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FichiersAttachesRevueTest {

    private static final String URL = "/api/administration/fichiers-attaches";
    private static final LocalDateTime DATE = LocalDateTime.of(2026, 3, 10, 9, 15, 30);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureFichiersAttaches fixture;

    private AdmUser admin;
    private String jeton;

    @BeforeEach
    void setUp() {
        admin = patrimoine.administrateur();
        jeton = "FR" + jeton();
    }

    @ParameterizedTest
    @ValueSource(strings = {"?categories=INCONNU", "?avecFichier=peutetre", "?modifieAu=pas-une-date"})
    void parametreIllisible_400(String requete) throws Exception {
        mockMvc.perform(get(URL + requete).with(connecte(admin))).andExpect(status().isBadRequest());
    }

    @Test
    void recherche_pourcentPrisLitteralement() throws Exception {
        UUID retenue = fixture.piece(jeton, DATE).avec(DESCRIPTION, jeton + "%x").inserer();
        fixture.piece(jeton, DATE).avec(DESCRIPTION, jeton + "ax").inserer();

        String corps = mockMvc.perform(get(URL).param("recherche", jeton + "%").with(connecte(admin)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(JsonPath.<List<String>>read(corps, "$.content[*].id")).containsExactly(retenue.toString());
    }

    @Test
    void groupe_espacesDeBordEnBase_retenuParLaValeurNettoyee() throws Exception {
        UUID retenue = fixture.piece(jeton, DATE).avec(GROUPE, "  G-" + jeton + "  ").inserer();

        String corps = mockMvc.perform(get(URL).param("recherche", jeton).param("groupes", "G-" + jeton)
                        .with(connecte(admin)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(JsonPath.<List<String>>read(corps, "$.content[*].id")).containsExactly(retenue.toString());
        assertThat(JsonPath.<List<String>>read(corps, "$.content[*].groupe")).containsExactly("G-" + jeton);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/rapports/administration/AdmX", "/api/rapports/administration/AdmX/resultat",
            "/api/rapports/administration/AdmX/export?format=xlsx", "/api/rapports/administration/AdmX/formulaires"})
    void rapportsAdministration_routesImbriquees_403SansAdministrateur(String url) throws Exception {
        mockMvc.perform(get(url).with(FixtureDossierVente.connecte(admin, "Commercial")))
                .andExpect(status().isForbidden());
    }

    @Test
    void rapportsAdministration_rapportNonImplemente_404PourLAdministrateur() throws Exception {
        mockMvc.perform(get("/api/rapports/administration/AdmInexistant" + jeton).with(connecte(admin)))
                .andExpect(status().isNotFound());
    }
}
