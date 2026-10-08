package com.tsix.immocrm.config.apidocs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Mécanisme des textes OpenAPI en Markdown (FEATURE-commentaires-et-docs-api.md, règle 5). */
class DocsApiMarkdownTest {

    @Tag(name = "Exemple")
    @RestController
    static class ExempleController {

        @GetMapping("/exemple/{id}")
        @Operation(summary = "Lit un exemple")
        @ApiResponse(responseCode = "200", description = "Trouvé.")
        @ApiResponse(responseCode = "404")
        public String lire(@PathVariable("id") String id, @RequestParam(name = "q", required = false) String filtre) {
            return id;
        }

        @GetMapping("/exemple")
        @Operation(summary = "Liste", description = "Déjà décrite dans l'annotation.")
        public String lister(@Parameter(description = "Déjà décrit.") @RequestParam String q) {
            return q;
        }
    }

    @Tag(name = "Autre", description = "Autre tag, décrit dans l'annotation.")
    @RestController
    static class AutreController {

        @GetMapping("/autre")
        public String lire() {
            return "";
        }
    }

    @Schema(description = "court")
    record DejaDecrit(@Schema(description = "court") String champ) {
    }

    record ExempleDto(String nom, @Schema(example = "1") int nombre) {
    }

    private static final String CONTROLEUR = """
            # ExempleController

            Texte du tag
            sur deux lignes.

            ## lire

            Description longue
            de l'opération.

            ### param id

            Identifiant.

            ### param q

            Filtre.

            ### reponse 404

            Introuvable.
            """;

    private static final String SCHEMA = """
            # ExempleDto

            Description du record.

            ## nom

            Description du champ.
            """;

    private static DocsApiMarkdown docs(Map<String, String> fichiers) {
        return new DocsApiMarkdown(fichiers, List.of(ExempleController.class, AutreController.class),
                noms -> Map.of("ExempleDto", List.of(ExempleDto.class), "DejaDecrit", List.of(DejaDecrit.class)));
    }

    private static DocsApiMarkdown docsExemple() {
        Map<String, String> fichiers = new LinkedHashMap<>();
        fichiers.put("apidocs/ExempleController.md", CONTROLEUR);
        fichiers.put("schemas/ExempleDto.md", SCHEMA);
        return docs(fichiers);
    }

    private static io.swagger.v3.oas.models.Operation operationLire() {
        io.swagger.v3.oas.models.Operation op = new io.swagger.v3.oas.models.Operation().summary("Lit un exemple");
        op.addParametersItem(new io.swagger.v3.oas.models.parameters.Parameter().name("id").in("path"));
        op.addParametersItem(new io.swagger.v3.oas.models.parameters.Parameter().name("q").in("query"));
        op.setResponses(new ApiResponses()
                .addApiResponse("200", new io.swagger.v3.oas.models.responses.ApiResponse().description("Trouvé."))
                .addApiResponse("404", new io.swagger.v3.oas.models.responses.ApiResponse().description("Not Found")));
        return op;
    }

    private static HandlerMethod handler(Object controleur, String methode) {
        for (var m : controleur.getClass().getMethods()) {
            if (m.getName().equals(methode)) return new HandlerMethod(controleur, m);
        }
        throw new IllegalArgumentException(methode);
    }

    @Test
    void injecteDescriptionsOperationParametreEtReponse() {
        io.swagger.v3.oas.models.Operation op = docsExemple()
                .personnaliserOperation(operationLire(), handler(new ExempleController(), "lire"));

        assertThat(op.getDescription()).isEqualTo("Description longue\nde l'opération.");
        assertThat(op.getSummary()).isEqualTo("Lit un exemple");
        assertThat(op.getParameters().get(0).getDescription()).isEqualTo("Identifiant.");
        assertThat(op.getParameters().get(1).getDescription()).isEqualTo("Filtre.");
        assertThat(op.getResponses().get("404").getDescription()).isEqualTo("Introuvable.");
        assertThat(op.getResponses().get("200").getDescription()).isEqualTo("Trouvé.");
    }

