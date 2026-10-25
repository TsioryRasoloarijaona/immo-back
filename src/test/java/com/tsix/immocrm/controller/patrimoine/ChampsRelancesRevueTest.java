package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureExportPatrimoine;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Revue P9d, règle I1 (FEATURE-patrimoine-import-export.md §0.1 « 403 sinon, avant tout 400 ») : un non-Administrateur
 * doit recevoir 403 même quand le corps ou l'id du chemin ne se lie pas (JSON mal formé, UUID illisible). Le
 * {@code @PreAuthorize} de méthode s'exécute après la liaison des paramètres : sans règle d'URL (comme
 * {@code /api/import-patrimoine/**} dans SecurityConfig), ces requêtes répondent 400.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ChampsRelancesRevueTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;

    @ParameterizedTest(name = "{0} {1} corps={2}")
    @CsvSource(delimiter = '|', value = {
            "PUT|/api/champs-specifiques/projet/{id}|{\"valeurs\":[{\"champId\":\"pas-un-uuid\",\"valeur\":\"1\"}]}",
            "PUT|/api/champs-specifiques/projet/{id}|{",
            "PUT|/api/champs-specifiques/projet/pas-un-uuid|{\"valeurs\":[]}",
            "POST|/api/champs-specifiques/projet/definitions|{\"libelle\":\"X\",\"typeDonnee\":\"String\",\"ordre\":\"abc\"}",
            "PATCH|/api/champs-specifiques/projet/definitions/pas-un-uuid|{\"libelle\":\"X\",\"typeDonnee\":\"String\"}",
            "DELETE|/api/champs-specifiques/projet/definitions/pas-un-uuid|{}",
            "POST|/api/tranches/pas-un-uuid/premieres-relances|{}",
            "GET|/api/tranches/pas-un-uuid/premieres-relances/apercu|{}"
    })
    void i1_nonAdmin_403_avantToutLiaison400(String methode, String url, String corps) throws Exception {
        AdmUser compte = patrimoine.administrateur();

        mockMvc.perform(request(HttpMethod.valueOf(methode), url.replace("{id}", UUID.randomUUID().toString()))
                        .contentType(MediaType.APPLICATION_JSON).content(corps)
                        .with(FixtureExportPatrimoine.commercial(compte)))
                .andExpect(status().isForbidden());
    }
}
