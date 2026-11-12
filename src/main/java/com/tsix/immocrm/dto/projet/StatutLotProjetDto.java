package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Statut de lot (StrStatus d'un genre de lot PrjMarket*) pour le filtre de la liste des lots.")
public record StatutLotProjetDto(
        @Schema(description = "StrStatus.PKey.") UUID id,
        @Schema(description = "Code du statut.", example = "ADJUDICATION") String code,
        @Schema(description = "Libellé du statut.") String description,
        @Schema(description = "Ordre dans le workflow (Sorting), null si absent.") Integer ordre,
        @Schema(description = "Catégorie du statut (StrStatusType.Code), null si absente.", example = "Nouveau") String typeCode,
        @Schema(description = "Genre de lot (StrEntity.Code).", example = "PrjMarket") String entiteCode,
        @Schema(description = "Nom de couleur .NET (ColorName) tel quel, null si absent.", example = "LimeGreen") String couleur,
        @Schema(description = "Genre de lot, comme LotListItemDto.genre ; null si genre inconnu.", example = "ENTREPRENEURS")
        String genre
) {
}
