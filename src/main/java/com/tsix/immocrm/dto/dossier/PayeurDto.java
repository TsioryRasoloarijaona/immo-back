package com.tsix.immocrm.dto.dossier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Payeur possible d'un règlement : acquéreur principal ou co-acquéreur du dossier.")
public record PayeurDto(

        @Schema(description = "`CfgTier.PKey` — à envoyer tel quel comme `payeurId` à "
                + "`POST /api/dossiers/{id}/echeancier/{echeanceId}/reglements`.")
        UUID id,

        @Schema(description = "Nom d'affichage du tiers.", example = "Fouad EL IHFADI")
        String nom,

        @Schema(allowableValues = {"ACQUEREUR_PRINCIPAL", "CO_ACQUEREUR"})
        String role,

        @Schema(example = "CIN: E400556")
        String pieceIdentite,

        BigDecimal quotePart
) {
}
