package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Statut de lot (StrStatus d'un genre de lot PrjMarket*), en lecture seule.")
public record StatutLotDto(
        @Schema(description = "StrStatus.PKey.") UUID id,
        @Schema(description = "Code du statut.", example = "ADJUDICATION") String code,
        @Schema(description = "Libellé du statut.") String description,
        @Schema(description = "Ordre dans le workflow (Sorting), null si absent.") Integer ordre,
        @Schema(description = "Nom de couleur .NET (ColorName) tel quel, null si absent.", example = "LimeGreen") String couleur,
        @Schema(description = "Genre de lot (StrEntity.PKey du groupe).") UUID groupeId,
        @Schema(description = "Code du genre de lot.", example = "PrjMarket") String groupeCode,
        @Schema(description = "Libellé du genre de lot (StrEntity.Description).", example = "Lot Entrepreneurs") String genre,
        @Schema(description = "Catégorie du statut (StrStatusType.Description), null si absente.", example = "Nouveau")
        String categorie
) {
}
