package com.tsix.immocrm.dto.prospect;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Première prospection créée avec le prospect ; exigée d'un compte agence, ignorée pour le siège.")
public record ProspectionALaCreationRequest(
        @Schema(description = "Tranche d'intérêt (CfgTranche) ; obligatoire, dans les projets de l'agence.")
        UUID trancheId,
        @Schema(description = "Source de contact (ComProspectionOrigin), facultative.")
        UUID origineId,
        @Schema(description = "Type de contact (ComActionMessageType), facultatif.")
        UUID typeContactId,
        @Schema(description = "Notes libres de la prospection, facultatives.")
        String notes
) {
}