    @Test
    void injecteTagSchemaEtChamp() {
        DocsApiMarkdown docs = docsExemple();
        OpenAPI openApi = new OpenAPI().components(new Components()
                .addSchemas("ExempleDto", new ObjectSchema()
                        .addProperty("nom", new StringSchema())
                        .addProperty("nombre", new StringSchema())));
        // springdoc omet le @Tag sans description de la liste globale ; l'autre y est.
        openApi.addTagsItem(new io.swagger.v3.oas.models.tags.Tag().name("Autre")
                .description("Autre tag, décrit dans l'annotation."));
        docs.personnaliserOperation(operationLire(), handler(new ExempleController(), "lire"));
        docs.personnaliserOperation(new io.swagger.v3.oas.models.Operation(), handler(new AutreController(), "lire"));

        docs.personnaliserOpenApi(openApi);

        assertThat(openApi.getTags()).extracting(io.swagger.v3.oas.models.tags.Tag::getName)
                .containsExactlyInAnyOrder("Exemple", "Autre");
        assertThat(openApi.getTags()).filteredOn(t -> t.getName().equals("Exemple"))
                .singleElement().extracting(io.swagger.v3.oas.models.tags.Tag::getDescription)
                .isEqualTo("Texte du tag\nsur deux lignes.");
        var schema = openApi.getComponents().getSchemas().get("ExempleDto");
        assertThat(schema.getDescription()).isEqualTo("Description du record.");
        assertThat(((io.swagger.v3.oas.models.media.Schema<?>) schema.getProperties().get("nom")).getDescription())
                .isEqualTo("Description du champ.");
        assertThat(((io.swagger.v3.oas.models.media.Schema<?>) schema.getProperties().get("nombre")).getDescription())
                .isNull();
    }

    @Test
    void ordreDesTagsEstCeluiQuAuraitDonneLeTexteDansLAnnotation() {
        // Rejoue la construction de springdoc (HashSet reconstruit à chaque opération) avec la
        // description dans l'annotation, et compare à ce que produit l'injection.
        List<io.swagger.v3.oas.models.tags.Tag> attendu = new ArrayList<>();
        var exemple = new io.swagger.v3.oas.models.tags.Tag().name("Exemple").description("Texte du tag\nsur deux lignes.");
        var autre = new io.swagger.v3.oas.models.tags.Tag().name("Autre").description("Autre tag, décrit dans l'annotation.");
        for (var t : List.of(autre, exemple, autre)) {
            Set<io.swagger.v3.oas.models.tags.Tag> s = new HashSet<>();
            s.add(t);
            s.addAll(attendu);
            attendu = new ArrayList<>(s);
        }

        DocsApiMarkdown docs = docsExemple();
        OpenAPI openApi = new OpenAPI().components(new Components().addSchemas("ExempleDto",
                new ObjectSchema().addProperty("nom", new StringSchema())));
        openApi.addTagsItem(new io.swagger.v3.oas.models.tags.Tag().name("Autre")
                .description("Autre tag, décrit dans l'annotation."));
        docs.personnaliserOperation(new io.swagger.v3.oas.models.Operation(), handler(new AutreController(), "lire"));
        docs.personnaliserOperation(operationLire(), handler(new ExempleController(), "lire"));
        docs.personnaliserOperation(new io.swagger.v3.oas.models.Operation(), handler(new AutreController(), "lire"));
        docs.personnaliserOpenApi(openApi);

        assertThat(openApi.getTags()).containsExactlyElementsOf(attendu);
    }

