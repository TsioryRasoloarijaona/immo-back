package com.tsix.immocrm.dto.dossier.liste;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Statut de dossier proposé au filtre de la liste (StrStatus des dossiers de vente).")
public record StatutListeDossierDto(
        @Schema(description = "Code à passer tel quel dans `?statut=` (StrStatus.Code).", example = "CONCRETISATION")
        String code,
        @Schema(description = "Libellé du statut (StrStatus.Description).", example = "CONCRETISATION")
        String libelle,
        @Schema(description = "Ordre d'affichage (StrStatus.Sorting).", example = "2")
        Integer ordre,
        @Schema(description = "Statut de sortie (annulation ou résiliation) : exclu de la liste par défaut.")
        boolean sortie) {
}
