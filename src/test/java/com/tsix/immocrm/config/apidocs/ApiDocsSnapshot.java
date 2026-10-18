package com.tsix.immocrm.config.apidocs;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Écrit le document OpenAPI sans démarrer le serveur : lancé par {@code .claude/scripts/snapshot-api.mjs}, qui le met
 * en forme dans {@code crm-prototype/docs/api-docsV4.json}. Nom sans suffixe {@code Test} : hors de la suite CI.
 */
// Le application.properties de test remplace celui de main : on y remet le chemin springdoc de production.
@SpringBootTest(properties = {"springdoc.api-docs.enabled=true", "springdoc.api-docs.path=/api-docs"})
@AutoConfigureMockMvc
class ApiDocsSnapshot {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void ecrireLeDocumentOpenApi() throws Exception {
        String sortie = System.getenv("SNAPSHOT_API_SORTIE");
        assumeTrue(sortie != null && !sortie.isBlank(), "SNAPSHOT_API_SORTIE absent : rien à écrire");

        String json = mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(json).contains("\"openapi\"");
        Files.writeString(Path.of(sortie), json, StandardCharsets.UTF_8);
    }
}
