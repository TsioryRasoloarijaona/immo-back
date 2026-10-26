package com.tsix.immocrm.dto.patrimoine.importation;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Lecture d'un classeur avant import : rien n'est écrit, le serveur ne garde aucun état.")
public record AnalyseImportDto(
        @Schema(description = "Noms des feuilles du classeur, dans l'ordre.") List<String> feuilles,
        @Schema(description = "Feuille lue (demandée, sinon la première).") String feuille,
        @Schema(description = "En-têtes non vides de la feuille, dans l'ordre ; leur rang est l'indexColonne du mapping.")
        List<String> entetes,
        @Schema(description = "Mapping proposé : en-tête = libellé cible après trim, sans casse.")
        MappingImportDto mappingPropose,
        @Schema(description = "Cinq premières lignes de données, une valeur texte par en-tête (\"\" = cellule vide).")
        List<List<String>> apercu,
        @Schema(description = "Colonnes ignorées ou refusées (« Active » d'une entité) : à lire avant d'importer.")
        List<String> avertissements
) {
}
