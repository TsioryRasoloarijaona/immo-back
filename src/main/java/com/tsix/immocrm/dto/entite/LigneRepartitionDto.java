package com.tsix.immocrm.dto.entite;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Ligne de répartition des parts d'une entité (CfgCompanyShareholder). En écriture, code et libellé "
        + "sont ignorés.")
public record LigneRepartitionDto(
        @Schema(description = "Actionnaire (CfgShareholder). Obligatoire en écriture ; inconnu → 400.") UUID actionnaireId,
        @Schema(description = "Code de l'actionnaire (lecture seule).") String actionnaireCode,
        @Schema(description = "Libellé de l'actionnaire (lecture seule).") String actionnaireLibelle,
        @Schema(description = "Nombre de parts (actions), entier ≥ 0.") Integer nombreParts
) {
}
