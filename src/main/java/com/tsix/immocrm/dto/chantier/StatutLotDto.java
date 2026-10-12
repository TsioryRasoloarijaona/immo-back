package com.tsix.immocrm.dto.chantier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Statut de lot (StrStatus d'une entité « PrjMarket* ») : une étiquette, jamais une règle métier.")
public record StatutLotDto(
        @Schema(description = "Identifiant du statut (PKey).") UUID id,
        @Schema(description = "Code du statut.", example = "ADJUDICATION") String code,
        @Schema(description = "Libellé du statut.") String description,
        @Schema(description = "Ordre chronologique dans le flux du lot (StrStatus.Sorting).", example = "40")
        Integer ordre,
        @Schema(description = "Type du statut (StrStatusType.Code : Nouveau, Intermédiaire, Terminé…) ; null si absent.")
        String typeCode,
        @Schema(description = "Entité de lot porteuse (StrEntity.Code).", example = "PrjMarket") String entiteCode) {
}
