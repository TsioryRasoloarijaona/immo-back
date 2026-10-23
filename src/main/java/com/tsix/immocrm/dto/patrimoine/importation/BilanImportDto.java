package com.tsix.immocrm.dto.patrimoine.importation;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Bilan d'un import de classeur, simulé ou réel : tout ou rien, une ligne par ligne de données.")
public record BilanImportDto(
        @Schema(description = "Type importé (articles, sous-articles).", example = "articles") String type,
        @Schema(description = "Vrai si l'import était une simulation (« Vérifier les données »).") boolean simulation,
        @Schema(description = "Vrai seulement si tout a été écrit : aucune erreur et pas de simulation.") boolean ecrit,
        @Schema(description = "Nombre de lignes de données lues.") int lignesLues,
        @Schema(description = "Lignes en CREATION.") int creations,
        @Schema(description = "Lignes en MISE_A_JOUR.") int misesAJour,
        @Schema(description = "Lignes en INCHANGEE.") int inchangees,
        @Schema(description = "Lignes en ERREUR ou NON_VERIFIEE ; > 0 = rien n'est écrit.") int erreurs,
        @Schema(description = "Colonnes ignorées, TVA saisie en pourcentage… : n'empêchent pas l'import.")
        List<String> avertissements,
        @Schema(description = "Une entrée par ligne de données, dans l'ordre du fichier.") List<LigneBilanDto> lignes
) {
}
