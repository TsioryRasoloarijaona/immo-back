package com.tsix.immocrm.dto.rapport;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "Section d'un rapport : un tableau et ses graphiques.")
public record SectionRapportDto(
        @Schema(description = "Identifiant stable de la section ([a-z0-9-]).", example = "stock") String id,
        @Schema(description = "Titre (aussi nom de feuille de l'export).", example = "Stock par tranche") String titre,
        @Schema(description = "Colonnes, dans l'ordre d'affichage ; propres à ce résultat.")
        List<ColonneRapportDto> colonnes,
        List<Map<String, Object>> lignes,
        @Schema(description = "Nom de la colonne de regroupement des lignes, null si aucun.", example = "trancheLibelle")
        String groupePar,
        Map<String, Object> totaux,
        Map<String, Map<String, Object>> sousTotaux,
        @Schema(description = "Graphiques déclarés sur les lignes de la section.") List<GraphiqueRapportDto> graphiques) {
}
