package com.tsix.immocrm.dto.prospect;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Une tranche à laquelle le prospect s'est intéressé — voir
 * {@code instructions/FEATURE-audit-tranches-prospect.md} §2.2.
 */
@Schema(description = "Tranche à laquelle le prospect s'est intéressé, via l'une de ses "
        + "prospections — en tranche principale ou en intérêt secondaire.")
public record TrancheInteretDto(

        @Schema(description = "Identifiant de la tranche (`CfgTranche.PKey`), exploitable "
                + "directement par `GET /api/tranches/{id}`.")
        UUID id,

        @Schema(description = "Code de la tranche.", example = "BELAIR")
        String code,

        @Schema(description = "Libellé de la tranche.", example = "AYAD RESIDENCE BEL AIR")
        String description
) {
}
