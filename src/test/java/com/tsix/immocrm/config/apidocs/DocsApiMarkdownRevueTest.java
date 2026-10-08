package com.tsix.immocrm.config.apidocs;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Revue étape 1 : règle 3 — une section « ### param » qui ne désigne pas un paramètre de
 * requête/chemin doit faire échouer le démarrage, pas seulement la génération de /api-docs.
 */
class DocsApiMarkdownRevueTest {

    @Tag(name = "Revue")
    @RestController
    static class RevueController {

        @PutMapping("/revue/{id}")
        public String remplacer(@PathVariable String id, @RequestBody String corps) {
            return id;
        }
    }

    @Test
    void echoueAuDemarrageSurParamQuiEstLeCorpsDeRequete() {
        // `corps` est le @RequestBody : il n'existe pas dans operation.parameters, donc
        // personnaliserOperation lèvera à l'appel de /api-docs (500). La règle 3 exige l'échec
        // au démarrage, c.-à-d. dans le constructeur.
        Map<String, String> fichiers = Map.of("apidocs/RevueController.md", """
                # RevueController

                ## remplacer

                ### param corps

                Texte.
                """);
        List<Class<?>> controleurs = List.of(RevueController.class);
        assertThatThrownBy(() -> new DocsApiMarkdown(fichiers, controleurs, noms -> Map.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("### param corps");
    }
}