    @Test
    void echoueSurMethodeInconnue() {
        assertThatThrownBy(() -> docs(Map.of("apidocs/ExempleController.md", """
                # ExempleController

                ## supprimer

                Texte.
                """)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("apidocs/ExempleController.md, section « ## supprimer »")
                .hasMessageContaining("aucune méthode de handler");
    }

    @Test
    void echoueSurParametreOuReponseInconnus() {
        assertThatThrownBy(() -> docs(Map.of("apidocs/ExempleController.md", """
                # ExempleController

                ## lire

                ### param filtre

                Texte.

                ### reponse 409

                Texte.
                """)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("section « ## lire / ### param filtre » : aucun paramètre « filtre »")
                .hasMessageContaining("section « ## lire / ### reponse 409 » : aucune @ApiResponse « 409 »");
    }

    @RestController
    static class ParametresController {

        @GetMapping("/parametres/{id}")
        public String lire(@PathVariable String id, String q, org.springframework.data.domain.Pageable pageable,
                           jakarta.servlet.http.HttpServletRequest requete, java.security.Principal principal,
                           @org.springframework.web.bind.annotation.RequestBody ExempleDto corps,
                           @Parameter(hidden = true) @RequestParam String cache) {
            return id;
        }
    }

    record AvecChampsOmis(String publie,
                          @com.fasterxml.jackson.annotation.JsonIgnore String ignore,
                          @Schema(hidden = true) String cache,
                          @com.fasterxml.jackson.annotation.JsonProperty("renomme") String nomJava) {
        static final String CONSTANTE = "x";
    }

    @Test
    void neConnaitQueLesParametresPubliesParSpringdoc() {
        DocsApiMarkdown docs = new DocsApiMarkdown(Map.of("apidocs/ParametresController.md", """
                # ParametresController

                ## lire

                ### param id

                Texte.

                ### param q

                Texte.

                ### param pageable

                Texte.
                """), List.of(ParametresController.class), noms -> Map.of());
        assertThat(docs).isNotNull();

        for (String nom : List.of("corps", "requete", "principal", "cache", "page")) {
            assertThatThrownBy(() -> new DocsApiMarkdown(Map.of("apidocs/ParametresController.md",
                    "# ParametresController\n\n## lire\n\n### param " + nom + "\n\nTexte.\n"),
                    List.of(ParametresController.class), noms -> Map.of()))
                    .as(nom)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("### param " + nom + " » : aucun paramètre");
        }
    }

    @Test
    void neConnaitQueLesChampsPubliesParOpenApi() {
        var resolveur = (java.util.function.Function<Set<String>, Map<String, List<Class<?>>>>)
                noms -> Map.of("AvecChampsOmis", List.of(AvecChampsOmis.class));
        assertThat(new DocsApiMarkdown(Map.of("schemas/AvecChampsOmis.md",
                "# AvecChampsOmis\n\n## publie\n\nTexte.\n\n## renomme\n\nTexte.\n"), List.of(), resolveur)).isNotNull();
        for (String champ : List.of("ignore", "cache", "nomJava", "CONSTANTE")) {
            assertThatThrownBy(() -> new DocsApiMarkdown(Map.of("schemas/AvecChampsOmis.md",
                    "# AvecChampsOmis\n\n## " + champ + "\n\nTexte.\n"), List.of(), resolveur))
                    .as(champ)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("section « ## " + champ + " » : aucun champ publié");
        }
    }

    @Test
    void echoueSurControleurRecordOuChampInconnus() {
        Map<String, String> fichiers = new LinkedHashMap<>();
        fichiers.put("apidocs/InconnuController.md", "# InconnuController\n\nTexte.\n");
        fichiers.put("schemas/InconnuDto.md", "# InconnuDto\n\nTexte.\n");
        fichiers.put("schemas/ExempleDto.md", "# ExempleDto\n\n## prenom\n\nTexte.\n");
        assertThatThrownBy(() -> docs(fichiers))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("apidocs/InconnuController.md, section « # InconnuController » : aucun contrôleur REST")
                .hasMessageContaining("schemas/InconnuDto.md, section « # InconnuDto » : aucune classe")
                .hasMessageContaining("schemas/ExempleDto.md, section « ## prenom » : aucun champ publié « prenom »");
    }

    @Test
    void echoueSurDoubleSource() {
        Map<String, String> fichiers = new LinkedHashMap<>();
        fichiers.put("apidocs/ExempleController.md", """
                # ExempleController

                ## lister

                Encore une description.

                ### param q

                Encore un texte.

                ## lire

                ### reponse 200

                Encore un texte.
                """);
        fichiers.put("apidocs/AutreController.md", "# AutreController\n\nEncore un tag.\n");
        fichiers.put("schemas/DejaDecrit.md", "# DejaDecrit\n\nEncore.\n\n## champ\n\nEncore.\n");
        assertThatThrownBy(() -> docs(fichiers))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("section « ## lister » : description déjà présente dans @Operation")
                .hasMessageContaining("section « ## lister / ### param q » : description déjà présente dans @Parameter")
                .hasMessageContaining("section « ## lire / ### reponse 200 » : description déjà présente dans @ApiResponse")
                .hasMessageContaining("apidocs/AutreController.md, section « # AutreController » : description déjà présente dans @Tag")
                .hasMessageContaining("schemas/DejaDecrit.md, section « # DejaDecrit » : description déjà présente dans @Schema")
                .hasMessageContaining("schemas/DejaDecrit.md, section « ## champ » : description déjà présente dans @Schema");
    }

    @Test
    void echoueSurMauvaisDomaineOuTitre() {
        Map<String, String> fichiers = new LinkedHashMap<>();
        fichiers.put("auth/ExempleController.md", "# ExempleController\n\nTexte.\n");
        fichiers.put("apidocs/AutreController.md", "# ExempleController\n\nTexte.\n");
        assertThatThrownBy(() -> docs(fichiers))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("le fichier devrait être sous api-docs/apidocs/")
                .hasMessageContaining("apidocs/AutreController.md : le titre « # ExempleController » doit être le nom du fichier");
    }

    @Test
    void lesFichiersDuDepotSontValides() throws Exception {
        // Le pilote et les fichiers à venir : leur lecture ne doit produire aucune erreur de format.
        Map<String, String> fichiers = DocsApiMarkdownConfig.lireFichiers();
        assertThat(fichiers).as("fichiers api-docs/**.md trouvés sur le classpath")
                .containsKey("auth/UtilisateurAdminController.md");
        for (var e : fichiers.entrySet()) {
            assertThat(FichierDocMarkdown.lire(e.getKey(), e.getValue()).erreurs).as(e.getKey()).isEmpty();
        }
    }
}
