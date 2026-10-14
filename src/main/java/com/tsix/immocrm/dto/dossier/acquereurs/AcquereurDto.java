package com.tsix.immocrm.dto.dossier.acquereurs;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Acquéreur du dossier : le client principal (`ComFolder.CfgCustomerId`) ou un co-acquéreur (`ComFolderCustomer`).")
public record AcquereurDto(
        @Schema(description = "Ligne `ComFolderCustomer.PKey` ; null pour le client principal.")
        UUID id,
        @Schema(description = "Tiers (`CfgTier.PKey`).")
        UUID tiersId,
        String nom,
        @Schema(description = "Pièce d'identité du tiers (`CfgTier.IDNumber`).")
        String pieceIdentite,
        @Schema(description = "Répartition, pourcentage 0–100 (base : ratio 0–1).", example = "50")
        BigDecimal quotePart,
        boolean principal
) {
}
