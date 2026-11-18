package com.tsix.immocrm.dto.tresorerie;

import com.tsix.immocrm.dto.tresorerie.TotauxTresorerieDto.TotauxRecettesDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "Liste Recettes : colonnes de la grille OMRAN et lignes indexées par la clé de colonne.")
public record RecettesDto(
        @Schema(description = "Colonnes de la vue, dans l'ordre d'affichage.") List<ColonneRecetteDto> colonnes,
        @Schema(description = "Lignes de la page : `id` (PKey) plus une entrée par clé de colonne ; [] si rien.")
        List<Map<String, Object>> lignes,
        @Schema(description = "Nombre de lignes de toute la liste filtrée.") long total,
        TotauxRecettesDto totaux) {

    @Schema(description = "Une colonne de la grille Recettes.")
    public record ColonneRecetteDto(
            @Schema(description = "Clé du champ dans chaque ligne.", example = "montantAPayer") String cle,
            @Schema(description = "En-tête en français.", example = "Net à payer") String libelle,
            @Schema(description = "Type d'affichage.", example = "MONTANT",
                    allowableValues = {"TEXTE", "NOMBRE", "MONTANT", "DATE", "BOOLEEN"}) String type,
            @Schema(description = "false : colonne masquée par défaut dans OMRAN.") boolean visible) {
    }
}
