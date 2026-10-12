package com.tsix.immocrm.dto.chantier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Bordereau définitif d'un lot ; lignes vides = lot au montant forfaitaire.")
public record BordereauDto(
        @Schema(description = "Identifiant du lot.") UUID lotId,
        @Schema(description = "Lignes, par ordre de ligne.") List<LigneBordereauDto> lignes,
        @Schema(description = "Totaux des lignes.") TotauxBordereauDto totaux) {
}
