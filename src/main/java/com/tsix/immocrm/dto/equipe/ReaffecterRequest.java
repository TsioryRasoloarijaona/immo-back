package com.tsix.immocrm.dto.equipe;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Réaffectation en lot de prospections ouvertes à un autre commercial de l'équipe.")
public record ReaffecterRequest(

        @Schema(description = "Prospections à réaffecter (au moins une).", requiredMode = Schema.RequiredMode.REQUIRED)
        List<UUID> prospectionIds,

        @Schema(description = "Employé (CfgTier) du nouveau commercial.", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID commercialId,

        @Schema(description = "Motif facultatif, ajouté à la ligne d'historique.", example = "Congé de Khadija")
        String motif
) {
}
